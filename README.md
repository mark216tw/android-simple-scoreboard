# 簡單記分板

一款以簡單、直覺與容易操作為目標的 Android 雙隊記分板。適合球類運動、桌遊、課堂競賽及各種需要快速記錄比分的活動。

> [!WARNING]
> `1.1.0-prerelease` 是 **Prerelease 測試版本**，不是正式發行版本。APK 已啟用 R8 與資源壓縮，但仍使用 Android Debug 金鑰簽署，僅供功能測試與意見回饋。

## 主要功能

- 點擊隊伍的大型分數區即可加一分
- 每隊提供 `歸零`、`−1`、`+3`、`+2` 快速計分按鈕
- 計分時播放清楚的短促提示音，可於設定中關閉
- 每筆紀錄保存操作內容、主客隊名稱與分數、當下局數及時間
- 計分紀錄可切換詳細格式或主隊分數、客隊分數、局數、時間的表格格式
- 支援依目前格式複製全部紀錄，或透過 Android 系統分享至其他 APP
- 自訂兩隊團隊名稱
- 支援隱藏局數、顯示目前局數或顯示雙方局數
- 一鍵交換團隊名稱、比分、雙方局數與隊伍顏色
- 重設本場比分、局數與計分紀錄，並保留名稱及外觀設定
- 支援直式與橫向自適應版面
- 工具列可循環切換跟隨系統、鎖定直式及鎖定橫式
- 提供全螢幕模式，隱藏手機狀態列與底部導覽列並保留 APP 工具列
- 提供活力橘、海洋藍、薄荷綠、葡萄紫、檸檬黃及紅藍對決六組主題
- 提供自訂色相滑桿，即時產生主隊色及客隊互補色
- 提供跟隨系統、淺色及深色三種顯示模式
- 主題與顯示模式選擇後立即套用
- 系統狀態列及導覽列圖示會配合淺色或深色介面
- 可設定保持螢幕常亮，預設為開啟
- 使用 DataStore 自動保存比分、局數、團隊名稱與 APP 設定
- 支援 Android Adaptive Icon 與 Android 13 單色主題圖示

## 操作方式

1. 點擊團隊名稱可進行編輯。
2. 點擊大型分數數字區增加一分。
3. 點擊隊伍下方的 `歸零`、`−1`、`+3`、`+2` 按鈕快速調整分數，分數最低為零。
4. 點擊自動旋轉圖示可循環切換跟隨系統、鎖定直式與鎖定橫式。
5. 使用局數列兩側的 `+`、`−` 調整局數。
6. 點擊交換圖示可交換兩隊的名稱、比分、局數與顏色。
7. 點擊全螢幕圖示可隱藏手機系統列，再次點擊同一位置的退出圖示即可恢復。
8. 點擊更多圖示可開啟計分紀錄、重設本場或 APP 設定。

計分紀錄只保留於本次 APP 執行期間；手機旋轉時會保留，但 APP 程序被關閉或系統回收後會清除。
局數會依計分當下的顯示模式記錄為「第 N 局」或「局數 A：B」；隱藏局數時顯示「局數未啟用」，後續調整局數不會修改舊紀錄。
每隊的「歸零」只清除該隊分數並新增一筆紀錄，Snackbar 提供短時間復原；複製或分享全部紀錄時會依最早到最新排列。表格模式會以 Markdown 表格匯出，格式選擇於本次 APP 執行期間保留。

## 系統需求

- Android 8.0（API 26）或更新版本
- 建議使用支援直式與橫向旋轉的手機或平板

## 取得 Prerelease 測試版

依目前發布安排，`1.1.0-prerelease` APK 僅在本機建置，不上傳至 GitHub Release。請依下方建置方式產生 APK。

安裝前請留意：

- APK 是 `prerelease` Build Type，不是 Google Play 正式版本。
- APK 已啟用 R8 程式壓縮、混淆與資源壓縮。
- APK 使用 Android Debug 金鑰簽署，並非正式 Release 金鑰。
- Android 可能要求允許瀏覽器或檔案管理器「安裝未知應用程式」。
- 未來正式版本更換簽署金鑰後，可能無法直接覆蓋安裝。
- 本版簽署憑證與 GitHub 上一個測試 APK 不同，升級前需先解除安裝舊版。
- 若 Launcher 顯示舊圖示，可先解除安裝舊版本再重新安裝。

## 開發環境

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin 8.9.2
- Gradle 8.11.1
- JDK 17
- Preferences DataStore
- 最低 SDK：26
- 目標 SDK：35

## 專案結構

```text
app/src/main/java/com/example/scoreboard/
├── MainActivity.kt                 # APP 進入點與主題套用
├── ScoreboardApplication.kt        # APP 生命週期背景工作
├── ScoreboardViewModel.kt          # 記分狀態與操作邏輯
├── audio/
│   └── ScoreSoundPlayer.kt         # 低延遲計分提示音
├── data/
│   ├── ScoreboardRepository.kt     # DataStore 持久化
│   └── ScoreboardState.kt          # 狀態與設定模型
└── ui/
    ├── ScoreboardApp.kt            # 主畫面及設定介面
    └── theme/ScoreboardTheme.kt     # 預設及自訂淺色與深色主題
```

## 建置方式

使用 Debug 金鑰簽署並啟用 R8 與資源壓縮，建置不可除錯的測試發行版。

在 Windows PowerShell 執行：

```powershell
.\gradlew.bat assemblePrerelease
```

在 macOS 或 Linux 執行：

```bash
./gradlew assemblePrerelease
```

建置後的 APK 位於：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

## 測試與檢查

Windows：

```powershell
.\gradlew.bat testPrereleaseUnitTest assemblePrerelease lintPrerelease
```

macOS 或 Linux：

```bash
./gradlew testPrereleaseUnitTest assemblePrerelease lintPrerelease
```

## 發行狀態

目前版本為 `1.1.0-prerelease` 測試發行版，APK 不上傳至 GitHub Release。正式發行前仍需完成實機相容性測試、正式套件名稱設定及 Release 簽署。

## 授權

本專案採用 [MIT License](LICENSE) 授權。
