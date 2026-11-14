# AiCoder client

Meteor Client addon（Minecraft 26.3）。目前只包含一個賽博龐克風格的 GUI 主題，會直接取代 Meteor 預設主題。

![preview](design/cyber-gui-still.png)

## 建置

需要 JDK 25。

```
./gradlew build
```

產物在 `build/libs/aicoder-client-<version>.jar`，和 Meteor Client 一起放進 `mods/` 即可。

## 使用

- 點模組列：選取，右欄顯示設定
- 點六角形，或對模組列按右鍵：開關模組
- `Ctrl+F`：搜尋模組與設定
- 建議開啟 Meteor 的 **Blur** 模組，玻璃視窗後方會有模糊效果
- 顏色、背景壓暗程度、資料雨與光點都可以在 GUI 分頁調整

同一時間只能安裝一個取代 Meteor 主題的 addon。啟動時 log 會出現一筆
`Theme with the name 'Meteor' has already been added.`，這是取代主題時的正常訊息。

設計稿與實作說明見 `CLAUDE.md` 與 `design/`。

---

# AiCoder 地球科學（`earth-science/`）

第二個獨立的 Meteor addon，產出 `AiCoder地球科學.jar`。內含十個可以來回切換的 GUI 主題：

| # | 主題 | # | 主題 |
|---|---|---|---|
| 01 | 地層 Strata | 06 | 大氣 Atmosphere |
| 02 | 火山 Magma | 07 | 地震儀 Seismograph |
| 03 | 冰河 Glacier | 08 | 礦物晶體 Geode |
| 04 | 深海 Abyss | 09 | 衛星地球 Orbit |
| 05 | 地形圖 Topography | 10 | 琥珀化石 Amber |

## 建置

```
cd earth-science
./gradlew build
```

產物在 `earth-science/build/libs/AiCoder地球科學.jar`。檔名含中文，建置時的系統語系要是 UTF-8
（例如 `LC_ALL=C.UTF-8`），否則 Gradle 寫不出這個檔名。

## 切換主題

- Modules 視窗標題列右側的 `◀ 03/10 ▶`：上一個／下一個主題，會循環
- 或在 GUI 分頁的「Theme」下拉選單直接選（名稱為 `AiCoder 01 Strata` … `AiCoder 10 Amber`）
- 第一次安裝會自動切到 01 地層；Meteor 原本的主題仍在下拉選單裡
- 每個主題的顏色各自存檔，GUI 分頁的「Backdrop Opacity」可調整背景遮住遊戲畫面的程度（0 = 關閉背景）

操作方式和賽博龐克主題相同：點模組列選取、點開關圖示或按右鍵開關模組、`Ctrl+F` 搜尋。

主題的中文名稱只有在關閉 Config 的「Custom Font」時才會顯示在標題列（Meteor 的自訂字型沒有中文字）。

---

# AiCoder 液態玻璃（`liquid-glass/`）

第三個獨立的 Meteor addon，產出 `AiCoder液態玻璃.jar`：圓角玻璃視窗，兩種顏色由左上漸層到右下。

```
cd liquid-glass
./gradlew build      # 系統語系要是 UTF-8，產物在 build/libs/AiCoder液態玻璃.jar
```

在 GUI 分頁最下方的 **Liquid Glass** 區塊調整：

| 設定 | 作用 |
|---|---|
| Color | 開＝用下面兩個顏色上色；關＝沒有顏色的透明玻璃 |
| Color 1 / Color 2 | 玻璃左上／右下的顏色 |
| Glass Opacity | 玻璃的實心程度，越低越透 |
| Liquid Motion | 玻璃內緩慢流動的光斑 |
| Auto Blur | 開啟 GUI 時自動打開 Meteor 的 Blur 模組，讓玻璃後方的畫面變模糊 |
| Backdrop Dim | 遊戲畫面壓暗的程度 |

主題名稱是 `AiCoder Glass`，第一次安裝會自動選用；Meteor 原本的主題仍在 Theme 下拉選單裡。
操作方式和另外兩個 addon 相同：點模組列選取、點圓形開關或按右鍵開關模組、`Ctrl+F` 搜尋。

---

# AiCoder 多欄賽博龐克（`columns/`）

