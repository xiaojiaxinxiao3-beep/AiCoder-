# AiCoder Assist 交接（給接手的模型）

這份文件分兩部分：
- **第一部分（現況交接）**：這個客戶端現在是什麼狀態、怎麼從零把環境弄起來、哪些驗證過、哪些沒有、使用者在意什麼。接手先看這部分。
- **第二部分**：把 Meteor addon 的模組轉成注入式模組的流程（較早寫的，內容仍然正確）。

更細的實作筆記在 repo 根目錄的 `CLAUDE.md`（「模組 addon：戰鬥巨集」與「注入式客戶端」兩節）和 `inject/README.md`。

---

# 第一部分：現況交接

## 1. 這是什麼
AiCoder Assist 是使用者自己的 Minecraft Java 版客戶端，**不靠 Fabric mod、不靠 Meteor**：一個 jar 用 Java Attach API 注入到已經在跑的遊戲裡。

- 支援：Minecraft **1.21.11** 與 **1.21.4**，原版或用 Fabric Loader 啟動的都可以（同一個 jar 自己判斷）
- 成品：`inject/work/AiCoderAssist.jar`／`.exe`、`inject/work-1.21.4/AiCoderAssist-1.21.4.jar`／`.exe`（`work*/` 不進 repo）
- 內容：15 個模組（1.21.4 是 13 個，沒有長矛的兩個）、選單（兩種版面、極光與液態玻璃兩種外觀、背景圖）、六個小遊戲、按鍵綁定、可自訂選單鍵、選用的授權檢查
- 另有基岩版（`bedrock/`，C++，只送按鍵、不讀遊戲），和這份文件無關

## 2. 從零開始（新的環境要先做這些）
`inject/work*/` 不在 repo 裡，新環境是空的。先把遊戲檔和工具抓回來：

```
cd inject
python3 tools/fetch.py 1.21.11 work            # 遊戲 jar、Mojang 對照表、函式庫、資源
python3 tools/fetch.py 1.21.4  work-1.21.4
python3 tools/fetch_remapper.py work           # tiny-remapper（寫出 work/remapper-cp.txt）
python3 tools/fetch_fabric.py 1.21.11 work     # 要 Fabric 支援才需要
python3 tools/fetch_fabric.py 1.21.4  work-1.21.4
./build.sh                                     # → work/AiCoderAssist.jar、.exe
V=1.21.4 ./build.sh                            # → work-1.21.4/AiCoderAssist-1.21.4.jar、.exe
```

- 需要：JDK（`JAVA_HOME`，預設找 `/opt/jdk/jdk-25.0.5+8`，編譯目標是 Java 21）、Python 3、mingw（只有 `.exe` 需要）、測試要 Xvfb／xdotool／ImageMagick
- **這套「從零抓檔」的流程沒有在乾淨的機器上從頭跑完過**：`fetch.py 1.21.11` 與 `fetch_remapper.py` 各自驗證過會產生需要的檔案，但原本的環境是用 Loom 快取裡的遊戲 jar 建的。第一次跑如果卡住，先看 `build.sh` 開頭的註解
- 模組原始碼**不在 `inject/`**：`inject/src/aicoder/assist/modules/` 是建置時由 `tools/port_modules.py` 從 `combat-macros/src/main/java/aicoder/combat/` 轉過來的。**改模組一律改 `combat-macros/`**，然後重跑 `./build.sh`。新增模組要改三處：`combat-macros` 的 `CombatAddon`、`inject/src/aicoder/assist/Client.java`、`inject/tools/port_modules.py` 的 `FILES`

## 3. 測試
```
./test.sh --quickPlaySingleplayer "New World"                    # 原版 1.21.11
V=1.21.4 ./test.sh --quickPlaySingleplayer "New World"
FABRIC=1 ./test.sh --quickPlaySingleplayer "New World"           # Fabric
```
它在 `Xvfb :77` 上啟動遊戲再注入。操作用 xdotool；`site/test-tools/x.sh` 有三個輔助函式（`click x y`、`snap 名稱`、`chat "指令"`，用之前把裡面的 `$SP` 設成你的暫存資料夾）。

