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

## 第六、第七個 addon：極光玻璃（`aurora-glass/`、`aurora-columns/`）
- 套件 `aicoder.aurora`／`aicoder.auroracolumns`，mod id `aicoder-aurora`／`aicoder-aurora-columns`，產出 `AiCoder極光玻璃.jar`／`AiCoder極光玻璃直立式.jar`；分別由 `liquid-glass/`、`glass-columns/` 複製後改寫。交接文件與設計稿在 `aurora-glass/CLAUDE.md`、`aurora-glass/design/`
- **兩個專案共用的檔案只在 `aurora-glass/` 改**：`Style`、`Fx`、`Aurora`、`Chrome` 與 `widgets/` 裡除了 `WAuroraModule` 以外的元件。`aurora-columns/` 的那幾份是用 sed 換套件名（以及 mixin 前綴 `aurora$` → `auroracol$`）複製過去的，改完要同步
- 設計稿的 1px = `theme.unit()` = `scale(1)`。主題覆寫了 `scale()`，乘上 `fit`（遊戲視窗相對 1920×1080 的比例，0.6～1），所以 Meteor 內建元件和文字也會一起縮小
- `gui/Aurora.java`：夜空是「位置 → 顏色」的函式（三條光帶用 screen 混色、暗角），整個螢幕用 40 欄的格子取樣畫出來；玻璃面板用同一個函式（光帶加寬、提高飽和度）填色，看起來像模糊了後面的極光。星點不畫在面板底下（用上一個畫面的面板位置判斷）
- `Fx.boxPaint()`／`Fx.outlinePaint()`：每個頂點的顏色由 `Fx.Paint` 決定的圓角方塊與外框。面板邊框的漸層（白 → 綠 → 紫 → 白）每 12 秒轉一圈，就是改 `Chrome.aimRim()` 的方向
- `Fx.animate` 為 false 時 `Fx.phase()` 固定回傳 0、`Fx.step()` 直接跳到終點：Animations 設定就是切這個
- 分頁列在單視窗版置中、在直立式版靠右（`AuroraGuiTheme.topBarRight()`，由 `WAuroraTopBar.calculateWidgetPositions()` 改 x）
- 沒做到的：開啟 GUI 時「96% → 100% 放大」的動畫（只有 Meteor 本身的淡入）；等寬字（Meteor 的字型沒有等寬字，編號與數值用同一種字）；沒有用 Shoreline 實測過

## 初版 UI 的直立式版本（`first-columns/`）
- 使用者最早那版 UI（青綠毛玻璃，形狀全用著色器 `Sdf` 畫，原始碼是使用者另外給的 zip，不在這個 repo 裡）改成每個分類一欄的版本。套件 `aicoder.firstcolumns`，mod id `aicoder-first-columns`，產出 `AiCoder初版直立式.jar`，只有 26.3
- 細節在 `first-columns/CLAUDE.md` 開頭：`ColumnsScreen`（分排排版）、模組列沒有圓形開關（開啟時整列亮起）

