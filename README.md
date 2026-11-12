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
