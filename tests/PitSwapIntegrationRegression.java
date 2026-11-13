package myau;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.jar.JarFile;
import java.io.InputStreamReader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import myau.modules.PantSwap;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.tree.*;
import sun.misc.Unsafe;

public final class PitSwapIntegrationRegression {
    private static int checks;
    private static void check(boolean value, String message) {
        ++checks;
        if (!value) throw new AssertionError(message);
    }
    private static class Player extends EntityPlayerSP {
        Player() { super(null, null, null, null); }
        public ItemStack func_71011_bu() { return null; }
    }
    private static class Controller extends PlayerControllerMP {
        int syncs;
        Controller() { super(null, null); }
        private void func_78750_j() { ++syncs; }
    }
    private static class PantHost implements PantSwap.Host {
        final PantSwapModule module;
        final Object[] slots = new Object[45];
        final Object world = new Object(), player = new Object(), screen = new Object();
        Object cursor;
        boolean opened;
        int clicks, closed;
        long time = 1000;
        short action = -10;
        PantHost(PantSwapModule module) { this.module = module; slots[7] = "pants-old"; slots[36] = "pants-new"; }
        public boolean ready() { return true; }
        public Object worldIdentity() { return world; }
        public Object playerIdentity() { return player; }
        public boolean playerContainerOpen() { return true; }
        public boolean inventoryScreenOpen() { return opened; }
        public boolean otherScreenOpen() { return false; }
        public boolean survivalOrAdventure() { return true; }
        public Object screenIdentity() { return screen; }
        public void openInventory() { opened = true; }
        public void closeInventory() { opened = false; ++closed; }
        public Object stack(int slot) { return slots[slot]; }
        public Object cursor() { return cursor; }
        public Object copy(Object stack) { return stack; }
        public boolean equal(Object first, Object second) { return java.util.Objects.equals(first, second); }
        public boolean leggings(Object stack) { return stack instanceof String && ((String)stack).startsWith("pants-"); }
        public boolean canEquipLeggings(Object stack) { return leggings(stack); }
        public long nowMillis() { return time; }
        public void click(int slot) {
            WindowClickEvent event = new WindowClickEvent(0, slot, 0, 0);
            module.onWindowClick(event);
            check(!event.isCancelled(), "PantSwap permits its own controller click");
            Object previous = slots[slot]; slots[slot] = cursor; cursor = previous;
            module.onPacketSend(new PacketSendEvent(new C0EPacketClickWindow(0, slot, 0, 0, null, ++action)));
            ++clicks;
        }
        public void message(String text) { }
    }
    private static int bind(Module module) throws Exception {
        int count = 0;
        for (Field field : module.getClass().getFields()) {
            if (!Setting.class.isAssignableFrom(field.getType())) continue;
            ((Setting<?>)field.get(module)).setModule(module);
            ++count;
        }
        return count;
    }
    public static void main(String[] args) throws Exception {
        PitModule pit = new PitModule();
        PantSwapModule pant = new PantSwapModule();
        SwordSwapModule sword = new SwordSwapModule();
        check(bind(pit) == 35, "all Pit settings registered");
        check(bind(pant) == 2, "all PantSwap settings registered");
        check(bind(sword) == 5, "all SwordSwap settings registered");
        check(!pit.isEnabled() && !pant.isEnabled() && !sword.isEnabled(), "new modules start disabled");
        check(ModuleCategory.of(pit) == ModuleCategory.EXPLOITS && ModuleCategory.of(pant) == ModuleCategory.PLAYER
                && ModuleCategory.of(sword) == ModuleCategory.PLAYER, "new modules have GUI categories");
        check(!pit.inventoryCleaner.getValue(), "inventory cleaner starts disabled");
        check(!pit.playerListMaxPlayers.isVisible(), "player list options start hidden");
        pit.playerList.setValue(true);
        check(pit.playerListMaxPlayers.isVisible(), "player list options follow parent");
        check(!pit.eventListScale.isVisible() && !pit.showInSpawn.isVisible(), "optional Pit controls start hidden");
        pit.eventList.setValue(true); pit.hidePitSpawn.setValue(2);
        check(pit.eventListScale.isVisible() && pit.showInSpawn.isVisible(), "Pit visibility follows parent modes");
        check(!pant.delay.setValue(1001) && !sword.slot1.setValue(0) && !sword.slot2.setValue(10), "swap bounds validated");
        check(pit.events().isEmpty() && !pit.eventsLoading(), "missing schedule stays explicitly unavailable");
        check(PitModule.loreCategory("\u00a7cVenom III") == 8, "Venom lore classification");
        check(PitModule.loreCategory("Regularity II") == 16 && PitModule.loreCategory("Bounty Hunter III") == 32, "other enchant categories");
        check(PitModule.loreCategory("Does more damage to Venom users") == 0, "descriptive lore is not an enchant");
        UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "VisibleName");
        String json = "{\"profileName\":\"CachedName\",\"profileId\":\"" + id.toString().replace("-", "") + "\"}";
        profile.getProperties().put("textures", new Property("textures", Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8))));
        check("CachedName".equals(PitModule.cachedProfileName(profile)), "cached profile identity must match player UUID");
        GameProfile borrowed = new GameProfile(UUID.randomUUID(), "Other");
        borrowed.getProperties().putAll(profile.getProperties());
        check(PitModule.cachedProfileName(borrowed) == null, "another player's skin is not treated as their identity");
        profile.getProperties().removeAll("textures");
        profile.getProperties().put("textures", new Property("textures", "invalid base64"));
        check(PitModule.cachedProfileName(profile) == null, "malformed skin metadata is ignored");
        StringBuilder nested = new StringBuilder("{\"unused\":");
        for (int level = 0; level < 2000; ++level) nested.append('[');
        nested.append('0');
        for (int level = 0; level < 2000; ++level) nested.append(']');
        nested.append('}');
        profile.getProperties().removeAll("textures");
        profile.getProperties().put("textures", new Property("textures", Base64.getEncoder().encodeToString(nested.toString().getBytes(StandardCharsets.UTF_8))));
        check(PitModule.cachedProfileName(profile) == null, "deep unrelated skin metadata does not recurse or invent an identity");
        S32PacketConfirmTransaction reply = new S32PacketConfirmTransaction(0, (short)-123, true);
        Object connection = new Object();
        check(new PacketReceiveEvent(reply, connection).getConnection() == connection, "packet origin is preserved");
        check(new PacketReceiveEvent(reply).getConnection() == null, "existing packet-event constructor remains compatible");
        PantHost pantHost = new PantHost(pant);
        PantSwap pantEngine = new PantSwap(pantHost);
        Field engineField = PantSwapModule.class.getDeclaredField("engine"); engineField.setAccessible(true); engineField.set(pant, pantEngine);
        pant.setEnabled(true);
        Field origin = PantSwapModule.class.getDeclaredField("connection"); origin.setAccessible(true); origin.set(pant, connection);
        WindowClickEvent manual = new WindowClickEvent(0, 36, 0, 0);
        pant.onWindowClick(manual);
        check(manual.isCancelled(), "PantSwap blocks unrelated inventory actions while active");
        pant.onPreTick(new PreTickEvent());
        check(pantHost.clicks == 1 && pantEngine.pendingTransaction() == pantHost.action, "outgoing click observer preserves the signed transaction ID");
        pant.onPacketReceive(new PacketReceiveEvent(new S32PacketConfirmTransaction(0, pantHost.action, true), new Object()));
        pantHost.time += 100; pant.onPreTick(new PreTickEvent());
        check(pantHost.clicks == 1, "confirmation from another connection cannot advance PantSwap");
        PacketReceiveEvent cancelled = new PacketReceiveEvent(new S32PacketConfirmTransaction(0, pantHost.action, true), connection);
        cancelled.setCancelled(true); pant.onPacketReceive(cancelled);
        pantHost.time += 100; pant.onPreTick(new PreTickEvent());
        check(pantHost.clicks == 1, "cancelled confirmation cannot advance PantSwap");
        for (int step = 0; step < 3; ++step) {
            pant.onPacketReceive(new PacketReceiveEvent(new S32PacketConfirmTransaction(0, pantHost.action, true), connection));
            pantHost.time += 100; pant.onPreTick(new PreTickEvent());
        }
        check(!pant.isEnabled() && pantHost.clicks == 3 && pantHost.cursor == null && pantHost.closed == 1, "confirmed PantSwap finishes and closes its own inventory");
        check("pants-new".equals(pantHost.slots[7]) && "pants-old".equals(pantHost.slots[36]), "PantSwap preserves both leggings stacks");
        Field access = Unsafe.class.getDeclaredField("theUnsafe"); access.setAccessible(true);
        Unsafe unsafe = (Unsafe)access.get(null);
        Minecraft game = Minecraft.func_71410_x();
        Player player = (Player)unsafe.allocateInstance(Player.class);
        Controller controller = (Controller)unsafe.allocateInstance(Controller.class);
        game.field_71439_g = player;
        game.field_71441_e = (WorldClient)unsafe.allocateInstance(WorldClient.class);
        game.field_71442_b = controller;
        game.field_71474_y = (GameSettings)unsafe.allocateInstance(GameSettings.class);
        game.field_71474_y.field_74312_F = new KeyBinding("attack", -100, "test");
        player.field_71071_by = new InventoryPlayer(player);
        player.field_71071_by.field_70462_a[0] = new ItemStack(new ItemSword(Item.ToolMaterial.IRON));
        player.field_71071_by.field_70462_a[2] = new ItemStack(new ItemSword(Item.ToolMaterial.EMERALD));
        sword.pitOnly.setValue(false); sword.killauraOnly.setValue(false);
        InputUtils.keys.add(-100);
        sword.setEnabled(true);
        sword.onPreTick(new PreTickEvent());
        check(player.field_71071_by.field_70461_c == 2 && controller.syncs == 1, "SwordSwap adapter selects and synchronizes second slot");
        sword.onPostTick(new PostTickEvent());
        sword.onPreTick(new PreTickEvent());
        check(player.field_71071_by.field_70461_c == 0 && controller.syncs == 2, "SwordSwap adapter alternates configured slots");
        sword.onPreTick(new PreTickEvent());
        InputUtils.keys.clear(); sword.onPreTick(new PreTickEvent()); sword.onPostTick(new PostTickEvent());
        check(player.field_71071_by.field_70461_c == 0, "SwordSwap restores the original slot after attack input ends");
        sword.setEnabled(false);
        try (JarFile mod = new JarFile(args[1]); JarFile mapped = new JarFile(args[0])) {
            ClassNode client = new ClassNode();
            new ClassReader(mod.getInputStream(mod.getJarEntry("myau/MyauClient.class"))).accept(client, 0);
            for (String module : new String[]{"PitModule", "PantSwapModule", "SwordSwapModule"}) {
                boolean found = false;
                for (Object entry : client.methods) {
                    MethodNode method = (MethodNode)entry;
                    if (!method.name.equals("registerModules")) continue;
                    for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext())
                        if (instruction instanceof TypeInsnNode && ((TypeInsnNode)instruction).desc.equals("myau/" + module)) found = true;
                }
                check(found, module + " is registered in the built client");
            }
            JsonObject refmap = new JsonParser().parse(new InputStreamReader(mod.getInputStream(mod.getJarEntry("mixins.myau.refmap.json")), "UTF-8")).getAsJsonObject();
            JsonObject worldHooks = refmap.getAsJsonObject("data").getAsJsonObject("searge").getAsJsonObject("myau/mixin/MixinWorld");
            ClassNode world = new ClassNode();
            new ClassReader(mapped.getInputStream(mapped.getJarEntry("net/minecraft/world/World.class"))).accept(world, 0);
            for (String hook : new String[]{"getEntitiesInAABBexcluding", "getEntitiesWithinAABB(Ljava/lang/Class;Lnet/minecraft/util/AxisAlignedBB;Lcom/google/common/base/Predicate;)Ljava/util/List;"}) {
                String target = worldHooks.get(hook).getAsString().split(";", 2)[1];
                boolean found = false;
                for (Object entry : world.methods) { MethodNode method = (MethodNode)entry; if ((method.name + method.desc).equals(target)) found = true; }
                check(found, "mapped Pit entity hook exists: " + hook);
            }
        }
        System.out.println("Pit/swap integration checks passed: " + checks);
    }
}
