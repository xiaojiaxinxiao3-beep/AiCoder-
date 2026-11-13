# Minecraft 1.21.11 版

這個資料夾是七個 addon 的 **Minecraft 1.21.11** 版本。原始碼不是手寫的，而是由 `port.py` 從上層的 26.3 版原始碼轉出來的
（26.3 用 Mojang 的命名，1.21.11 用 Yarn 命名，Meteor 的 API 也略有不同）。

```
python3 mc-1.21.11/port.py          # 在 repo 根目錄執行，重新產生全部七個
cd mc-1.21.11/glass-columns
./gradlew build                     # 產物在 build/libs/，檔名結尾是 -1.21.11.jar
```

- 要改功能或外觀：改上層的 26.3 版原始碼，再重跑 `port.py`。**不要直接改這裡的 `src/`**，下次重跑會被蓋掉
- 建置時 Gradle 本身要用 JDK 25 執行（Loom 的要求），編譯目標是 Java 21；系統語系要是 UTF-8
- 需要 1.21.11 版的 Meteor Client
