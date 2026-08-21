# アプリアイコン刷新 設計

## 背景

現在のランチャーアイコンは `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` が `@drawable/ic_notification` をそのまま foreground に流用している。通知アイコンは 24dp・単色シルエット前提の Material グリフであり、Adaptive Icon の 108dp キャンバスに引き伸ばされているため、安全領域の設計が成立していない。また `<monochrome>` レイヤーが無く、Android 13+ のテーマアイコン設定時にこのアプリだけ色付きのまま浮く。

あわせて、`ic_notification.xml` が `app/src/main/res/drawable/` と `core/data/src/main/res/drawable/` に同名で重複している。内容は現時点で完全に一致する。通知を送出しているのは `core/data` の `ArticleNotificationSender` だが、リソースマージでは app モジュールが優先されるため、実際に表示されているのは app 側の定義である。ランチャー用に流用した副産物と考えられる。

## コンセプト

「六法 × ときどき」の合成。閉じた本を正面から捉え、そこに垂れるしおりを重ねる。しおりは「ときどき開く」という利用のリズムを表す。

しおりの上辺は可視域の上端で断ち切り、V ノッチは安全領域の内側に収める。V ノッチこそが「しおり」の識別記号であるため、これを外周に置くとマスク形状によっては平らに切られ、単なる縦縞に退化する。上辺側は「本の綴じ目から回り込んできた」と読めるため、平らに切れても意味が通る。

## ジオメトリ

キャンバスは 108dp。可視域は中央 72dp（マスク外径）、保証される安全領域は中央 66dp（中心 (54,54) から半径 33dp）。

| 要素 | 座標・寸法 | 色 |
| --- | --- | --- |
| 背景 | 全面 | `#3B5998` |
| 本 | `x=34, y=25, w=40, h=53`, 角丸 3dp | `#F9F9FF` |
| 背（本の左端） | `x=34, y=25, w=7, h=53` | `#B4C5E4` |
| しおり | `x=58, w=12`, 上辺 `y=0` で断ち切り、下端 `y=88`、V ノッチ頂点 `y=81` | `#D8503F` |

本の四隅は中心から 33dp（安全領域ぴったり）に収まる。これが 3:4 比を保ったまま全マスクで無傷に残る上限である。44×58dp に拡大すると四隅が円マスクで削られ、比例して下がるしおりの V ノッチも可視域外に出る。

背景色 `#3B5998` は `LightColorScheme.primary`（`app/src/main/java/blue/starry/tokidokiroppou/TokidokiRoppouTheme.kt`）および既存の `ic_launcher_background` と一致するため、そのまま維持する。

## レイヤー構成

`ic_launcher.xml` を 3 レイヤーに再構成する。

| レイヤー | リソース | 内容 |
| --- | --- | --- |
| background | `@color/ic_launcher_background` | `#3B5998` を維持 |
| foreground | 新規 `app/src/main/res/drawable/ic_launcher_foreground.xml` | 本としおりのみ。背景は含めない |
| monochrome | 新規 `app/src/main/res/drawable/ic_launcher_monochrome.xml` | 本としおりを単色の和集合として描き、背とノッチを透明の隙間で表現する |

monochrome はシステムが単色で塗り潰したうえで拡縮するため、色による分離が一切効かない。本の背としおりの境界は、塗りの隙間（透明のヘアライン）として明示的に描く必要がある。

minSdk は 28 のため `mipmap-anydpi-v26` のみで全端末をカバーでき、レガシーの `mipmap-*dpi` PNG は作成しない。`AndroidManifest.xml` は `android:roundIcon` を参照していないため追加対応は不要。

## 通知アイコン

`core/data/src/main/res/drawable/ic_notification.xml` を新シルエットに差し替える。24dp キャンバス、純白のアルファシルエット（本＋しおり）。ランチャー foreground とは別ファイルとして維持し、24dp 向けに線幅を独立して調整できるようにする。

`app/src/main/res/drawable/ic_notification.xml` は削除する。ランチャーが参照しなくなり、通知の実体は `core/data` 側にあるため、重複を残す理由がない。リソース名 `ic_notification` は据え置くので、`ArticleNotificationSender` 側のコード変更は発生しない。

## 検証

- `./gradlew assembleStagingDebug` および `./gradlew lintStagingDebug` が通ること。
- エミュレータでランチャーとステータスバーの before / after スクリーンショットを取得する。
- テーマアイコンを有効化した状態のランチャーも撮影し、monochrome レイヤーが機能していることを確認する。
- 朱 `#D8503F` と紺 `#3B5998` のコントラスト比は約 1.7:1 であり、本より下に出たしおりの尾が小サイズで沈む懸念がある。44px 相当で視認できるか実機で確認し、沈む場合は朱の明度を上げるか、しおりに白のヘアラインを追加する。この判断は実機確認の結果に従う。

## スコープ外

- Play Store 掲載用の 512×512 PNG。マスクされない前提で角丸を自前で描く別アセットであり、今回は作成しない。
