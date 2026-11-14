#!/usr/bin/env python3
"""Regenerates the Minecraft 1.21.11 ports from the 26.3 sources.

26.3 is unobfuscated and Meteor there uses Mojang's names; 1.21.11 is built against Yarn names and a
slightly older Meteor API. The five addons are otherwise identical, so the ports are produced by copying
each project and rewriting the names that differ, instead of maintaining ten source trees by hand.

    python3 mc-1.21.11/port.py        # run from the repository root, then build each directory in here
"""
import json, os, re, shutil, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'mc-1.21.11')
MC = '1.21.11'

# source directory (relative to the repo root), output directory, jar name
PROJECTS = [
    ('.', 'cyber', 'AiCoder賽博龐克'),
    ('earth-science', 'earth-science', 'AiCoder地球科學'),
    ('liquid-glass', 'liquid-glass', 'AiCoder液態玻璃'),
    ('columns', 'columns', 'AiCoder多欄賽博龐克'),
    ('glass-columns', 'glass-columns', 'AiCoder液態玻璃直立式'),
    ('aurora-glass', 'aurora-glass', 'AiCoder極光玻璃'),
    ('aurora-columns', 'aurora-columns', 'AiCoder極光玻璃直立式'),
    ('combat-macros', 'combat-macros', 'AiCoder戰鬥巨集'),
]

# Plain text replacements, applied in order to every Java file
RENAMES = [
    # Minecraft classes
    ('net.minecraft.client.gui.GuiGraphicsExtractor', 'net.minecraft.client.gui.DrawContext'),
    ('GuiGraphicsExtractor', 'DrawContext'),
    ('net.minecraft.client.gui.screens.Screen', 'net.minecraft.client.gui.screen.Screen'),
    ('net.minecraft.util.Mth', 'net.minecraft.util.math.MathHelper'),
    ('Mth.', 'MathHelper.'),
    ('net.minecraft.client.input.MouseButtonEvent', 'net.minecraft.client.gui.Click'),
    ('MouseButtonEvent', 'Click'),
    ('net.minecraft.client.input.KeyEvent', 'net.minecraft.client.input.KeyInput'),
    ('KeyEvent', 'KeyInput'),
    ('net.minecraft.nbt.CompoundTag', 'net.minecraft.nbt.NbtCompound'),
    ('CompoundTag', 'NbtCompound'),
    ('.getBooleanOr(', '.getBoolean('),
    ('.getStringOr(', '.getString('),
    ('net.minecraft.resources.Identifier', 'net.minecraft.util.Identifier'),
    ('Identifier.fromNamespaceAndPath(', 'Identifier.of('),
    ('net.minecraft.sounds.SoundEvent', 'net.minecraft.sound.SoundEvent'),
    ('SoundEvent.createVariableRangeEvent(', 'SoundEvent.of('),
    ('net.minecraft.client.resources.sounds.SimpleSoundInstance', 'net.minecraft.client.sound.PositionedSoundInstance'),
    ('SimpleSoundInstance.forUI(', 'PositionedSoundInstance.ui('),
    # Input constants: 26.3 has its own, 1.21.11 uses GLFW's
    ('import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;', 'import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;'),
    ('import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;', 'import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;'),
    ('import static com.mojang.blaze3d.platform.InputConstants.KEY_F;', 'import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;'),
    ('import static com.mojang.blaze3d.platform.InputConstants.MOD_CONTROL;', 'import static org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;'),
    ('import static com.mojang.blaze3d.platform.InputConstants.MOD_SUPER;', 'import static org.lwjgl.glfw.GLFW.GLFW_MOD_SUPER;'),
    ('import static com.mojang.blaze3d.platform.InputConstants.*;', 'import static org.lwjgl.glfw.GLFW.*;'),
    # Game state
    ('mc.gui.screen()', 'mc.currentScreen'),
    ('mc.gui.setScreen(', 'mc.setScreen('),
    ('mc.mouseHandler.xpos()', 'mc.mouse.getX()'),
    ('mc.mouseHandler.ypos()', 'mc.mouse.getY()'),
    ('import org.lwjgl.sdl.SDLMouse;', 'import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;'),
    ('SDLMouse.SDL_WarpMouseInWindow(mc.getWindow().handle(), (float) mouseX, (float) mouseY);',
     'glfwSetCursorPos(mc.getWindow().getHandle(), mouseX, mouseY);'),
    ('mc.getFps()', 'mc.getCurrentFps()'),
    ('.getTranslatedKeyMessage()', '.getBoundKeyLocalizedText()'),
    ('import org.jspecify.annotations.NonNull;\n', ''),
    ('@NonNull ', ''),
    # Textures
    ('com.mojang.renderpearl.api.GpuFormat', 'com.mojang.blaze3d.textures.TextureFormat'),
    ('com.mojang.renderpearl.api.textures.FilterMode', 'com.mojang.blaze3d.textures.FilterMode'),
    ('GpuFormat.RGBA8_UNORM', 'TextureFormat.RGBA8'),
    ('.getTextureView()', '.getGlTextureView()'),
]

