# AiCoder Assist：注入式客戶端（Minecraft 1.21.11）

不需要 Fabric 也不需要 Meteor。先啟動**原版** Minecraft 1.21.11，再執行 `AiCoderAssist.exe`，
七個模組和兩個極光玻璃介面就會進到正在執行的遊戲裡。在遊戲中按**右 Shift** 開啟選單。選單鍵可以自己改：上方 STYLE → 最下面的 **Menu Key**，點一下再按想要的鍵（Esc 保留原本的）；Insert 永遠也能開，當作後路。存成 `ui.menu-key`（GLFW 鍵碼）。

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

## Wind Jump、Mace Dive 長矛模式、Spear Smash、Offhand Totem
- **Wind Jump**：預設改成「先丟風彈，下一個 tick 才按跳」（`throw-timing` -1）。實測（原版 1.21.11、沒有任何效果）：-1 是 9.1～12.1 格、平均 11；同一個 tick（0）是 6.9～9.1 格、平均 8.7。設定改了名字，所以舊存檔的值不會蓋掉新預設
- **Mace Dive 的 `spear-mode`**：俯衝開始時放煙火並同時把長矛放平（按住使用鍵），之後連續三個 tick：長矛在 2～4.5 格內戳到 → 放開並換胸甲 → 選重錘攻擊。實測（高度 20、攻擊距離 1.99、鑽石長矛）：每輪長矛與重錘都命中，兩輪打死鐵巨人。另外修掉：高度設得比目標搜尋範圍大時，爬升途中會丟失目標
- **重錘用長矛的距離出手**：伺服器接受 3 格再加 3 格緩衝內的攻擊，卡在 3 格的只有客戶端的準心，而拿長矛時準心有 4.5 格。所以接重錘時是拿著長矛按攻擊、在按下的瞬間才選重錘。Spear Smash、Mace Dive 長矛模式、Mace Swap 都是這樣。實測：站在 3.7 與 4.2 格外，命中的是重錘（扣的是重錘的耐久）
- **Spear Smash**：手拿長矛、正在下落（`min-fall` 預設 1.5）時戳到人（左鍵戳刺或按住放平都算）→ 下一個 tick 選重錘並按攻擊，之後切回長矛；穿著鞘翅時先換上快捷列的胸甲。實測：從約 11 格落下，鐵巨人 100 → 74。只有 1.21.11（1.21.4 沒有長矛）
- **Offhand Totem**：副手沒有圖騰時，按物品欄鍵 → 把游標移到圖騰那一格 → 對該格按「與副手交換」鍵 → 關閉。真的會開物品欄、真的移動游標。`delay` 是各步驟之間的 tick 數；游標移過去後遊戲要到下一個畫面才認得，所以最少仍要等 2 tick。實測：原版 1.21.11、原版 1.21.4、Fabric 1.21.11 都能換上，整個過程不到半秒
- 測試時發現的陷阱：測試角色身上如果有緩降效果，伺服器的掉落距離永遠是 0，重錘就沒有落下加成（和模組無關）

## 授權（一機一碼）
預設**沒有**授權檢查。建置時給了伺服器位址和公鑰才會開：

```
LICENSE_URL=https://你的伺服器/verify LICENSE_PUBKEY=<公鑰> ./build.sh
```

開了之後，沒有有效授權時所有模組都開不起來（選單還是能開），原因會顯示在聊天欄。

