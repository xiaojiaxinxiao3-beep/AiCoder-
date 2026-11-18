import base64, io, zipfile, os, sys
root = sys.argv[1]
def b64(p): return base64.b64encode(open(p,'rb').read()).decode()
def zipof(names):
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w', zipfile.ZIP_STORED) as z:
        for n in names: z.write(os.path.join(root,'pack',n), n)
    return base64.b64encode(buf.getvalue()).decode(), len(buf.getvalue())
jars = ['AiCoder賽博龐克.jar','AiCoder地球科學.jar','AiCoder液態玻璃.jar','AiCoder多欄賽博龐克.jar','AiCoder液態玻璃直立式.jar','AiCoder極光玻璃.jar','AiCoder極光玻璃直立式.jar']
items = []
for j in jars:
    data, size = zipof([j]); items.append((j, data, os.path.getsize(os.path.join(root,'pack',j))))
alld, alls = zipof(jars)
def zipof121(names):
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w', zipfile.ZIP_STORED) as z:
        for n in names: z.write(os.path.join(root,'pack121',n), n)
    return base64.b64encode(buf.getvalue()).decode()
jars121 = [j[:-4] + '-1.21.11.jar' for j in jars]
extra = {'%%ZIP_' + k + '121%%': zipof121([j]) for k, j in zip(['CYBER','EARTH','GLASS','COLUMNS','GLASSCOL','AURORA','AURORACOL'], jars121)}
extra['%%ZIP_ALL121%%'] = zipof121(jars121)
html = open(os.path.join(root,'template.html'), encoding='utf-8').read()
cz, _ = zipof(['AiCoder戰鬥巨集.jar'])
extra['%%ZIP_COMBAT%%'] = cz
abuf = io.BytesIO()
with zipfile.ZipFile(abuf, 'w', zipfile.ZIP_DEFLATED) as z:
    for n in ['AiCoderAssist.exe', 'README.txt']: z.write(os.path.join(root, 'assist', n), n)
extra['%%ZIP_ASSIST%%'] = base64.b64encode(abuf.getvalue()).decode()
bbuf = io.BytesIO()
with zipfile.ZipFile(bbuf, 'w', zipfile.ZIP_DEFLATED) as z:
    for n in ['AiCoderAssist.jar', 'AiCoderAssist.bat', 'README.txt']: z.write(os.path.join(root, 'assist-noexe', n), n)
extra['%%ZIP_ASSISTBAT%%'] = base64.b64encode(bbuf.getvalue()).decode()
extra['%%IMG_ASSIST%%'] = b64(os.path.join(root, 'assist.jpg'))
extra['%%KB_ASSIST%%'] = str(round(os.path.getsize(os.path.join(root, 'assist', 'AiCoderAssist.exe')) / 1024))
extra['%%ZIP_COMBAT121%%'] = zipof121(['AiCoder戰鬥巨集-1.21.11.jar'])
extra['%%IMG_COMBAT%%'] = b64(os.path.join(root,'combat.jpg'))
extra['%%KB_COMBAT%%'] = str(round(os.path.getsize(os.path.join(root,'pack','AiCoder戰鬥巨集.jar'))/1024))
extra['%%ZIP_ZORYNTH%%'] = b64(os.path.join(root,'zorynth.zip'))
extra['%%IMG_ZORYNTH%%'] = b64(os.path.join(root,'zorynth.jpg'))
extra['%%KB_ZORYNTH%%'] = '45'
kbuf = io.BytesIO()
with zipfile.ZipFile(kbuf, 'w', zipfile.ZIP_DEFLATED) as z:
    for n in sorted(os.listdir(os.path.join(root, 'bedrock'))): z.write(os.path.join(root, 'bedrock', n), n)
extra['%%ZIP_BEDROCK%%'] = base64.b64encode(kbuf.getvalue()).decode()
extra['%%IMG_BEDROCK%%'] = b64(os.path.join(root, 'bedrock.jpg'))
extra['%%KB_BEDROCK%%'] = str(round(sum(os.path.getsize(os.path.join(root, 'bedrock', n)) for n in os.listdir(os.path.join(root, 'bedrock')) if n.endswith(('.exe', '.dll'))) / 1024))
for tag, folder in (('%%ZIP_ASSIST4%%', 'assist4'), ('%%ZIP_ASSIST4BAT%%', 'assist4-noexe')):
    zbuf = io.BytesIO()
    with zipfile.ZipFile(zbuf, 'w', zipfile.ZIP_DEFLATED) as z:
        for n in sorted(os.listdir(os.path.join(root, folder))): z.write(os.path.join(root, folder, n), n)
    extra[tag] = base64.b64encode(zbuf.getvalue()).decode()
for key, val in extra.items(): html = html.replace(key, val)
for key, val in {
  '%%IMG_AURORA%%': b64(os.path.join(root,'aurora.jpg')), '%%IMG_AURORACOL%%': b64(os.path.join(root,'auroracol.jpg')), '%%ZIP_AURORA%%': items[5][1], '%%ZIP_AURORACOL%%': items[6][1], '%%KB_AURORA%%': str(round(items[5][2]/1024)), '%%KB_AURORACOL%%': str(round(items[6][2]/1024)),
  '%%IMG_CYBER%%': b64(os.path.join(root,'cyber.jpg')), '%%IMG_EARTH%%': b64(os.path.join(root,'earth.jpg')), '%%IMG_GLASS%%': b64(os.path.join(root,'glass.jpg')), '%%IMG_GLASSCOL%%': b64(os.path.join(root,'glasscol.jpg')), '%%ZIP_GLASSCOL%%': items[4][1], '%%KB_GLASSCOL%%': str(round(items[4][2]/1024)), '%%IMG_COLUMNS%%': b64(os.path.join(root,'columns.jpg')),
  '%%ZIP_CYBER%%': items[0][1], '%%ZIP_EARTH%%': items[1][1], '%%ZIP_GLASS%%': items[2][1], '%%ZIP_COLUMNS%%': items[3][1], '%%KB_COLUMNS%%': str(round(items[3][2]/1024)), '%%ZIP_ALL%%': alld,
  '%%KB_CYBER%%': str(round(items[0][2]/1024)), '%%KB_EARTH%%': str(round(items[1][2]/1024)), '%%KB_GLASS%%': str(round(items[2][2]/1024)), '%%KB_ALL%%': str(round(alls/1024)),
}.items(): html = html.replace(key, val)
open(os.path.join(root,'aicoder-downloads.html'),'w',encoding='utf-8').write(html)
print(len(html))
