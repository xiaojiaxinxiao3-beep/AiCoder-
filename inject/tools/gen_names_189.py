"""names.properties for 1.8.9: the obfuscated owner, name and descriptor of each method the agent patches,
and of the private fields read by reflection. Read from mcp.tiny (tools/mcp_names.py)."""
import sys

classes, methods, fields = {}, {}, {}
for line in open(sys.argv[1], encoding='utf-8'):
    p = line.rstrip('\n').split('\t')
    if p[0] == 'CLASS':
        classes[p[3]] = p[1]
    elif p[0] == 'METHOD':
        methods.setdefault((p[1], p[5]), []).append((p[3], p[2]))
    elif p[0] == 'FIELD':
        fields[p[1], p[5]] = p[3]

C = 'net/minecraft/client/'
out = {}


def method(key, owner, name, desc=None):
    o = classes[owner]
    found = [m for m in methods[o, name] if desc is None or m[1] == desc]
    assert len(found) == 1, (key, name, found)
    out[key] = o
    out['%s.%s' % (key, name)], out['%s.%s.desc' % (key, name)] = found[0]


method('Minecraft', C + 'Minecraft', 'runTick')
method('Minecraft', C + 'Minecraft', 'dispatchKeypresses')
method('Minecraft', C + 'Minecraft', 'getMinecraft')
method('EntityRenderer', C + 'renderer/EntityRenderer', 'updateCameraAndRender', '(FJ)V')
for name in ('timer', 'launchedVersion'):
    out['Minecraft.' + name] = fields[classes[C + 'Minecraft'], name]
with open(sys.argv[2], 'w') as f:
    f.write(''.join('%s=%s\n' % kv for kv in sorted(out.items())))