- 模組的設定與綁定鍵可以事先寫進 `work*/game/aicoder-assist.properties`（例如 `wind-jump.bind=71`），注入版沒有聊天指令
- 讀結果：`grep -a "CHAT" work*/game.log`
- 不要用 `| tail` 接 `test.sh`；殺遊戲用 `pkill -f "client.main.[M]ain"`、`pkill -f "knot.[K]notClient"`
- **世界檔 `New World` 不在 repo 裡**，新環境要自己先建一個（第一次不帶 `--quickPlaySingleplayer` 啟動，在遊戲裡建超平坦、創造、允許指令的世界，取名 New World）

### 踩過的測試陷阱（每一個都曾讓我回報錯的結果）
1. **舊橋**：測搭橋要換一條沒用過的路線，並先用 `/execute if block … air` 確認是空的。`/fill … air` 對沒載入的區塊會失敗。在舊橋上「搭橋」怎樣都不會掉
2. **緩降效果**：測試角色身上如果有 `slow_falling`，伺服器的掉落距離永遠是 0，重錘沒有落下加成。先 `/effect clear`
3. **判定方式**：「世界上沒有 TNT 礦車」不等於「炸了」，也可能根本沒放出去。要同時確認物品離開了快捷列
4. **殘留方塊**：上一輪沒炸掉的鐵軌會讓下一輪選到錯的方塊。每輪清場
5. **模組開著時打指令**：會吃到按鍵或被模組打斷。打指令前先關模組
6. **看錯截圖**：曾把使用者截圖上的 20 看成 90。數字不確定就問

## 4. 各功能的驗證狀態
「實測」都是在 Linux 的單人世界。**沒有任何一項在 Windows 上或多人伺服器上驗證過**，使用者實際是在 Windows 上用。

| 功能 | 狀態 |
|---|---|
| 注入、選單、兩種外觀、背景圖、小遊戲 | 1.21.11 與 1.21.4、原版與 Fabric 都實測過 |
| Anchor／Crystal Macro、連點、Crosshair Drift、Wind Pearl、Mace Swap、Spear Lunge、Trigger Bot | 1.21.11 實測過；1.21.4 大多只確認編譯 |
| Wind Jump（預設先丟後跳，`throw-timing` -1） | 1.21.11 實測：平均 11 格 |
| Mace Dive（含 `spear-mode`） | 1.21.11 實測（高度 20、攻擊距離 1.99、鑽石長矛對鐵巨人） |
| Spear Smash | 1.21.11 實測；穿鞘翅那條路徑只確認編譯 |
| Offhand Totem | 1.21.11、1.21.4、Fabric 1.21.11 實測。使用者回報過 Windows 上「滑鼠不會動」，之後加了直接呼叫 `MouseHandler.onMove`，**使用者還沒回報修好了沒** |
| Telly Bridge（Telly 與 Diagonal 兩種模式） | 1.21.11 實測；Diagonal 在 1.21.4 只確認編譯 |
| Cart Bomb（`draw` 預設 4） | 1.21.11 實測 8/8，1.21.4 測過兩輪 |
| 自訂選單鍵 | 1.21.11 實測 |
| 授權（HWID 綁定） | 用本機測試伺服器，1.21.11／1.21.4／Fabric 都實測；Windows 的硬體指紋與 `start-license-server.bat` 沒有實機驗證 |

## 5. 進行中與還沒做的事
- **使用者說過會傳影片**，要把 Telly 搭橋改得像真人那樣「半空中放一坨方塊」（他嫌現在太平整）。影片還沒來。做的時候是照影片改成固定的搭法；如果目的是加隨機抖動讓反作弊認不出來，那個不做
- **授權**：預設關閉（建置時沒給 `LICENSE_URL`／`LICENSE_PUBKEY` 就沒有授權檢查，下載頁上的是沒有的）。已經給過使用者一包本機測試用的東西（伺服器 jar、啟動腳本、5 組序號、連 127.0.0.1 的授權版客戶端）。使用者還沒有正式的授權伺服器
- **下載頁沒有更新到最後幾次改動以外的東西**：見第 7 節

