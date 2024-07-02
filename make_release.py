import shutil, pathlib

curdir = str(pathlib.Path(__file__).parent.resolve())
modname = "Apex Design Collective"

src = curdir
temp = curdir + "\\temp\\" + modname
toIgnore = shutil.ignore_patterns('*.gitignore', '.idea', '.run', 'out', 'src', '*.iml','*.py','*.md','.git','temp','changes.txt','apex_lineup.png','.gitattributes',"apex_lineup_relic.png")
print("\nDid you update the version number in BOTH the mod_info and the .version?\n")
print("Copying to temp directory...")
shutil.copytree(src, temp, ignore = toIgnore)
print("Creating .zip...")
shutil.make_archive(modname, 'zip', curdir + "\\temp")
print("Deleting temp directory...")
shutil.rmtree(curdir + "\\temp")