## 模組 addon：戰鬥巨集（`combat-macros/`）
- 套件 `aicoder.combat`，mod id `aicoder-combat`，產出 `AiCoder戰鬥巨集.jar`。1.21.11 版一樣由 `port.py` 產生：玩家／物品／實體的命名對照在 `GAMEPLAY`、`GAMEPLAY_WORDS`，只套用在 `GAMEPLAY_PROJECTS` 列出的專案（有些規則很短，例如 `.is(` → `.isOf(`，套到 GUI 專案會誤傷）；1.21.11 的 `swingHand()` 不帶動畫參數，由 `port_gameplay()` 改寫
- **自己的分頁**（`AiCoderTab`，做法和 Shoreline 一樣）：`Tabs.add()` 加一個分頁，`createScreen()` 直接回傳 `theme.modulesScreen()`，所以任何主題都能用。兩個 mixin（都是 `remap = false`，目標是 Meteor 的類別）：`ModulesMixin` 攔 `Modules.loopCategories()`，目前畫面是 AiCoder 分頁時只回傳自己的三個分類、是 Modules 分頁時回傳其他分類、其餘情況回傳全部；`TabScreenAccessor` 把畫面的 `tab` 欄位（final）改成自己的分頁，分頁列才會標對。判斷「目前是哪個分頁」用 `mc.gui.screen()` 的 `tab`，建構期間另外用 `creating` 旗標
- 十二個模組分在 `AiCoderTab.EXPLOSIVES`／`MELEE`／`UTILITY` 三個分類：`AnchorMacro`、`CrystalMacro`、`CrosshairDrift`、`WindPearl`、`RightClicker`、`LeftClicker`、`MaceSwap`、`SpearLunge`、`MaceDive`、`WindJump`、`TriggerBot`、`PacketLimiter`
- `SpearLunge` 是「按住才動作」：模組開著只是待命，用 `Input.isKeyPressed()` 看 `hold-key` 設定的按鍵（列舉，鍵碼取自 `InputConstants.KEY_*`，這樣 Meteor 版和注入版可以共用，不需要按鍵綁定設定）。一輪是：`keyJump.setDown(true)` → 選長矛欄位並 `KeyMapping.click(攻擊鍵)` → 下一個 tick 選回原欄位。新增模組時三處都要加：`CombatAddon`、`inject/src/aicoder/assist/Client.java`、`inject/tools/port_modules.py` 的 `FILES`
- `WindJump`（第十個模組，在 UTILITY）：開啟一次跳一次，落地後自己關閉。視角轉到正下方、選風彈並按使用鍵、隔一個 tick 按跳（`throw-timing` -1；0 是同一個 tick，正數是先跳後丟）、切回並轉回視角。`auto-tune` 開著時每次換一個時機（-3～4）並量實際高度，八種都試過後把最好的寫進 `throw-timing`
- `TriggerBot`（第十一個模組，在 MELEE）：不轉視角、不選目標，只看 `mc.hitResult` 是不是 `EntityHitResult`、打到的是不是玩家（`mobs` 開著時也含生物）、眼睛到命中點的距離是否 ≤ `range`，條件成立且冷卻到了就 `KeyMapping.click(攻擊鍵)`。冷卻 `Auto` 是等 `getAttackStrengthScale(0.5f)` 到 `charge`%，`Custom` 是固定 tick 數。實測（1.21.11、鑽石劍對鐵巨人）：Auto 每一下都是滿蓄力的 7 點；Custom 40 tick 約 6 秒打 3 下；超出距離或沒對準時不出手。基岩版做不到（讀不到準心下的東西）
- `TellyBridge`（第十三個模組，在 UTILITY，Meteor 版與注入版都有）：自動 Telly 疊橋。開啟時把方向取成最接近的東西南北，橋的高度與中線（`floorY`、`lineX`／`lineZ`）固定不再更新。地面：按住前進＋疾跑，前方 `edge-distance` 處沒有地板就按跳。空中：視角轉 180°、按住後退，每個 tick 往回找最後一個實心方塊，眼睛超過它朝前那一面的平面後就對準那一面按使用鍵（`KeyMapping.click`，原版在同一個 tick 稍後自己做 pick 與右鍵）；下降且離橋面不到 `turn-back-height` 時轉回前方。瞄準點的橫向座標取玩家自己的（夾在方塊寬度內），偏航才會保持正 180°，否則按後退會把人往側邊推（第一版因此跑 40 格後掉下去）。實測：原版 1.21.11 三十秒約 170 格、Fabric 1.21.4 二十秒約 137 格
  - `bridge-mode` = `Diagonal`（使用者要的「45 度斜著跳疊」，而且**不要模組轉頭**）：全程倒著走。使用者要的是「45 度由模組轉，但不要 180 度的大轉身」：使用者背對要搭的方向站好，開啟時由視角反推行進方向（`heading = yaw − 180 ∓ 22.5` 再取最近的東西南北；偏半個 45° 是為了讓「正背對」和「已經偏好 45°」兩種站法都落在同一個方向），模組把左右視角轉到 行進方向 + 180° ∓ 45° 並鎖住，只有上下會跟著要點的那一面動。按住後退加一個橫移鍵（`side`：Left 是 −45° 配左鍵，Right 是 +45° 配右鍵），兩個輸入合起來剛好沿橋直走；`jump` 開著時一直按住跳。每個 tick 找最後一個實心方塊，眼睛一超過它朝前那一面的平面（`beyond > 0.06`）就點「45° 視線和那個面的交點」；交點落在面外時才稍微偏轉。停止鍵是前進或潛行。實測（原版 1.21.11，每次都換沒用過的路線）：正背對開啟，左右兩邊各 15 秒 58 格以上，事後檢查方塊都在
  - `bridge-mode` = `Speed`（Speed Telly，使用者給了兩支教學影片，按鍵順序照影片）：四個階段 `phase`（0 朝前、1 轉身 2 tick、2 回頭、3 轉回前方 2 tick）。地上：前進＋疾跑，每次落地都按跳。一離地、而且預測的落地點沒有方塊就轉身（經過 forwardYaw+90）：放開前進按後退，第二個 tick 起加按左。右鍵從轉身第一個 tick 到轉回前方最後一個 tick 都照 `cps`（預設 25）用 `KeyMapping.click` 連點，不看準心在哪。回頭時視角「瞄一次、停一個 tick」（`hold`），快落地而腳下還沒有方塊時才每個 tick 都瞄。落地前 2 tick 而且落地點有方塊時轉回前方（經過 forwardYaw+270，那個 tick 按右，剛好是往前推），最後按前進＋疾跑。**每跳往上一格**（`level = floorY + 1`）：這一層還沒有方塊時先點橋尾那格的頂面放台階（腳高過它或人已經離開那一格才放得下，大約起跳第 3 tick），之後點這一層最後一格的側面往前接；第 9 tick 落在高一層，不用等第 12 tick 落回原高度。台階沒放成而且已經掉到那一層以下時，退回原高度接。左右位置用按鍵修正：回頭時按左是往行進方向的右邊推，偏右超過 0.16 格改按右。落地預測（`under()`）要求落地點前後 0.15 格內有方塊。實測（原版 1.21.11，三條新路線）：12 秒 71 格、24 秒 152 格、24 秒 144 格，沒有掉落；每階 3～4 格，用掉的方塊約是前進格數的 1.35 倍
  - 後來照使用者的要求改的（都實測過，原版 1.21.11）：第一下一定是跑到方塊最邊緣（`lip` 0.1 格）起跳的普通 Telly，接平的，`jumps > 1` 之後才往上；回頭時視角不會低於 `max-pitch`（預設 80°，只有離落地 2 tick 而腳下還沒有方塊時例外），台階改點身後那格的頂面而不是腳下；準心對到位置才連點（`onSpot`），沒有「瞄一次停一 tick」了；`rise` 關掉是平的一直線。實測：往上 15 秒 82 格、平的 15 秒 80 格與 19 秒 94 格，都沒有掉
  - Speed 模式的方塊形狀（使用者給了一張 Speed Telly 的側面圖）：`rise` 開時每一層是 4 格一段（第 1 格疊在下一層最後一格的頂面、往前接 3 格），`bumps` 開著時在每段第 2 格上再疊一顆（每跳一次，`bumped`）；`run(level)` 找這一層的橋尾時不算比下一層橋尾還後面超過一格的方塊，那是上一段的凸起。`rise` 關（使用者叫它「一般的 Telly」）時橋面高度固定在開啟時那一層（落在凸起上也不更新 `floorY`），凸起不是指定的：回頭後視角以 `mouse-speed`（預設每 tick 25°）往下移、右鍵照 `cps` 連點，還沒降到位的那幾下點在後面方塊的頂面。`flatClick()` 自己沿視線找第一個實心方塊，只有「橋面那層的頂面（上面是空的）」或「橋尾朝前的側面」才點，否則方塊會疊在凸起上越疊越高（第一版因此在第 4～15 格掉下去）。橋落後一格以上或快落地時視角直接到位。實測（原版 1.21.11）：往上＋凸起 15 秒 87 格；平的＋凸起 15 秒 82 格、用 134 格方塊；都沒有掉
  - **現在的選項**（使用者找不到「一般的 Telly」之後改的）：`rise` 開關拿掉了，改由 `bridge-mode` 選。`Telly`＝一般的 Telly（平的），`Speed`＝Speed Telly（每跳往上一格，`rising()`），兩個都走 `telly()`；舊的「每格精準瞄準」Telly 與它的 `edge-distance`／`turn-back-height`／`hold-back` 設定已經刪掉。一般的 Telly 不是一離地就回頭：先面向前方衝刺飛一段，等「離落地的 tick 數 ≤ 要補的格數 + 4」才轉身（每 tick 一格、轉身 2 tick、多留 2 tick），把身後的格子補回來；回頭後視角以 `mouse-speed`（預設每 tick 8°）往下移、連點不停，所以大約每跳有一下落在頂面變成凸起。實測（原版 1.21.11）：Telly 15 秒 89 格、用 111 格方塊；Speed 15 秒 90 格；都沒有掉
  - **視角是「移動」不是「定位」**（使用者說每 tick 強制定位畫面很亂）：`onTick` 包了一層，先把視角放回上個 tick 的終點 `toYaw/toPitch`，跑 `step()`（原本的邏輯，照樣直接設視角，這個 tick 的 pick 與點擊用的就是它），再記下 `from`→`to`；`onRender`（`Render3DEvent`）每一幀把視角設成兩者依 `tickDelta` 的內插。`LocalPlayer.getViewYRot()` 不內插，所以要自己每幀設。副作用是模組開著時玩家自己的滑鼠無效（每幀被蓋掉），這是使用者要的。實測（原版 1.21.11）搭橋結果和改之前一樣；「玩家動滑鼠不影響」沒有實測（這個環境 xdotool 的相對滑鼠移動無效）
  - **轉向固定同一邊**（使用者回報「一下左一下右」）：原因是落地時還面向後方就用 `turn(ahead())` 挑最短方向轉回前方，剛好 180° 時左右不定；畫面內插的 `wrap()` 也一樣。現在落地時還沒轉回來就在地上接著走第 3 階段（經過 forwardYaw+270，多花一個 tick，那個 tick 不跳），`onTick` 記 `toYaw` 時把 ±180 一律當成 +180，`onRender` 直接用 `toYaw - fromYaw` 不再 wrap。實測（原版 1.21.11）：Telly 15 秒 84 格、Speed 15 秒 81 格，沒有掉
  - **現在的轉向與左右修正**（使用者說還是亂轉，並要求用鍵盤修正偏移）：回頭固定往右（forwardYaw+90 → +180），轉回前方固定往左原路回來（+90 → forwardYaw），落地時還沒轉回來也一樣；轉回來的半途臉朝橋的右側，所以那個 tick 按的是左鍵（往前推），不是右鍵。左右位置全程用 `steer()` 的左右鍵修，視角保持正前／正後，`ahead()`（靠偏轉視角修正）已刪掉：看「現在的偏移 + 側向速度 × 幾個 tick」，超過 0.1 格才按，面向前方與回頭時左右鍵相反。實測（原版 1.21.11，起點故意偏 0.3 格）：Telly 15 秒 82 格、Speed 15 秒 84 格，結束時離中線 0.03 與 0.17 格，都沒有掉
  - **「繞一圈」的真正原因與落地掉落**（把每個 tick 的視角印出來才查到，猜了兩次都沒中）：回頭期間要瞄的那一面還在身體前面（還沒飛過它）時，`atan2` 算出的偏航朝前，視角一個 tick 轉到正前方、下一個 tick 又轉回正後方。現在回頭時視角只留在後方：目標不在身後（`ax*dx + az*dz >= -0.05`）就維持正後方、不點（`spotAhead`），偏航夾在正後方 ±30° 內。另外落地時還沒轉回來就在地上花一個 tick 轉頭而不跳，會從橋尾走出去（Speed 連三輪在第 8 格掉）；現在那個 tick 照樣起跳（`takeOff()`），臉朝橋右側所以按左鍵往前推，下一個 tick 在空中轉正再按前進。實測（原版 1.21.11）：每 tick 的轉動只有 +90、−90、0；Speed 兩輪各 15 秒 72 格、Telly 15 秒 77 格，沒有掉。速度比之前慢（每秒約 5 格）
  - **三個模式與轉頭速度（以這一條為準）**：`bridge-mode` 有 `Telly`（平的）、`TellyUp`（一般的 Telly 往上）、`Speed`、`Diagonal`。使用者說明的機制：不往上＝往前飛很久才回頭補，接得到就好；往上＝回頭早，人還在高處，點出來的方塊在高一層，直接落在上面；回頭時視角一路往下拉，拉得慢連點就會落在方塊頂面。實作：每一跳起跳時由 `takeOff(mayGoUp)` 決定 `upJump`，Speed 第一跳之後每跳都是，TellyUp 一跳平一跳上交替（`!upJump`）。往上的跳早回頭（`airT >= max(1, 5 − n)`）、台階用 55° 的視角點在身後較遠那格的頂面，所以新的一層疊在舊的一層尾段上；`runStart` 記這一跳台階的位置，`run()` 只認它之後的方塊。平的跳晚回頭（離落地 ≤ 要補的格數 + n + 2）。`turn-ticks`（n，預設 4，Speed 固定 2）是半圈轉幾個 tick；轉頭途中 `turnKeys()` 依視角偏角換鍵（<67° 前進、67～113° 左鍵、>113° 後退，只有 ≤20° 才疾跑），推力一直朝前、左右互相抵銷；不這樣做速度會掉到每秒 2 格。視角下拉是先快後慢（每 tick 走差距的 0.45，上限 `mouse-speed`），只用在平的跳；`bumps`（指定在第 2 格上疊一顆）只剩 Speed 有。**`turn-ticks` 大於 2 時 TellyUp 來不及早回頭，橋會是平的**：落地後轉回前方要 n tick、再回頭又要 n tick，往上的跳只有 9 tick。使用者否決了「背對著起跳」（沒有衝刺太慢）。實測（原版 1.21.11）：預設 4 tick 時 Telly 15 秒 57 格、TellyUp 57 格（沒有往上）、Speed 94 格；`turn-ticks` 2 時 TellyUp 15 秒 97 格、有往上、形狀接近使用者的回放影片；都沒有掉
  - **視角下拉的曲線**（使用者要求「往下是慢慢變快」）：取代上一條的「先快後慢」。回頭開始後的第 t 個 tick，下拉速度上限 = `mouse-speed` × min(1, t/6)²（`downT`），不超過目標角度。實測（原版 1.21.11，`mouse-speed` 25）：Telly `turn-ticks` 4 是 15 秒 55 格、用 82 格方塊；`turn-ticks` 2 是 15 秒 80 格、用 75 格（橋面上幾乎沒有多的方塊）；TellyUp `turn-ticks` 2 是 15 秒 97 格、有往上；都沒有掉
  - **`pull-curve`**（使用者要「前面非常慢、後面很快」）：下拉速度上限 = `mouse-speed` × min(1, t/6)^`pull-curve`，預設 4（上一條的二次曲線等於 2），`mouse-speed` 預設改成 45。實測（原版 1.21.11）：Telly `turn-ticks` 4 是 15 秒 55 格、用 71 格方塊；`turn-ticks` 2 是 15 秒 80 格、用 75 格（沒有多的方塊）；TellyUp `turn-ticks` 2 是 15 秒 97 格、有往上；都沒有掉。曲線越陡，多出來的方塊越少：前段準心還沒碰到橋，後段又被保底直接拉到位
  - **下拉放慢的位置（以這一條為準，取代上面兩條的計時方式）**：使用者要的是「準心剛掃到橋的那一段不要拉太快」，不是整段前慢後快。現在準心還沒指到任何方塊時用 `mouse-speed` 全速下拉；`seesBlock()` 第一次為真的那個 tick 起（`touched`）才開始算 `downT`，速度從最低 1.5°／tick 照 `pull-curve`（預設改回 2）加速回 `mouse-speed`。實測（原版 1.21.11，`mouse-speed` 45）：Telly `turn-ticks` 4 是 15 秒 54 格、用 73 格方塊；`turn-ticks` 2 是 15 秒 74 格、用 103 格（多出約 29 格，前兩版這一組是 0）；TellyUp `turn-ticks` 2 是 15 秒 82 格、有往上；都沒有掉。使用者說 Speed 模式「是完美的」，不要再動
  - **上面那層方塊是算出來的（以這一條為準，`mouse-speed`／`pull-curve`／下拉曲線都已刪掉）**：使用者試了三種下拉曲線都說「還是沒有」，要求先計算。算出來的幾何：從後上方看橋尾那格，點到「頂面」（方塊疊在上面）和點到「側面」（往前接）的仰角只差 4～9°（例：人在橋尾後 1.5 格、跳到最高點，頂面 57°、側面 63°）；照「頂面 → 側面 → 下一格」的順序，每個 tick 視角只要動 0～4°，把準心帶過邊緣的是人往前飛，不是滑鼠下拉。之前每 tick 拉 8～45° 都是一次跨過那個範圍，所以點不到頂面。實作：平的跳（Telly，以及 TellyUp 的平跳）補方塊時，這一跳還沒疊滿 `top-blocks`（預設 2）、人已經離開橋尾那格（`beyond > 0.4`）、上面是空的、而且不是要救落地的時候，先瞄橋尾頂面離前緣 0.35 格的位置點一下（`topSpot`、`tops`），下一個 tick 再瞄側面往前接；回頭時機提早 `top-blocks` 個 tick。視角直接設到算出來的角度。實測（原版 1.21.11）：Telly `turn-ticks` 4 是 15 秒 53 格、用 95 格方塊；`turn-ticks` 2 是 15 秒 72 格、用 124 格；TellyUp `turn-ticks` 2 是 15 秒 76 格、有往上；都沒有掉，橋面上是一段一段連著的第二層（每段 2～4 格）
  - **TellyUp 往上那一跳的順序（使用者用錄影截圖指出）**：下一次 Telly 要從橋尾「下面那格」的前面接出去，不是到後面的方塊頂面另外疊一層蓋過來。現在 TellyUp 的往上跳是：先在原本那層往前接一格（`baseSpot` → `baseDone`、`baseIndex`），人還在高處時點這格新方塊的頂面當台階（離前緣 0.35 格，要人已經離開那一格或腳高過它），再從台階側面接高的那一層。Speed 的台階做法沒動。實測（原版 1.21.11，`turn-ticks` 2）：TellyUp 兩輪各 15 秒 76 格、有往上，Telly 15 秒 72 格，都沒有掉；每一層的最後一格上面就是下一層的第一格，形狀接近使用者傳的那張 Telly 圖
  - **TellyUp 照回放逐格分析後的做法（以這一條為準，取代上面「一跳平一跳上」與 `base` 那兩條）**：把使用者的側面回放拆成每秒 30 張，用顏色遮罩量每一層方塊的左右端隨時間的變化（腳本不在 repo）。結果：方塊是一下一上成對出現的，「往前接一格 → 在它上面疊一格 → 下一格 → 再疊」，0.6 秒放 10 格（幾乎每 tick 一格），一次疊出兩層；人落在上層，下一段把上層當底層往前接、上面再疊新的一層；兩段之間約半秒不放方塊；最開頭那一下是平著接 3 格、只在其中一格上多疊一顆。實作：`takeOff()` 裡 TellyUp 第一跳之後 `pairJump` 為真（`upJump` 只剩 Speed 用）。這種跳一離地就轉身；每個 tick 先看這一跳接出去的方塊（`takeoffEnd` 之後）裡第一顆上面還空著的，人的腳高過它（`feetY >= level + 2`）或已經離開它就點它的頂面（`pairTop`，這個目標幾乎在腳下，允許看到 89°），否則照常點下層橋尾的側面往前接。落地預測先看上層（`y <= level + 2` 而且 `under(…, level + 1)`），有就當成落在上層；落地後 `floorY` 取腳下那層。實測（原版 1.21.11，`turn-ticks` 2）：TellyUp 兩輪各 15 秒 67 格、有往上、兩層交疊，Telly 15 秒 72 格，都沒有掉。回放裡人的移動速度量出來約每秒 5 格
  - **TellyUp 疊厚**（使用者說「太細了，要厚一點」）：成對疊的那種跳，除了這一跳新接的方塊，也回頭補前幾跳留在身後的空位，從橋尾往回看 6 格：第一輪補「腳下這層的缺口」（下面那層有方塊、這層是空的），第二輪補「這層有方塊、上面是空的」。人的腳還沒高過、也還沒離開的那一格先跳過（第二輪遇到就先往前接下層）；超過 4.2 格的不點；快落地而腳下還沒方塊時不補。這些點擊不經過 `flatClick()`（它只認腳下那層的頂面）。實測（原版 1.21.11，`turn-ticks` 2）：兩輪各 15 秒 69 格、有往上、沒有掉，方塊用量從 117 增加到 181，側面看是實心的兩三層
  - **`fill-back`**（使用者說上一條「太厚了，取中間值」）：上一條的回頭補位加了每跳上限。補「腳下這層的缺口」以及補起跳點（`takeoffEnd`）之前那些方塊的頂面，都算 `backFills`，一跳最多 `fill-back`（預設 2，0～6）格；這一跳新接的方塊上面照樣疊，不受限。實測（原版 1.21.11，`turn-ticks` 2）：兩輪各 15 秒 68 格、用 163 格方塊（不補是 117、不限是 181），沒有掉
  - **`turn-side`**（使用者要能自己選往左或往右轉）：`Right`（預設，和之前一樣）或 `Left`，Telly／TellyUp／Speed 共用。只有三處看 `turnSign()`：回頭與轉回時的偏航（`forwardYaw + turnSign() * …`）、`turnKeys()` 在視角垂直橋身那一段按的鍵（右轉按左鍵、左轉按右鍵）、`onTick` 把剛好半圈當成哪一邊。實測（原版 1.21.11，Left，`turn-ticks` 2，各 15 秒）：Telly 72 格、TellyUp 68 格、Speed 94 格，沒有掉
  - **Speed 的開場是 4b**（使用者要求，Speed 其餘部分仍然不要動）：第一跳（`jumps == 1`、`opening`）不是一離地就回頭，改用一般 Telly 的時機（先面向前方疾跑飛、剩下的時間剛好夠補才回頭），接到 `OPENING_BLOCKS`（4）格就不再接，所以開場是一直線的 4 格。一離地就回頭的寫法只飛得到 3 格。使用者確認過 `TellyReplay` 用「中途面向後方再跳一次」的版本（他問過別人），已照那版發布到下載站。實測（原版 1.21.11，15 秒）：開場層 4 格、93 格、沒有掉；1.21.4 只有編譯
  - **`TellyReplay`：落腳的方塊不是最高的（補在下面幾條之上）**：使用者指出影片第 113 格已經有一顆比落腳處更高的方塊、第 114 格才放下腳踩的那顆。往上那一跳的順序是：身後那格頂面疊一顆 → 下層往前接一顆 → **在身後那顆上面再疊一顆**（`rHigher`，兩格高）→ 才在下層新接的那顆上面放落腳的方塊。所以每階開頭是一根兩格高的柱子（之前把它當成被擋住的誤疊而拿掉，是錯的）。另外查證過：影片是原速（放第一顆到終點約 140 格 ≈ Time 5.550），一循環 21 tick；空白鍵整個循環幾乎都按著（只在落地前放開 2～3 格），所以中途腳碰到剛接的方塊時是遊戲自動再跳，使用者看起來是「一個循環只按一次跳」。使用者認為一循環只有一跳，這點和 12＋9 tick 的推論沒有完全對上，之後他再提時要先對這件事。實測（原版 1.21.11，15 秒）：71 格、每循環升一層、沒有掉；1.21.4 只有編譯
  - **`TellyReplay` 的起跳是疾跑跳（補在下面兩條之上，以這一條為準）**：使用者要求「起跳一定要是跑」，並指出影片裡落地的那一瞬間就邊轉頭邊跳。每循環第一跳落地時把表對到第 1 tick（`REC_FIRST_JUMP`，準心偏左 29°）而不是第 0 tick（偏左 55°、沒按 W），而且那一格的按鍵是 `WD`（量到的是只有 `W`，但前後兩格 D 都按著）。原因用 `tools/telly-sim/lateral.py`（同一張表在水平面上的模擬）算過：起跳那一推沿著當時的視角，偏左 55° 時每循環往左偏 0.65 格；偏左 29° 只按 W 是 0.51 格（進遊戲第三階就掉，10° 的修正拉不回來）；偏左 29° 按 W+D 是 +0.08 格。實測（原版 1.21.11，15 秒）：71 格、每循環穩定前進 5 格升一層、離中線 ±0.15 格內、沒有掉；每層橫排約 6～7 格、和上一層重疊 1～2 格再加一顆單獨的（使用者的圖是約 8 格、重疊約 5 格，還沒到）。1.21.4 只有編譯
  - **`TellyReplay` 放方塊的位置（補在下一條之上，以這一條為準）**：使用者傳了正確的側面圖（每一層是約 8 格的橫排，上一層疊在它後段上面，兩層重疊約 5 格；每層開頭是「單獨 1～2 顆、空一格、再接橫排」），並要求把影片五個循環都逐格看過。五個循環以「視角開始回頭」對齊（第 16、41、68、94、120 格），後三個幾乎一格不差。每循環 8 顆的順序：落地前連接 3 顆同層（接在上一顆前面）→ 落地立刻再跳，在**身後那一格**上面疊 1 顆（自己站的那格疊不了，就是圖上的空格）→ 飛過去後下層再接 1 顆、上面疊 1 顆（落在這顆上，高一層）→ 再接 1 顆下層，起跳後朝前下方點、把上層疊到它上面。`replay()` 的點擊目標就是這套規則（`rCell` 是第二跳起跳時站的那格、`rBehind` 只允許身後 1 顆）。**兩個坑**：(1) 身後疊兩顆時第二顆會被第一顆擋住、疊到它上面變成兩格高的柱子，所以 `replayClick()` 取樣時用 `replaySees()` 確認視線沒被擋；(2) 把起跳那格的按鍵從量到的 `D` 改成 `WD`（想讓它疾跑起跳）會沿著當時偏左 55° 的視角多推一把，側向偏出近兩格掉下去，表要用量到的值。`tools/telly-sim/try6.py` 用同一張表跑模擬，先在那裡看側面形狀再進遊戲。實測（原版 1.21.11，15 秒）：57 格、每階「一顆、空一格、一排」、沒有掉；橫排比使用者的圖短（約 4～5 格，重疊也較少），因為起跳沒有疾跑的那一推
  - **`TellyReplay` 現在是「照影片錄下來的動作表重播」（以這一條為準；更早的 `calm`／`glide`／`stay-round`／兩跳規則版都被取代，那些設定已隱藏，`replay()` 不走 `telly()`）**：使用者否決了我自己設計流程的幾個版本（「根本沒有往後走」「長得完全不一樣」），要求直接把影片裡準心在 3D 空間的方向與按鍵逐格記下來重播，「空白鍵用看的不看按鍵表」，模式除了開始停止沒有調節功能；之後又同意要有小幅修正，因為影片只有五個循環、純重播會累積誤差。做法：`tools/telly-sim/track.py`＋`pose.py`＋`solve.py`（OpenCV：遮掉沙岩與 HUD，對遠處的島和雲取 SIFT 特徵，相鄰兩格用射線的 Kabsch＋RANSAC 求旋轉，再用「滾轉為零」定出起始俯仰與焦距）得到每格的偏航與俯仰；按鍵讀左下角顯示的明暗；放方塊讀快捷列數字。取第 91→117 格那個循環，換成 21 tick 的表 `REC_YAW`／`REC_PITCH`／`REC_KEYS`／`REC_CLICKS`。量到的重點：視角在前方約 −20°～−55° 與正後方之間擺動（往左），面向前方只有起跳前後兩三個 tick；回頭約 4 格、轉回前方約 7 格；回頭後俯仰從約 44° 慢慢壓到 75°；每循環有兩次落地（平跳約 12 tick、接著往上的一跳約 9 tick），腳著地就跳。**教訓：空中也能按空白鍵，不能用按鍵顯示判斷起跳；使用者指出第 90 格落地、第 91 格起跳。** 修正只有三個：`replayClick()` 在要點的面上取樣、挑離表上方向最近的點；偏離中線時偏航最多修 10°；落地時把表對到第 0 或第 11 tick。點哪一面由位置決定：平跳把起跳那層接到落點下、往上那一跳先接下層再疊上層（落腳那格最後放）、剛起跳的那兩下疊在剛離開的方塊上。實測（原版 1.21.11，15 秒）：純重播在第 9 格掉下去；加修正後 52 格、一路往上、99 格方塊、沒有掉。橋的形狀（每階一根兩格高的柱子）還沒有得到使用者確認；1.21.4 只有編譯。`finish-jump`：覆寫 `toggle()`，搭橋中被關掉時先 `finishStep()`（轉正、W+疾跑、落地跳一次），`stop()` 這類強制停止走 `forceOff`
  - **`TellyReplay` 模式（以這一條為準：使用者說放進 TellyUp 的版本不如舊版，所以 TellyUp 改回 `turn-side` 那版的做法，`calm` 只在 `bridge-mode` = `TellyReplay` 時成立；`tellyUp()` 是兩個模式共用的兩層疊法）**：使用者另外要求滑鼠軌跡與落地位置也照影片。從放大的影格量到的視角：回頭 2 格、回頭完約朝下 30°，接著約 6 tick 慢慢滑到約 50° 而且不放方塊，再壓到約 70° 連放；轉回前方約 4 格、開始轉的頭兩格還在放。實作：回頭固定 2 tick；`glide-ticks`（**預設改成 2**：使用者回報「新模式不會往上」；實測 3 以上就來不及疊上層、橋是平的，2 以下才穩定往上，所以影片裡那段約 6 tick 的滑行在預設值下等於沒有）內 `gliding` 為真，視角 = 30 + 3.5 ×（airT − 2）、不點（離落地 4 tick 內而腳下沒方塊時例外）；轉回前方 `REPLAY_FORWARD` = 3 tick（設 4 時起跳太慢，15 秒 58 格而且全平）。`turn-ticks` 在這個模式沒有作用。實測（原版 1.21.11，各 15 秒，都沒有掉）：`glide-ticks` 6 是 68 格、用 91 格方塊、橋是平的每跳上面一格；4 是 66 格、升 1 層；0 是 68 格、用 127 格、和 TellyUp 一樣往上。落地位置沒有辦法從第一人稱影片量出格數，只做到「面向後方落在這一跳剛接的方塊上」；「轉回前方的頭兩格還在放」沒有做。視角每個 tick 一個點、畫面每幀在兩點之間內插（`onRender`），不是定位。以下是加進這個模式之前、同一套 `calm` 邏輯的說明：影片（25 fps，231 格，每格都看了，並把快捷列的方塊數字裁出來對時間）裡每次轉頭也只花 2～3 格，和我們一樣快；差別是**一個循環約 21 tick 才轉一來一回**：轉向前方只有 3～4 tick（起跳），其餘 14 tick 都面向後方，方塊集中在一段 10～12 tick 內放 7～9 格，接著約 10 tick 不放。我們原本是落地當下就轉回去起跳，9～12 tick 就一來一回。現在 TellyUp 的成對跳（`calm`）：空中不提前轉回前方，面向後方落地；落地後 `staying`（至少 `stay-round` tick，預設 5）繼續按後退倒著走、`floorY` 取腳下那層並把它往前接（這段不疊上層，視角可以到 89°）；等前方剩下的橋夠轉身用（`overhang() <= -(0.22 × turn-ticks − 0.1)`）才轉正，轉的時候照常用 `turnKeys()` 保持速度，面向前方就起跳；轉到一半橋尾已經到腳下（`overhang() >= 0.15`）就邊轉邊跳。第一版是落地後站著不動轉身，結果起跳沒速度、橋變成平的，不要改回去。實測（原版 1.21.11，各 15 秒）：`turn-ticks` 2 是 70 格、升 11 層、用 135 格方塊；3 是 68 格、有往上（以前 3 是平的）；4 是 59 格、只升 1 層；都沒有掉。`stay-round` 0 是原本的做法
    - 寫之前用 Python 做過 2D 模擬（不在 repo）：視角完全固定（固定仰角一路連點）撐不過第二跳；每跳往上一格、「瞄一次停一 tick」可以一直跑。模擬裡的常數要用對：地面加速 0.1274（0.13×0.98）、起跳 +0.2、空中 0.0255／0.0196、起跳那個 tick 的摩擦是 0.546、之後 0.91
    - 使用者說過「不要全部都放必要方塊、回頭時不要刻意調視角」：做法是固定的（連點不停、視角隔一個 tick 才修正），沒有加隨機或假失誤，那個不做
  - **測試陷阱**：`x=0` 那條線上留著第一次測 Telly 時搭的橋，`/fill … air` 對沒載入的區塊會失敗而且不容易注意到。在舊橋上「搭橋」怎樣都不會掉，斜跳模式第一版的數字就是這樣來的，不算數。每次測搭橋都換一條新路線，並用 `/execute if block … air` 先確認是空的
