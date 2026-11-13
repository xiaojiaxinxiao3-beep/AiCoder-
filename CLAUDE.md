# AiCoder client — 賽博龐克 GUI 主題交接（給 Claude Code）

## 目標
- 專案：**AiCoder client**，Meteor Client addon，**Minecraft 26.3**
- 這次只做 **GUI 主題**：照 `design/cyber.html` 做出賽博龐克風格。**不做任何模組功能。**
- 這是一個**獨立的主題**：直接取代 Meteor 預設的主題，不需要做樣式切換。

## 設計參考
- `design/cyber.html`：定稿設計稿，用瀏覽器開啟會有動畫
- `design/cyber-gui.gif`、`design/cyber-gui-still.png`：預覽圖
- 設計稿裡的模組名稱與設定只是示意，實際內容以 Meteor 的資料為準

## 配色（只有洋紅與青，**不要用黃色**）
| 用途 | 值 |
|---|---|
| 青（主色） | `#00f0ff` |
| 洋紅（強調色） | `#ff2a6d` |
| 紫 | `#b026ff`（只用在背景光點） |
| 白 | `#eaf6ff` |
| 次要文字 | `#6f8aa6` |
| 分隔線 | `rgba(0,229,255,.22)` |

- 洋紅用在：選中的分類、選中的模組、勾選框內部、視窗外框、分頁底線、REC 標記、標題副字
- 青用在：開關、分段標題、數值、四角定位框

## 造型與版面
- **視窗**：840×480，**切角**（左上、右下各斜切 16px），其餘兩角是直角
  - 玻璃底色：160° 漸層 `rgba(28,6,40,.50)` → `rgba(6,8,26,.58)`
  - 背景模糊：使用 Meteor 的 Blur 模組
  - 外框：1px 洋紅 `rgba(255,42,109,.45)`
  - 頂邊左段、右下角各有一段較亮的發光線
- **四角定位框**：視窗外 6px 處，青色 2px 的 L 形框（右上、左下兩角），另外在兩個切角外側各有一條短斜線
- **頂部分頁列**（Meteor 的 Modules / Config / GUI / HUD…）
  - 平行四邊形半透明底，頂部 1px 青線
  - 文字大寫、字距 2.2
  - 選中的分頁：白字，底下 2px 洋紅底線
- **標題列**（高 46）
  - 依序為：`AICODER`（粗體、字距 6、白色加青色光暈）、`// CLIENT`（洋紅、等寬字）、`サイバー・システム`（洋紅）、切角小標籤 `SYS v0.3 · MC 26.3`
  - 右側：洋紅 `● REC`、`LINK`、5 格訊號、`STABLE`
  - 標題列下方：1px 青線，加每 12px 一格的刻度
- **三欄**：分類 170｜模組 280｜設定欄占剩餘寬度；底列高 34
- **分類項目**：高 38，右上切角 10
  - 前面加編號 `01`～`06`
  - 選中時：左側 3px 洋紅亮條，底色為洋紅漸層
- **模組列**：高 44，右下切角 8
  - 名稱下方一行小字編號（例如 `MOD-0x2F`）
  - 選中的模組：左側 3px 洋紅亮條；底線 2px，洋紅與青交替**流動**
- **開關＝六角形**（22×24）
  - 關閉：空心，邊框 `rgba(120,170,210,.45)`
  - 開啟：青色徑向漸層填滿，中心有白色小六角，外發光會呼吸
- **設定欄**
  - 標題：模組名稱（大寫、粗體），後接 `// SETTINGS`（青）；下一行是說明文字
  - 分段標題：`GENERAL ──── [–]`，文字與線為青色
  - 數值列：標籤（寬 112）、切角數值框（54×20，等寬字）、**分段滑桿**、重設鈕（20×20，↻）
    - 分段滑桿：22 格，格距 2，高 9；已填的格子是青→洋紅漸層，**最後一格為白色，帶洋紅光暈**
  - 下拉選單：切角框，寬度上限 150，▾ 為青色
  - 勾選框：14×14；勾選時內部填洋紅方塊並發光
- **底列**
  - 左側：洋紅與深紫相間的**斜紋警示條**（60×10）
  - 接著：`[Y] GUI`、`60 FPS`、`0 ms`、`MEM 41%`（數值為青色）
  - 右側：**示波線兩條**（青與洋紅）

