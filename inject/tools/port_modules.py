#!/usr/bin/env python3
"""Regenerates src/aicoder/assist/modules/ from the Meteor addon in ../combat-macros.

The modules are written once, against Meteor. Here Meteor is replaced by the small look-alike API in
aicoder.assist.api, and the few places where Minecraft 1.21.11 differs from 26.3 are rewritten.

    python3 tools/port_modules.py        # run from the inject/ directory
"""
import os, re

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(HERE, '..', 'combat-macros', 'src', 'main', 'java', 'aicoder', 'combat')
DST = os.path.join(HERE, 'src', 'aicoder', 'assist', 'modules')
FILES = ['AnchorMacro', 'CrystalMacro', 'CrosshairDrift', 'LeftClicker', 'MaceSwap', 'RightClicker', 'WindPearl', 'Clicks']


def port(text):
    text = text.replace('package aicoder.combat;', 'package aicoder.assist.modules;')

    # Meteor imports go; the stand-in API comes in their place
    text = re.sub(r'import (static )?meteordevelopment\.[^\n]*\n', '', text)
    text = text.replace('\nimport ', '\nimport aicoder.assist.api.*;\nimport aicoder.assist.api.Module;\nimport aicoder.assist.api.Events.*;\n\nimport static aicoder.assist.Client.mc;\n\nimport ', 1)

    text = text.replace('AiCoderTab.', 'Category.')
    text = text.replace('mc.gui.screen()', 'mc.screen')
    text = re.sub(r'\(\(KeyMappingAccessor\) (mc\.options\.\w+)\)\.meteor\$getKey\(\)', r'Input.keyOf(\1)', text)

    # There is no friends list here: drop the setting and the check
    text = re.sub(r'    private final Setting<Boolean> (friends|ignoreFriends) = .*?\n    \);\n\n', '', text, flags=re.S)
    text = re.sub(r'\n *if \((friends|ignoreFriends)\.get\(\) && Friends\.get\(\)\.isFriend\(player\)\) return( false)?;', '', text)
    text = re.sub(r'if \(target instanceof Player player\) \{\n\s*\} else if \(([^\n]*)\) \{', r'if (!(target instanceof Player) && \1) {', text)

    # 1.21.11: an arm swing does not name an animation, and a predicted swing is called CLIENT
    text = text.replace('import net.minecraft.world.item.component.SwingAnimation;\n', '')
    text = re.sub(r'\n *SwingAnimation swing = [^;]+;', '', text)
    text = text.replace('swingIfPredicted(result, swing)', 'swingIfPredicted(result)')
    text = text.replace('swingIfPredicted(InteractionResult result, SwingAnimation swing)', 'swingIfPredicted(InteractionResult result)')
    text = text.replace('mc.player.swing(InteractionHand.MAIN_HAND, swing, false)', 'mc.player.swing(InteractionHand.MAIN_HAND)')
    text = text.replace('SwingSource.PREDICTED', 'SwingSource.CLIENT')
    return text


def port_theme(text):
    """Fx, Chrome, Aurora and Style of the aurora theme, moved onto the stand-in renderer."""
    text = text.replace('package aicoder.aurora.gui;', 'package aicoder.assist.aurora;')
    text = re.sub(r'import aicoder\.aurora\.mixin\.[^\n]*\n', '', text)
    text = re.sub(r'import meteordevelopment\.meteorclient\.(gui\.GuiTheme|gui\.renderer\.GuiRenderer|renderer\.text\.TextRenderer|utils\.render\.color\.Color);',
                  lambda m: 'import aicoder.assist.gfx.%s;' % m.group(1).split('.')[-1], text)
    text = re.sub(r'import meteordevelopment\.[^\n]*\n', '', text)
    text = re.sub(r'import net\.minecraft\.client\.gui\.GuiGraphicsExtractor;\n', '', text)
    # Text here always goes through the bundled font
    text = text.replace('return !Config.get().customFont.get();', 'return false;')
    text = re.sub(r'\n *GuiGraphicsExtractor graphics = [^\n]*\n', '\n', text)
    text = text.replace('tr.begin(graphics, first.scale);', 'tr.begin(first.scale);')
    return text


THEME_SRC = os.path.join(HERE, '..', 'aurora-glass', 'src', 'main', 'java', 'aicoder', 'aurora', 'gui')
THEME_DST = os.path.join(HERE, 'src', 'aicoder', 'assist', 'aurora')
os.makedirs(THEME_DST, exist_ok=True)
for name in ['Fx', 'Chrome', 'Aurora', 'Style']:
    source = open(os.path.join(THEME_SRC, name + '.java'), encoding='utf-8').read()
    open(os.path.join(THEME_DST, name + '.java'), 'w', encoding='utf-8').write(port_theme(source))

os.makedirs(DST, exist_ok=True)
for name in FILES:
    source = open(os.path.join(SRC, name + '.java'), encoding='utf-8').read()
    open(os.path.join(DST, name + '.java'), 'w', encoding='utf-8').write(port(source))
print('ported', len(FILES), 'files')
