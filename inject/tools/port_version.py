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
elif version == '1.8.9':
    # A far older game: no official names (the sources here use MCP's), LWJGL 2, fixed-function OpenGL,
    # Java 8. src-1.8.9/aicoder/assist/compat answers the newer names the shared code uses with the old
    # game, so the menu, the settings and Telly Bridge are the same source; what the old game has no
    # counterpart for is left out.
    A = 'aicoder/assist/'
    KEEP = ('TellyBridge', 'LeftClicker', 'RightClicker', 'TriggerBot', 'CrosshairDrift')
    dropped = []
    for name in sorted(os.listdir(os.path.join(out, A, 'modules'))):
        if name.endswith('.java') and name[:-5] not in KEEP:
            os.remove(os.path.join(out, A, 'modules', name))
            dropped.append(name[:-5])
    os.remove(os.path.join(out, A, 'gfx', 'Mesh.java'))
    # The licence check is not built for this version: License here is the one from src-1.8.9, which never locks
    for name in ('HwidProvider.java',):
        os.remove(os.path.join(out, A, 'license', name))

    # Two modules keep their settings and get a body written for the old game (src-1.8.9/.../*.tail)
    for name in ('TriggerBot', 'CrosshairDrift'):
        full = os.path.join(out, A, 'modules', name + '.java')
        text = open(full, encoding='utf-8').read()
        tail = os.path.join(out, A, 'modules', name + '.tail')
        text = text[:text.index('    @EventHandler\n')] + open(tail, encoding='utf-8').read()
        os.remove(tail)
        open(full, 'w', encoding='utf-8').write(text)

    client = [('import aicoder.assist.modules.%s;\n' % m, '') for m in dropped if m != 'Clicks']
    client += [('        add(new %s());\n' % m, '') for m in dropped if m != 'Clicks']
    client += [
        ('import net.minecraft.client.Minecraft;', 'import aicoder.assist.compat.Minecraft;'),
        ('import net.minecraft.client.gui.screens.TitleScreen;\n', ''),
        ('import net.minecraft.network.chat.Component;\nimport net.minecraft.network.chat.MutableComponent;\nimport net.minecraft.network.chat.Style;\nimport org.lwjgl.glfw.GLFW;\n', ''),
        ('    private record Handler(Module module, Method method, int priority) {\n    }',
         '    private static final class Handler {\n        final Module module;\n        final Method method;\n        final int priority;\n\n'
         '        Handler(Module module, Method method, int priority) {\n            this.module = module;\n            this.method = method;\n            this.priority = priority;\n        }\n    }'),
        ('list.sort(Comparator.comparingInt(Handler::priority).reversed());', 'list.sort(Comparator.comparingInt((Handler h) -> h.priority).reversed());'),
        ('GLFW.GLFW_KEY_RIGHT_SHIFT', '344'), ('GLFW.GLFW_KEY_INSERT', '260'),
        ('        aicoder.assist.license.License.begin();\n', ''),
        ('return com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(menuKey).getDisplayName().getString();', 'return aicoder.assist.compat.Keys.name(menuKey);'),
        ('        // Without a licence nothing runs: whatever was left switched on goes off\n        aicoder.assist.license.License.tick();\n'
         '        if (aicoder.assist.license.License.locked()) {\n            for (Module module : modules) if (module.isActive()) module.setActive(false);\n        }\n\n', ''),
        ('mc.level != null && mc.gui != null', 'mc.level != null'),
        ('if (mc.screen instanceof AuroraScreen) mc.screen.onClose();', 'if (mc.screen instanceof AuroraScreen) ((AuroraScreen) mc.screen).onClose();'),
        ('mc.screen instanceof TitleScreen', 'mc.screen instanceof net.minecraft.client.gui.GuiMainMenu'),
        ('        if (mc.gui == null || mc.level == null) return;\n\n'
         '        MutableComponent text = Component.literal("[" + from + "] ").withStyle(Style.EMPTY.withColor(color & 0xFFFFFF));\n'
         '        text.append(Component.literal(message).withStyle(Style.EMPTY.withColor(0xEEFAFF)));\n'
         '        mc.gui.getChat().addMessage(text);',
         '        if (mc.level == null) return;\n\n'
         '        // The old game has sixteen chat colours and no others: the name in aqua, the message in white\n'
         '        mc.real().ingameGUI.getChatGUI().printChatMessage(new net.minecraft.util.ChatComponentText("\\u00a7b[" + from + "] \\u00a7f" + message));'),
    ]
    edit(A + 'Client.java', client)
    edit(A + 'api/Module.java', [
        ('import net.minecraft.client.Minecraft;', 'import aicoder.assist.compat.Minecraft;'),
    ])
    edit(A + 'Injector.java', [('1.21.11', '1.8.9'),
                               # Java 8 has no ProcessHandle
                               ('String.valueOf(ProcessHandle.current().pid())', 'java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]'),
                               ('started without mods.', 'started plain or with OptiFine.'),
                               # A newer Java attaching to the Java 8 this game runs on reads the old side's "0"
                               # (its word for success) as a failure. Whether the agent really failed is in the
                               # reason it leaves behind.
                               ('                machine.loadAgent(payload, licenceKey);\n',
                                '                try {\n                    machine.loadAgent(payload, licenceKey);\n'
                                '                } catch (com.sun.tools.attach.AgentLoadException e) {\n'
                                '                    if (!String.valueOf(e.getMessage()).endsWith(": 0") || machine.getSystemProperties().getProperty(Agent.ERROR_PROPERTY) != null) throw e;\n'
                                '                }\n')])
    # No module of the Explosives column exists in this version
    edit(A + 'api/Category.java', [('List.of(EXPLOSIVES, MELEE, UTILITY)', 'List.of(MELEE, UTILITY)')])
    edit(A + 'gfx/GuiRenderer.java', [('import net.minecraft.client.gui.GuiGraphics;', 'import aicoder.assist.compat.GuiGraphics;')])
    edit(A + 'aurora/AuroraScreen.java', [
        ('import net.minecraft.client.gui.GuiGraphics;\nimport net.minecraft.client.gui.screens.Screen;\nimport com.mojang.blaze3d.platform.InputConstants;\n'
         'import net.minecraft.client.input.KeyEvent;\nimport net.minecraft.client.input.MouseButtonEvent;\nimport org.lwjgl.glfw.GLFW;\nimport net.minecraft.network.chat.Component;\n',
         'import aicoder.assist.compat.Component;\nimport aicoder.assist.compat.GuiGraphics;\nimport aicoder.assist.compat.KeyEvent;\nimport aicoder.assist.compat.Keys;\n'
         'import aicoder.assist.compat.MouseButtonEvent;\nimport aicoder.assist.compat.Screen;\n'),
        ('minecraft.getWindow().getWidth()', 'minecraft.real().displayWidth'),
        ('minecraft.getWindow().getHeight()', 'minecraft.real().displayHeight'),
        ('Math.min(0.1, minecraft.getDeltaTracker().getRealtimeDeltaTicks() / 20.0)', 'frameSeconds()'),
        ('InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString()', 'Keys.name(key)'),
        ('GLFW.GLFW_KEY_ESCAPE', '256'),
    ])
    for name in KEEP:
        full = os.path.join(out, A, 'modules', name + '.java')
        text = open(full, encoding='utf-8').read()
        text = text.replace('import net.minecraft.client.KeyMapping;', 'import aicoder.assist.compat.KeyMapping;')
        text = text.replace('import net.minecraft.core.BlockPos;', 'import net.minecraft.util.BlockPos;')
        text = text.replace('import net.minecraft.world.item.BlockItem;\n', '').replace('instanceof BlockItem', 'instanceof net.minecraft.item.ItemBlock')
        text = text.replace('import net.minecraft.world.phys.HitResult;', 'import aicoder.assist.compat.HitResult;')
        text = text.replace('import net.minecraft.world.entity.Entity;', 'import net.minecraft.entity.Entity;')
        text = text.replace('import net.minecraft.world.phys.Vec3;', 'import net.minecraft.util.Vec3;')
        text = re.sub(r'import net\.minecraft\.(world\.entity\.LivingEntity|world\.entity\.player\.Player|world\.phys\.EntityHitResult|world\.phys\.AABB|util\.Mth);\n', '', text)
        open(full, 'w', encoding='utf-8').write(text)
else:
    sys.exit('port_version: no rules for ' + version)
print('ported to', version)