## 動態（一個循環約 4 秒）
**原則**：慢而且不規則，**不要轉圈，不要橫掃整個視窗的掃描線**。

1. **標題故障閃爍**
   - 每個循環在約 12%、47%、81% 三個時間點，各閃約 0.2 秒
   - 標題分成洋紅與青兩層：左右錯位幾 px，各自只顯示一段水平條帶，整體亮度跟著閃
   - 其餘時間完全正常
2. **資料雨**
   - 片假名混十六進位字元，直排往下流
   - 青色與洋紅色混合，透明度約 0.55，上下兩端淡出，速度不一
   - 畫在視窗背後，隔著玻璃若隱若現
3. **背景光點**：青、洋紅、紫三色，緩慢漂移並明暗變化
4. **視窗內點陣**：洋紅細點，透過一個會緩慢游移的圓形光暈區塊才看得到
5. **選中模組的底線**：洋紅與青交替，往右流動
6. **警示條**：斜紋緩慢平移
7. **呼吸效果**：四角定位框、六角開關的光暈
8. **示波線**：用多個正弦波疊加做出不規則波動

**實作提示**：所有不規則運動都用「多個正弦波疊加」，而且頻率取整數倍，這樣循環接得起來。設計稿裡的 `flow()` 函式可以直接照抄。

## Meteor 實作要點（已從 Meteor 原始碼查證）
- **主題繼承 `MeteorGuiTheme`**，不要直接繼承 `GuiTheme`
  - 原因：`MeteorWidget.theme()` 會強制轉型成 `MeteorGuiTheme`，繼承它才能沿用 Meteor 內建的 W* 元件
- **註冊方式**：在 addon 的 `onInitialize()` 呼叫 `GuiThemes.add(new AiCoderGuiTheme())`
  - 主題名稱寫死為 `"Meteor"`，同名主題會**取代**原本的主題（log 會出現一筆錯誤，屬正常現象）
  - 因此**同一時間只能裝一個這類主題 addon**
- **顏色**：顏色都是 public 的 `Setting<SettingColor>` 欄位，例如 `accentColor`、`checkboxColor`、`backgroundColor`
  - 在建構子裡用 `.set(...)` 設成上方配色，使用者之後也能在遊戲內自己調整
- **需要覆寫的工廠方法**
  - `checkbox()`：勾選框
  - `window()`：視窗外觀
  - `module()`：模組列與六角開關
  - 其他元件：依設計稿需要覆寫，否則沿用 `WMeteor*`
- `GuiRenderer` 只有四邊形（可做漸層）、`triangle()`、文字、貼圖，**沒有圓形和圓角**，所以：
  - 切角、六角形、斜紋警示條：用三角形拼
  - 分段滑桿、刻度、點陣：用小四邊形
  - 示波線：用細四邊形或三角形連成折線
  - 發光：疊幾層半透明、逐漸放大的形狀來近似
- **片假名需要支援 CJK 的字型**：
  - 在 Meteor 的字型設定裡選一個 CJK 字型（例如 Noto Sans CJK）
  - 如果沒有，資料雨就改成只用十六進位字元，日文裝飾字可以省略
- 動畫用系統時間計算相位，`phase = (時間毫秒 % 4000) / 4000`

## 建置設定
- 版本（取自 Meteor 原始碼的 `gradle/libs.versions.toml`）：
  - minecraft `26.3`
  - fabric-loader `0.19.5`
  - fabric-api `0.162.0+26.3`
  - loom `1.18-SNAPSHOT`
- Meteor 的 maven：
  - `https://maven.meteordev.org/releases`
  - `https://maven.meteordev.org/snapshots`
- 照 Meteor 官方 addon template 建立專案，mappings 與依賴寫法以 template 為準

## 工作流程
1. 實作
2. `gradle build`，進遊戲開啟 GUI
3. 截圖，和 `design/cyber-gui-still.png` 對照
4. 回到第 1 步調整，直到一致

