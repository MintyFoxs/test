#!/usr/bin/env python3
"""Switch every rendered game text surface to the original Epilepsy Sans font."""
from pathlib import Path

root = Path("app/src/main/java/ru/pgws/pocketpet")
pixel = root / "PixelFont.java"
s = pixel.read_text(encoding="utf-8")
assert '"LCD5x8H.ttf"' in s, "Expected LCD5x8H PixelFont base"
s = s.replace('"LCD5x8H.ttf"', '"EpilepsySans.ttf"')
s = s.replace("LCD5x8H original dot matrix TrueType, with no fake per-character stretch.",
              "Epilepsy Sans: original Latin and Cyrillic pixel font.")
pixel.write_text(s, encoding="utf-8")

memorial = root / "MemorialCard.java"
s = memorial.read_text(encoding="utf-8")
start = s.index("        try {p.setTypeface(")
end = s.index("\n    }\n    private void box", start)
s = (s[:start] +
     '        try {p.setTypeface(Typeface.createFromAsset(ctx.getAssets(),"EpilepsySans.ttf"));}\n'
     '        catch(Exception ignored){p.setTypeface(Typeface.create("monospace",Typeface.BOLD));}' +
     s[end:])
memorial.write_text(s, encoding="utf-8")

gradle = Path("app/build.gradle")
s = gradle.read_text(encoding="utf-8")
s = s.replace("versionCode 9", "versionCode 10").replace("versionName '8.0.0'", "versionName '8.1.0'")
assert "versionCode 10" in s and "versionName '8.1.0'" in s
gradle.write_text(s, encoding="utf-8")

readme = Path("README.md")
s = readme.read_text(encoding="utf-8")
s = s.replace("LCD5x8H", "Epilepsy Sans")
s += (
    "\n## Epilepsy Sans — attribution and license\n"
    "Interface, notifications and memorial card use the unmodified Epilepsy Sans "
    "font by Rebecca Bettencourt / Kreative Software. "
    "Original download: https://www.kreativekorp.com/swdownload/fonts/relaybm/epilepsy.zip. "
    "The license text is embedded in `app/src/main/assets/EpilepsySans-LICENSE.txt`. "
    "The font is distributed free of charge with credit and license as required.\n"
)
readme.write_text(s, encoding="utf-8")
print("Switched UI, memorial, README and version to Epilepsy Sans")
