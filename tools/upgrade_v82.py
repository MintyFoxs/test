#!/usr/bin/env python3
"""Non-destructive v8.1 -> v8.2 release patch; never modify pet saves."""
from pathlib import Path

root=Path(__file__).resolve().parent.parent
src=root/'app/src/main/java/ru/pgws/pocketpet'
font=src/'PixelFont.java'
s=font.read_text(encoding='utf-8')
old='    public float width(String s,float height){p.setTextSize(height);return p.measureText(s==null?"":s);}'
new='''    // Epilepsy Sans is optically smaller than its nominal em size.
    // Enlarge text while preserving LCD touch regions and glyph aspect ratios.
    private static float readable(float size){
        return size * (size <= 20f ? 1.20f : 1.12f);
    }
    public float width(String s,float height){p.setTextSize(readable(height));return p.measureText(s==null?"":s);}'''
if old not in s: raise RuntimeError('Unexpected PixelFont.java; no unsafe overwrite')
s=s.replace(old,new)
for old,new in [
    ('p.setColor(color);p.setTextSize(height);','p.setColor(color);p.setTextSize(readable(height));'),
    ('p.setTextSize(height*available/w);','p.setTextSize(readable(height)*available/w);')
]:
    if old not in s: raise RuntimeError('Missing font drawing pattern: '+old)
    s=s.replace(old,new)
font.write_text(s,encoding='utf-8')

build=root/'app/build.gradle'
s=build.read_text(encoding='utf-8')
for expected in ("applicationId 'ru.pgws.pocketpet'","versionCode 10","versionName '8.1.0'",'signingConfig signingConfigs.debug'):
    if expected not in s: raise RuntimeError('Unexpected Gradle config: '+expected)
s=s.replace('versionCode 10','versionCode 11').replace("versionName '8.1.0'","versionName '8.2.0'")
s=s.replace('            signingConfig signingConfigs.debug\n','')
build.write_text(s,encoding='utf-8')

readme=root/'README.md'
s=readme.read_text(encoding='utf-8')
s+='''\n## v8.2.0: readable text and compatible updates\n
- Epilepsy Sans is 20% larger in normal UI and 12% larger in headings.
- Package name is still `ru.pgws.pocketpet`; versionCode 11 succeeds versionCode 10.
- Pet data stays in `pocketpet96_save_v1` and `pocketpet96_debug_clock` without schema changes.
- Releases MUST be signed with the original v8.x release key, NEVER the CI debug key.
- Play certification requires an actual Play Console account and Play App Signing.\n'''
readme.write_text(s,encoding='utf-8')
print('Applied v8.2 patch; save files and game logic untouched')