- **序號從哪來**：注入器會讀 jar 旁邊的 `AiCoderAssist-license.txt`；沒有這個檔案、又是在命令視窗裡執行時，會問一次並存起來。也可以直接把序號寫在遊戲資料夾的 `aicoder-assist-license.txt`（一行）
- **硬體指紋**（`license/HwidProvider.java`）：Windows 取 MachineGuid、主機板 UUID、處理器 ID；macOS 取 IOPlatformUUID 與序號；Linux 取 machine-id、主機板 UUID 與序號、處理器型號。全部一起做 SHA-256，只有雜湊值會送出去。不取使用者名稱、電腦名稱、檔案路徑、網路位址。什麼都讀不到時回傳 null，不會用假值頂替
- **協定**（`license/License.java` 開頭有完整說明）：POST 表單 `key`、`hwid`、`product`；回應是文字，`status=` 加上 `token=內容.簽章`。狀態：`BOUND`（首次綁定）、`OK`、`HWID_MISMATCH`（已綁別台）、`INVALID`、`EXPIRED`
- **憑證**：內容是 `v1|序號|指紋|到期時間`，伺服器用 Ed25519 私鑰簽，客戶端用建置時放進去的公鑰驗。存在遊戲資料夾的 `aicoder-assist-license.properties`；伺服器連不上時，憑證還沒到期就照用
- **測試用伺服器**：`java tools/LicenseServer.java <資料夾> [埠]`，序號列在該資料夾的 `licenses.txt`（一行一個，可加 `,到期秒數`），啟動時會印出 `LICENSE_PUBKEY`。它只聽 127.0.0.1，是用來測試和說明伺服器該做什麼的；正式上線還需要 HTTPS、請求次數限制、妥善保管私鑰
- **給使用者的臨時測試包**：`LicenseServer.java` 編成 `LicenseServer.jar`（`Main-Class: LicenseServer`），配上 `launcher/start-license-server.bat`（找 PATH 上的 Java、執行中的遊戲用的 Java、或官方啟動器的 runtime）、一個放了序號的 `data/licenses.txt`，以及用 `LICENSE_URL=http://127.0.0.1:8765/verify` 建出來的兩個版本的客戶端和 `AiCoderAssist-license.txt`。官方啟動器的 Java 21 runtime 含 `jdk.httpserver` 與 `jdk.crypto.ec`（查過 `release` 檔），所以伺服器可以直接用遊戲的 Java 跑。這一包在 Linux 上整包跑通過；bat 沒在 Windows 上測過
- **沒有混淆**。這擋的是「一個序號多人共用」，擋不了有心修改程式的人：任何完全在使用者電腦上執行的檢查都能被改掉
- **要告訴買家**：程式會把一個由硬體識別碼算出的雜湊值送到你的伺服器

實測（本機測試伺服器）：
- 原版 1.21.11：沒有序號、首次綁定、再次驗證、已綁其他電腦、過期、無效，六種的訊息與鎖定狀態都正確；序號由注入器的檔案帶入；伺服器關閉時使用已存的憑證；已存的憑證被改一個字元後鎖定；回應由另一把金鑰簽署時不接受
- 原版 1.21.4：有效序號、已綁其他電腦
- Fabric 1.21.11：有效序號、無效序號
- 沒測：Windows 與 macOS 上的硬體指紋（只在 Linux 上跑過）、注入器在命令視窗裡詢問序號的那一段、真正放在網路上的伺服器

## Cart Bomb
火焰弓引爆 TNT 礦車。快捷列要有火焰附魔的弓、箭、鐵軌、TNT 礦車。看著前方幾格的方塊頂面後開啟（綁一個鍵最方便），一次做一輪：

1. 選弓、拉 `Draw`（預設 4）個 tick，同時把視角調到這支慢箭會落在那一格的角度
2. 放開（箭先出去），同一個 tick 內在那一格放鐵軌、再放 TNT 礦車
3. 箭落在礦車上，礦車爆炸

- 爆炸點離你只有幾格，自己也會受傷
- 不會檢查弓有沒有火焰附魔；沒有的話箭不會引爆
- `Draw` 越小越快但射程越短：3 是最少，只到約 2.8 格；4 到約 3.6 格
- 那一格如果已經有鐵軌，直接把礦車放上去
- 實測（Linux 單人世界，原版 1.21.11，每輪清場）：拉 4 與 5 各 8 輪全部引爆；拉 3 射得到的 6 輪全中。原版 1.21.4 測過兩輪（當時是拉 6）。沒有在多人伺服器測過

## Telly Bridge
站在地上、快捷列有方塊、面向要搭橋的方向後開啟（在設定的 BIND 綁一個鍵最方便）。流程：按住前進與疾跑跑到邊緣 → 跳 → 空中把視角轉 180° 朝後、按住後退、對準最後一個方塊的側面按右鍵 → 快落地時轉回正前方 → 落地後繼續。

