#!/usr/bin/env python3
"""What the Fabric variant needs, from Fabric's intermediary mappings.

    python3 tools/fabric_names.py <mojang.tiny> <intermediary.tiny> <names.properties> <out fabric.tiny> <out names-intermediary.properties>

Under Fabric Loader the game's classes carry intermediary names (class_310, method_1574) instead of the
obfuscated ones, so the client is converted to those: fabric.tiny maps named -> intermediary for the remapper,
and the second output is the table of patch targets in intermediary names.
"""
import re, sys

mojang, inter_path, names_in, tiny_out, names_out = sys.argv[1:6]

classes, fields, methods = {}, {}, {}
for line in open(inter_path, encoding='utf-8'):
    p = line.rstrip('\n').split('\t')
    if p[0] == 'CLASS':
        classes[p[1]] = p[2]
    elif p[0] == 'FIELD':
        fields[(p[1], p[2], p[3])] = p[4]
    elif p[0] == 'METHOD':
        methods[(p[1], p[2], p[3])] = p[4]

with open(tiny_out, 'w', encoding='utf-8') as out:
    out.write('v1\tofficial\tnamed\tintermediary\n')
    for line in open(mojang, encoding='utf-8'):
        p = line.rstrip('\n').split('\t')
        if p[0] == 'CLASS':
            out.write('CLASS\t%s\t%s\t%s\n' % (p[1], p[2], classes.get(p[1], p[1])))
        elif p[0] in ('FIELD', 'METHOD'):
            table = fields if p[0] == 'FIELD' else methods
            # Intermediary lists a method only where it is first declared; an override is left out here too
            # and the remapper carries the name down the class hierarchy itself
            name = table.get((p[1], p[2], p[3]))
            if name is not None:
                out.write('%s\t%s\t%s\t%s\t%s\t%s\n' % (p[0], p[1], p[2], p[3], p[4], name))


def desc(d):
    return re.sub(r'L([^;]+);', lambda m: 'L' + classes.get(m.group(1), m.group(1)) + ';', d)


props = dict(line.rstrip('\n').split('=', 1) for line in open(names_in, encoding='utf-8') if '=' in line)
out = {}
for key, value in props.items():
    if '.' not in key:
        out[key] = classes.get(value, value)
for key, value in props.items():
    if key.endswith('.desc') or '.' not in key:
        continue
    owner = props[key.split('.')[0]]
    d = props.get(key + '.desc')
    if d is None:
        # A field read by reflection: found by owner and name alone
        hits = [v for (o, _, n), v in fields.items() if o == owner and n == value]
        if len(hits) != 1:
            sys.exit('fabric_names: no single intermediary field for %s (%s.%s)' % (key, owner, value))
        out[key] = hits[0]
        continue
    name = methods.get((owner, d, value))
    if name is None:
        sys.exit('fabric_names: no intermediary name for %s (%s.%s%s)' % (key, owner, value, d))
    out[key] = name
    out[key + '.desc'] = desc(d)
open(names_out, 'w', encoding='utf-8').write(''.join('%s=%s\n' % kv for kv in sorted(out.items())))
