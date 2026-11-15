#!/usr/bin/env python3
"""Downloads what a build and a test run need for one game version, from Mojang's own servers:
the client jar, its official mappings, the libraries (-> cp.txt) and the assets.

    python3 tools/fetch.py 1.21.4 work-1.21.4
"""
import hashlib, json, os, sys, urllib.request
from concurrent.futures import ThreadPoolExecutor

version, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
ASSETS = os.path.expanduser('~/.gradle/caches/fabric-loom/assets')


def get(url, path, sha1=None):
    if os.path.exists(path) and (sha1 is None or hashlib.sha1(open(path, 'rb').read()).hexdigest() == sha1):
        return path
    os.makedirs(os.path.dirname(path), exist_ok=True)
    for attempt in range(4):
        try:
            data = urllib.request.urlopen(url, timeout=60).read()
            open(path, 'wb').write(data)
            return path
        except Exception as e:
            last = e
    raise last


manifest = json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json', timeout=60))
meta_url = next(v['url'] for v in manifest['versions'] if v['id'] == version)
meta = json.load(urllib.request.urlopen(meta_url, timeout=60))

get(meta['downloads']['client']['url'], os.path.join(out, 'minecraft-client.jar'), meta['downloads']['client']['sha1'])
get(meta['downloads']['client_mappings']['url'], os.path.join(out, 'client.txt'))

libs = []
for lib in meta['libraries']:
    rules = lib.get('rules')
    if rules and not any(r['action'] == 'allow' and r.get('os', {}).get('name', 'linux') == 'linux' for r in rules):
        continue
    art = lib.get('downloads', {}).get('artifact')
    if art:
        libs.append(get(art['url'], os.path.join(out, 'libraries', art['path']), art['sha1']))
open(os.path.join(out, 'cp.txt'), 'w').write(':'.join([os.path.abspath(os.path.join(out, 'minecraft-client.jar'))] + [os.path.abspath(p) for p in libs]))

index = meta['assetIndex']
index_path = get(index['url'], os.path.join(ASSETS, 'indexes', index['id'] + '.json'))
objects = json.load(open(index_path))['objects']


def asset(entry):
    h = entry['hash']
    path = os.path.join(ASSETS, 'objects', h[:2], h)
    if not os.path.exists(path):
        get('https://resources.download.minecraft.net/%s/%s' % (h[:2], h), path)


with ThreadPoolExecutor(16) as pool:
    list(pool.map(asset, objects.values()))
open(os.path.join(out, 'asset-index.txt'), 'w').write(index['id'])
print(version, 'libraries', len(libs), 'assets', len(objects), 'index', index['id'], 'java', meta.get('javaVersion'))