- `Bridge Mode` 選 **Speed** 是 Speed Telly，按鍵順序照真人的做法：地上按住前進＋疾跑、跳躍鍵一直按著（落地就跳）→ 一離地就用 2 tick 轉身，放開前進按後退，接著按住後退＋左 → 右鍵從開始轉身就用 `Cps`（預設 25）連點到轉回前方為止 → 落地前 2 tick 轉回前方，轉到一半按右，最後按前進。每一跳往上搭一格：第一下點在橋尾那格的頂面（台階），之後從台階的側面往前接，所以橋是一路往上的階梯，每階 3～4 格。回頭時視角是「瞄一次、停一個 tick、再瞄」，不是每一格重新對準。實測（原版 1.21.11，三條全新的路線）：12 秒 71 格、24 秒 152 格、24 秒 144 格，都沒有掉落，左右偏移不到 0.2 格；1.21.4 只確認編譯
- Speed 模式的補充：開啟後第一下一定是跑到方塊最邊緣起跳的普通 Telly（平的），之後才是 Speed Telly。`Rise` 開是每跳往上一格的階梯，關是平的一直線。`Max Pitch`（預設 80）是回頭時視角最多往下看的角度，台階點的是身後那格的頂面；準心對到位置才連點，不會多放方塊。實測（原版 1.21.11）：往上 15 秒 82 格、平的 15 秒 80 格，沒有掉落
- Speed 模式的形狀：`Rise` 開（Speed Telly）時每一層 4 格一段，`Bumps` 開著會在每段第 2 格上多疊一顆。`Rise` 關（一般的 Telly）時橋是平的，回頭後準心以 `Mouse Speed` 慢慢往下、右鍵連點不停，所以有些方塊會疊在橋面上而不是接在前面；`Mouse Speed` 調快凸起就少。實測（原版 1.21.11）：往上 15 秒 87 格、平的 15 秒 82 格，沒有掉落
- **目前的選項以這一條為準**：`Bridge Mode` 的 **Telly** 是一般的 Telly（平的橋）：起跳後先面向前方衝一段，後段才回頭把身後的格子補回來，回頭後準心以 `Mouse Speed`（預設 8）慢慢往下、右鍵連點，所以大約每跳有一顆方塊疊在橋面上。**Speed** 是 Speed Telly（每跳往上一格，`Bumps` 控制每段第 2 格上的那一顆）。`Rise` 開關已經拿掉，舊的「每格精準瞄準」Telly 也拿掉了。實測（原版 1.21.11）：Telly 15 秒 89 格、Speed 15 秒 90 格，沒有掉落
- 轉頭是連續移動的：每個 tick 算出視角的終點，畫面每一幀從上一個位置滑過去。模組開著的期間你自己的滑鼠移動不會生效，關掉後恢復
- 轉向：回頭固定往右轉，轉回前方固定往左轉回來（Telly 與 Speed 都是）。偏離橋的中線時自動按左右鍵修回來，視角保持正前或正後
- **三個模式（以這一條為準）**：**Telly** 是平的橋，往前飛一段才回頭補。**Telly Up** 是一般的 Telly 往上：一跳平著接、一跳早回頭往上搭一層，新的一層疊在上一層尾段上面。**Speed** 是每跳都往上的 Speed Telly。`Turn Ticks`（預設 4）是半圈轉幾個 tick，越大轉得越慢、越看得清旁邊，但留給放方塊的時間越少：大於 2 時 Telly Up 來不及早回頭，橋會是平的；Speed 固定 2。`Mouse Speed` 是回頭時視角往下拉的最快速度，調小會有更多方塊疊在橋面上。實測（原版 1.21.11）：預設下 Telly 與 Telly Up 15 秒 57 格、Speed 94 格；`Turn Ticks` 2 時 Telly Up 15 秒 97 格且有往上；都沒有掉落
- `Top Blocks`（Telly 與 Telly Up，預設 2）：每一跳在橋面上多疊幾格。補方塊時對橋尾那格先點頂面、再點側面；兩者的仰角只差 4～9 度，視角每 tick 只動幾度。`Mouse Speed` 設定已經拿掉。實測（原版 1.21.11）：Telly 15 秒 53 格（`Turn Ticks` 4）或 72 格（2），Telly Up 76 格，沒有掉落
- `Bridge Mode` 選 **Diagonal** 是另一種搭法：全程倒著走，按住後退加一個橫移鍵（`Side` 選左或右），邊跳邊放，不蹲下。這個模式沒有 180 度的大轉身：背對要搭的方向站好再開啟，模組把視角往選的那一邊轉 45 度並鎖住，之後只有上下會動。按前進或潛行停止。實測（原版 1.21.11，全新的路線）：左右兩邊各 15 秒 58 格以上，沒有掉落
- 只走正東／西／南／北（開啟時取最接近的方向），橋是直的；Telly 與 Diagonal 是同一高度，Speed 每跳高一格
- 瞄哪裡是每個 tick 依位置算的，不是固定仰角；放方塊是遊戲自己的右鍵，一個 tick 一下
- 自己按後退鍵或潛行鍵、開啟任何畫面、方塊用完、掉到橋面以下，都會停止並把按鍵還給鍵盤
- 實測（Linux 單人世界）：原版 1.21.11 往南 30 秒約 170 格沒有掉落；Fabric 1.21.4 往西 20 秒約 137 格。沒有在多人伺服器上測過，延遲高時方塊可能被伺服器退回