# Gameplay code (players, items, blocks, clicks). Only applied to the projects named in GAMEPLAY_PROJECTS:
# several of these are short enough to hit unrelated GUI code.
GAMEPLAY_PROJECTS = {'combat-macros'}
GAMEPLAY = [
    # Classes
    ('net.minecraft.core.BlockPos', 'net.minecraft.util.math.BlockPos'),
    ('BlockPos.MutableBlockPos', 'BlockPos.Mutable'),
    ('net.minecraft.world.InteractionHand', 'net.minecraft.util.Hand'),
    ('InteractionHand', 'Hand'),
    ('net.minecraft.world.InteractionResult', 'net.minecraft.util.ActionResult'),
    ('InteractionResult', 'ActionResult'),
    ('SwingSource.PREDICTED', 'SwingSource.CLIENT'),
    ('net.minecraft.world.entity.boss.enderdragon.EndCrystal', 'net.minecraft.entity.decoration.EndCrystalEntity'),
    ('net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge', 'net.minecraft.entity.projectile.AbstractWindChargeEntity'),
    ('net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl', 'net.minecraft.entity.projectile.thrown.EnderPearlEntity'),
    ('net.minecraft.world.entity.player.Player', 'net.minecraft.entity.player.PlayerEntity'),
    ('net.minecraft.world.entity.LivingEntity', 'net.minecraft.entity.LivingEntity'),
    ('net.minecraft.world.entity.Entity', 'net.minecraft.entity.Entity'),
    ('net.minecraft.world.item.ItemStack', 'net.minecraft.item.ItemStack'),
    ('net.minecraft.world.item.Items', 'net.minecraft.item.Items'),
    ('import net.minecraft.world.item.component.SwingAnimation;\n', ''),
    ('net.minecraft.world.level.Level', 'net.minecraft.world.World'),
    ('net.minecraft.world.level.block.Blocks', 'net.minecraft.block.Blocks'),
    ('net.minecraft.world.level.block.state.BlockState', 'net.minecraft.block.BlockState'),
    ('net.minecraft.world.level.block.state.properties.BlockStateProperties', 'net.minecraft.state.property.Properties'),
    ('BlockStateProperties.RESPAWN_ANCHOR_CHARGES', 'Properties.CHARGES'),
    ('net.minecraft.world.phys.BlockHitResult', 'net.minecraft.util.hit.BlockHitResult'),
    ('net.minecraft.world.phys.EntityHitResult', 'net.minecraft.util.hit.EntityHitResult'),
    ('net.minecraft.world.phys.HitResult', 'net.minecraft.util.hit.HitResult'),
    ('net.minecraft.world.phys.AABB', 'net.minecraft.util.math.Box'),
    ('net.minecraft.world.phys.Vec3', 'net.minecraft.util.math.Vec3d'),
    # Game state
    ('mc.level.dimension() == Level.NETHER', 'mc.world.getRegistryKey() == World.NETHER'),
    ('mc.level.entitiesForRendering()', 'mc.world.getEntities()'),
    ('mc.hitResult', 'mc.crosshairTarget'),
    ('mc.level', 'mc.world'),
    ('mc.gameMode.isDestroying()', 'mc.interactionManager.isBreakingBlock()'),
    ('mc.gameMode.attack(', 'mc.interactionManager.attackEntity('),
    ('mc.gameMode.useItemOn(', 'mc.interactionManager.interactBlock('),
    ('mc.gameMode.useItem(', 'mc.interactionManager.interactItem('),
    ('mc.gameMode', 'mc.interactionManager'),
    ('mc.options.keyUse', 'mc.options.useKey'),
    ('mc.options.keyAttack', 'mc.options.attackKey'),
    ('.setDown(', '.setPressed('),
    # Methods
    ('.getMainHandItem()', '.getMainHandStack()'),
    ('.getOffhandItem()', '.getOffHandStack()'),
    ('.getItemInHand(', '.getStackInHand('),
    ('.getInventory().getItem(', '.getInventory().getStack('),
    ('.getCooldowns().isOnCooldown(', '.getItemCooldownManager().isCoolingDown('),
    ('.isShiftKeyDown()', '.isSneaking()'),
    ('.getEyePosition(event.tickDelta)', '.getCameraPosVec(event.tickDelta)'),
    ('.getEyePosition()', '.getEyePos()'),
    ('.getViewVector(', '.getRotationVec('),
    ('.getPosition(tickDelta)', '.getLerpedPos(tickDelta)'),
    ('.position()', '.getEntityPos()'),
    ('.getDeltaMovement()', '.getVelocity()'),
    ('.getKnownMovement()', '.getMovement()'),
    ('.distanceToSqr(', '.squaredDistanceTo('),
    ('.blockPosition()', '.getBlockPos()'),
    ('.onGround()', '.isOnGround()'),
    ('.tickCount', '.age'),
    ('.getYRot()', '.getYaw()'), ('.setYRot(', '.setYaw('),
    ('.getXRot()', '.getPitch()'), ('.setXRot(', '.setPitch('),
    ('.getYsize()', '.getLengthY()'),
    ('.getEyeHeight()', '.getStandingEyeHeight()'),
    ('.dot(', '.dotProduct('),
    ('.above()', '.up()'),
    ('.is(', '.isOf('),
    ('.getValue(', '.get('),
    ('.getDirection()', '.getSide()'),
    ('.relative(', '.offset('),
    ('.immutable()', '.toImmutable()'),
]
GAMEPLAY_WORDS = [
    ('EndCrystal', 'EndCrystalEntity'), ('AbstractWindCharge', 'AbstractWindChargeEntity'),
    ('ThrownEnderpearl', 'EnderPearlEntity'), ('Player', 'PlayerEntity'), ('AABB', 'Box'), ('Vec3', 'Vec3d'),
]

