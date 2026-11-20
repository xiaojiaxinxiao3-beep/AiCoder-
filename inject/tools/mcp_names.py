#!/usr/bin/env python3
"""Minecraft 1.8.9 has no official mappings. Its readable names come from MCP (the names Forge mods of
that time were written against): joined.srg gives obfuscated -> "searge" names (func_1234_a), and
fields.csv / methods.csv give the readable name of each searge name.

    python3 tools/mcp_names.py work-1.8.9

Writes work-1.8.9/mcp.tiny with three name columns: official (what the plain game runs with), srg (what
the game runs with under Forge) and named (what the sources are written against). Field lines carry no
type in MCP; they are taken from fields.txt (tools/java/FieldDump.java run on the game jar).
"""
import csv, os, sys

w = sys.argv[1]
readable = {}
for name in ('fields.csv', 'methods.csv'):
    with open(os.path.join(w, name), newline='', encoding='utf-8') as f:
        for row in csv.DictReader(f):
            readable[row['searge']] = row['name']

# The type of every field, read from the game jar by tools/java/FieldDump.java
types = {}
for line in open(os.path.join(w, 'fields.txt'), encoding='utf-8'):
    owner, name, desc = line.split()
    types[owner, name] = desc

out = ['v1\tofficial\tsrg\tnamed']
for line in open(os.path.join(w, 'joined.srg'), encoding='utf-8'):
    p = line.split()
    if p[0] == 'CL:':
        out.append('CLASS\t%s\t%s\t%s' % (p[1], p[2], p[2]))
    elif p[0] == 'FD:':
        owner, name = p[1].rsplit('/', 1)
        srg = p[2].rsplit('/', 1)[1]
        out.append('FIELD\t%s\t%s\t%s\t%s\t%s' % (owner, types.get((owner, name), 'I'), name, srg, readable.get(srg, srg)))
    elif p[0] == 'MD:':
        owner, name = p[1].rsplit('/', 1)
        srg = p[3].rsplit('/', 1)[1]
        out.append('METHOD\t%s\t%s\t%s\t%s\t%s' % (owner, p[2], name, srg, readable.get(srg, srg)))
open(os.path.join(w, 'mcp.tiny'), 'w', encoding='utf-8').write('\n'.join(out) + '\n')
print(len(out) - 1, 'names')
