#!/usr/bin/env python3
"""Downloads Fabric Loader and its libraries for one game version (after tools/fetch.py has been run for it),
for building and testing the Fabric variant.

    python3 tools/fetch_fabric.py 1.21.4 work-1.21.4
Writes fabric-cp.txt (loader libraries + the game's own class path), fabric-main.txt and intermediary.tiny.
"""
import io, json, os, sys, urllib.request, zipfile

version, out = sys.argv[1], sys.argv[2]


def fetch(url):
    for attempt in range(4):
        try:
            return urllib.request.urlopen(url, timeout=60).read()
        except Exception as e:
            last = e
    raise last


loaders = json.loads(fetch('https://meta.fabricmc.net/v2/versions/loader/' + version))
loader = next(l for l in loaders if l['loader']['stable'])['loader']['version']
profile = json.loads(fetch('https://meta.fabricmc.net/v2/versions/loader/%s/%s/profile/json' % (version, loader)))

paths = []
for lib in profile['libraries']:
    group, name, ver = lib['name'].split(':')[:3]
    rel = '%s/%s/%s/%s-%s.jar' % (group.replace('.', '/'), name, ver, name, ver)
    path = os.path.join(out, 'fabric', rel)
    if not os.path.exists(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        open(path, 'wb').write(fetch(lib.get('url', 'https://maven.fabricmc.net/').rstrip('/') + '/' + rel))
    paths.append(os.path.abspath(path))
    if name == 'intermediary':
        with zipfile.ZipFile(path) as z:
            open(os.path.join(out, 'intermediary.tiny'), 'wb').write(z.read('mappings/mappings.tiny'))

# As a launcher does: where the profile and the game name the same library, the profile's version wins
def artifact(path):
    parts = path.replace(os.sep, '/').split('/')
    return parts[-3] if len(parts) >= 3 else path


mine = {artifact(p) for p in paths}
game = [p for p in open(os.path.join(out, 'cp.txt')).read().strip().split(':') if 'minecraft-client' in p or artifact(p) not in mine]
open(os.path.join(out, 'fabric-cp.txt'), 'w').write(':'.join(paths + game))
open(os.path.join(out, 'fabric-main.txt'), 'w').write(profile['mainClass'])
print('fabric loader', loader, 'main', profile['mainClass'], 'libraries', len(paths))
