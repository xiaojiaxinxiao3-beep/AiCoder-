"""Writes names.properties: the obfuscated owner, name and descriptor of each method the agent patches."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from names import Mappings

m = Mappings(sys.argv[1])
C = 'net.minecraft.client.'
TARGETS = [
    ('Minecraft', C + 'Minecraft', 'tick', []),
    ('Minecraft', C + 'Minecraft', 'startAttack', []),
    ('GameRenderer', C + 'renderer.GameRenderer', 'render', [C + 'DeltaTracker', 'boolean']),
    ('MultiPlayerGameMode', C + 'multiplayer.MultiPlayerGameMode', 'useItemOn',
     [C + 'player.LocalPlayer', 'net.minecraft.world.InteractionHand', 'net.minecraft.world.phys.BlockHitResult']),
    ('MultiPlayerGameMode', C + 'multiplayer.MultiPlayerGameMode', 'useItem',
     ['net.minecraft.world.entity.player.Player', 'net.minecraft.world.InteractionHand']),
    ('ClientLevel', C + 'multiplayer.ClientLevel', 'addEntity', ['net.minecraft.world.entity.Entity']),
    ('KeyboardHandler', C + 'KeyboardHandler', 'keyPress', ['long', 'int', C + 'input.KeyEvent']),
]

out = {}
for key, owner, name, args in TARGETS:
    out[key] = m.cls(owner).replace('.', '/')
    out['%s.%s' % (key, name)], out['%s.%s.desc' % (key, name)] = m.method(owner, name, args)

os.makedirs(os.path.dirname(sys.argv[2]), exist_ok=True)
with open(sys.argv[2], 'w') as f:
    f.write(''.join('%s=%s\n' % kv for kv in sorted(out.items())))
