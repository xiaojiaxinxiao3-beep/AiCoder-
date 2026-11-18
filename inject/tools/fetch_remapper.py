#!/usr/bin/env python3
"""Downloads tiny-remapper and the libraries it needs, and writes remapper-cp.txt for build.sh.

    python3 tools/fetch_remapper.py work

Only needed on a machine where Gradle has not already put these jars in its cache.
"""
import os, sys, urllib.request

out = sys.argv[1]
JARS = [
    ('https://maven.fabricmc.net/net/fabricmc/tiny-remapper/0.14.1/tiny-remapper-0.14.1.jar', 'tiny-remapper-0.14.1.jar'),
    ('https://maven.fabricmc.net/net/fabricmc/mapping-io/0.8.0/mapping-io-0.8.0.jar', 'mapping-io-0.8.0.jar'),
] + [('https://repo1.maven.org/maven2/org/ow2/asm/%s/9.10.1/%s-9.10.1.jar' % (n, n), '%s-9.10.1.jar' % n)
     for n in ('asm', 'asm-commons', 'asm-tree', 'asm-util', 'asm-analysis')]

folder = os.path.abspath(os.path.join(out, 'remapper'))
os.makedirs(folder, exist_ok=True)
paths = []
for url, name in JARS:
    path = os.path.join(folder, name)
    if not os.path.exists(path):
        open(path, 'wb').write(urllib.request.urlopen(url, timeout=60).read())
    paths.append(path)
open(os.path.join(out, 'remapper-cp.txt'), 'w').write(':'.join(paths))
print('remapper-cp.txt:', len(paths), 'jars')
