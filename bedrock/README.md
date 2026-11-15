# AiCoder Assist — 基岩版（Windows，國際版）

注入到執行中的 Minecraft 基岩版。產物：`work/AiCoderAssist-Bedrock.exe`（注入器）與 `work/AiCoderAssist-Bedrock.dll`。

## 用法
1. 兩個檔案放在同一個資料夾
2. 開 Minecraft 基岩版，進入世界
3. 執行 `AiCoderAssist-Bedrock.exe`，回到遊戲按**右 Shift** 開選單
4. 選單右上角 `UNLOAD` 可以卸載

## 它怎麼運作（和 Java 版完全不同）
- **不讀、不改遊戲內部的任何東西**：沒有特徵碼、沒有掛遊戲函式，所以不會因為遊戲更新而失效
- 選單是蓋在遊戲視窗上的一個獨立視窗（開啟時遊戲會失去焦點，基岩版會自己暫停）
- 模組只透過 `SendInput` 按滑鼠鍵和數字鍵，和人按的一樣。它**看不到**你手上拿什麼、準心對著什麼
- 模組只在「遊戲視窗在前景、而且滑鼠游標是隱藏的」時動作（游標出現＝開了背包、聊天或選單）
- 不做任何隱藏：DLL 用自己的名字出現在遊戲的模組清單，視窗和行程都沒有偽裝

## 模組
| 模組 | 做什麼 |
|---|---|
| Left Clicker | 按住左鍵時連點（CPS 1～100、變化量、延後開始） |
| Right Clicker | 按住右鍵時連點 |
| Crystal Clicker | 按住右鍵時「放、打、放、打」輪流（手上要自己拿水晶） |
| Anchor Macro | 你手動按一次右鍵放重生錨後：按螢光石那格的數字鍵 → 右鍵充能 → 按回原本那格 → 右鍵引爆。兩個欄位自己設定；建議綁成 HOLD，只在要用時按住 |

每個模組都能綁按鍵，模式可選 TOGGLE／HOLD（設定最下面的 BIND；Esc／Backspace／右鍵清除）。兩種極光介面用右上角的 `WINDOW`／`COLUMNS` 切換。設定存在 `%APPDATA%\AiCoderAssist\bedrock.ini`。

## 從 Java 版拿掉的功能，以及原因
- **Mace Swap（錘切）、Spear Lunge**：靠的是 Java 版「伺服器每 tick 才比對一次主手物品」和攻擊蓄力的機制。基岩版沒有蓄力冷卻，這個特性不成立
- **Wind Pearl**：預判是照 Java 版的拋射物數值算的，而且需要讀取視角；基岩版數值不同，這裡也讀不到視角
- **Crosshair Drift、Mace Dive**：需要知道其他玩家的位置，必須讀遊戲記憶體，這一版刻意不做
- **Crystal 的自動補黑曜石**：需要知道點的是什麼方塊

## 測試範圍
在 Linux 的 Wine 裡，用一個替身程式（`test/fakegame.c`：會隱藏游標並記錄收到的輸入，檔名取成 `Minecraft.Windows.exe`）測過：注入、提示、兩種版面、設定視窗、拖曳滑桿、綁定 G＋HOLD、Left Clicker（12 CPS 設定下 1 秒約 12 下）、Crystal、Anchor（收到 2、右鍵、1、右鍵）、游標顯示時不動作、UNLOAD 後再次注入。

**沒有在真正的基岩版和真正的 Windows 上跑過。** 不確定的地方：
- 遊戲是否接受 `SendInput` 的滑鼠與數字鍵（一般的巨集軟體是可以的）
- 遊戲在世界中是否真的隱藏系統游標（若不是，模組會完全不動作）
- 獨佔全螢幕時選單可能蓋不上去，請用視窗或無邊框
- 商店舊版（UWP）的行程能否載入 DLL（注入器已替 DLL 加上 ALL APPLICATION PACKAGES 的讀取權限）
- 防毒軟體很可能會擋注入器，原因和 Java 版的 exe 一樣；這裡不做任何規避

## 建置
`./build.sh`（mingw-w64 交叉編譯，字型 Comfortaa 以資源嵌入 DLL）
