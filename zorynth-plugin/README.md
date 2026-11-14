# AiCoder Themes — Zorynth 外掛（API 1）

把 AiCoder 的 Meteor 主題做成 Zorynth 外掛。專案結構、`build.gradle`、`build.ps1`、`META-INF/zorynth-plugin.json` 都照外掛模板。

## 安裝
把 `build/aicoder-themes-1.0.0.jar` 放進遊戲實例的 `zorynth/plugins/`（不是 `mods/`），重啟客戶端。遊戲內輸入 `.plugins` 可以看載入狀態。

## 內容
Client 分類會多出 13 個模組，一個模組就是一個主題：

- AiCoder Cyberpunk、AiCoder Liquid Glass、AiCoder Aurora
- AiCoder 01 Strata ～ 10 Amber（地球科學十款）

開啟其中一個：套用該主題的主色／正文色／輔助色（`context.theme`），並在介面背後畫動態背景、在最上層畫四角框與標題／讀數（`context.ui`）。開啟另一個時，原本那個會自動關閉。

每個模組的設定：`Backdrop`（背景）、`Frame`（四角框與文字）、`Animations`（動畫）、`Strength`（背景濃度 0.2～1）。

## 和 Meteor 版不同的地方（API 的限制）
- API 1 不能替換客戶端自己的介面類別，所以視窗的形狀、三欄／直立式版面、六角開關、分段滑桿這些**做不到**；直立式的兩款主題因此沒有另外的模組（顏色與背景和單視窗版相同）。
- `Canvas` 只有矩形和文字：光暈、極光、等高線都是用小方塊拼的，沒有模糊、圓角、圖片壁紙。
- 冰河、地形圖、地震儀在 Meteor 是淺色主題；這裡宿主的面板是深色，所以保留強調色、文字改成淺色。

## 建置
- Windows：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -Jdk "JDK25 目錄"`（需要 `sdk\zorynth-plugin-api-1.jar`）
- Linux／macOS：`./build.sh`

收到的模板沒有附 `sdk/zorynth-plugin-api-1.jar`。`sdk-stub/` 是只供編譯用的替身，裡面的每個方法簽名都是從模板附的 `ExamplePlugin` 編譯檔與說明書讀出來的，不會被打包進外掛。有真正的 SDK 時放到 `sdk/` 就會改用它。

`Layer` 的背景層常數名稱在模板裡沒有出現過，所以程式用 `Layer.values()` 找「不是 FOREGROUND 的那一個」，不寫死名稱。

## 測試範圍
`preview/Preview.java` 用假的宿主載入外掛、把每個主題畫成 PNG，並檢查「只會開著一個主題」。**沒有在真正的 Zorynth 客戶端裡跑過**。
