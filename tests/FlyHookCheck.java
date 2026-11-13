package myau;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.util.jar.JarFile;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;

public final class FlyHookCheck {
    public static void main(String[] args) throws Exception {
        try (JarFile game = new JarFile(args[0]); JarFile mod = new JarFile(args[1])) {
            ClassNode player = new ClassNode();
            new ClassReader(game.getInputStream(game.getJarEntry("net/minecraft/client/entity/EntityPlayerSP.class"))).accept(player, 0);
            int matches = 0;
            for (Object entry : player.methods) {
                MethodNode method = (MethodNode)entry;
                if (!method.name.equals("func_175161_p") || !method.desc.equals("()V")) continue;
                for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                    if (!(instruction instanceof FieldInsnNode)) continue;
                    FieldInsnNode field = (FieldInsnNode)instruction;
                    if (field.getOpcode() == Opcodes.GETFIELD && field.owner.equals(player.name) && field.name.equals("field_70122_E") && field.desc.equals("Z")) ++matches;
                }
            }
            if (matches != 5) throw new AssertionError("expected five vanilla walking packet ground reads, found " + matches);
            JsonObject refmap = new JsonParser().parse(new InputStreamReader(mod.getInputStream(mod.getJarEntry("mixins.myau.refmap.json")), "UTF-8")).getAsJsonObject();
            JsonObject mappings = refmap.getAsJsonObject("data").getAsJsonObject("searge");
            JsonObject hook = mappings.getAsJsonObject("myau/mixin/MixinFlyPlayer");
            if (!hook.get("onUpdateWalkingPlayer").getAsString().equals("L" + player.name + ";func_175161_p()V")) throw new AssertionError("walking method mapping");
            if (!hook.get("Lnet/minecraft/client/entity/EntityPlayerSP;onGround:Z").getAsString().equals("L" + player.name + ";field_70122_E:Z")) throw new AssertionError("ground field mapping");
            if (!mappings.getAsJsonObject("myau/mixin/AccessorC03PacketPlayer").get("y").getAsString().equals("field_149477_b:D")) throw new AssertionError("packet Y mapping");
            if (mod.getJarEntry("myau/mixin/MixinFlyPlayer.class") == null) throw new AssertionError("missing packaged Fly mixin");
        }
        System.out.println("Fly packet hook and package checks passed");
    }
}