## 實作現況（程式碼導覽）
- 進入點：`aicoder.client.AiCoderAddon`，在 `onInitialize()` 註冊 `AiCoderGuiTheme`
- `gui/Cyber.java`：配色、動畫時鐘（`phase()`、`flow()`）、繪圖基本元件（切角、六角形、發光、視窗外框）、任意字級文字
- `gui/Backdrop.java`：全螢幕背景（壓暗、光點、資料雨）
- `gui/screens/CyberModulesScreen.java`：Modules 分頁的三欄視窗（分類｜模組｜設定）
- `gui/widgets/`：覆寫的元件（分頁列、視窗、模組列、勾選框、滑桿、下拉、按鈕、輸入框、分段標題、提示框）
- `mixin/GuiRendererAccessor.java`：取得 `GuiRenderer` 的 graphics，用來畫 Meteor 沒有提供的字級

### 單位
- 設計稿 1px = `AiCoderGuiTheme.unit()` = `scale(1.5)` 個實際像素（Meteor GUI 縮放 1 時，視窗為 1260×720）
- 遊戲視窗放不下時，Modules 視窗會自動等比縮小（`WFrame.layout()`）

### 實作時查到、和上面原始說明不同的地方
- **片假名**：Meteor 的自訂字型只打包拉丁、希臘、西里爾字元，換成 CJK 字型也畫不出片假名。
  只有關掉 Config 的「Custom Font」（改用 Minecraft 內建字型）才會出現片假名；開著時資料雨只用十六進位，標題列的日文省略。
- **繪製順序**：同一個 scissor 裡，Meteor 會先畫所有色塊、再畫貼圖、最後畫文字。需要「先畫完這層再畫下一層」時，用 `scissorStart/scissorEnd` 包起來強制送出。
- **內建字型的文字永遠在最上層**（由遊戲自己的 GUI pass 畫），所以該模式下資料雨會避開視窗範圍。
- **背景由誰畫**：`theme.backdrop(renderer)` 每個畫面只畫一次，由最先被繪製的分頁列／視窗／Modules 外框觸發。
  （分頁列在 Modules 畫面是第一個元件，在其他分頁卻是最後一個，不能只靠它。）
- **既有的主題存檔**：原版 Meteor 主題存下的顏色不會被載入（`fromTag` 檢查 `aicoder` 標記），避免蓋掉配色。

### 操作
- 點模組列＝選取並在右欄顯示設定；點六角形或對模組列按右鍵＝開關模組
- `Ctrl+F`：在模組欄上方開啟搜尋

### 建置
- 需要 JDK 25；`./gradlew build`，產物在 `build/libs/`

## 第二個 addon：AiCoder 地球科學（`earth-science/`）
- 獨立的 Gradle 專案（自己的 `settings.gradle.kts` 與 wrapper），套件 `aicoder.earth`，mod id `aicoder-earth`，產出 `AiCoder地球科學.jar`
- 十個主題共用同一套版面與元件，差異全部放在 `gui/Style.java`（配色、圓角／切角、開關圖示、滑桿種類、背景、底列文字）
  - 新增或調整主題：改 `Style.java`；開關圖示在 `gui/Toggles.java`，背景在 `gui/Backdrops.java`
- 十個主題都是 `EarthGuiTheme` 的實例。`MeteorGuiTheme` 的名稱寫死為 `"Meteor"`，所以用 mixin（`GuiThemeAccessor`，`@Mutable`）改名後各自註冊，不會取代 Meteor 原本的主題
- 切換：`EarthModulesScreen.switchTheme()` 呼叫 `GuiThemes.select(name)` 後重開分頁；切換鈕的位置固定，不能隨主題改變
- 圓角、圓形、橢圓都在 `gui/Fx.java` 用三角形拼；外框要畫成「內外兩圈之間的帶狀」，用線段接會在半透明時出現亮點
- 建置時語系要是 UTF-8，否則寫不出中文檔名

## 第三個 addon：AiCoder 液態玻璃（`liquid-glass/`）
- 獨立的 Gradle 專案，套件 `aicoder.glass`，mod id `aicoder-glass`，產出 `AiCoder液態玻璃.jar`；由 `earth-science/` 複製後改寫，`Fx.java` 多了四角漸層與漸層外框
- 單一主題 `AiCoder Glass`（`GlassGuiTheme`）。`gui/Style.java` 不是固定配色，而是每個畫面由設定（兩個顏色、是否上色、不透明度）重新計算的調色盤
- 玻璃的層次都在 `gui/Chrome.java`：`panel()`（視窗）、`plate()`（分類／模組列）、`field()`（輸入框、按鈕）、`droplet()`（圓形開關）
- 背景模糊不是自己畫的：`GlassGuiTheme.ensureBlur()` 在開啟 Modules 畫面時打開 Meteor 的 Blur 模組（可用 Auto Blur 關掉）

