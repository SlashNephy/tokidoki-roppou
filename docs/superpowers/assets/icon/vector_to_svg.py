#!/usr/bin/env python3
"""Android Vector Drawable を SVG に変換する。

ランチャーアイコンの図像の出所は `ic_launcher_foreground.xml` ただ一つであり、
ラスタライズ用に別途 SVG を手で維持すると二重管理になる。`android:pathData` は
SVG のパス文法そのものなので、パスと塗り色を読み出して SVG に載せ替えるだけで
図像が完全に一致する。

対応するのは本アイコンが実際に使っている構成（トップレベルの <path> と
android:fillColor / android:pathData / android:fillType）に限る。グラデーション、
<group>、<clip-path>、ストロークなどが現れた場合は黙って無視せずエラーにする。
ここで落ちたときは、変換器を拡張するか、ラスタライズの方法自体を見直す合図。
"""

import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"

SUPPORTED_ROOT_ATTRS = {"width", "height", "viewportWidth", "viewportHeight"}
SUPPORTED_PATH_ATTRS = {"pathData", "fillColor", "fillType", "name"}


def fail(message: str) -> None:
    sys.exit(f"vector_to_svg: {message}")


def local(name: str) -> str:
    return name[len(ANDROID):] if name.startswith(ANDROID) else name


def check_attrs(element: ET.Element, supported: set[str], label: str) -> None:
    for raw in element.attrib:
        if raw.startswith("xmlns"):
            continue
        name = local(raw)
        if name == raw and ":" not in raw:
            # 名前空間なしの属性は android 名前空間ではないので対象外
            continue
        if name not in supported:
            fail(f"unsupported attribute on {label}: {raw}")


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit("usage: vector_to_svg.py <vector-drawable.xml>")

    root = ET.parse(sys.argv[1]).getroot()
    if local(root.tag) != "vector":
        fail(f"root element must be <vector>, got <{root.tag}>")
    check_attrs(root, SUPPORTED_ROOT_ATTRS, "<vector>")

    width = root.get(f"{ANDROID}viewportWidth")
    height = root.get(f"{ANDROID}viewportHeight")
    if width is None or height is None:
        fail("<vector> must declare android:viewportWidth and android:viewportHeight")

    paths = []
    for child in root:
        tag = local(child.tag)
        if tag != "path":
            fail(f"unsupported element <{tag}>; only top-level <path> is handled")
        check_attrs(child, SUPPORTED_PATH_ATTRS, "<path>")

        data = child.get(f"{ANDROID}pathData")
        if data is None:
            fail("<path> without android:pathData")
        fill = child.get(f"{ANDROID}fillColor")
        if fill is None:
            fail("<path> without android:fillColor")
        if not fill.startswith("#"):
            fail(f"unsupported fillColor {fill!r}; only literal #RRGGBB is handled")

        attrs = [f'fill="{fill}"']
        fill_type = child.get(f"{ANDROID}fillType")
        if fill_type is not None:
            if fill_type not in ("nonZero", "evenOdd"):
                fail(f"unknown fillType {fill_type!r}")
            attrs.append(f'fill-rule="{"evenodd" if fill_type == "evenOdd" else "nonzero"}"')
        attrs.append(f'd="{data}"')
        paths.append("  <path " + " ".join(attrs) + " />")

    print(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}">')
    print("\n".join(paths))
    print("</svg>")


if __name__ == "__main__":
    main()
