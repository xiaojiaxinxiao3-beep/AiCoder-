"""Converts Mojang's mappings (client.txt) to the tiny v1 format the remapper reads.

    python3 tools/tiny.py work/client.txt work/mojang.tiny
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from names import Mappings

src, dst = sys.argv[1], sys.argv[2]
m = Mappings(src)
with open(dst, 'w', encoding='utf-8') as out:
    out.write('v1\tofficial\tnamed\n')
    for named, obf in m.classes.items():
        out.write('CLASS\t%s\t%s\n' % (obf.replace('.', '/'), named.replace('.', '/')))
    for named, members in m.members.items():
        owner = m.classes[named].replace('.', '/')
        for kind, typ, name, args, obf in members:
            if kind == 'field':
                out.write('FIELD\t%s\t%s\t%s\t%s\n' % (owner, m.desc(typ), obf, name))
            elif name not in ('<init>', '<clinit>'):
                desc = '(' + ''.join(m.desc(a) for a in args) + ')' + m.desc(typ)
                out.write('METHOD\t%s\t%s\t%s\t%s\n' % (owner, desc, obf, name))
