# 交接：把 Meteor addon 改成「不靠 Meteor 的注入式模組」

這份文件給接手的 Claude Code 分頁看。目標是：拿一個寫給 Meteor Client 的模組（addon），讓它能注入到**執行中的原版或 Fabric 版 Minecraft**，不需要 Fabric mod、不需要 Meteor。

這個 repo 的 `inject/` 已經做完一次（AiCoder Assist，支援 1.21.11 與 1.21.4）。要改新的 mod，最省事的做法是**沿用 `inject/` 的骨架，只換模組**。下面先講流程，再講為什麼這樣做、踩過哪些坑。

---

## 一、整體概念（先看這段）

Meteor addon 依賴三樣東西，注入版要各自找替代品：

| Meteor addon 靠的 | 注入版的替代 |
|---|---|
| Fabric 把 mod 載入遊戲 | Java Attach API：對執行中的遊戲 `loadAgent(jar)`，跑 `agentmain` |
| Mixin 改遊戲的方法 | ASM 重新轉換（retransform）：在目標方法**開頭插入一行靜態呼叫** |
| Meteor 的 API（Module、Setting、事件、GUI、繪圖、字型） | 自己寫一套同名同形狀的小 API（`aicoder.assist.api`、`gfx`） |
| Yarn／intermediary 名稱（Loom 幫忙轉） | 對著 Mojang 名稱編譯，建置時用 tiny-remapper 整包轉成遊戲實際的名稱 |

關鍵決策：**模組原始碼只維護一份（在 Meteor addon 那邊）**，注入版的模組是用腳本從那份轉出來的。所以流程的核心是「寫一個長得像 Meteor 的替身 API」加「一個文字轉換腳本」。

---

## 二、流程（照順序做）

### 1. 準備遊戲檔與對照表
```
python3 inject/tools/fetch.py <版本> inject/work-<版本>         # client jar、Mojang 對照表 client.txt、函式庫、資源
python3 inject/tools/fetch_fabric.py <版本> inject/work-<版本>   # Fabric Loader 與 intermediary 對照表（要支援 Fabric 才需要）
```
- 都是從 Mojang／Fabric 官方伺服器下載。`work*/` 不進 repo。
- 1.21.11 是例外：它的檔案在 `inject/work/`，client jar 用的是 Loom 快取裡的那份。

### 2. 把模組轉過來：`inject/tools/port_modules.py`
它做的事（都是文字替換）：
- 套件名 `aicoder.combat` → `aicoder.assist.modules`
- 刪掉所有 `import meteordevelopment.*`，換成 `aicoder.assist.api.*`
- `AiCoderTab.X` → `Category.X`；`mc.gui.screen()` → `mc.screen`
- `((KeyMappingAccessor) mc.options.keyX).meteor$getKey()` → `Input.keyOf(mc.options.keyX)`
- 好友名單相關的設定與判斷整段刪掉（注入版沒有好友名單）
- 26.3 與目標版本之間的 API 差異（例如揮手動畫參數）

**新 mod 要做的**：把 `SRC`、`FILES` 改成新模組的路徑與類別清單，再依編譯錯誤補替換規則。

### 3. 補齊替身 API：`inject/src/aicoder/assist/api/`
模組用到 Meteor 的什麼，這裡就要有同名的東西。目前有：
- `Module`（`category`、`name`、`settings`、`toggle()`、`isActive()`、`onActivate/onDeactivate`、`info/warning/error`、`bind`、`holdBind`）
- `Setting` 與 `BoolSetting`、`IntSetting`、`DoubleSetting`、`EnumSetting`（各有和 Meteor 一樣寫法的 `Builder`），`SettingGroup`、`Settings`
- `Category`、`EventHandler`、`EventPriority`
- `Events`：`TickEvent.Pre`、`DoAttackEvent`、`InteractBlockEvent`、`InteractItemEvent`、`EntityAddedEvent`、`Render3DEvent`
- `Input`（`isKeyPressed`、`isButtonPressed`、`isPressed`、`keyOf`）、`PlayerUtils`

