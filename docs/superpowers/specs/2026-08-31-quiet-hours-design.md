# 夜間の通知抑止 (Quiet Hours) 設計

- Issue: https://github.com/SlashNephy/tokidoki-roppou/issues/109
- 日付: 2026-08-31

## 背景と目的

通知間隔を 30 分などの短い値にしていると、就寝中に通知が蓄積し、起床時に大量の未読通知が並ぶ。
これを避けるため、ユーザーが指定した時間帯は条文通知を送らないようにする。

通知チャンネルは `IMPORTANCE_LOW` かつ `setSound(null, null)` であり、夜間に音やバイブで起こされる問題は元々存在しない。
解決したいのは「通知が溜まること」であって「通知音」ではない。

### 採用しなかった案

- **通知 ID を固定して置き換える**: 通知が常に 1 件になるため蓄積は解消するが、条文ごとに通知を積む挙動は意図した設計であり、変更しない。
- **端末のサイレントモード (DND) 連動**: `NotificationManager.getCurrentInterruptionFilter()` は通知ポリシーアクセス (`ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`) の特別な許可を必要とし、未許可では `INTERRUPTION_FILTER_UNKNOWN` を返す。条文通知アプリが要求する権限として重い。`AudioManager.getRingerMode()` は Android 10 以降 DND と連動しないため代替にならない。就寝時間モードには公開 API がない。時刻判定は権限不要・決定的・ユニットテスト可能であり、こちらを採用する。将来 DND を足す場合は「時間帯 OR DND」の OR 条件として後付けできる。

## スコープ

### 含む

- 指定時間帯に `ArticleNotificationWorker` が通知を送らないようにする
- 設定画面での有効/無効の切り替えと、開始・終了時刻の指定

### 含まない

- ウィジェット更新の抑止 (`ArticleWidgetWorker` は変更しない)。ウィジェットは音を鳴らさず通知としても蓄積しないため。
- 抑止した通知の埋め合わせ (明け方にまとめて送る等)。抑止期間中は単純にスキップし、次の周期を待つ。
- 通知 ID の設計変更

## 設計

### 1. ドメイン (`core:domain`)

```kotlin
data class QuietHours(
    val startMinutesOfDay: Int,
    val endMinutesOfDay: Int,
) {
    fun contains(minutesOfDay: Int): Boolean
}
```

時刻は「その日の 0:00 からの経過分」(0..1439) で表現する。DataStore に Int で素直に保存でき、日跨ぎの判定も単純な比較で書ける。

判定規則:

| ケース | 条件 | 挙動 |
| --- | --- | --- |
| 通常区間 (`start < end`) | `start <= t && t < end` | 抑止 |
| 日跨ぎ区間 (`start > end`) | `t >= start \|\| t < end` | 抑止 |
| 空区間 (`start == end`) | — | 抑止しない |

- 開始時刻ちょうどは抑止し、終了時刻ちょうどは抑止しない (半開区間)。
- `start == end` を「24 時間抑止」ではなく「抑止しない」と定義するのは、通知が丸一日止まる事故を避けるため。UI 側でも同値の保存を弾く。
- 判定は端末のローカル時刻で行う。

`ApplicationSettings` に以下を追加する。

```kotlin
val isQuietHoursEnabled: Boolean = true,
val quietHours: QuietHours = QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60),
```

デフォルトは有効 (23:00〜7:00)。既存ユーザーもキー未設定のためデフォルトが適用される。

### 2. 永続化 (`core:data`)

`ApplicationSettingsRepositoryImpl` に DataStore キーを追加する。

- `quiet_hours_enabled` (Boolean, 既定 `true`)
- `quiet_hours_start_minutes` (Int, 既定 `1380`)
- `quiet_hours_end_minutes` (Int, 既定 `420`)

`ApplicationSettingsRepository` に以下を追加する。

- `suspend fun setQuietHoursEnabled(enabled: Boolean)`
- `suspend fun setQuietHours(startMinutesOfDay: Int, endMinutesOfDay: Int)`

開始と終了は片方だけ変えると一時的に不整合な状態になりうるため、1 つのメソッドでまとめて書き込む。

### 3. Worker (`core:data`)

`ArticleNotificationWorker.doWork()` の `isNotificationEnabled` チェック直後に判定を挿入する。

```kotlin
if (settings.isQuietHoursEnabled && settings.quietHours.contains(clock.nowMinutesOfDay())) {
    Timber.d("In quiet hours, skipping")
    return Result.success()
}
```

条文の抽選前に返すため、抑止時は DB も API も触らない。
`ArticleNotificationScheduler` は変更せず、WorkManager の周期実行はそのまま回す。抑止時間帯が終われば次の周期から通常どおり通知される。

現在時刻は `java.time.Clock` を Hilt で注入して取得する (minSdk 28 のため `java.time` が使える)。テストで固定クロックを差し込めるようにするため。

### 4. 設定 UI (`feature:settings`)

`SettingsScreen` の「通知」セクション、通知間隔の下に追加する。

- `SettingItem`「夜間は通知しない」+ `Switch` (`Icons.Default.Bedtime`)。supporting は現在の時間帯 (例: `23:00 〜 7:00`)
- 有効時のみ「開始時刻」「終了時刻」の 2 項目を表示し、それぞれ supporting に `HH:mm` を表示。タップで Material3 `TimePicker` を載せた `AlertDialog` を開く
- 開始と終了が同値になる保存は弾く (ダイアログの確定ボタンを無効化する)

### 5. データフロー

```
SettingsScreen -> SettingsScreenViewModel -> ApplicationSettingsRepository -> DataStore
                                                                                |
                                                      ArticleNotificationWorker -+
                                                                                |
                                                          QuietHours.contains(now)
                                                                                |
                                                    抑止 -> Result.success() で終了
                                                    通常 -> 条文を抽選して通知
```

## エラー処理

- 保存済みの時刻が範囲外 (0..1439 の外) だった場合はデフォルト値にフォールバックする。手で DataStore を壊した場合や将来の仕様変更に対する保険。
- 抑止時は `Result.success()` を返す。`Result.failure()` や `retry()` は WorkManager のバックオフを誘発し、抑止解除後のスケジュールを乱すため使わない。

## テスト

- `QuietHoursTest` (`core:domain`) — 通常区間、日跨ぎ区間、開始時刻の境界 (抑止する)、終了時刻の境界 (抑止しない)、空区間 (抑止しない)
- `ApplicationSettingsRepositoryImplTest` — 追加キーの読み書きと、未設定時のデフォルト値、範囲外値のフォールバック

## 検証

- `./gradlew test` と `./gradlew lintStagingDebug`
- 実機で設定画面のスクリーンショット (before / after)
- 抑止時間帯に入る設定にした状態で通知 Worker を実行し、通知が出ないことを確認する。時間帯を外した状態では通知が出ることも併せて確認する
