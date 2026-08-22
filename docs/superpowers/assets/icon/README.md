# アプリアイコンのアセット

ランチャーアイコンの図像の出所は `app/src/main/res` の Adaptive Icon（`drawable/ic_launcher_foreground.xml` と `@color/ic_launcher_background`）で、このディレクトリに図像そのものは置いていない。ここにあるのは、そこからレガシー mipmap を焼き出すための道具だけ。

| ファイル | 役割 |
| --- | --- |
| `vector_to_svg.py` | Android Vector Drawable の `pathData` / `fillColor` を読み出して SVG に載せ替える。想定外の要素・属性が現れたらエラーで落ちる |
| `generate_mipmaps.sh` | 上記を経由して `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png`（48 / 72 / 96 / 144 / 192 px）を再生成する |

## レガシー mipmap を出荷している理由

minSdk は 28 なので、端末上は `mipmap-anydpi-v26` の Adaptive Icon だけで足りる。レガシー mipmap は端末のためではなく、APK からアイコンを取り出す外部のコンシューマ（Firebase App Distribution など）のために置いている。それらは Adaptive Icon の XML を解釈せず、密度別のビットマップを探すため、これが無いとアイコンが表示されない。

同じ理由で出力は PNG にしている。WebP を解釈できないコンシューマがあるため。

端末上の見た目には影響しない。`android:icon="@mipmap/ic_launcher"` の解決では、API 26 以降は常に `mipmap-anydpi-v26` が優先される。

## 使い方

`rsvg-convert`（librsvg）と `magick`（ImageMagick 7）が必要。

```bash
./docs/superpowers/assets/icon/generate_mipmaps.sh
```

Adaptive Icon を変更したら再実行して、出力をコミットする。同じ入力からはバイト単位で同じ PNG が出る。
