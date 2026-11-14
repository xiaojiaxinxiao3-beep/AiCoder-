# AiCoder 注入式客戶端（實驗）

目標：不靠 Fabric、不靠 Meteor，把我們的模組和介面注入到**已經在執行的原版 Minecraft 1.21.11**。

## 現在做到哪裡
只有第一步：證明這條路走得通。`proof-title-screen.jpg` 是實際畫面——原版 1.21.11（沒有任何模組載入器）啟動後，
注入器把程式送進去，遊戲左上角畫出 `AiCoder injected | tick N | FPS`，tick 數會跟著遊戲走。

還沒有任何模組、沒有介面、沒有 `.exe`。

## 怎麼運作
- `Injector`：用 JDK 的 Attach API 找到執行中的 Minecraft，要求它的 JVM 載入 `aicoder-payload.jar`。
- `Agent`：在遊戲裡被呼叫後，用 ASM 改寫幾個已載入的遊戲類別，在方法開頭或 return 前插入一行對 `Hook` 的呼叫。
  執行中的 JVM 只允許改方法內容，不能加欄位或方法，所以所有掛鉤都是這種形式。
- `Hook`：被遊戲呼叫的地方。目前只數 tick、在畫面上寫一行字。
- `Names`：遊戲類別和方法的混淆名稱。`tools/names.py` 從 Mojang 官方的對照表（`client.txt`）查出來，
  建置時寫進 `names.properties`。對照表不放進 repo，網址在 Mojang 的版本資訊裡。

## 已知問題
- `Gui.render`（遊戲內 HUD）這個掛鉤改寫後驗證失敗（`VerifyError`），所以目前只有選單畫面看得到那行字，進世界後看不到。
- 目前用反射呼叫遊戲方法。模組一多就該改成「對著 Mojang 名稱編譯，再整包轉成混淆名稱」。
- Java 21 以後動態載入 agent 會印警告，未來的 Java 版本預設會禁止。
- 只在 Linux 上、用手動啟動的原版客戶端測過。官方啟動器、Windows 都沒測。

## 不做的事
不做任何躲避偵測的功能（隱藏自己、清除痕跡、繞過伺服器或啟動器的檢查）。

## 在這個環境重跑
`scripts/` 裡是當時用的腳本，路徑寫死在暫存目錄，要先改路徑。順序是：`build.sh`（編譯並打包）→
`run.sh 名稱`（啟動原版、注入、截圖）。原版客戶端的 classpath 取自 Loom 的快取，另外要加上 LWJGL 的 `natives-linux` jar。
