AiCoder Assist 臨時授權伺服器＋授權版客戶端
==========================================

這一包是讓你在「自己的電腦上」把整個授權流程跑一遍用的。
伺服器只聽 127.0.0.1，所以只有同一台電腦上的遊戲連得到。


裡面有什麼
----------
server\
  start-server.bat        啟動伺服器（視窗開著就是在跑，關掉視窗就停）
  LicenseServer.jar       伺服器本體
  data\licenses.txt       有效的序號，一行一個（已經放了 5 組）
  data\signing-key.pkcs8  伺服器的私鑰（簽憑證用，不要給別人）
  data\public-key.txt     對應的公鑰（已經放進這一包的客戶端裡）
  data\bindings.properties 伺服器自己寫的：哪個序號綁在哪台電腦（第一次綁定後才會出現）

client-1.21.11\  和  client-1.21.4\
  AiCoderAssist(.jar/.bat)     授權版客戶端，已設定成連 http://127.0.0.1:8765/verify
  AiCoderAssist-license.txt    授權檔：裡面是一組序號（AICODER-BUTST-PPDGB-5EBDF）


怎麼用
------
1. 執行 server\start-server.bat，看到 listening on http://127.0.0.1:8765/verify 就是好了。
   它需要 Java。找不到的話先開 Minecraft 再執行一次，會借用遊戲的 Java。
2. 開 Minecraft（1.21.11 或 1.21.4，用對應的資料夾），進到世界。
3. 執行該資料夾裡的 AiCoderAssist(.bat)。
4. 聊天欄會出現 [Licence] Licence activated ...（第一次）或 Licence valid.（之後）。
   伺服器視窗會同時印出 status=BOUND 或 status=OK。


想試其他情況
------------
- 換序號：改 AiCoderAssist-license.txt 裡的那一行（從 licenses.txt 挑一組），
  並把遊戲資料夾裡的 aicoder-assist-license.txt 刪掉，再重新注入。
- 無效序號：填一組不在 licenses.txt 裡的。
- 過期序號：在 licenses.txt 的序號後面加 ,1000（例如 AICODER-XXXXX-XXXXX-XXXXX,1000）。
- 「已綁其他電腦」：把 data\bindings.properties 裡那個序號等號後面的 64 個字元改掉任何一個。
- 解除綁定：刪掉 bindings.properties 裡那一行。
- 新增序號：在 licenses.txt 加一行（只能用英數字、- 和 _）。改完不用重開伺服器。
- 伺服器關掉：已經驗證過的電腦 3 天內還能用，之後會鎖住。


要注意
------
- 這一包的客戶端只能連這台電腦上的伺服器。要給別人用，需要把伺服器放到別人連得到的地方，
  並用那個位址和公鑰重新建置客戶端（見原始碼 inject/README.md 的「授權」一節）。
- 私鑰和這幾組序號是臨時產生的測試用資料。正式販售時請重新產生
  （刪掉 signing-key.pkcs8 和 public-key.txt 再啟動伺服器，就會產生新的一對）。
- 伺服器沒有 HTTPS、沒有請求次數限制，不適合直接放到網路上。
- 啟動腳本和硬體指紋在 Windows 上還沒有實機測過。整個流程是在 Linux 上驗證的。