- `SpearSmash`（MELEE，1.21.11 才有）：長矛命中後接重錘。戳刺由 `DoAttackEvent` 看（準心或長矛指向的生物，戳刺的距離比準心遠所以用 `nearestAhead()` 補），放平（按住使用鍵）由「前方生物的 `hurtTime` 變大」看：客戶端沒有別的方式知道放平的長矛打中了（`recentKineticEnemies` 只有伺服器有）。之後下一個 tick 選重錘＋`KeyMapping.click(攻擊鍵)`；穿鞘翅時先用和 `MaceDive` 一樣的同 tick 換胸甲
  - 使用者回報「重錘沒有觸發」後改的：`falling()` 在鞘翅滑翔且下降時直接算（滑翔中 `fallDistance` 會被壓在 1 以下）；等待重錘進入距離的時間（`follow-up-window`）從 8 改成 20 tick；沒有重錘、或時間內沒進到 3 格內，都會在聊天欄說（`messages` 預設開）。重錘那一下仍然要準心對著目標且在 3 格內，這是原版攻擊的條件，長矛戳得到的距離比這個遠
  - `OffhandTotem` 在注入版另外直接呼叫遊戲的 `MouseHandler.onMove`（反射，名稱在 `gen_names.py`），因為使用者回報 Windows 上滑鼠沒有動；創造模式的物品欄版面不同，只提示不動作