## 6. 使用者的習慣與底線
- 用**繁體中文**回覆。使用者的訊息很短、常有錯字（「長毛」是長矛、「慧星」是 Meteor、「鞘次」是鞘翅），不確定就問，但問題要具體
- 使用者會在工作中途插話改需求，這很正常。每一步盡量短，做完先回報
- 分支 `claude/new-session-bpyhbc`，直接 commit 並 push 到這個分支。**沒被要求不要開 PR**
- 回報時要分清楚：進遊戲測過的、只確認編譯的、完全沒測的
- **原則：只做原版按鍵會做的事**。點擊照抄 `Minecraft.startUseItem()`／`startAttack()` 的呼叫順序（`combat-macros` 的 `Clicks.java`），切物品只改選取的欄位，多按一下用 `KeyMapping.click(按鍵)`，視角是真的轉
- **明確不做的事**（使用者要求過其中幾項，都拒絕了）：
  - 躲避偵測：隱藏類別或行程、偽裝檔名、清痕跡、繞過啟動器或反作弊、為了過防毒而加殼、給轉頭加假的顫抖或失誤
  - 用 ProGuard 混淆（類別名稱維持 `aicoder.assist.*`）
  - 「下載的只是載入器，模組本體授權後才從伺服器抓下來執行」
  - 網易版基岩版（要繞過它的保護）、讀基岩版的記憶體
  - 小遊戲不要做成既有遊戲的名字或樣子

## 7. 下載頁
使用者是從一個 claude.ai Artifact 頁面下載成品的：<https://claude.ai/artifact/2gE73NCveR1ri1pgXGqaZF>（只有使用者本人能開）。

- 產生方式：`python3 site/site.py <資料夾>`，資料夾裡要有 `template.html`、各張 `.jpg`、`zorynth.zip`，以及放成品的子資料夾 `pack/`、`pack121/`（Meteor 的 jar）、`assist/`、`assist-noexe/`、`assist4/`、`assist4-noexe/`、`bedrock/`。輸出 `aicoder-downloads.html`，再用 Artifact 工具以上面的網址發布（頁面上限 16MB，目前約 15.5MB，`capabilities` 的 `downloads` 會沿用）
- `site/` 裡放了 `site.py`、`template.html`、圖片，以及各下載包附的 `README.txt`（`site/readme/`）。**成品 jar／exe 不在 repo**，要重新建置後放進對應的子資料夾
- 更新客戶端時要一起改 `site/readme/assist*-README.txt`（英文，使用者下載包裡的說明）
- 如果你的環境沒有 Artifact 工具，就把 jar 直接傳給使用者

## 8. 檔案在哪
```
combat-macros/src/main/java/aicoder/combat/   ★ 模組的原始碼（Meteor 版，也是注入版的來源）
inject/
  build.sh  test.sh  README.md  HANDOFF.md
  tools/      fetch.py fetch_fabric.py fetch_remapper.py（抓檔）、port_modules.py port_version.py（轉原始碼）、
              gen_names.py fabric_names.py names.py tiny.py（名稱對照）、LicenseServer.java（測試用授權伺服器）
  launcher/   launcher.c（exe）、AiCoderAssist.bat、start-license-server.bat
  res/        字型、內建背景圖
  src/aicoder/assist/
    Agent.java Injector.java Client.java Hooks.java Names.java
    api/      Meteor 的替身 API
    gfx/      繪圖（Backend 是唯一和版本有關的）
    aurora/   選單、兩種外觀、小遊戲
    license/  HwidProvider、License
    modules/  ★ 產生出來的，不要手改
  src-1.21.4/ 1.21.4 專用的檔案（目前只有 gfx/Backend.java）
site/         下載頁的產生器、圖片、下載包裡的 README、測試輔助腳本
```

---

# 第二部分：把 Meteor addon 改成「不靠 Meteor 的注入式模組」

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