**新模組用到別的 Meteor 類別時**：先看能不能在這裡加一個同名的最小實作；不行再在 `port_modules.py` 加替換。新的**事件**一定要有對應的掛鉤（下一步）。

### 4. 掛鉤：`Agent.java`、`Hooks.java`、`tools/gen_names.py`
- `Agent` 的 `patches` 清單：每一筆是「哪個類別的哪個方法 → 呼叫 `Hooks` 的哪個靜態方法、傳哪些參數」。參數寫成 `"A2"`（slot 2 的物件）、`"I3"`（slot 3 的 int）。slot 0 是 `this`，`long`／`double` 佔兩格。
- `Hooks` 的方法把參數轉成事件物件，`Client.post(事件)` 發給模組。**不能丟例外**（它跑在遊戲自己的方法裡）。
- `gen_names.py` 的 `TARGETS`：用 Mojang 名稱寫出要掛的方法與參數型別，建置時查出混淆名稱寫進 `names.properties`。

目前六個掛鉤：`Minecraft.tick`、`Minecraft.startAttack`、`GameRenderer.render`、`MultiPlayerGameMode.useItemOn`、`MultiPlayerGameMode.useItem`、`ClientLevel.addEntity`、`KeyboardHandler.keyPress`。

**新增掛鉤的步驟**：`TARGETS` 加一筆 → `Agent.patches` 加一筆 → `Hooks` 加方法 → `Events` 加事件類別。

### 5. 註冊模組：`Client.java`
`start()` 裡 `add(new XxxModule())`。`add()` 會用反射找出標了 `@EventHandler` 的方法並記下來。

### 6. 建置：`inject/build.sh`
```
./build.sh                # 1.21.11 → work/AiCoderAssist.jar、.exe
V=1.21.4 ./build.sh       # 其他版本 → work-1.21.4/AiCoderAssist-1.21.4.jar、.exe
```
它依序做：
1. `tools/tiny.py`：`client.txt` → `mojang.tiny`（official → named）
2. tiny-remapper 把遊戲 jar 轉成 Mojang 名稱（`minecraft-named.jar`），給編譯用
3. 把 ASM 搬到自己的套件（`aicoder/assist/asm`），避免和遊戲裡的 ASM 衝突
4. `port_modules.py` 轉模組；非 1.21.11 再跑 `port_version.py`
5. `javac --release 21` 對著 `minecraft-named.jar` 編譯
6. tiny-remapper 把我們的類別從 named 轉回 official（遊戲實際的混淆名稱）
7. `gen_names.py` 寫 `names.properties`
8. 有 `intermediary.tiny` 時：`fabric_names.py` 產生 Fabric 用的對照與名稱表，再轉一份 `fabric-payload.jar` 包進去
9. 打成一個 jar，manifest 同時有 `Main-Class`（注入器）與 `Agent-Class`（被注入的部分）
10. 用 mingw 編 `launcher.c`，後面接上 jar 變成 `.exe`

### 7. 測試：`inject/test.sh`
```
./test.sh --quickPlaySingleplayer "New World"                       # 原版 1.21.11
V=1.21.4 ./test.sh --quickPlaySingleplayer "New World"              # 原版 1.21.4
FABRIC=1 V=1.21.4 ./test.sh --quickPlaySingleplayer "New World"     # Fabric 1.21.4
```
- 它在 `Xvfb :77` 上用官方 jar 啟動遊戲、等載入、再注入。
- 操作用 xdotool（輔助函式在 scratchpad 的 `tools/x.sh`：`click`、`snap`、`chat`）。
- 模組設定與按鍵綁定可以**事先寫進** `work*/game/aicoder-assist.properties`（例如 `wind-jump.bind=71`），因為注入版沒有聊天指令。
- 讀結果：`grep -a "CHAT" work*/game.log`。
- **不要**用 `| tail` 接 `test.sh` 的輸出，背景的遊戲會讓管線等不到結束。
- 要殺遊戲用 `pkill -f "client.main.[M]ain"`、`pkill -f "knot.[K]notClient"`（方括號是避免殺到自己）。