# Whole-word renames of the input constants themselves
WORDS = [
    ('MOUSE_BUTTON_LEFT', 'GLFW_MOUSE_BUTTON_LEFT'), ('MOUSE_BUTTON_RIGHT', 'GLFW_MOUSE_BUTTON_RIGHT'),
    ('KEY_F', 'GLFW_KEY_F'), ('MOD_CONTROL', 'GLFW_MOD_CONTROL'), ('MOD_SUPER', 'GLFW_MOD_SUPER'),
]


def port_gameplay(text):
    for old, new in GAMEPLAY:
        text = text.replace(old, new)
    for old, new in GAMEPLAY_WORDS:
        text = re.sub(r'(?<![A-Za-z_.])' + old + r'(?![A-Za-z_])', new, text)

    # 1.21.11 swings without naming an animation
    text = re.sub(r'\n *SwingAnimation swing = [^;]+;', '', text)
    text = text.replace('SwingAnimation swing', 'Object swing')
    text = text.replace('swingIfPredicted(result, swing)', 'swingIfPredicted(result)').replace('swingIfPredicted(ActionResult result, Object swing)', 'swingIfPredicted(ActionResult result)')
    text = text.replace('mc.player.swing(Hand.MAIN_HAND, swing, false)', 'mc.player.swingHand(Hand.MAIN_HAND)')
    return text


def port_java(text, gameplay=False):
    if gameplay:
        text = port_gameplay(text)
    for old, new in RENAMES:
        text = text.replace(old, new)
    for old, new in WORDS:
        text = re.sub(r'(?<![A-Za-z_])' + old + r'\b', new, text)

    # 1.21.11's text renderer needs no graphics handle, so the accessor mixin and its uses go away
    text = re.sub(r'import [\w.]+\.mixin\.GuiRendererAccessor;\n', '', text)
    text = re.sub(r'\n *DrawContext graphics = \(\(GuiRendererAccessor\) renderer\)\.\w+\$getGraphics\(\);\n', '\n', text)
    text = text.replace('tr.begin(graphics, first.scale);', 'tr.begin(first.scale);')
    if 'DrawContext' not in text.replace('import net.minecraft.client.gui.DrawContext;', ''):
        text = text.replace('import net.minecraft.client.gui.DrawContext;\n', '')

    # Meteor API differences
    text = re.sub(r'Input\.modsMatch\((\w+)\.modifiers\(\), (\w+)\)', r'\1.modifiers() == \2', text)
    text = text.replace('import com.mojang.datafixers.util.Pair;', 'import net.minecraft.util.Pair;')
    text = re.sub(r'\b(p|entry)\.getFirst\(\)', r'\1.getLeft()', text)
    text = re.sub(r'\b(p|entry)\.getSecond\(\)', r'\1.getRight()', text)
    text = re.sub(r'Pair\.of\(([^()]+)\)', r'new Pair<>(\1)', text)
    text = text.replace('renderer.fill(', 'renderer.quad(')
    # 1.21.11 has no setting for the modules help text; it is always shown
    text = text.replace('if (theme.modulesHelpText()) {', 'if (true) {')
    if 'Input.' not in text:
        text = text.replace('import meteordevelopment.meteorclient.utils.misc.input.Input;\n', '')

    # Java 21 has no unnamed variables
    text = text.replace('.onChanged(_ ->', '.onChanged(ignored ->')
    return text


