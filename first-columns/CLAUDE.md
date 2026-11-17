# AiCoder 初版直立式（`first-columns/`）

這個專案是使用者最早那版 UI（`aicoder-client`，青綠毛玻璃、用著色器畫形狀）的**直立式版本**。下面第二節起是原版的交接文件，設計規格（配色、玻璃、極光）仍然適用；版面那一段（單一視窗三欄）不適用。

## 和原版不同的地方
- 套件 `aicoder.firstcolumns`，mod id `aicoder-first-columns`，著色器資源在 `assets/aicoder-first-columns/shaders/`，產出 `AiCoder初版直立式.jar`。主題名稱仍是 `"Meteor"`，所以和其他取代 Meteor 主題的 addon（包括原版）不能同時裝
- `gui/screens/ColumnsScreen.java` 取代原本的 `AiCoderModulesScreen`：每個分類一個玻璃欄（`WAiCoderWindow`），左鍵開關、右鍵開設定畫面，`Ctrl+F` 搜尋。極光、左上角 logo、底部狀態列與等化器畫在整個畫面的容器（`WCategoryController.onRender`）上
- 排版：先量出欄寬，算一排放得下幾欄（`perRow()`），放不下就分成多排，每排平分高度（`view.maxHeight`），欄內超出的部分捲動。所以所有欄位都在畫面內，不會蓋到底部狀態列
- **模組列沒有圓形開關**（使用者要求）：`WAiCoderModule` 開啟時整列填上兩個主題色並有流光邊框，關閉時是一般的卡片。設定畫面裡的勾選框、滑桿維持原版的樣子
- 只有 26.3 版。原版的繪圖直接用 26.3 的 render pipeline（`Sdf.java`、`BackdropBlur.java`），`mc-1.21.11/port.py` 的文字替換轉不過去
- 實測（26.3，這個環境是軟體 Vulkan、約 5 FPS）：欄位畫面、點擊後整列亮起、1280×720 下五欄一排加第二排。右鍵開設定在這個幀率下沒點成功，沒有驗證

---

# AiCoder client — GUI 主題交接（給 Claude Code）

## 專案目標（目前階段）
- 專案名稱：**AiCoder client**
- 形式：**Meteor Client addon**，目標 **Minecraft 26.3**（與最新 Meteor 相同）
- **這個階段只做 GUI 主題**：把 Meteor 的 GUI 換成 `design/mockup.html` 的外觀。
- 模組功能**先不實作**；之後另外處理。

## 設計參考
- `design/mockup.html`：已定稿的設計稿（用瀏覽器開啟，會動）。`bg.jpg` 只是用來比對透明度的背景照片。
- `design/aicoder-gui.gif`、`design/aicoder-gui-still.png`：預覽圖。
- 注意：設計稿標題還寫 `N O V A`，實作時改成 **AiCoder client**。

## 設計規格（照這些值做）

### 配色
| 用途 | 值 |
|---|---|
| 主色 1（綠） | `#2fe08f` |
| 主色 2（藍） | `#2f8cff` |
| 主色 3（青） | `#22d3ee` |
| 主要文字 | `#c7d6f5`，選取或開啟時為 `#ffffff` |
| 次要文字 | `#9fb6e6` |
| 設定標籤 | `#aebbe0` |
| 分隔線 | `rgba(120,180,255,0.12–0.14)` |
| **不使用**的顏色 | 黃色、洋紅、紫色 |

### 視窗（彩色毛玻璃）
- 尺寸 820×440，圓角 18
- 玻璃底色：150° 漸層 `rgba(34,150,148,.09)` → `rgba(28,98,166,.12)`（位置 58%）→ `rgba(42,172,132,.09)`
  - 要**很透**，背景必須看得見。
- 背景模糊：開啟 Meteor 內建的 **Blur 模組**（GUI 開啟時模糊背景），飽和度偏高。
- 內側頂端高光：1px `rgba(255,255,255,.20)`
- 外發光：`0 0 55px rgba(25,227,255,.20)`、`0 0 110px rgba(47,140,255,.14)`
- 邊框流光：沿邊框流動的綠／青／藍漸層，模糊約 7px，**不規則流動，不要整圈旋轉**。