---

## 三、限制與原則（不要違反）

- **掛鉤只能是「在方法開頭插入一行靜態呼叫」**。執行中的 JVM 不允許加欄位或方法；在 `return` 前插入曾讓 `Gui.render` 驗證失敗。所以**不能取消**遊戲的動作（Packet Limiter 因此沒有注入版）。
- **只做原版按鍵會做的事**：點擊照抄 `Minecraft.startUseItem()`／`startAttack()` 的呼叫順序（見 `combat-macros` 的 `Clicks.java`），切物品只改選取的欄位，要「多按一下」用 `KeyMapping.click(按鍵)`。
- **不做躲避偵測**：不隱藏類別、不偽裝檔名或行程、不清痕跡、不繞過啟動器或反作弊、不為了過防毒而加殼。類別名稱就是 `aicoder.assist.*`，Java 的 dynamic agent 警告保留。使用者要求這類功能時要拒絕並說明原因。
- 使用者用繁體中文，回覆也用繁體中文。分支是 `claude/new-session-bpyhbc`，沒被要求不要開 PR。

---

## 四、踩過的坑（按「會卡多久」排序）

### Fabric
- Fabric 下遊戲類別是 **intermediary 名稱**（`class_310`、`method_1574`），而且由 **Knot class loader** 載入，系統 class loader 上的 agent 看不到它們。
- 做法：建置時把整份客戶端另外轉一份成 intermediary 名稱（`fabric-payload.jar`），`Agent.joinFabric()` 把它寫到暫存檔，用 `FabricLauncherBase.getLauncher().addToClassPath(path)` 交給 Knot，並改用 `names-intermediary.properties`。
- 合併對照表時，**intermediary 沒列的成員要略過**，不能填 official 名稱，否則 tiny-remapper 報 `Unfixable conflicts`（intermediary 只在方法第一次宣告的類別列出它，覆寫的那層沒有）。
- 判斷是不是 Fabric：`net.minecraft.client.main.Main` 的 class loader 不是系統 loader。
- 沒有 payload 的舊版在 Fabric 下會「顯示成功、其實什麼都沒掛上」（找不到任何同名類別，patched 0 of 0）。
- 只測過**沒有裝任何模組**的 Fabric Loader 0.20.1。

### 注入器
- `VirtualMachine.list()` 在遊戲用 `-XX:-UsePerfData` 啟動時是空的（很多第三方啟動器會加）。所以 `.bat`／`.exe` 是把所有 java 行程的 PID 傳進來，逐一 attach 後看 `sun.java.command` 有沒有 `minecraft`。
- 用**遊戲自己的 `java.exe`** 來跑注入器（官方 runtime 含 `jdk.attach` 與 `java.instrument`）。
- **同一次遊戲只能試一個版本的包**：試過別的版本（就算失敗），那個 jar 已經掛在 classpath 上，之後正確的包會解析到舊類別。要重開遊戲。
- jar 的路徑含中文等非 ASCII 字元時，Windows 上遊戲收到的路徑會壞掉，所以注入器會先複製到純 ASCII 的暫存位置。
- 失敗原因由 agent 寫進系統屬性 `aicoder.assist.error`，注入器讀出來顯示，並存成 jar 旁邊的 `AiCoderAssist-log.txt`。使用者通常只會說「打不開」，要靠這個檔案。
- 進到世界後聊天欄會印一次 `[AiCoder Assist] loaded`，用來分辨「沒注入」和「選單開不了」。
- `.exe` 沒有簽章、做的事就是注入，防毒很可能會擋。提供 `jar + .bat` 的版本，不要幫忙規避。

### 版本差異（1.21.11 ↔ 1.21.4）
- `src/` 是 1.21.11 的寫法。`tools/port_version.py` 複製到 `work-<版本>/src`、蓋上 `src-<版本>/` 的檔案、再做文字替換。**每一條替換都必須命中**，沒命中就讓建置失敗。
- 版本相關的繪圖只放在 `gfx/Backend.java`：
  - 1.21.11：把四邊形收成 `Mesh`（實作 `GuiElementRenderState`），交給 `GuiGraphics` 裡 private 的 `guiRenderState`（反射）
  - 1.21.4：`GuiGraphics.drawSpecial` 直接寫進 `RenderType.gui()`／`guiTextured(id)`，貼圖要先向 `TextureManager` 註冊