第四個獨立的 Meteor addon，產出 `AiCoder多欄賽博龐克.jar`：沿用 Meteor 原本「每個分類一欄、左右並排」的版面，換成賽博龐克外觀，並加上開關音效。

```
cd columns
./gradlew build      # 系統語系要是 UTF-8，產物在 build/libs/AiCoder多欄賽博龐克.jar
```

- 左鍵點模組＝開關（開是上升的合成音、關是下降的），右鍵＝開啟該模組的設定視窗
- 滑鼠停在模組上會在旁邊出現說明卡：名稱、說明、編號與開關狀態
- 多了一個青色的 **AICODER** 分類欄，目前裡面沒有模組
- 放不下的視窗（Search、Favorites）會排到最短那一欄的下面；欄位都可以拖曳，位置會記住
- GUI 分頁的 **Cyber** 區塊可以關掉音效（Toggle Sound）或調音量（Sound Volume），也能調背景壓暗、資料雨、光點

這個主題和第一個「賽博龐克」一樣是**取代** Meteor 原本的主題，兩個不能同時安裝。

---

# AiCoder 液態玻璃直立式（`glass-columns/`）

第五個獨立的 Meteor addon，產出 `AiCoder液態玻璃直立式.jar`：液態玻璃的外觀，配上「每個分類一欄」的直立式版面，開關模組時有水滴落在玻璃上的音效。

```
cd glass-columns
./gradlew build      # 系統語系要是 UTF-8，產物在 build/libs/AiCoder液態玻璃直立式.jar
```

- 左鍵點模組＝開關（開是往上的水滴聲、關是往下的），右鍵＝開啟該模組的設定視窗
- 滑鼠移到模組上會有一聲輕敲玻璃的聲音，每個模組的音高略有不同；可用 Hover Sound 關掉
- 滑鼠停在模組上會在旁邊出現說明卡
- 顏色、透明度、音效開關與音量都在 GUI 分頁的 **Liquid Glass** 區塊，和原本的液態玻璃相同，多了 Toggle Sound 與 Sound Volume
- 主題名稱是 `AiCoder Glass Columns`，可以和原本的液態玻璃一起安裝，在 Theme 下拉選單切換

---

# AiCoder 極光玻璃（`aurora-glass/`）與極光玻璃直立式（`aurora-columns/`）

第六、第七個獨立的 Meteor addon：夜空、極光光帶加上液態玻璃面板。設計稿在 `aurora-glass/design/`，交接文件是 `aurora-glass/CLAUDE.md`。

- `aurora-glass/` → `AiCoder極光玻璃.jar`：單視窗三欄（分類｜模組｜設定）。點模組列是選取，點右邊的開關或按右鍵是開關模組，`Ctrl+F` 搜尋。
- `aurora-columns/` → `AiCoder極光玻璃直立式.jar`：每個分類一欄。左鍵開關、右鍵設定，停留時顯示說明卡，沿用直立式液態玻璃的音效。

```
cd aurora-glass      # 或 aurora-columns
./gradlew build      # 系統語系要是 UTF-8，產物在 build/libs/
```

- 主題名稱是 `AiCoder Aurora`、`AiCoder Aurora Columns`，第一次安裝會自動切換過去，之後可以在 GUI 分頁的 Theme 下拉選單換回來。
- GUI 分頁的「Aurora Glass」群組：`Animations`（關掉就是靜止的定稿樣子）、`Blur`（開啟 Meteor 的 Blur 模組；關掉時玻璃會變得比較不透明）、`Night Sky`（夜空蓋住遊戲畫面的程度）。
- 版面以 1920×1080 為基準，遊戲視窗比較小的時候整個 GUI（包含 Meteor 內建的元件）會等比縮小，最小到 60%。
- 1.21.11 版在 `mc-1.21.11/aurora-glass/`、`mc-1.21.11/aurora-columns/`，一樣由 `port.py` 產生。

# AiCoder 戰鬥巨集（`combat-macros/`）

不是主題，而是七個 Meteor 模組，放在自己的 **AiCoder 分頁**裡（在 Modules、Config… 的旁邊），產出 `AiCoder戰鬥巨集.jar`；1.21.11 版在 `mc-1.21.11/combat-macros/`。
共同原則：只做「幫你按按鍵」的事——切快捷列、對準心指到的東西按右鍵或左鍵、真的轉動視角。送出的封包和原版客戶端按同樣按鍵時一樣。

