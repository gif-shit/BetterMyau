package myau;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import myau.mixin.MixinItemEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import sun.misc.Unsafe;

public final class ItemEffectsRegression {
    private static int checks;
    private static class Renderer extends MixinItemEffects { }
    private static class Player extends EntityPlayerSP {
        Map<Potion, PotionEffect> effects;
        Player() { super(null, null, null, null); }
        public PotionEffect func_70660_b(Potion potion) { return effects.get(potion); }
    }
    private static class Scale extends Args {
        Scale(float value) { super(new Object[] {value, value, value}); }
        public <T> void set(int index, T value) { values[index] = value; }
        public void setAll(Object... values) { System.arraycopy(values, 0, this.values, 0, 3); }
    }

    private static void check(boolean condition, String message) {
        ++checks;
        if (!condition) throw new AssertionError(message);
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.0001f, message + ": " + actual);
    }

    public static void main(String[] args) throws Exception {
        Field access = Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        Unsafe unsafe = (Unsafe) access.get(null);
        Field registered = net.minecraft.init.Bootstrap.class.getDeclaredField("field_151355_a");
        registered.setAccessible(true);
        registered.setBoolean(null, true);
        net.minecraft.block.Block.func_149671_p();
        net.minecraft.item.Item.func_150900_l();
        Minecraft mc = Minecraft.func_71410_x();
        Player player = (Player) unsafe.allocateInstance(Player.class);
        player.effects = new HashMap<>();
        player.field_82175_bq = true;
        player.field_70733_aJ = 0.4f;
        mc.field_71439_g = player;
        mc.field_71441_e = (WorldClient) unsafe.allocateInstance(WorldClient.class);
        ItemEffectsModule effects = new ItemEffectsModule();
        MyauClient.moduleManager.modules.put(ItemEffectsModule.class, effects);
        effects.itemSize.setModule(effects);
        effects.attackSpeed.setModule(effects);
        check(!effects.isEnabled() && ModuleCategory.of(effects) == ModuleCategory.RENDER, "ItemEffects defaults off in Render");
        near(effects.itemSize.getValue(), 1, "default item size");
        near(effects.attackSpeed.getValue(), 1, "default attack animation speed");
        check(effects.itemSize.setValue(0.25f) && effects.itemSize.setValue(3f), "item size accepts both endpoints");
        check(!effects.itemSize.setValue(0.24f) && !effects.itemSize.setValue(3.01f), "item size rejects values outside its range");
        check(effects.attackSpeed.setValue(0.1f) && effects.attackSpeed.setValue(5f), "animation speed accepts both endpoints");
        check(!effects.attackSpeed.setValue(0.09f) && !effects.attackSpeed.setValue(5.01f), "animation speed rejects values outside its range");
        near(effects.sizeMultiplier(), 1, "disabled module leaves the item size unchanged");
        near(effects.renderSwingProgress(player, 0.4f, 0.5f), 0.4f, "disabled module preserves vanilla animation");
        effects.itemSize.setValue(1f);
        effects.attackSpeed.setValue(1f);
        effects.setEnabled(true);
        near(effects.renderSwingProgress(player, 0.4f, 0.5f), 0.4f, "one is exactly the vanilla animation");
        Renderer renderer = new Renderer();
        Method resize = MixinItemEffects.class.getDeclaredMethod("resizeHeldItem", Args.class);
        resize.setAccessible(true);
        for (float base : new float[] {0.4f, 0.38f}) {
            for (float size : new float[] {0.25f, 1f, 3f}) {
                effects.itemSize.setValue(size);
                Scale scale = new Scale(base);
                resize.invoke(renderer, scale);
                for (int axis = 0; axis < 3; axis++) near((Float) scale.get(axis), base * size, "item and map scaling is uniform");
            }
        }
        effects.attackSpeed.setValue(5f);
        effects.onSwing();
        effects.onPostTick(new PostTickEvent());
        near(effects.renderSwingProgress(player, 0.2f, 0.6f), 0.5f, "five times speed reaches half a swing after six tenths of a tick");
        effects.onPostTick(new PostTickEvent());
        near(effects.renderSwingProgress(player, 0.4f, 0.5f), 0, "fast visual swing ends before the vanilla swing");
        effects.attackSpeed.setValue(0.1f);
        effects.onSwing();
        for (int tick = 0; tick < 6; tick++) effects.onPostTick(new PostTickEvent());
        near(effects.renderSwingProgress(player, 0, 1), 0.1f, "slow visual swing continues after the vanilla swing ends");
        mc.paused = true;
        effects.onPostTick(new PostTickEvent());
        near(effects.renderSwingProgress(player, 0, 1), 0.1f, "paused game freezes the visual clock");
        mc.paused = false;
        for (int tick = 6; tick < 61; tick++) effects.onPostTick(new PostTickEvent());
        near(effects.renderSwingProgress(player, 0, 1), 0, "slow visual swing finishes normally");
        effects.attackSpeed.setValue(2f);
        player.effects.put(Potion.field_76422_e, new PotionEffect(Potion.field_76422_e.field_76415_H, 200, 1));
        effects.onSwing();
        effects.onPostTick(new PostTickEvent());
        near(effects.renderSwingProgress(player, 0, 1), 0.5f, "speed multiplier respects vanilla haste animation length");
        effects.onWorldLoad(new WorldLoadEvent());
        near(effects.renderSwingProgress(player, 0.4f, 1), 0, "world change clears pending visual animation");
        effects.onSwing();
        effects.setEnabled(false);
        near(effects.renderSwingProgress(player, 0.4f, 1), 0.4f, "disable restores vanilla animation immediately");
        near(effects.sizeMultiplier(), 1, "disable restores vanilla size immediately");
        check(player.field_82175_bq && player.field_70733_aJ == 0.4f && mc.handler == null, "visual options leave real swing state and networking untouched");
        hooks(args[0], args[1]);
        System.out.println("ItemEffects visual checks passed: " + checks);
    }

    private static void hooks(String gamePath, String modPath) throws Exception {
        try (JarFile game = new JarFile(gamePath); JarFile mod = new JarFile(modPath)) {
            ClassNode renderer = new ClassNode();
            new ClassReader(game.getInputStream(game.getJarEntry("net/minecraft/client/renderer/ItemRenderer.class"))).accept(renderer, 0);
            for (String name : new String[] {"func_178096_b", "func_178097_a"}) {
                int matches = 0;
                for (Object entry : renderer.methods) {
                    MethodNode method = (MethodNode) entry;
                    if (!method.name.equals(name)) continue;
                    for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                        if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals("func_179152_a")) ++matches;
                    }
                }
                check(matches > 0, "size hook matches vanilla " + name);
            }
            ClassNode player = new ClassNode();
            new ClassReader(game.getInputStream(game.getJarEntry("net/minecraft/client/entity/EntityPlayerSP.class"))).accept(player, 0);
            boolean swingTarget = false;
            for (Object entry : player.methods) {
                MethodNode method = (MethodNode) entry;
                if (method.name.equals("func_71038_i") && method.desc.equals("()V")) swingTarget = true;
            }
            check(swingTarget, "swing observer matches the real player method");
            ClassNode playerMixin = new ClassNode();
            new ClassReader(mod.getInputStream(mod.getJarEntry("myau/mixin/MixinEntityPlayerSP.class"))).accept(playerMixin, 0);
            boolean observer = false;
            for (Object entry : playerMixin.methods) {
                MethodNode method = (MethodNode) entry;
                if (!method.name.equals("observeItemEffectsSwing")) continue;
                observer = true;
                for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                    check(!(instruction instanceof FieldInsnNode), "swing observer does not write player fields");
                    if (instruction instanceof MethodInsnNode) {
                        check(((MethodInsnNode) instruction).owner.equals("myau/ItemEffectsModule"), "swing observer only updates visual effects");
                    }
                }
            }
            check(observer, "swing observer is packaged");
            JsonObject mixins = new JsonParser().parse(new InputStreamReader(mod.getInputStream(mod.getJarEntry("mixins.myau.json")), "UTF-8")).getAsJsonObject();
            check(mixins.getAsJsonArray("client").toString().contains("MixinItemEffects"), "item effects renderer hook is packaged");
            for (String path : new String[] {"myau/ItemEffectsModule.class", "myau/mixin/MixinItemEffects.class"}) {
                ClassNode type = new ClassNode();
                new ClassReader(mod.getInputStream(mod.getJarEntry(path))).accept(type, 0);
                for (Object entry : type.methods) {
                    MethodNode method = (MethodNode) entry;
                    for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                        if (instruction instanceof FieldInsnNode) {
                            FieldInsnNode field = (FieldInsnNode) instruction;
                            check(field.getOpcode() != Opcodes.PUTFIELD || !field.owner.startsWith("net/minecraft/"), "effects never write game state");
                        }
                        if (instruction instanceof MethodInsnNode) {
                            MethodInsnNode call = (MethodInsnNode) instruction;
                            check(!call.owner.contains("network/") && !call.owner.equals("myau/PacketUtils"), "effects never send packets");
                        }
                    }
                }
            }
        }
    }
}
