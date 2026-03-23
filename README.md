# ときどき六法

一定間隔で日本の法令の条文を通知してくれる Android アプリです。日常の中で自然と法律に触れる機会を作ります。

## スクリーンショット

<!-- markdownlint-disable MD033 MD045 -->
<p align="center">
  <img src="image/screenshot_home.png" alt="ホーム画面" width="19%" />
  <img src="image/screenshot_list.png" alt="法令一覧画面" width="19%" />
  <img src="image/screenshot_collection.png" alt="コレクション画面" width="19%" />
  <img src="image/screenshot_settings.png" alt="設定画面" width="19%" />
  <img src="image/screenshot_notifications.png" alt="通知" width="19%" />
</p>
<!-- markdownlint-enable MD033 MD045 -->

## ダウンロード

[GitHub Releases](https://github.com/SlashNephy/tokidoki-roppou/releases/latest) から安定版の APK をダウンロードできます。

### テスト版

Firebase App Distribution でテスト版を配布しています。以下のリンクからテスターとして参加できます。

<https://appdistribution.firebase.dev/i/0276daed965e4d28>

## 機能

- 設定した間隔で条文が通知される (オフラインでも動作)
- 通知タップでアプリ内に条文を表示
- 表示中の条文から参照されている関連条文も併せて表示
- 法令名・条文番号・条文内容で横断検索
- 気になる条文をコレクションに保存
- 全角かっこの半角化・漢数字の算用数字変換オプション
- 法令データは [e-Gov 法令 API](https://laws.e-gov.go.jp/api/2/) からローカルに取得・キャッシュ

## 対応法令

| カテゴリ | 法令 |
| --- | --- |
| 六法 | 日本国憲法、民法、商法、刑法、民事訴訟法、刑事訴訟法 |
| 民法関連 | 借地借家法 |
| 行政法 | 内閣法、国家行政組織法、行政機関情報公開法、公文書等の管理に関する法律、行政手続法、行政代執行法、行政不服審査法、行政事件訴訟法、国家賠償法、地方自治法 |
| 商法関連 | 会社法 |
| 行政書士業務関連 | 行政書士法、戸籍法、住民基本台帳法 |
| 情報関連法 | デジタル行政推進法、個人情報保護法、番号利用法、情報公開・個人情報保護審査会設置法、電子消費者契約法、電子署名法、公的個人認証法 |
| その他 | 道路交通法 |

## 技術スタック

- Kotlin / Jetpack Compose / Material 3
- マルチモジュール構成 (app, core:domain, core:data, core:ui, feature:home, feature:laws, feature:collection, feature:settings)
- Navigation 3、Hilt (DI)、Room (ローカルキャッシュ)、WorkManager (バックグラウンド通知・キャッシュ更新)、DataStore (設定)
- Ktor Client (e-Gov 法令 API)
- GitHub Actions CI / Firebase App Distribution