- **長矛之後的重錘不用等進到 3 格**（使用者指出、已從原始碼與實測確認）：伺服器收攻擊封包時的距離檢查是 `isWithinRange(player, box, 3.0)`，也就是攻擊距離再加 3 格緩衝；只有客戶端的準心（`entityAttackRange()`，看手上的物品）卡在 3 格，而拿長矛時準心可以到 4.5 格。做法：拿著長矛按 `KeyMapping.click(攻擊鍵)`，在 `DoAttackEvent`（`startAttack()` 開頭）才選重錘，原版接著對準心選到的目標送攻擊。`SpearSmash.swapNow`、`MaceDive.maceOnAttack` 就是這個旗標；`MaceSwap` 原本「超過重錘距離就不換」的限制也拿掉了。實測：站在 3.7 格與 4.2 格外，扣耐久的是重錘。送出去的順序固定是長矛 → 胸甲 → 重錘（中間客戶端把長矛選回手上只是為了準心，不會送封包）
- `MaceDive` 的 `spear-mode`（使用者要求：要放煙火，長矛 → 胸甲 → 重錘照順序幾乎同時切）：
  - 長矛只打得到 2～4.5 格（`AttackRange`），而且放平後要 `KineticWeapon.delayTicks`（鑽石 10、獄髓 8）才算傷害。煙火飛行的速度約 1.7 格／tick，所以從 20 格俯衝只有約 11 tick：俯衝開始的那個 tick 用 `fireworkNow()`（直接 `Clicks.useItem()`）放煙火，同一個 tick 選長矛並 `Clicks.useItem()` 放平、按住使用鍵。距離超過 `SPEAR_LEAD`（28）的俯衝則先照常放煙火，進到 28 格內再補一發（把速度拉回 1.7）後放平
  - 三步各佔一個 tick：長矛在距離內待滿一個 tick（`spearHad`，不等 `hurtTime`，它會晚一兩個 tick）→ 放開、換胸甲 → Strike 選重錘按攻擊。Strike 在長矛模式用 `max(attack-range, 3)`，因為沒有時間等使用者設的較短距離
  - 實測（1.21.11，高度 20、攻擊距離 1.99、鑽石長矛）：每輪都是長矛命中後重錘命中，兩輪打死鐵巨人
  - 目標一旦選定就鎖住（`locked`），不會因為爬升超過 `range` 而丟失：以前高度設得比 `range`（預設 40）大時會永遠到不了俯衝