- 其他差異：螢幕的按鍵與滑鼠事件參數（事件物件 ↔ 純數字）、`getInventory().setSelectedSlot()` ↔ `.selected`、`getWindow().handle()` ↔ `.getWindow()`、幾個搬過家的套件、1.21.4 要自己呼叫 `renderBlurredBackground()`、鍵盤掛鉤的參數不同、1.21.4 沒有長矛。
- 新增版本：`fetch.py` 抓檔 → 在 `port_version.py` 加一段規則 → 視需要加 `src-<版本>/…/Backend.java` → `gen_names.py` 加該版本的差異 → 編譯、照錯誤補規則。

### 介面
- 選單是自己的 `Screen` 子類別（`aurora/AuroraScreen.java`，手寫）。`Fx`、`Chrome`、`Aurora`、`Style` 是從 `aurora-glass/` 轉過來的（`port_modules.py` 的 `port_theme()`）。
- 字型是用 STB truetype 自己烘的，數值照 Meteor 的 `CustomTextRenderer`，這樣版面不用改。
- 設定存在遊戲資料夾的 `aicoder-assist.properties`（純文字）。

---

## 五、檔案地圖

```
inject/
  build.sh  test.sh  README.md  HANDOFF.md（這份）
  tools/
    fetch.py            下載某版本的遊戲檔
    fetch_fabric.py     下載 Fabric Loader 與 intermediary
    names.py tiny.py    讀 Mojang 對照表、轉成 tiny
    gen_names.py        掛鉤目標的混淆名稱 → names.properties
    fabric_names.py     Fabric 用的對照與名稱表
    port_modules.py     Meteor 模組與主題 → 注入版原始碼
    port_version.py     1.21.11 的原始碼 → 其他版本
  launcher/             launcher.c（exe）、AiCoderAssist.bat
  res/                  字型、內建背景圖
  src/aicoder/assist/
    Agent.java          agentmain：版本檢查、Fabric 處理、ASM 掛鉤
    Injector.java       找遊戲行程、loadAgent、顯示原因
    Client.java         模組清單、事件派送、按鍵綁定、存檔
    Hooks.java          被掛鉤呼叫的靜態方法
    Names.java          名稱表
    api/                Meteor 的替身 API
    gfx/                繪圖（Backend 是唯一和版本有關的）
    aurora/             選單、兩種外觀、小遊戲
    modules/            ★ 產生出來的，不要手改
  src-1.21.4/           1.21.4 專用的檔案（目前只有 gfx/Backend.java）
```

產生出來、不要手改的：`src/aicoder/assist/modules/*`、`aurora/{Fx,Chrome,Aurora,Style}.java`、`work*/` 底下所有東西。

---

## 六、給新 mod 的最短路徑

1. 在 Meteor addon 那邊把模組寫好、測好（Meteor 有現成的開發環境，改起來最快）。
2. `port_modules.py`：改 `SRC`、`FILES`。
3. `./build.sh`，照編譯錯誤補：替身 API 缺的類別或方法、`port_modules.py` 的替換、需要的新掛鉤。
4. `Client.start()` 註冊模組。
5. `./test.sh` 進遊戲實測；再跑 `FABRIC=1`；需要別的版本再跑 `V=<版本>`。
6. 回報時分清楚：哪些進遊戲測過、哪些只確認編譯成功、哪些完全沒測（Windows、裝了其他模組的 Fabric）。

環境備註：JDK 在 `/opt/jdk/jdk-25.0.5+8`；mingw、Xvfb、xdotool、ImageMagick 都有；建置前 `unset JAVA_TOOL_OPTIONS`（`build.sh` 已經做了）。專案的整體說明在 repo 根目錄的 `CLAUDE.md`。
