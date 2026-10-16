#!/usr/bin/env python3
"""Копирует картинки сайта в ресурсы приложения с именами, которые ждёт Site.media().

Запуск (из корня проекта):
    python tools/import_media.py путь/к/huegrad-site/media
    python tools/import_media.py путь/к/huegrad-site/media --webp   # сжатие в WebP, нужен Pillow
"""
import re
import shutil
import sys
from pathlib import Path

ALIASES = {"пиздюк2.jpg": "pizdjuk2.jpg"}  # кириллические имена -> латиница
EXTS = {".jpg", ".jpeg", ".png", ".webp"}
DEST = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable-nodpi"


def res_name(filename: str) -> str:
    stem = ALIASES.get(filename, filename).rsplit(".", 1)[0].lower()
    return "m_" + re.sub(r"[^a-z0-9]", "_", stem)


def main() -> None:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    webp = "--webp" in sys.argv
    if len(args) != 1:
        sys.exit(__doc__)
    src = Path(args[0])
    if webp:
        from PIL import Image  # pip install pillow
    DEST.mkdir(parents=True, exist_ok=True)

    seen: dict[str, str] = {}
    copied = 0
    for f in sorted(src.iterdir()):
        if f.suffix.lower() not in EXTS:
            continue
        name = res_name(f.name)
        if name in seen:
            print(f"! конфликт имён: {f.name} и {seen[name]} -> {name}, пропущен")
            continue
        seen[name] = f.name
        for old in DEST.glob(name + ".*"):
            old.unlink()
        if webp:
            img = Image.open(f)
            img.thumbnail((1600, 1600))
            img.save(DEST / f"{name}.webp", "WEBP", quality=80)
        else:
            shutil.copy2(f, DEST / (name + f.suffix.lower().replace(".jpeg", ".jpg")))
        copied += 1
    print(f"Готово: {copied} файлов -> {DEST}")


if __name__ == "__main__":
    main()