- `OffhandTotem`（UTILITY）：副手沒有圖騰時 `KeyMapping.click(物品欄鍵)` → 把游標移到圖騰那格 → `screen.keyPressed(與副手交換鍵)` → `screen.onClose()`。格子位置用 `(width-176)/2` 算，對不上（配方書開著）就試另一個位置，用 `hoveredSlot` 驗證（Meteor 的 accessor；注入版用反射，名稱在 `gen_names.py`）。游標要連續移兩次、差 1 像素：X11 的 GLFW 會吞掉「自己移到的那個位置」的移動事件，遊戲收不到。26.3 是 SDL 的寫法（`SDL_WarpMouseInWindow`），沒有實測
- `WindJump` 的時機設定改名為 `throw-timing`，可以是負數（先丟、之後才跳），預設 -1：實測平均 11 格，同 tick 是 8.7 格
- **測試角色不能有緩降效果**：有的話伺服器的 `fallDistance` 一直是 0，重錘沒有落下加成，客戶端的 `fallDistance` 也跟著是接近 0 的怪數字
- `CartBomb`（EXPLOSIVES，Meteor 版與注入版兩個版本都有）：火焰弓引爆 TNT 礦車，開一次做一輪。開啟時準心要在某個方塊的頂面上（`ground`）。選弓、按住使用鍵，拉弓期間每個 tick 把視角轉到「箭會落在礦車上」的角度（`aimPitch()`：箭從眼睛下方 0.1 出發，速度 = 弓力 × 3，弓力 = (t²+2t)/3、t 是拉弓秒數；每 tick 先移動、再乘 0.99、再 `vy -= 0.05`；從直視角度往上每 0.25° 試到不再落在目標前面為止）。拉滿 `draw`（預設 4；使用者說不要拉太滿否則太慢。3 是最少、只到約 2.8 格，4 到 3.6 格）後同一個 tick 內依序：`releaseUsingItem`（箭先出去）→ 視角轉回方塊、`Clicks.repick()` → 選鐵軌 `Clicks.useOnBlock` → 再 `repick()`（準心換到鐵軌上）→ 選礦車 `useOnBlock`。伺服器同一個 tick 依序處理這三個封包，箭的第一次移動在之後，所以再近也不會比礦車早到。`Clicks.repick()` 在 26.3 是 `mc.hitResult = mc.player.raycastHitResult(1.0f, mc.player)`（那一版 `Minecraft.pick` 是 private），轉到 1.21.x 時換成 `mc.gameRenderer.pick(1.0f)`／Yarn 的 `updateCrosshairTarget`。準心選到的那一格如果已經是鐵軌（上一輪沒炸掉的），`railDown` 為真，跳過放鐵軌直接放礦車。沒有檢查弓有沒有火焰附魔。實測（原版 1.21.11，每輪清場，以「礦車離開快捷列且世界上沒有礦車」判定）：拉 4 與 5 各 8 輪全部引爆，拉 3 是射得到的 6 輪全中、另外 2 輪太遠直接提示；已有鐵軌的情況 1 輪引爆。**測試陷阱**：只看「世界上沒有礦車」會把「根本沒放出去」也算成功
- `PacketLimiter`（第十二個模組，在 UTILITY，**只有 Meteor 版**）：`PacketEvent.Send` 裡依封包的協定 id（`packet.type().id().getPath()`，例如 `use_item`、`move_player_pos`）計數，超過該種每秒上限或總上限就 `event.cancel()`（丟棄，不是延後）。設定是在建構子裡用反射列出 `GamePacketTypes` 的 serverbound 型別自動產生的，所以兩個版本共用同一份原始碼；`PROTECTED` 裡的（傳送確認、區塊批次、簽章聊天等）不列出也不限制，keep-alive 之類不在 `GamePacketTypes` 裡所以本來就碰不到。注入版沒有：那邊的掛鉤只能在方法開頭插入呼叫，不能取消。實測（1.21.11）：`use-item` 上限 5／秒，按住右鍵 3 秒丟出 15 瓶，另外 98 個被丟棄
- `MaceDive`（鞘翅＋重錘循環）要點：
  - 鞘翅與胸甲的互換：備用的那件放快捷列。同一個 tick 內「選那一格 → `Clicks.useItem()`（直接呼叫，立刻生效）→ 選回武器」。伺服器每個 tick 只在最後比對一次主手物品，所以它看不到武器離手，劍的蓄力不會被重置。備用那件放副手也行（主手拿武器按右鍵，原版會改用副手的物品）。使用者回報過：沒有換上胸甲就完全沒有傷害，所以 Prepare 階段找不到胸甲會直接警告並關閉
  - 爬升預設垂直往上（`climb-steepness` 90），俯衝才對準目標，所以是斜著打下來，不會先飛到目標正上方
  - 胸甲要在命中**前一個 tick** 換上：伺服器在自己的 tick 才清掉滑翔狀態，滑翔中重錘沒有落下加成。所以距離 ≤ 攻擊距離 + 1.5 × 速度時先換，進到攻擊距離才選重錘並按左鍵
  - 起飛是每個 tick 交替按放跳躍鍵：第一下離地，空中的下一下打開鞘翅
  - 所有「使用」都用 `KeyMapping.click(使用鍵)`，由原版在同一個 tick 稍後處理，所以每個 tick 只安排一個動作
  - 實測（1.21.11、對鐵巨人）：循環約 3.5 秒一次，一次命中 36.6 點。測試時要給目標加 tag 並清掉其他生物，`mobs` 選項會選最近的任何生物；聊天視窗開著時模組會暫停
