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
