#!/usr/bin/env bash
# レガシー mipmap（Adaptive Icon を解釈しない消費者向け）を再生成する。
#
# 図像の出所は app/src/main/res の Adaptive Icon そのもので、
#   foreground: drawable/ic_launcher_foreground.xml
#   background: @color/ic_launcher_background (values/colors.xml)
# を vector_to_svg.py で SVG 化してラスタライズする。別途 SVG を持たないので、
# Adaptive Icon を直せばレガシー側もこのスクリプトの再実行だけで追従する。
#
# 出力は WebP ではなく PNG。Firebase App Distribution など APK からアイコンを
# 取り出す外部のコンシューマには WebP を解釈できないものがあるため。
set -euo pipefail

dir="$(cd "$(dirname "$0")" && pwd)"
res="$dir/../../../../app/src/main/res"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

# Adaptive Icon の 108dp キャンバスのうち、マスク後に見えるのは中央 72dp。
# 768 * 108 / 72 = 1152 で描いてから中央 768px を切り出すと、実機と同じ寸法比になる。
canvas=1152
visible=768

background="$(sed -n 's/.*<color name="ic_launcher_background">\(#[0-9A-Fa-f]*\)<\/color>.*/\1/p' "$res/values/colors.xml")"
if [[ -z "$background" ]]; then
  echo "failed to read ic_launcher_background from values/colors.xml" >&2
  exit 1
fi

python3 "$dir/vector_to_svg.py" "$res/drawable/ic_launcher_foreground.xml" > "$tmp/foreground.svg"
rsvg-convert -w "$canvas" -h "$canvas" "$tmp/foreground.svg" -o "$tmp/fg.png"

magick -size "${canvas}x${canvas}" "xc:$background" "$tmp/fg.png" -compose over -composite \
  -gravity center -crop "${visible}x${visible}+0+0" +repage "$tmp/visible.png"

# ランチャー外のコンシューマはマスクをかけないので、四隅は自前で丸めて透過させる。
# 半径 210/768 は Adaptive Icon のスクワークルマスクに近い比率。
magick "$tmp/visible.png" \
  \( -size "${visible}x${visible}" xc:none -fill white \
     -draw "roundrectangle 0,0 $((visible - 1)),$((visible - 1)) 210,210" \) \
  -alpha set -compose DstIn -composite "$tmp/masked.png"

emit() { # $1=density $2=size
  mkdir -p "$res/mipmap-$1"
  # -depth 8: 既定の 16bit は情報量に見合わずファイルが太るだけなので 8bit に落とす
  # -strip: png:tIME（生成時刻）を落とし、再生成がバイト単位で再現するようにする
  magick "$tmp/masked.png" -resize "$2x$2" -depth 8 -strip "$res/mipmap-$1/ic_launcher.png"
  echo "wrote mipmap-$1/ic_launcher.png (${2}px)"
}

emit mdpi 48
emit hdpi 72
emit xhdpi 96
emit xxhdpi 144
emit xxxhdpi 192