- `MaceSwap` 掛在 `DoAttackEvent`（`startAttack()` 的開頭，原版還沒讀手上物品之前）改選取的欄位，之後原版的 `attack()` 會先送換欄位封包再送攻擊封包。伺服器要到下一個 tick 才套用新物品的屬性並重置蓄力，所以這一擊是「重錘當武器、原武器的攻擊力」：實測鐵巨人扣的是鑽石劍的 7 點，扣耐久的是重錘。判斷武器用登錄名稱（`BuiltInRegistries.ITEM.getKey`），不要用 `Item.toString()`，1.21.11 沒有覆寫它
- `CrystalMacro` 的 `auto-obsidian`：切到黑曜石、點擊、切回水晶在**同一個 tick** 內做完，否則玩家按住右鍵時原版的連續使用會多放一塊
- 連點模組用 `Input.isPressed(keyUse／keyAttack)` 判斷實體按鍵；每一下點擊是 `KeyMapping.click(綁定的按鍵)`（按鍵用 Meteor 的 `KeyMappingAccessor.meteor$getKey()` 取得），等於多按一次，由原版的 `handleKeybinds()` 在同一個 tick 逐一處理，所以一個 tick 可以有好幾下（100 CPS = 每 tick 5 下）。不要用 `Utils.rightClick()`，它一個 tick 只會點一次
- `AnchorMacro` 的延遲都是 0 時，`onTick()` 的迴圈會在同一個 tick 內走完切換、補充、切回、引爆（補充後方塊狀態是客戶端預測的，不用等伺服器）。改過預設值的設定要換名字（`swap-delay`、`use-delay`），否則玩家存檔裡的舊值會蓋掉新預設
- **每個模組的聊天提示開關**（使用者說提示太多會蓋掉重要訊息）：`chat-hints`（預設開）。Meteor 版在 `HintModule`（所有模組的基底，覆寫 `info`／`warning`／`error`），注入版在 `api/Module.java` 本身，`port_modules.py` 把 `extends HintModule` 換回 `extends Module`。新模組要繼承 `HintModule`。`Look`（STYLE）不是真的模組，建構子裡把 General 群組拿掉。各模組原本的 `messages`／`feedback` 細項開關還在。實測（注入版 1.21.11）：Wind Jump 沒有風彈時，開著有提示、關掉沒有；Meteor 版這個環境沒有編譯
- **原則是只做原版按鍵會做的事**。所有點擊都走 `Clicks.java`，呼叫順序照抄 `Minecraft.startUseItem()`／`startAttack()`（先取揮手動畫、再 `useItemOn`／`useItem`／`attack`、結果是 `PREDICTED` 才揮手）。切物品只用 `inventory.setSelectedSlot()`，點方塊只用 `mc.hitResult`，不偽造視角
- 偵測「玩家自己放的」：`InteractBlockEvent`（重生錨、水晶）、`InteractItemEvent` 加 `EntityAddedEvent`（珍珠）。模組自己的點擊也會觸發這些事件，所以用 `clicking` 旗標擋掉
- `WindPearl` 的物理（從 26.3 的 bytecode 查到的）：
  - 珍珠每 tick：`vy -= 0.03`，速度 `*= 0.99`，再移動
  - 玩家丟的風彈 `accelerationPower = 0`、慣性 1.0：固定每 tick 1.5 格直線飛，沒有重力
  - 珍珠能打到風彈是因為 `Projectile.isPickable()` 對 `REDIRECTABLE_PROJECTILE` 標籤回傳 true；碰撞框只多出 `computeMargin()` 的 0～0.3 格，所以實際要在約 0.45 格內
  - `CLIENT_LAG = 2`：畫面上的珍珠比伺服器慢的 tick 數，是在單人世界試出來的。26.3 在這個環境只有 5 FPS，結果有雜訊；1.21.11（OpenGL，流暢）用這個值從仰角 15° 到 85° 六次全中
