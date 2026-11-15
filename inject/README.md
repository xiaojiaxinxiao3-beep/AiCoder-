# AiCoder Assist：注入式客戶端（Minecraft 1.21.11）

不需要 Fabric 也不需要 Meteor。先啟動**原版** Minecraft 1.21.11，再執行 `AiCoderAssist.exe`，
七個模組和兩個極光玻璃介面就會進到正在執行的遊戲裡。在遊戲中按**右 Shift** 開啟選單。

## 內容
- 九個模組，和 `combat-macros/` 是同一份原始碼：Anchor Macro、Crystal Macro、Crosshair Drift、Left Clicker、Mace Dive、Mace Swap、Right Clicker、Spear Lunge、Wind Pearl
- 兩個介面，可以在選單上方切換：**COLUMNS**（每個分類一欄）和 **WINDOW**（單視窗三欄）。`MOTION` 是動態的開關
- 每個模組都能綁一個鍵盤按鍵：在模組設定最下面的 BIND 區，`Key` 點一下再按鍵（Esc 或右鍵清除），`Mode` 選 TOGGLE 或 HOLD
- 設定存在遊戲資料夾的 `aicoder-assist.properties`

## 使用
1. 啟動 Minecraft 1.21.11（官方啟動器、沒有模組的原版），等到標題畫面
2. 執行 `AiCoderAssist.exe`。它會用遊戲自己的 Java 去注入，所以不用另外安裝 Java
3. 回到遊戲按右 Shift

防毒軟體可能會擋下 `.exe`：它沒有簽章，而且做的事（找到別的行程、把程式載入進去）正是防毒會注意的行為。這裡不會為了通過掃描去改它。
`launcher/AiCoderAssist.bat` 用一段看得懂的腳本做同樣的事，和 `AiCoderAssist.jar` 放在同一個資料夾執行即可。使用者在 Windows 上回報用這個方式注入成功。

沒有 `.exe` 可用的環境（Linux、macOS）：`java -jar AiCoderAssist.jar`，需要 JDK 21 以上。

## 測過與沒測過的
- 測過：Linux 上手動啟動的原版 1.21.11。注入、五個掛鉤、兩個介面、設定視窗，以及五個模組的實際效果
  （Anchor Macro、Crystal Macro 含自動黑曜石、Right Clicker、Mace Swap、Wind Pearl）
- 測過：把 `.exe` 這個檔案直接當成注入內容（它的結尾就是 jar），可以注入
- `.exe`：使用者在 Windows 上回報過一次，確認它能執行、找得到 Java 行程並啟動注入器；當時注入器自己找不到遊戲（見下一點），修正後的版本還沒有在 Windows 上確認
- 注入器不能靠 `VirtualMachine.list()` 找遊戲：遊戲若以 `-XX:-UsePerfData` 啟動（不少第三方啟動器會這樣），那份清單是空的。現在由 `.exe` 把所有 Java 行程的編號傳進來，注入器逐一 attach，用 `sun.java.command` 判斷哪個是 Minecraft。這個情況已在 Linux 上重現並驗證
- 沒測過：官方啟動器啟動的遊戲、Crosshair Drift、Left Clicker
- 只支援 1.21.11。其他版本會拒絕注入並顯示訊息，因為遊戲內部的名稱每個版本都不同
- 不支援已經裝了 Fabric 或其他載入器的遊戲

## 不做的事
不做任何躲避偵測的功能。載入的類別就叫 `aicoder.assist.*`，Java 印出的「有程式被動態載入」警告也留著。

## 怎麼運作
- `Injector`：用 Java 的 Attach API 找到執行中的 Minecraft，要求它的 JVM 載入這個 jar
- `Agent`：在遊戲裡被呼叫後，用 ASM 改寫六個已載入的遊戲方法，在開頭插入一行對 `Hooks` 的呼叫。
  執行中的 JVM 只允許改方法內容，不能加欄位或方法，所以所有掛鉤都是這種形式
- `Hooks` → `Client`：把這些時刻變成事件，交給開著的模組
- `api/`：Meteor 的模組與設定 API 的替身。有了它，模組原始碼可以從 `combat-macros/` 原封不動轉過來
- `gfx/`：Meteor 繪圖器的替身，畫在原版的 GUI 繪圖流程上；`TextRenderer` 自己用 STB 畫 Comfortaa 字體，
  尺寸和量測方式照著 Meteor 的文字繪製器，所以排版結果一樣