## 第四個 addon：AiCoder 多欄賽博龐克（`columns/`）
- 獨立的 Gradle 專案，套件 `aicoder.columns`，mod id `aicoder-columns`，產出 `AiCoder多欄賽博龐克.jar`；由根目錄的賽博龐克專案複製後改寫。交接文件與設計稿在 `columns/CLAUDE.md`、`columns/design/`
- 主題名稱仍是 `"Meteor"`（取代原主題），所以和根目錄的賽博龐克 addon 不能同時安裝
- `gui/screens/ColumnsModulesScreen.java` 是 Meteor `ModulesScreen` 的改寫版：欄位從 logo 下方排起、欄距 12、最小欄寬 162、加上編號；放不下的視窗排到最短那一欄下面
- `gui/widgets/WColumnWindow.java`：欄位視窗。標題由自己畫，Meteor 加的置中標題 label 被隱藏（不能不呼叫 `super.init()`，基底類別的 `render` 會用到它建立的三角形）
- `gui/widgets/WColumnModule.java`：模組列與停留時的說明卡（用 `absolutePost` 畫在最上層）
- 音效：`gui/Sounds.java` 播放 `assets/aicoder-columns/sounds.json` 定義的 `toggle_on`／`toggle_off`。兩個 ogg 是程式合成的，不是取樣
- AICODER 分類在 `AiCoderAddon.onRegisterCategories()` 註冊，沒有模組時仍會顯示該欄

## 第五個 addon：AiCoder 液態玻璃直立式（`glass-columns/`）
- 獨立的 Gradle 專案，套件 `aicoder.glasscolumns`，mod id `aicoder-glass-columns`，產出 `AiCoder液態玻璃直立式.jar`
- 外觀來自 `liquid-glass/`（`Fx`、`Chrome`、`Style`、設定），版面與互動來自 `columns/`（`GlassColumnsScreen` 是 `ColumnsModulesScreen` 的改寫，模組列左鍵開關、右鍵設定、停留說明卡）
- 主題名稱 `AiCoder Glass Columns`；mixin accessor 的方法前綴是 `glasscol$`，和 `liquid-glass/` 的 `glass$` 不同，兩個 addon 才能同時安裝
- 音效：`assets/aicoder-glass-columns/sounds.json` 的 `drop_on`／`drop_off`（程式合成的水滴聲）與 `hover`（滑鼠移入模組列時的輕敲聲；`Sounds.hover()` 依模組固定的 seed 調音高，並限制最短間隔 45ms）
- 分類欄用較小的圓角（14）且不畫流動光斑；其他視窗維持完整的玻璃效果（`WGlassWindow.column`）

## 第六個 addon：AiCoder 鞘翅巡航（`elytra-cruise/`）
- 獨立的 Gradle 專案，套件 `aicoder.cruise`，mod id `aicoder-cruise`，產出 `AiCoder鞘翅巡航.jar`。**不是主題**：沒有 mixin，只註冊一個模組 `ElytraCruise`（`Categories.Movement`，不另外註冊分類）
- 全部邏輯在 `ElytraCruise.java`，是每 tick（`TickEvent.Pre`）跑一次的狀態機：`WALK_TO_SKY`（選用）→ `TAKEOFF` → `CLIMB` → `CRUISE`
  - `TAKEOFF`：跳起來，**開始下落後**才送 `START_FALL_FLYING`（同一 tick 送伺服器有時不認）
  - `CLIMB`：pitch 固定 `climb-pitch`，速度低於 `min-climb-speed` 且過了冷卻才補一發煙火
  - `CRUISE`：Meteor `ElytraFly` Pitch40 的數值（俯衝 37.72°、拉到 -54.77°、5.45°／tick 拉起、0.90°／tick 壓回）。和原版唯一的差別：超過上界時用 5.45°／tick 壓回，否則會衝過上界約 60～80 格