- 實測方式：`--quickPlaySingleplayer "New World"` 直接進世界，用聊天指令給物品、`/tp @s x y z yaw pitch` 設定視角（xdotool 的相對滑鼠移動在這裡無效）、`/data get entity @s Pos` 讀結果。超平坦世界要先 `/difficulty peaceful`，否則史萊姆會把人推走

## 注入式客戶端：AiCoder Assist（`inject/`）
- 不靠 Fabric、不靠 Meteor：注入到執行中的**原版 1.21.11**。產物 `inject/work/AiCoderAssist.jar` 與 `.exe`（`work/` 不進 repo）。說明、測試範圍與建置流程都在 `inject/README.md`
- **`inject/src/aicoder/assist/modules/` 與 `aurora/{Fx,Chrome,Aurora,Style}.java` 是產生出來的**：`inject/tools/port_modules.py` 從 `combat-macros/` 和 `aurora-glass/` 轉過來，改那兩邊再重跑 `./build.sh`
- 程式對著 Mojang 的名稱寫（`work/minecraft-named.jar`），建置時用 tiny-remapper 整包轉成混淆名稱。只有兩種東西要查表（`tools/gen_names.py` → `names.properties`）：被 ASM 改寫的方法、用反射讀的 private 欄位
- 掛鉤只能是「在方法開頭插入一行靜態呼叫」（執行中的 JVM 不允許加欄位或方法）。目前六個：`Minecraft.tick`、`startAttack`、`GameRenderer.render`、`MultiPlayerGameMode.useItemOn`／`useItem`、`ClientLevel.addEntity`、`KeyboardHandler.keyPress`。在 return 前插入的寫法曾讓 `Gui.render` 驗證失敗，沒有再用
- 繪圖：`gfx/GuiRenderer` 把四邊形收成一個實作 `GuiElementRenderState` 的 `Mesh`，交給 `GuiGraphics` 裡的 `guiRenderState`（package-private，用反射拿）。座標是 framebuffer 像素，用 pose 縮回 GUI 單位。和 Meteor 一樣，一個 scissor 內先送色塊、再跑 `post()` 的工作（文字）
- 字體：`gfx/TextRenderer` 照 Meteor 的 `CustomTextRenderer`（五種高度 27～81、`getWidth` 除以 1.5 等）實作，量出來的寬高一致，所以 `Fx` 的排版不用改
- 官方啟動器附的 Java 21 runtime 含 `java.instrument` 和 `jdk.attach`（查過 Windows 與 Linux 的 `release` 檔），所以 `.exe` 直接用遊戲行程旁邊的 `java.exe` 來注入
- `.exe` 是 `launcher/launcher.c`（mingw 編譯）後面接上 jar；Java 從檔尾讀 jar，所以同一個檔案可以當 `-jar` 的參數，也可以當 agent。`.exe` 在使用者的 Windows 上執行過舊版（能找到 Java 並啟動注入器），之後被防毒擋下；`launcher/AiCoderAssist.bat`＋jar 的版本使用者回報注入成功
- 按鍵綁定（注入版自己的；Meteor 版用 Meteor 內建的）：`Module.bind`（GLFW 鍵碼，-1 是沒有）與 `holdBind`。`Client.key()` 由鍵盤掛鉤呼叫：按下只在沒有開畫面時生效，放開一律生效（按住的模組不會因為開了聊天而卡在開啟）。按住模式的模組存檔時一律存成關閉。設定畫面的 BIND 區在 `AuroraScreen.settings()` 的最後，等待按鍵時由 `keyPressed()` 接手
- 選單鍵可以自訂（Java 與基岩版都有）：Java 是 `Client.menuKey`（預設右 Shift，存成 `ui.menu-key`），在 STYLE 視窗最後一列 Menu Key 設定，等待按鍵時 `listening == Client.look`；Insert 固定也能開。基岩版是 `g_menuKey`（`[ui] menu_key`），上方 `KEY: …` 按鈕；選到的那一下按鍵用 `g_skipMenuKey` 擋掉，才不會順便把選單關掉。選單鍵不能同時當模組的綁定鍵（設定時會把用到它的模組綁定清掉）
- 外觀有兩套（極光／液態玻璃），由 `aurora/Look.java` 的設定決定（它借用 `Module` 的設定機制但不在模組清單裡，用上方的 STYLE 開啟，存成 `style.*`）。`AuroraScreen` 的繪圖都經過 `aurora/Ui.java`，再分派到產生出來的 `Chrome`（極光）或手寫的 `aurora/Glass.java`（液態玻璃，照 `liquid-glass/` 的 `Chrome` 改寫）；`Ui.frame()` 每個畫面也會把 `Style` 裡的顏色改成對應外觀的配色。背景圖在 `gfx/Wallpaper.java`：用 `ImageIO` 解碼成 `DynamicTexture`，`renderIn()` 用自己的 scissor 立刻送出一塊圓角貼圖；玩家的圖放 `<遊戲資料夾>/aicoder-assist-wallpapers/`，內建圖在 `res/aicoder/assist/wallpapers/`
- 小遊戲在 `aurora/Arcade.java`（GAMES 分頁：左邊清單、右邊遊戲區；每個畫面把繪圖用的東西放進 static 欄位再呼叫遊戲的 `update`／`draw`）與六個 `aurora/Game*.java`（`Arcade.Game` 的子類別）。`AuroraScreen` 在 `Arcade.open` 時把鍵盤、滑鼠、滾輪轉給它（Esc 與選單鍵除外）。名稱與外觀都是自己的，不要改成既有遊戲的名字或樣子。`GameDig` 是 2D 沙盒：世界是 `byte[W*H]`，存成 gzip 的 `aicoder-assist-dig.dat`
- **1.21.4 版**：`V=1.21.4 ./build.sh`（先 `python3 tools/fetch.py 1.21.4 work-1.21.4`）。`src/` 是 1.21.11 的寫法；`tools/port_version.py` 把它複製到 `work-1.21.4/src`、蓋上 `src-1.21.4/` 的檔案、再做文字替換（每條都必須命中）。版本相關的繪圖只在 `gfx/Backend.java`（1.21.11：`Mesh`＋`GuiRenderState`；1.21.4：`GuiGraphics.drawSpecial` 寫進 `RenderType.gui()`／`guiTextured()`，貼圖要先向 `TextureManager` 註冊），`GuiRenderer`／`TextRenderer`／`Wallpaper` 兩版共用。1.21.4 沒有長矛所以沒有 `SpearLunge`；鍵盤掛鉤的參數是 `(long,int,int,int,int)`；螢幕要自己呼叫 `renderBlurredBackground()` 才有模糊。右 Shift（或 Insert）開關選單都在 `Client.tick()` 處理；進到世界後會在聊天欄印一次 `[AiCoder Assist] loaded`，使用者可以用它分辨「沒注入」和「選單開不了」。**同一次遊戲只能試一個版本的包**：注入過別的版本（即使因版本不符而失敗），那個 jar 已經掛在遊戲的 classpath 上，之後載入的正確版本會解析到舊的類別而失敗，要重開遊戲。失敗原因由 agent 寫進系統屬性 `aicoder.assist.error`，注入器讀出來顯示，視窗內容同時存成 jar 旁邊的 `AiCoderAssist-log.txt`。版本檢查會看 classpath 上所有的 `version.json`（有些啟動器會在遊戲前面放別的 jar）。jar 的路徑含非 ASCII 字元時，注入器會先複製到純 ASCII 的暫存位置再載入（Windows 上路徑會被轉壞）
- **Fabric**（兩個版本的 jar 都內建，不是另一個下載）：Fabric 下遊戲類別是 intermediary 名稱、而且由 Knot class loader 載入。`build.sh` 在有 `intermediary.tiny`（`tools/fetch_fabric.py`）時，用 `tools/fabric_names.py` 產生 `fabric.tiny`（named → intermediary；intermediary 沒列的成員要**略過**，不能填 official 名稱，否則 tiny-remapper 報 Unfixable conflicts）與 `names-intermediary.properties`，再把 `named.jar` 轉成 `fabric-payload.jar` 包進 jar。`Agent.joinFabric()` 在遊戲的 `Main` 不是系統 loader 載入時，把 payload 寫到暫存檔、用 `FabricLauncherBase.getLauncher().addToClassPath()` 加進 Knot，並改用 intermediary 名稱表；`Names.shared()` 依自己是哪個 loader 載入的選名稱表。測試：`FABRIC=1 ./test.sh`。沒有 payload 的舊版在 Fabric 下會「顯示成功但什麼都沒掛上」，現在會明確失敗
- **授權（一機一碼）**：`license/HwidProvider.java`（硬體識別碼的 SHA-256）與 `license/License.java`（送出序號與指紋、用 Ed25519 公鑰驗伺服器簽的憑證、快取在遊戲資料夾）。建置時有 `LICENSE_URL` 與 `LICENSE_PUBKEY` 才會把 `license.properties` 放進 jar，沒有就完全沒有授權檢查（下載頁上的是沒有的）。鎖定的地方只有兩處：`Module.setActive()` 不讓開、`Client.tick()` 把開著的關掉。序號由注入器用 agent 參數帶進來，再放進系統屬性 `aicoder.assist.license.key`（Fabric 下 Agent 和客戶端在不同的 class loader，只有系統屬性兩邊都讀得到）。測試伺服器是 `tools/LicenseServer.java`。協定與實測範圍在 `inject/README.md`
  - 使用者問過兩件事，都**不做**：用 ProGuard 混淆（類別名稱維持公開，而且防共用靠的是伺服器簽章）；「下載的只是載入器、模組本體授權後才從伺服器抓下來執行」（和惡意程式的下載器是同一種結構）