- `aurora/`：`Fx`、`Chrome`、`Aurora`、`Style` 是從 `aurora-glass/` 轉過來的；`AuroraScreen` 是這裡自己寫的，
  負責排版和滑鼠（原本由 Meteor 的元件負責）

## 建置
`./build.sh`，產物在 `work/`。流程：
1. 把 Mojang 的官方對照表（`work/client.txt`，不放進 repo）轉成 tiny 格式，用 tiny-remapper 做出一份「名稱看得懂」的遊戲 jar
2. `tools/port_modules.py` 從 `combat-macros/` 和 `aurora-glass/` 產生模組與繪圖的原始碼（**不要手改 `modules/` 和 `aurora/` 裡這幾個檔**）
3. 對著看得懂的名稱編譯，再整包轉回遊戲真正的（混淆過的）名稱
4. 被改寫的方法和用反射讀的欄位，名稱由 `tools/gen_names.py` 寫進 `names.properties`
5. ASM 搬到自己的套件名底下一起打包；`.exe` 是一個小啟動程式後面接上 jar

`./test.sh` 在虛擬螢幕上啟動原版並注入。`work/cp.txt`（遊戲的函式庫 classpath）和 `work/remapper-cp.txt` 取自 Loom 的快取。

## 外觀：極光與液態玻璃（STYLE）
選單上方多了 `STYLE`，按下去是外觀設定（存檔時和模組設定存在同一個檔案）：

- `Skin`：`Aurora`（極光玻璃）或 `Glass`（液態玻璃）。兩種外觀都有 WINDOW、COLUMNS 兩種版面，所以一共四種介面
- 液態玻璃的設定：`Tinted`（是否上色）、兩個玻璃顏色各自的紅／綠／藍、`Glass Opacity`、`Backdrop Dim`
- 背景圖片：`Image` 那一格左鍵換下一張、右鍵換上一張（`NONE`＝不用圖片）；`Image Opacity` 調圖片濃度。圖片只畫在玻璃面板裡
- **自己的圖片**：把 PNG 或 JPG 放進遊戲資料夾裡的 `aicoder-assist-wallpapers`（第一次開外觀設定時會自動建立），再點 `Image` 就會出現。太大的圖會縮到最長邊 2048

和 Meteor 版液態玻璃的差別：顏色是用六條滑桿調（這裡沒有做調色盤）；沒有背景模糊的開關（原版在開啟畫面時本來就會把世界模糊）。

實測（Linux、原版 1.21.11）：切到 Glass、兩種版面、STYLE 視窗、換成自己放進資料夾的圖、拖動顏色滑桿、關閉後設定有存檔。

## 小遊戲（GAMES）
選單上方的 `GAMES` 會把模組清單換成小遊戲，再按一次或按 `WINDOW`／`COLUMNS` 回到模組。Minecraft 本身照常在背景運作（選單不會暫停遊戲）。外觀跟著 STYLE 的極光／液態玻璃與顏色走。

| 遊戲 | 玩法 | 操作 |
|---|---|---|
| Drop | 落下的四格方塊，排滿一列就消除 | 方向鍵移動與旋轉、下鍵加速、空白鍵直落、C 暫存 |
| Trail | 吃點變長，撞牆或撞到自己結束 | 方向鍵或 WASD |
| Sweep | 打開所有安全的格子，數字是周圍的地雷數 | 左鍵打開、右鍵標記 |
| Merge | 往一個方向滑，相同的數字合併成兩倍 | 方向鍵或 WASD |
| Bounce | 用板子接球，打掉所有磚塊 | 滑鼠或方向鍵移動、空白鍵或點擊發球 |
| Dig | 2D 側視沙盒：隨機地形、洞穴、三種礦石，可以挖和放 | A／D 走、W 或空白鍵跳、按住左鍵挖、右鍵放、1～9 或滾輪選格、連按兩次 N 換新世界 |

共通：`P` 暫停、`R` 重新開始（Dig 除外）。最高分存在遊戲資料夾的 `aicoder-assist-games.properties`；Dig 的世界存在 `aicoder-assist-dig.dat`（離開遊戲、關選單時存檔，另外每 30 秒自動存一次）。

Dig 目前有的：360×128 格的世界、樹、沙地、洞穴、越深越多的礦石、九格快捷列、燈。沒有的：合成、怪物、日夜、水。

實測（Linux、原版 1.21.11）：六個遊戲都開起來玩過幾步；Dig 挖了三格、放回一格、關選單後存檔有寫出。