| 模組 | 做什麼 | 主要設定 |
|---|---|---|
| `anchor-macro` | 你親手放下重生錨後，切到螢光石補一格、切回來再點一下引爆 | `switch-delay`、`click-delay`、`random-delay`、`explode` |
| `crystal-macro` | 你放下終界水晶後，準心在水晶上時幫你按左鍵。按住右鍵就會一直放、一直炸。開 `auto-obsidian` 後，點到不能放水晶的方塊會先從快捷列放一塊黑曜石，再把水晶放上去 | `attack-delay`、`random-delay`、`only-my-crystals`、`auto-obsidian`、`obsidian-delay`、`crystal-delay` |
| `crosshair-drift` | 設定距離內有玩家時，視角慢慢轉向他（有速度上限，靠近時減速，準心到了就放手） | `range`、`speed`（度／秒）、`softness`、`cone`、`aim-at` |
| `right-clicker` | 長按右鍵超過 `start-after` 之後變成連點右鍵，放開就停 | `start-after`、`interval`（1 = 每秒 20 下，2 = 每秒 10 下）、`random-delay` |
| `left-clicker` | 同上，但是左鍵；和右鍵是兩個獨立的模組。預設準心在方塊上時不連點，所以長按仍然可以正常挖方塊 | `start-after`、`interval`、`random-delay`、`not-on-blocks` |
| `mace-swap` | 拿著劍、斧、長矛或三叉戟打人時，如果快捷列有重錘，出手前先切到重錘，幾個 tick 後切回來。這一擊以重錘為武器（重擊加成、附魔），但用的是原本武器的攻擊力和蓄力 | `swap-back-delay`、`min-fall`、`charged-only`、`players-only`、`weapons-only` |
| `wind-pearl` | 丟出終界珍珠後自動補丟風彈，預判兩者在空中相遇，珍珠在那裡觸發 | `delay`、`extra-lead`、`ping-compensation`、`report-result` |

- AiCoder 分頁用的是目前主題的模組畫面，所以換主題它也跟著換；裡面分三欄：Explosives（Anchor Macro、Crystal Macro）、Melee（Crosshair Drift、Left Clicker、Mace Swap）、Utility（Right Clicker、Wind Pearl）。這三欄不會出現在原本的 Modules 分頁。
- 重生錨、螢光石、水晶、黑曜石、重錘、風彈都要放在**快捷列**；模組不會開背包搬東西。
- `anchor-macro` 在地獄不會動作（那裡點重生錨是設重生點）；按住潛行時也不動作。
- `wind-pearl`：珍珠碰到風彈的範圍只有大約半格，所以時機很重要。在流暢的單人世界裡從接近水平到垂直都打得中；
  延遲高或掉幀時平拋比較容易錯過，往上丟最穩。可以開 `report-result` 看風彈是從珍珠前面還是後面經過，再調 `extra-lead`。
  `delay` 越大，相遇點越遠（往上丟就是越高）。

```
cd combat-macros
./gradlew build      # 系統語系要是 UTF-8
```

# 壁紙（液態玻璃、液態玻璃直立式）

兩個液態玻璃 addon 的面板裡可以顯示一張圖片：圖片以「鋪滿整個畫面」的方式排版，但只有面板內看得到，
所以每一欄像是同一張圖的窗口，面板以外仍然是遊戲畫面。

- 內建一張 `Crimson Scythe`，預設啟用
- 加自己的圖：把 PNG 或 JPG 放進遊戲資料夾的 `meteor-client/aicoder-wallpapers/`（開過一次 GUI 後會自動建立），
  回到 GUI 分頁，在 **Wallpaper** 區塊的下拉選單選它。兩個 addon 共用這個資料夾
- `Wallpaper` 選 `None` 就是沒有圖片的純玻璃
- `Wallpaper Opacity`：圖片的濃淡；`Glass Opacity`（Liquid Glass 區塊）：蓋在圖片上的玻璃顏色有多實

---

# Minecraft 1.21.11 版（`mc-1.21.11/`）

五個 addon 都有 1.21.11 的版本，功能和 26.3 版相同，jar 檔名結尾是 `-1.21.11.jar`。
原始碼由 `mc-1.21.11/port.py` 從 26.3 版轉出，說明見 `mc-1.21.11/README.md`。