- **遊戲的 Java 沒有 `jdk.attach` 時**（使用者朋友的電腦：`NoClassDefFoundError: com/sun/tools/attach/VirtualMachine`，啟動器用精簡的 Java 跑遊戲）：`Injector.main()` 一開始檢查，載不到就用 `javaThatAttaches()` 找電腦上完整的 Java 重新執行自己（帶 `-Daicoder.assist.relaunched=true` 防止重複），找不到就請使用者裝 JDK 21。細節與實測範圍在 `inject/README.md` 最後一節
- 原則和戰鬥巨集一樣，另外明確不做躲避偵測的功能
- 測試：`inject/test.sh`（手動用快取裡的官方 jar 啟動原版，再注入）。腳本裡不要用 `| tail` 接它的輸出，背景的遊戲會讓管線等不到結束

## 壁紙（`liquid-glass/` 與 `glass-columns/` 共同的功能）
- `gui/Wallpaper.java`（兩個專案各一份，只有 namespace 不同）。圖片**只畫在面板裡**，不是全螢幕背景：`frame()` 每個畫面算一次「鋪滿螢幕」的排版，`renderIn()` 由 `Chrome.panel()` 呼叫，用自己的 `Renderer2D` 畫該面板範圍的那一塊（UV 依螢幕座標），圓角用水平細條拼
- `renderIn()` 用自己的 scissor 立刻送出，所以面板之後畫的染色、反光、亮邊都疊在圖片上面
- 內建壁紙放在 `assets/<namespace>/wallpapers/`，並登記在 `Wallpaper.BUNDLED`；玩家自己的圖放 `meteor-client/aicoder-wallpapers/`
- 新增內建壁紙：把圖放進兩個專案的 `wallpapers/`，各在 `BUNDLED` 加一行

## Minecraft 1.21.11 版（`mc-1.21.11/`）
- 七個主題 addon 和戰鬥巨集的 1.21.11 版本，各自是獨立的 Gradle 專案，產出 `<名稱>-1.21.11.jar`
- **原始碼是產生出來的**：`python3 mc-1.21.11/port.py` 會把 26.3 版的原始碼複製過來並改寫命名（Mojang → Yarn）與 API 差異。要改東西就改 26.3 版再重跑，不要手改 `mc-1.21.11/*/src/`
- 兩版的主要差異都列在 `port.py` 裡：`DrawContext`、`Click`／`KeyInput`、GLFW 常數與游標、`NbtCompound`、`net.minecraft.util.Pair`、`PositionedSoundInstance.ui`、`TextureFormat`；1.21.11 的 `TextRenderer.begin()` 不需要 graphics，所以沒有 `GuiRendererAccessor` mixin；沒有 `modulesHelpText()`
- 建置：Loom 用 `net.fabricmc.fabric-loom-remap` 外掛（Gradle 以 JDK 25 執行、編譯目標 Java 21），最終檔名設在 `remapJar` 而不是 `jar`
- 這個環境可以用 OpenGL 跑 1.21.11（26.3 只能走軟體 Vulkan）

## Zorynth 外掛：AiCoder Themes（`zorynth-plugin/`）
- 另一個客戶端（Zorynth，外掛 API 1、UI 擴充 2）的主題外掛，結構照使用者朋友給的模板；`sdk/` 是模板附的 SDK（含原始碼 jar，API 以它為準）。說明、操作與沒驗證的假設在 `zorynth-plugin/README.md`。建置：`./build.sh`（會清掉 `build/`）
- 13 個模組＝13 個主題（`Style.java` 一列一個，配色抄自 Meteor 版）。開啟後用 `replaceScreen(MODULES)` 換掉整個模組介面：`ThemeScreen.java`（WINDOW 三欄與 COLUMNS 直立式兩種版面、設定、綁定、搜尋；每個畫面重建點擊區 `hits` 與滾動區 `zones`），`Draw.java`（依主題的圓角／切角方塊、面板、開關圖示），`Backdrops.java`（背景）
- 客戶端沒有 UI 擴充 2 時退回 `context.ui` 的背景層＋四角框
- `preview/Preview.java` 是用 Java2D 實作 Canvas 的假宿主，出圖並模擬點擊；沒有在真的客戶端跑過

## 基岩版注入客戶端（`bedrock/`）
- C++（mingw 交叉編譯）：`src/assist.cpp` 是 DLL，`src/injector.c` 是用 `LoadLibraryW` 遠端執行緒載入它的 exe。說明、拿掉的功能與原因、沒驗證的假設在 `bedrock/README.md`
- **刻意不碰遊戲內部**（沒有特徵碼、沒有掛鉤）：選單是自己的 layered window（GDI+ 畫、`UpdateLayeredWindow`），模組只用 `SendInput`。九個模組都是固定的按鍵序列（`Step` 陣列，由巨集執行緒逐步送出），物品在第幾格由使用者設定；需要讀記憶體的功能（例如準心飄移）不要加。Mace Swap 會在滑鼠掛鉤裡吃掉實體左鍵（`g_swallowLeft`），改由序列代按
- 小遊戲在 `src/games.h`（由 `assist.cpp` 在繪圖輔助函式之後 include；`games::render/key/mouse/scroll/leave`），是 Java 注入版 `aurora/Game*.java` 的 C++ 移植，改其中一邊時另一邊要跟著改。鍵碼用 Windows 的 VK
- 三個執行緒：UI（畫面與右 Shift）、巨集（1ms 迴圈）、滑鼠 LL 掛鉤（只記實體按鍵狀態，忽略 `LLMHF_INJECTED`；獨立執行緒才不會讓滑鼠卡）
- 測試：這個環境裝得起 Wine（`apt-get install wine64 wine`）。`Xvfb :78` 下跑 `work/Minecraft.Windows.exe`（替身，`test/fakegame.c`，把收到的輸入寫到 `fakegame.log`；按 Z 歸零、按 C 切換游標），再跑注入器（stdin 接 `/dev/null`），用 xdotool 操作。`xdotool key Shift_R` 太短的敲擊靠 `GetAsyncKeyState` 的低位元補抓