## 1.21.4 版
同一份原始碼也能建出給原版 **1.21.4** 用的版本，產物是 `work-1.21.4/AiCoderAssist-1.21.4.jar` 與 `.exe`。兩個版本不能混用：注入器會先看遊戲版本，不符就不注入。

```
python3 tools/fetch.py 1.21.4 work-1.21.4     # 從 Mojang 下載該版本的 client jar、對照表、函式庫、資源
V=1.21.4 ./build.sh
V=1.21.4 ./test.sh --quickPlaySingleplayer "New World"
```

和 1.21.11 版的差別：
- **沒有 Spear Lunge**：長矛是 1.21.11 才有的物品。其餘模組都在
- 繪圖方式不同（1.21.4 沒有 1.21.11 的 GUI 繪圖狀態），所以 `gfx/Backend.java` 各有一份：`src/` 的是 1.21.11，`src-1.21.4/` 的是 1.21.4。其他差異（按鍵與滑鼠事件的參數、快捷列欄位、幾個搬過家的套件）由 `tools/port_version.py` 用文字替換處理，每一條替換都必須命中，否則建置失敗

實測（Linux、原版 1.21.4，世界是在遊戲裡新建的）：注入、右 Shift 開關選單、極光與液態玻璃、背景圖、STYLE 視窗、背景模糊、GAMES（Drop、Dig）、Wind Jump（兩次都是 9.1 格）、Trigger Bot（`mobs` 開著打鐵巨人，100 → 44）。其餘模組在 1.21.4 上只確認編譯成功。

## Fabric
同一個 jar 也能注入到用 **Fabric Loader** 啟動的遊戲（1.21.11 與 1.21.4 都可以），不用另外下載。注入器會自己判斷。

Fabric 和原版有兩個差別，注入版原本因此完全對不上：
- 遊戲的類別換成了 intermediary 名稱（`class_310`、`method_1574`），不是原版的混淆名稱
- 遊戲的類別由 Fabric 自己的 class loader（Knot）載入，它看不到注入進來的 jar

做法：建置時把整份客戶端另外轉一份成 intermediary 名稱，放在 jar 裡的 `aicoder/assist/fabric-payload.jar`；掛鉤目標的名稱表也多一份 `names-intermediary.properties`。agent 發現 `net.minecraft.client.main.Main` 不是由系統 class loader 載入時，就把那份寫到暫存檔，用 Fabric 自己加模組的方法（`FabricLauncherBase.getLauncher().addToClassPath`）交給 Knot，再用 intermediary 的名稱表去掛鉤。

```
python3 tools/fetch_fabric.py 1.21.4 work-1.21.4      # Fabric Loader 與 intermediary 對照表
V=1.21.4 ./build.sh
FABRIC=1 V=1.21.4 ./test.sh --quickPlaySingleplayer "New World"
```

實測（Linux、Fabric Loader 0.20.1、**沒有裝任何模組**）：
- 1.21.4：注入、選單、液態玻璃與背景圖、GAMES、Wind Jump（8.3 格）、Trigger Bot（鐵巨人 100 → 44）
- 1.21.11：注入、聊天欄提示、選單
- 兩個版本的同一個 jar 在原版上仍然正常

沒測過的：裝了 Fabric API、Sodium、Iris 或其他模組的 Fabric；Forge、NeoForge、Quilt、OptiFine（不支援，注入器會說明）。
