#!/usr/bin/env python3
"""Adaptive Icon（`mipmap-anydpi-v26/ic_launcher.xml`）を 1 枚の SVG に変換する。

ランチャーアイコンの図像の出所は Adaptive Icon ただ一つであり、ラスタライズ用に
別途 SVG を手で維持すると二重管理になる。`ic_launcher.xml` の background /
foreground の参照をたどってリソースを解決し、SVG に載せ替える。レイヤーの参照先を
差し替えても、ここを通せばラスタも追従する。

Vector Drawable の `android:pathData` は SVG のパス文法そのものなので、パスと塗り色を
読み出して載せ替えるだけで図像が完全に一致する。

対応するのは本アイコンが実際に使っている構成に限る。グラデーション、<group>、
<clip-path>、ストロークなどが現れた場合は黙って無視せずエラーにする。ここで落ちた
ときは、変換器を拡張するか、ラスタライズの方法自体を見直す合図。

monochrome レイヤーはランチャーがテーマアイコン用に単色化して使うものでラスタには
現れないため、意図的に無視する。
"""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ANDROID = "{http://schemas.android.com/apk/res/android}"

SUPPORTED_VECTOR_ATTRS = {"width", "height", "viewportWidth", "viewportHeight"}
SUPPORTED_PATH_ATTRS = {"pathData", "fillColor", "fillType", "name"}
SUPPORTED_LAYER_ATTRS = {"drawable"}


def fail(message: str) -> None:
    sys.exit(f"adaptive_icon_to_svg: {message}")


def local(name: str) -> str:
    return name[len(ANDROID):] if name.startswith(ANDROID) else name


def check_attrs(element: ET.Element, supported: set[str], label: str) -> None:
    for raw in element.attrib:
        if raw.startswith("xmlns") or not raw.startswith(ANDROID):
            continue
        if local(raw) not in supported:
            fail(f"unsupported attribute on {label}: {raw}")


def parse_vector(path: Path) -> tuple[str, str, list[str]]:
    """Vector Drawable を (viewportWidth, viewportHeight, SVG パス要素) に変換する。"""
    root = ET.parse(path).getroot()
    if local(root.tag) != "vector":
        fail(f"{path}: root element must be <vector>, got <{root.tag}>")
    check_attrs(root, SUPPORTED_VECTOR_ATTRS, f"{path.name} <vector>")

    width = root.get(f"{ANDROID}viewportWidth")
    height = root.get(f"{ANDROID}viewportHeight")
    if width is None or height is None:
        fail(f"{path}: <vector> must declare android:viewportWidth and android:viewportHeight")

    paths = []
    for child in root:
        tag = local(child.tag)
        if tag != "path":
            fail(f"{path}: unsupported element <{tag}>; only top-level <path> is handled")
        check_attrs(child, SUPPORTED_PATH_ATTRS, f"{path.name} <path>")

        data = child.get(f"{ANDROID}pathData")
        if data is None:
            fail(f"{path}: <path> without android:pathData")
        fill = child.get(f"{ANDROID}fillColor")
        if fill is None:
            fail(f"{path}: <path> without android:fillColor")
        if not fill.startswith("#"):
            fail(f"{path}: unsupported fillColor {fill!r}; only literal #RRGGBB is handled")

        attrs = [f'fill="{fill}"']
        fill_type = child.get(f"{ANDROID}fillType")
        if fill_type is not None:
            if fill_type not in ("nonZero", "evenOdd"):
                fail(f"{path}: unknown fillType {fill_type!r}")
            attrs.append(f'fill-rule="{"evenodd" if fill_type == "evenOdd" else "nonzero"}"')
        attrs.append(f'd="{data}"')
        paths.append("  <path " + " ".join(attrs) + " />")

    return width, height, paths


def lookup_color(res: Path, name: str) -> str:
    """values/colors.xml から色リテラルを引く。"""
    for color in ET.parse(res / "values" / "colors.xml").getroot():
        if color.tag == "color" and color.get("name") == name:
            value = (color.text or "").strip()
            if not value.startswith("#"):
                fail(f"@color/{name} is not a literal color: {value!r}")
            return value
    fail(f"@color/{name} not found in values/colors.xml")


def layer_reference(icon: ET.Element, layer: str) -> str:
    elements = [child for child in icon if child.tag == layer]
    if len(elements) != 1:
        fail(f"<adaptive-icon> must have exactly one <{layer}>, found {len(elements)}")
    check_attrs(elements[0], SUPPORTED_LAYER_ATTRS, f"<{layer}>")
    reference = elements[0].get(f"{ANDROID}drawable")
    if reference is None:
        fail(f"<{layer}> without android:drawable")
    return reference


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit("usage: adaptive_icon_to_svg.py <res-dir>")
    res = Path(sys.argv[1])

    icon = ET.parse(res / "mipmap-anydpi-v26" / "ic_launcher.xml").getroot()
    if icon.tag != "adaptive-icon":
        fail(f"root element must be <adaptive-icon>, got <{icon.tag}>")

    foreground = layer_reference(icon, "foreground")
    if not foreground.startswith("@drawable/"):
        fail(f"unsupported <foreground> reference {foreground!r}; only @drawable/... is handled")
    width, height, body = parse_vector(res / "drawable" / f"{foreground.removeprefix('@drawable/')}.xml")

    background = layer_reference(icon, "background")
    if background.startswith("@color/"):
        color = lookup_color(res, background.removeprefix("@color/"))
        body.insert(0, f'  <rect x="0" y="0" width="{width}" height="{height}" fill="{color}" />')
    elif background.startswith("@drawable/"):
        bg_path = res / "drawable" / f"{background.removeprefix('@drawable/')}.xml"
        bg_width, bg_height, bg_body = parse_vector(bg_path)
        if (bg_width, bg_height) != (width, height):
            fail(
                f"viewport mismatch: foreground is {width}x{height} but "
                f"{bg_path.name} is {bg_width}x{bg_height}"
            )
        body = bg_body + body
    else:
        fail(f"unsupported <background> reference {background!r}")

    print(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}">')
    print("\n".join(body))
    print("</svg>")


if __name__ == "__main__":
    main()