### 版面
- 標題列高 52：左邊放 Logo「AiCoder client」，字距加寬，綠→白→藍漸層，加柔光；右邊不放任何東西（**沒有紅綠燈按鈕**，開關介面靠 RSHIFT）。
- 三欄：分類欄寬 168｜模組欄寬 272｜設定欄占剩餘寬度
- 底列高 40：`[RSHIFT] GUI`、FPS、延遲（ms）；右下角放小型跳動等化器。

### 元件
- **分類**：圓角 11，內距 11×14，字 13px 粗體、字距 1.5。選中時底色為綠青半透明漸層，加 1px 青色邊框和柔光。
- **模組列**：圓角 11，內距 11×14。選中的模組外圈有流光邊框。
- **開關（圓形，不要 iPhone 那種滑動膠囊）**：直徑 24
  - 關：底色 `rgba(16,24,44,.5)`，2px 外框 `rgba(160,190,255,.32)`
  - 開：徑向漸層 綠→青，外發光會緩慢呼吸，中心有 8px 的 `#eafff6` 圓點
- **勾選框**：18×18，圓角 5。勾選時填 綠→青 漸層並發光。
- **滑桿**：軌道高 7，圓角 5，底色 `#141a30`；填色 綠→藍；白色圓形把手直徑 14，帶發光。

### 背景與動態
- **極光光簾 3 道**，垂直、很柔和：
  - 青綠 `rgba(40,214,185,.5)`
  - 綠 `rgba(70,216,110,.52)`
  - 藍 `rgba(56,120,250,.52)`
  - 模糊約 72px，透明度約 0.22 ± 0.15
- **運動方式**：用多個正弦波疊加做出不規則擺動，加上輕微傾斜和亮度呼吸。
  - **不要轉圈，速度要慢**（一個循環約 3.3 秒以上）。
- 細顆粒雜訊：疊加一層，透明度約 0.1。
- **已移除**：掃描線、紅綠燈按鈕。

## Meteor 實作要點（已從 Meteor 原始碼查證）
1. **主題要繼承 `MeteorGuiTheme`**，不要直接繼承 `GuiTheme`。
   - 原因：`MeteorWidget.theme()` 會強制轉型成 `MeteorGuiTheme`，Meteor 內建的 W* 元件才能沿用。
2. **主題名稱**：`GuiTheme.name` 是 final，`MeteorGuiTheme` 寫死為 `"Meteor"`。
   - 用 `GuiThemes.add()` 加入同名主題時，會**取代**原本的主題（只會在 log 留一筆錯誤，不會失敗）。
   - 所以 addon 的 `onInitialize()` 呼叫 `GuiThemes.add(new AiCoderGuiTheme())` 即可。
3. **配色**：主題的顏色都是 `Setting<SettingColor>` 的 public 欄位（例如 `accentColor`、`checkboxColor`、`backgroundColor` 等），在子類別的建構子裡 `.set(...)` 成上面的配色。
4. **要覆寫的工廠方法**：
   - `checkbox()` → 圓形開關
   - `window()` → 圓角毛玻璃、極光背景、邊框流光
   - `module()` → 模組列加圓形開關
   - 其他元件沿用 `WMeteor*`。
5. **繪圖**：`GuiRenderer` 有實心四邊形、四角漸層四邊形、`triangle()`、文字、貼圖，**沒有**圓形和圓角。
   - 圓形和圓角都用 `triangle()` 拼出來。
6. **字體**：Meteor 有自訂字型系統（`Fonts`、`SystemFontFace`），可以選系統字型，讓字體跟設計稿一致。

## 建置
- 版本（取自 Meteor 的 `gradle/libs.versions.toml`）：
  - minecraft `26.3`
  - fabric-loader `0.19.5`
  - fabric-api `0.162.0+26.3`
  - loom `1.18-SNAPSHOT`
- Meteor 的 maven：
  - `https://maven.meteordev.org/releases`
  - `https://maven.meteordev.org/snapshots`
- 開始前先確認 Meteor 目前發佈的 addon template 用的是哪套 mappings 和依賴寫法，照官方 template 建立專案。

## 工作流程
1. 寫或修改主題
2. `gradle build`，啟動遊戲
3. 開 GUI 截圖
4. 和 `design/aicoder-gui-still.png` 對照
5. 回到第 1 步調整，直到一致
