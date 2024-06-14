from PIL import Image
import glob, os
for f in glob.glob(os.getcwd()+'\\graphics\\maps\\**\\*_normal.png', recursive=True):
    # f is our normal map
    #print(os.path.basename(f))
    # create the corresponding file name
    normalname = os.path.basename(f)
    nlen = len(normalname)
    orig = normalname[:nlen-11]+".png"
    orig_f = glob.glob(os.getcwd() + "\\graphics\\**\\" + orig, recursive=True)[0]
    #print(orig_f)
    #print("processing " + orig)
    normal = Image.open(f)
    original = Image.open(orig_f, 'r')
    try:
        normal.putalpha(original.split()[-1])
    except:
        print("failed to process " + orig)
    normal.save(f)
    normal.close()
    original.close()