def port_project(src_rel, name, jar):
    src = os.path.join(ROOT, src_rel)
    dst = os.path.join(OUT, name)

    for sub in ('src', 'gradle', 'design'):
        shutil.rmtree(os.path.join(dst, sub), ignore_errors=True)
    os.makedirs(dst, exist_ok=True)

    shutil.copytree(os.path.join(src, 'src'), os.path.join(dst, 'src'))
    shutil.copytree(os.path.join(src, 'gradle'), os.path.join(dst, 'gradle'))
    for f in ('gradlew', 'gradlew.bat', '.gitignore', 'gradle.properties', 'settings.gradle.kts'):
        shutil.copy2(os.path.join(src, f), os.path.join(dst, f))

    # Build files
    toml = open(os.path.join(dst, 'gradle/libs.versions.toml'), encoding='utf-8').read()
    toml = re.sub(r'jdk = "\d+"', 'jdk = "21"', toml)
    toml = re.sub(r'minecraft = "[^"]+"', 'minecraft = "%s"\nyarn = "%s+build.6"' % (MC, MC), toml)
    toml = re.sub(r'meteor = "[^"]+"', 'meteor = "%s-SNAPSHOT"' % MC, toml)
    toml = toml.replace('fabric-loader = { module', 'yarn = { module = "net.fabricmc:yarn", version.ref = "yarn" }\nfabric-loader = { module')
    toml = toml.replace('id = "net.fabricmc.fabric-loom"', 'id = "net.fabricmc.fabric-loom-remap"')
    open(os.path.join(dst, 'gradle/libs.versions.toml'), 'w', encoding='utf-8').write(toml)

    gradle = open(os.path.join(src, 'build.gradle.kts'), encoding='utf-8').read()
    gradle = gradle.replace('''    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)

    // Meteor
    implementation(libs.meteor.client)''', '''    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn) { classifier("v2") })
    modImplementation(libs.fabric.loader)

    // Meteor
    modImplementation(libs.meteor.client)''')
    assert 'modImplementation(libs.meteor.client)' in gradle, name
    # Loom's remapped jar is the one that runs in the game, so that is the one that gets the final name
    gradle = re.sub(r' *// The file players download: .*\n *archiveFileName = "[^"]*"\n', '', gradle)
    final_name = '%s-%s.jar' % (jar.encode('unicode_escape').decode('ascii'), MC)
    gradle = gradle.replace('    jar {\n', '''    remapJar {
        // The file players download: %s-%s.jar
        archiveFileName = "%s"
    }

    jar {
''' % (jar, MC, final_name), 1)
    assert 'remapJar' in gradle, name
    open(os.path.join(dst, 'build.gradle.kts'), 'w', encoding='utf-8').write(gradle)

    # Java
    java_root = os.path.join(dst, 'src/main/java')
    for folder, _, files in os.walk(java_root):
        for f in files:
            path = os.path.join(folder, f)
            if f == 'GuiRendererAccessor.java':
                os.remove(path)
                continue
            text = open(path, encoding='utf-8').read()
            open(path, 'w', encoding='utf-8').write(port_java(text, name in GAMEPLAY_PROJECTS))

    # Mixin config: drop the accessor that no longer exists, and the whole config if nothing is left
    res = os.path.join(dst, 'src/main/resources')
    mod_path = os.path.join(res, 'fabric.mod.json')
    mod = open(mod_path, encoding='utf-8').read()
    for f in os.listdir(res):
        if not f.endswith('.mixins.json'):
            continue
        cfg_path = os.path.join(res, f)
        cfg = json.load(open(cfg_path, encoding='utf-8'))
        cfg['client'] = [c for c in cfg.get('client', []) if c != 'GuiRendererAccessor']
        if cfg['client']:
            json.dump(cfg, open(cfg_path, 'w', encoding='utf-8'), indent=2)
        else:
            os.remove(cfg_path)
            mod = re.sub(r'  "mixins": \[\s*"%s"\s*\],\n' % re.escape(f), '', mod)
            shutil.rmtree(os.path.join(java_root, *cfg['package'].split('.')), ignore_errors=True)
    open(mod_path, 'w', encoding='utf-8').write(mod)


if __name__ == '__main__':
    only = sys.argv[1:]
    for src_rel, name, jar in PROJECTS:
        if not only or name in only:
            port_project(src_rel, name, jar)
            print('ported', name)
