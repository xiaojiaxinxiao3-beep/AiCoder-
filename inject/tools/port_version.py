#!/usr/bin/env python3
"""Builds the source tree for an older game version from src/.

    python3 tools/port_version.py 1.21.4 work-1.21.4/src

src/ is written for 1.21.11. For another version: files in src-<version>/ replace the ones in src/,
and the text edits below cover the small places where the game's API is spelled differently.
Every edit must match, so a change in src/ that breaks one fails the build instead of passing silently.
"""
import os, re, shutil, sys

version, out = sys.argv[1], sys.argv[2]
HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
shutil.rmtree(out, ignore_errors=True)
shutil.copytree(os.path.join(HERE, 'src'), out)
overlay = os.path.join(HERE, 'src-' + version)
if os.path.isdir(overlay):
    shutil.copytree(overlay, out, dirs_exist_ok=True)


def edit(path, pairs, regex=False):
    full = os.path.join(out, path)
    text = open(full, encoding='utf-8').read()
    for old, new in pairs:
        if regex:
            text, n = re.subn(old, new, text)
        else:
            n = text.count(old)
            text = text.replace(old, new)
        if n == 0:
            sys.exit('port_version: nothing matched in %s: %r' % (path, old))
    open(full, 'w', encoding='utf-8').write(text)


def everywhere(pairs):
    for root, _, files in os.walk(out):
        for name in files:
            if not name.endswith('.java'):
                continue
            full = os.path.join(root, name)
            text = open(full, encoding='utf-8').read()
            new = text
            for old, repl in pairs:
                new = new.replace(old, repl)
            if new != text:
                open(full, 'w', encoding='utf-8').write(new)


if version == '1.21.4':
    A = 'aicoder/assist/'
    edit(A + 'Agent.java', [('GAME_VERSION = "1.21.11"', 'GAME_VERSION = "1.21.4"'),
                            # keyPress(long window, int key, int scancode, int action, int mods): key is local 3, action local 5
                            ('"KeyboardHandler.keyPress", "key", "I3", "A4"', '"KeyboardHandler.keyPress", "key", "I5", "I3"')])
    edit(A + 'Hooks.java', [('import net.minecraft.client.input.KeyEvent;\n', ''),
                            ('public static void key(int action, Object event)', 'public static void key(int action, int key)'),
                            ('Client.key(((KeyEvent) event).key(), action == 1)', 'Client.key(key, action == 1)')])
    edit(A + 'Injector.java', [('1.21.11', '1.21.4')])
    os.remove(os.path.join(out, A, 'gfx', 'Mesh.java'))

    # The window handle, and the selected hotbar slot, are spelled differently
    everywhere([('net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.', 'net.minecraft.world.entity.projectile.windcharge.'),
                ('net.minecraft.world.entity.projectile.throwableitemprojectile.', 'net.minecraft.world.entity.projectile.'),
                ('getWindow().handle()', 'getWindow().getWindow()'),
                ('.getInventory().getSelectedSlot()', '.getInventory().selected')])
    for root, _, files in os.walk(out):
        for name in files:
            full = os.path.join(root, name)
            if not name.endswith('.java'):
                continue
            text = open(full, encoding='utf-8').read()
            new = re.sub(r'\.getInventory\(\)\.setSelectedSlot\(([^;]+)\);', r'.getInventory().selected = \1;', text)
            if new != text:
                open(full, 'w', encoding='utf-8').write(new)

    # Spears came later than 1.21.4
    os.remove(os.path.join(out, A, 'modules', 'SpearLunge.java'))
    os.remove(os.path.join(out, A, 'modules', 'SpearSmash.java'))
    edit(A + 'Client.java', [('import aicoder.assist.modules.SpearLunge;\n', ''), ('        add(new SpearLunge());\n', ''),
                             ('import aicoder.assist.modules.SpearSmash;\n', ''), ('        add(new SpearSmash());\n', '')])
    edit(A + 'modules/OffhandTotem.java', [('import net.minecraft.client.input.KeyEvent;\n', ''), ('screen.keyPressed(new KeyEvent(code, 0, 0))', 'screen.keyPressed(code, 0, 0)')])

    # Screens get raw numbers here, not event objects
    edit(A + 'aurora/AuroraScreen.java', [
        ('import net.minecraft.client.input.KeyEvent;\n', ''),
        ('import net.minecraft.client.input.MouseButtonEvent;\n', ''),
        ('public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {\n        double px = click.x() * scale(), py = click.y() * scale();',
         'public boolean mouseClicked(double clickX, double clickY, int button) {\n        double px = clickX * scale(), py = clickY * scale();'),
        ('return super.mouseClicked(click, doubled);', 'return super.mouseClicked(clickX, clickY, button);'),
        ('public boolean keyPressed(KeyEvent event) {', 'public boolean keyPressed(int keyCode, int scanCode, int modifiers) {'),
        ('return super.keyPressed(event);', 'return super.keyPressed(keyCode, scanCode, modifiers);'),
        ('public boolean keyReleased(KeyEvent event) {', 'public boolean keyReleased(int keyCode, int scanCode, int modifiers) {'),
        ('return super.keyReleased(event);', 'return super.keyReleased(keyCode, scanCode, modifiers);'),
        ('public boolean mouseReleased(MouseButtonEvent click) {', 'public boolean mouseReleased(double clickX, double clickY, int button) {'),
        ('return super.mouseReleased(click);', 'return super.mouseReleased(clickX, clickY, button);'),
        # This version does not blur the world behind a screen unless the screen asks for it
        ('        R.begin(graphics, guiScale);', '        renderBlurredBackground();\n        R.begin(graphics, guiScale);'),
        ('click.button()', 'button'),
        ('event.key()', 'keyCode'),
    ])
else:
    sys.exit('port_version: no rules for ' + version)
print('ported to', version)
