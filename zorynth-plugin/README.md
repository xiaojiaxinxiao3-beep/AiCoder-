# AiCoder Themes — Zorynth 外掛（API 1，UI 擴充 2）

把 AiCoder 的 Meteor 主題做成 Zorynth 外掛。專案結構、`build.gradle`、`build.ps1`、`META-INF/zorynth-plugin.json`、`sdk/` 都照外掛模板。

## 安裝
把 `build/aicoder-themes-1.0.0.jar` 放進遊戲實例的 `zorynth/plugins/`（不是 `mods/`），重啟客戶端。遊戲內輸入 `.plugins` 可以看載入狀態。需要支援 UI 擴充 2 的客戶端。

## 內容
Client 分類會多出 13 個模組，一個模組就是一個主題：Cyberpunk、Liquid Glass、Aurora，以及 01 Strata ～ 10 Amber。

開啟其中一個，客戶端的模組介面就整個換成該主題自己的介面（`context.replaceScreen(MODULES)`），HUD 的三個顏色也跟著換（`context.theme`）。開另一個主題時，原本那個會自動關閉。

介面最上面一排按鈕：

- `WINDOW`／`COLUMNS`：兩種版面。WINDOW 是單一視窗三欄（分類｜模組｜設定）；COLUMNS 是每個分類一欄
- `<` 主題名 `>`：直接換上一個／下一個主題
- `ORIGINAL UI`：關掉主題，回到客戶端原本的介面

操作：

- WINDOW：左鍵點模組列＝選取並顯示設定；點右邊的開關圖示或對模組列按右鍵＝開關模組
- COLUMNS：左鍵＝開關模組；右鍵＝開啟設定視窗（點視窗外面關閉）
- 直接打字＝依名稱搜尋，Backspace 刪字
- 設定：數值可拖曳滑桿，或對數值框左鍵加、右鍵減；選項左鍵下一個、右鍵上一個；文字、顏色（十六進位）、方塊清單點一下後輸入，Enter 確認；按鍵設定點一下再按鍵
- 每個模組設定最下面的 BIND：`Key` 點一下再按鍵（Backspace／Delete 或右鍵清除），`Mode` 點一下切換 Toggle／Hold
- Esc／右 Shift 由客戶端處理，一律可以離開

每個主題模組自己的設定：`Layout`、`Backdrop`、`Animations`、`Backdrop Strength`。

## 和 Meteor 版不同的地方
- 沒有背景模糊（Canvas 沒有模糊），玻璃感是半透明加漸層做的
- 用客戶端的預設字型，沒有另外打包字型
- 圖片壁紙、音效沒有做
- HUD 編輯畫面沒有替換，仍是客戶端原本的
- 客戶端若不支援 UI 擴充 2，會退回只換顏色＋畫背景與四角框

## 建置
- Windows：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -Jdk "JDK25 目錄"`
- Linux／macOS：`./build.sh`

## 測試範圍
`preview/Preview.java` 是用 Java2D 實作 Canvas 的假宿主：載入外掛、給它 41 個假模組，把 13 個主題的兩種版面畫成 PNG，並模擬點擊檢查了選取、右鍵開關、拖曳滑桿、搜尋、設定視窗、只開一個主題。**沒有在真正的 Zorynth 客戶端裡跑過**，以下是照模板範例推定、沒辦法在這裡確認的：

- 多邊形的頂點順序（照範例用順時針）
- 字高約 8px、按鍵碼是 GLFW（範例用 257＝Enter、259＝Backspace）
- `bindMode()` 的字串是 `Toggle`／`Hold`（說明書只提到名稱；程式會沿用原本的大小寫）
- 滾輪事件的位置用 `UiView.mouseX/Y`
