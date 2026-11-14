"""Looks names up in Mojang's official mappings (client.txt).

    Mappings(path).cls('net.minecraft.client.Minecraft')                      -> 'gfj'
    .method('net.minecraft.client.Minecraft', 'tick', [])                     -> ('x', '()V')
    .field('net.minecraft.client.Minecraft', 'font')                          -> ('g', 'Lgio;')
"""
import re

PRIM = {'void': 'V', 'int': 'I', 'boolean': 'Z', 'float': 'F', 'double': 'D', 'long': 'J', 'byte': 'B', 'char': 'C', 'short': 'S'}


class Mappings:
    def __init__(self, path):
        self.classes = {}     # mojang name -> obfuscated name
        self.members = {}     # mojang class -> list of (kind, return/type, name, args or None, obfuscated)
        current = None
        for line in open(path, encoding='utf-8'):
            if line.startswith('#') or not line.strip():
                continue
            if not line.startswith(' '):
                name, obf = line.rstrip(':\n').split(' -> ')
                self.classes[name] = obf
                current = self.members.setdefault(name, [])
                continue
            m = re.match(r'\s+(?:\d+:\d+:)?(\S+) ([^\s(]+)(?:\(([^)]*)\))?(?::\d+:\d+)? -> (\S+)', line)
            if not m:
                continue
            typ, name, args, obf = m.groups()
            current.append(('method' if args is not None else 'field', typ, name, None if args is None else [a for a in args.split(',') if a], obf))

    def cls(self, name):
        return self.classes[name]

    def desc(self, typ):
        dims = typ.count('[]')
        typ = typ.replace('[]', '')
        base = PRIM.get(typ) or 'L' + self.classes.get(typ, typ).replace('.', '/') + ';'
        return '[' * dims + base

    def method(self, owner, name, args):
        hits = [m for m in self.members[owner] if m[0] == 'method' and m[2] == name and m[3] == list(args)]
        if len(hits) != 1:
            raise KeyError('%s.%s(%s): %d matches' % (owner, name, ','.join(args), len(hits)))
        _, ret, _, a, obf = hits[0]
        return obf, '(' + ''.join(self.desc(x) for x in a) + ')' + self.desc(ret)

    def field(self, owner, name):
        hits = [m for m in self.members[owner] if m[0] == 'field' and m[2] == name]
        if len(hits) != 1:
            raise KeyError('%s.%s: %d matches' % (owner, name, len(hits)))
        return hits[0][4], self.desc(hits[0][1])

    def methods_named(self, owner, name):
        return [m for m in self.members[owner] if m[0] == 'method' and m[2] == name]