- 高度範圍是兩個絕對 Y 座標的設定 `min-height`／`max-height`（預設 320／360）。玩家要的是「整段都在某個高度以上」，所以 `min-height` 是硬下限：
  - 俯衝在 `min-height + 5`（`PULL_UP_LEAD`）就開始拉起，因為開始拉之後還會再沉約 3 格。實測 320 → 最低 321.1，400 → 最低 401.3
  - `max-height` 只是「開始壓機頭」與煙火爬升的目標，拉起的慣性仍會衝過去（320～360 實測到 387，400～480 到 493）
  - `top()` 會把上界當成至少 `min-height + 40`
- 尋路只用 Meteor 的 `PathManagers`／`IPathManager`，不直接參考 Baritone 的類別；沒裝時是 `NopPathManager`
  - 要用 `moveTo(pos, true)`：`moveTo(pos)` 走到目標**旁邊**就算到達，可能還在屋簷下
  - Baritone 計算路徑時 `isPathing()` 是 false，所以開始後有 60 tick 的寬限才判定為放棄
  - 「頭頂是否空曠」用 `MOTION_BLOCKING` heightmap 判斷，不用 `canSeeSky()`（玻璃會被當成看得到天空）
- 聊天訊息與設定說明用英文：Meteor 的自訂字型畫不出中文
- 沒有 1.21.11 版：`mc-1.21.11/port.py` 只涵蓋 GUI 相關的命名，玩家／物品／封包的 Yarn 命名要另外補
- 測試方式（這個環境沒有顯示卡，世界裡很慢）：起一個離線的原版伺服器（`~/.gradle/caches/fabric-loom/26.3/minecraft-server.jar`，`online-mode=false`、flat 世界，啟動後在主控台下 `whitelist off`），
  客戶端用 `./gradlew runClient --args="--quickPlayMultiplayer localhost:25565"` 直接連進去，不用操作選單；
  給裝備、傳送、放方塊都從伺服器主控台下指令，用 `data get entity @a[limit=1] Pos` 和 `clear @a firework_rocket 0` 讀座標與煙火數，比截圖準。
  只有 `.toggle elytra-cruise`、`.settings elytra-cruise <設定> <值>` 需要用 `xdotool` 打進聊天欄
  - 測 Baritone：把 `meteordevelopment:baritone:26.3-SNAPSHOT` 的 jar 放進 `elytra-cruise/run/mods/`（`run/` 不進版控）

## 壁紙（`liquid-glass/` 與 `glass-columns/` 共同的功能）
- `gui/Wallpaper.java`（兩個專案各一份，只有 namespace 不同）。圖片**只畫在面板裡**，不是全螢幕背景：`frame()` 每個畫面算一次「鋪滿螢幕」的排版，`renderIn()` 由 `Chrome.panel()` 呼叫，用自己的 `Renderer2D` 畫該面板範圍的那一塊（UV 依螢幕座標），圓角用水平細條拼
- `renderIn()` 用自己的 scissor 立刻送出，所以面板之後畫的染色、反光、亮邊都疊在圖片上面
- 內建壁紙放在 `assets/<namespace>/wallpapers/`，並登記在 `Wallpaper.BUNDLED`；玩家自己的圖放 `meteor-client/aicoder-wallpapers/`
- 新增內建壁紙：把圖放進兩個專案的 `wallpapers/`，各在 `BUNDLED` 加一行

## Minecraft 1.21.11 版（`mc-1.21.11/`）
- 五個 addon 的 1.21.11 版本，各自是獨立的 Gradle 專案，產出 `<名稱>-1.21.11.jar`
- **原始碼是產生出來的**：`python3 mc-1.21.11/port.py` 會把 26.3 版的原始碼複製過來並改寫命名（Mojang → Yarn）與 API 差異。要改東西就改 26.3 版再重跑，不要手改 `mc-1.21.11/*/src/`
- 兩版的主要差異都列在 `port.py` 裡：`DrawContext`、`Click`／`KeyInput`、GLFW 常數與游標、`NbtCompound`、`net.minecraft.util.Pair`、`PositionedSoundInstance.ui`、`TextureFormat`；1.21.11 的 `TextRenderer.begin()` 不需要 graphics，所以沒有 `GuiRendererAccessor` mixin；沒有 `modulesHelpText()`
- 建置：Loom 用 `net.fabricmc.fabric-loom-remap` 外掛（Gradle 以 JDK 25 執行、編譯目標 Java 21），最終檔名設在 `remapJar` 而不是 `jar`
- 這個環境可以用 OpenGL 跑 1.21.11（26.3 只能走軟體 Vulkan）
