package myau.openb4.module.minigames;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import myau.BooleanSetting;
import myau.FloatSetting;
import myau.openb4.client.BotCheck;
import myau.openb4.client.Chat;
import myau.openb4.client.ChatCommands;
import myau.openb4.client.Hypixel;
import myau.openb4.client.InfoHud;
import myau.openb4.event.MotionEvent;
import myau.openb4.module.Module;
import myau.openb4.module.ModuleManager;
import myau.openb4.util.BlockUtils;
import myau.openb4.util.Executor;
import myau.openb4.util.ItemUtils;
import myau.openb4.util.PlayerUtils;
import myau.openb4.util.RenderUtils;
import myau.openb4.util.Strings;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class BedWarsModule extends Module {

    public final BooleanSetting bedTracker = new BooleanSetting("bed-tracker", true);
    public final myau.BedTrackerModule bedGuard = new myau.BedTrackerModule(this, bedTracker);
    public static final FloatSetting alertInterval = new FloatSetting("alert-interval", 2.0f, 0.0f, 30.0f, 1.0f);
    public static final FloatSetting closestEnemy = new FloatSetting("closest-enemy", 0.0f, 0.0f, 10.0f, 1.0f);
    public static final BooleanSetting instantShop = new BooleanSetting("instant-shop", true);
    public static final BooleanSetting scrollToDepositWithdraw = new BooleanSetting("scroll-to-deposit-withdraw", true);
    public static final BooleanSetting whitelistOwnBed = new BooleanSetting("whitelist-own-bed", true);
    public static final BooleanSetting ignoreTeammates = new BooleanSetting("ignore-teammates", false);
    public static final BooleanSetting sendPartyChat = new BooleanSetting("send-party-chat", false);
    public static final BooleanSetting shouldPing = new BooleanSetting("should-ping", false);
    public static final BooleanSetting dragon = new BooleanSetting("dragon", true);
    public static final BooleanSetting magicMilkTimer = new BooleanSetting("magic-milk-timer", true);
    public static final BooleanSetting bow = new BooleanSetting("bow", true);
    public static final BooleanSetting diamondArmor = new BooleanSetting("diamond-armor", true);
    public static final BooleanSetting dreamDefender = new BooleanSetting("dream-defender", true);
    public static final BooleanSetting enderPearl = new BooleanSetting("ender-pearl", true);
    public static final BooleanSetting enchants = new BooleanSetting("enchants", true);
    public static final BooleanSetting fireball = new BooleanSetting("fireball", true);
    public static final BooleanSetting knockbackStick = new BooleanSetting("knockback-stick", true);
    public static final BooleanSetting magicMilk = new BooleanSetting("magic-milk", true);
    public static final BooleanSetting obsidian = new BooleanSetting("obsidian", true);
    public static final BooleanSetting potions = new BooleanSetting("potions", true);
    public static final BooleanSetting rotationItems = new BooleanSetting("rotation-items", true);
    public static final BooleanSetting waterBucket = new BooleanSetting("water-bucket", true);
    public static final BooleanSetting invisiblePlayers = new BooleanSetting("invisible-players", true);

    public enum Alert {

        BLOCK_ZAPPER("&7is holding &3Block Zapper", false),
        BOW("&7is holding &6Bow", false),
        BOW_POWER("&7is holding &6Power Bow", false),
        BOW_PUNCH("&7is holding &6Punch Bow", false),
        CHARLIE_UNICORN("&7is holding &eCharlie Kirk", false),
        DIA_ARMOR("&7has purchased &bDiamond Armor", true),
        DIA_PICKAXE("&7is holding &bDiamond Pickaxe", false),
        DREAM_DEFENDER("&7is holding &fDream Defender", false),
        ENCHANT_PROT("&7has purchased &bProtection", true),
        ENCHANT_SHARPNESS("&7has purchased &bSharpness", true),
        ENDERPEARL("&7is holding &3Ender Pearl", false),
        FIREBALL("&7is holding &6Fireball", false),
        ICE_BRIDGE("&7is holding &bIce Bridge", false),
        INVISIBLE("&7is now &bInvisible", false),
        KB_STICK("&7is holding &6Knockback Stick", false),
        MAGIC_MILK("&7is holding &fMagic Milk", false),
        OBSIDIAN_HELD("&7is holding &dObsidian", false),
        OBSIDIAN_PLACED("&7placed &dObsidian", false),
        POTION_INVIS("&7is holding &dInvis &7Potion", false),
        POTION_JUMP("&7is holding &aJump &7Potion", false),
        POTION_SPEED("&7is holding &bSpeed &7Potion", false),
        SLEEPING_DUST("&7is holding &fSleeping Dust", false),
        WATER_BUCKET("&7is holding &bWater Bucket", false);

        public final String text;

        final boolean permanent;

        BooleanSetting button;

        HashSet<Integer> flagged;

        Alert(String text, boolean permanent) {
            this.text = text;
            this.permanent = permanent;
        }

        boolean hasSet() {
            return this.flagged != null;
        }

        boolean shouldAlert() {
            return this.button.getValue();
        }

        boolean flag(EntityPlayer p, ItemStack stack, boolean teammate) {
            switch(this) {
                case BLOCK_ZAPPER:
                    return ItemUtils.is(stack, Items.field_179562_cC);
                case BOW:
                case BOW_POWER:
                case BOW_PUNCH:
                    {
                        if (ItemUtils.is(stack, Items.field_151031_f)) {
                            if (stack.func_77948_v()) {
                                NBTTagList list = stack.func_77986_q();
                                int n = list != null ? list.func_74745_c() : 0;
                                if (n == 1) {
                                    return this == BOW_POWER;
                                }
                                if (n == 2) {
                                    return this == BOW_PUNCH;
                                }
                            } else {
                                return this == BOW;
                            }
                        }
                        return false;
                    }
                case CHARLIE_UNICORN:
                    return ItemUtils.is(stack, Items.field_151136_bY);
                case DIA_ARMOR:
                    {
                        ItemStack legs = p.field_71071_by.field_70460_b[1];
                        return legs != null && legs.func_77973_b() == Items.field_151173_ae;
                    }
                case DIA_PICKAXE:
                    return ItemUtils.is(stack, Items.field_151046_w);
                case DREAM_DEFENDER:
                    return ItemUtils.is(stack, Items.field_151063_bx) && stack.func_82833_r().contains("Dream Defender");
                case ENCHANT_PROT:
                    {
                        if (teammate) {
                            return false;
                        }
                        ItemStack legs = p.field_71071_by.field_70460_b[1];
                        return legs != null && legs.func_77948_v();
                    }
                case ENCHANT_SHARPNESS:
                    if (teammate) {
                        return false;
                    }
                    return ItemUtils.isSword(stack) && stack.func_77948_v();
                case ENDERPEARL:
                    return ItemUtils.is(stack, Items.field_151079_bi);
                case FIREBALL:
                    return ItemUtils.is(stack, Items.field_151059_bz);
                case ICE_BRIDGE:
                    return BlockUtils.blockOf(stack) == Blocks.field_150432_aD;
                case INVISIBLE:
                    {
                        Minecraft mc = Minecraft.func_71410_x();
                        return p.func_82150_aj()
                                && p.field_70173_aa >= 100
                                && mc.field_71439_g.field_71071_by.field_70460_b[1] != null
                                && !p.func_70005_c_().equals(mc.field_71439_g.func_70005_c_())
                                && p.func_145748_c_().func_150260_c().contains(" ");
                    }
                case KB_STICK:
                    return ItemUtils.is(stack, Items.field_151055_y) && stack.func_77948_v();
                case MAGIC_MILK:
                    return ItemUtils.is(stack, Items.field_151117_aB);
                case OBSIDIAN_HELD:
                    return BlockUtils.blockOf(stack) == Blocks.field_150343_Z;
                case POTION_INVIS:
                case POTION_JUMP:
                case POTION_SPEED:
                    {
                        List<PotionEffect> list;
                        if (stack != null
                                && stack.func_77973_b() instanceof ItemPotion
                                && (list = ((ItemPotion) stack.func_77973_b()).func_77832_l(stack)) != null) {
                            for (PotionEffect e : list) {
                                int id = e.func_76456_a();
                                if (id == 14) {
                                    return this == POTION_INVIS;
                                }
                                if (id == 8) {
                                    return this == POTION_JUMP;
                                }
                                if (id != 1)
                                    continue;
                                return this == POTION_SPEED;
                            }
                        }
                        return false;
                    }
                case SLEEPING_DUST:
                    return ItemUtils.is(stack, Items.field_151126_ay) && stack.func_82833_r().contains("Sleeping Dust");
                case WATER_BUCKET:
                    return ItemUtils.is(stack, Items.field_151131_as);
                default:
                    return false;
            }
        }
    }

    private static boolean inGame;
    public static BlockPos ownBed;
    private static List<InfoHud.Line> enemies;
    private static final List<BlockPos> obsidianPlaced = new ArrayList<BlockPos>();
    private static final HashMap<UUID, HashMap<Alert, Long>> lastAlerts = new HashMap<UUID, HashMap<Alert, Long>>();
    private static String partyMessage;
    private static double dragonDistance = -1.0;
    private static int milkTicks;
    private static String milkText;
    private static long lastParty, lastPing;
    private static final int DRAGON_COLOR = new Color(214, 91, 191).getRGB();
    private static final List<String> START_LINES = Arrays.asList("  Protect your bed", "  Select an ultimate", "  Collect Lucky Blocks", "  Become a Soul Collector", "  All generators are maxed!", "  Destroy the enemy bed and", "  Every few seconds brings a new");
    private static final Set<Item> RESOURCES = new HashSet<Item>(Arrays.asList(Items.field_151042_j, Items.field_151043_k, Items.field_151045_i, Items.field_151166_bC));
    private static final Set<String> SHOPS = new HashSet<String>(Arrays.asList("Quick Buy", "Blocks", "Melee", "Armor", "Tools", "Ranged", "Potions", "Utility", "Rotating Items", "Purchase Enemy Tracker"));

    public BedWarsModule(int key) {
        super("BedWars", key);
    }

    private static Minecraft mc() {
        return Minecraft.func_71410_x();
    }

    public void link() {
        BooleanSetting[] buttons = { rotationItems, bow, bow, bow, rotationItems, diamondArmor, null, dreamDefender, enchants, enchants, enderPearl, fireball, rotationItems, invisiblePlayers, knockbackStick, magicMilk, obsidian, null, potions, potions, potions, rotationItems, waterBucket };
        for (Alert a : Alert.values()) {
            a.button = buttons[a.ordinal()];
            a.flagged = a.button != null ? new HashSet<Integer>() : null;
        }
    }

    @SubscribeEvent
    public void onUseFinish(PlayerUseItemEvent.Finish e) {
        if (!magicMilkTimer.getValue() || !inGame || e.item.func_77973_b() != Items.field_151117_aB) {
            return;
        }
        milkTicks = 600;
    }

    @Override
    public void onDisable() {
        bedGuard.onDisable();
        reset();
        enemies = null;
    }

    private static int compare(boolean byBed, EntityPlayer a, EntityPlayer b) {
        double da;
        double db;
        if (byBed) {
            da = a.func_174818_b(ownBed);
            db = b.func_174818_b(ownBed);
        } else {
            da = a.func_70068_e(mc().field_71439_g);
            db = b.func_70068_e(mc().field_71439_g);
        }
        return Double.compare(da, db);
    }

    private static void alert(EntityPlayer p, Alert a) {
        alert(p, a, Hypixel.sameTeam(p));
    }

    public static boolean isOwnBed(BlockPos pos) {
        return ModuleManager.enabled("Bed Wars")
                && whitelistOwnBed.getValue()
                && ownBed != null
                && (ownBed.equals(pos)
                || pos != null
                && pos.func_177956_o() == ownBed.func_177956_o()
                && Math.abs(pos.func_177958_n() - ownBed.func_177958_n()) + Math.abs(pos.func_177952_p() - ownBed.func_177952_p()) == 1
                && BlockUtils.getBlock(pos) == Blocks.field_150324_C);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        Minecraft mc = mc();
        if (!inGame || !Chat.inWorld()) {
            return;
        }
        if (dragon.getValue()) {
            for (Entity entity : mc.field_71441_e.field_72996_f) {
                if (!(entity instanceof EntityDragon))
                    continue;
                renderDragon((EntityDragon) entity, DRAGON_COLOR, e.partialTicks);
            }
            if (dragonDistance != -1.0) {
                InfoHud.add("&7Dragon: &" + distanceColor(dragonDistance) + (int) dragonDistance + "m");
            }
        }
        if (enemies != null) {
            for (InfoHud.Line line : enemies) {
                InfoHud.add(line);
            }
        }
        if (milkText != null) {
            InfoHud.add(milkText);
        }
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre e) {
        Minecraft mc = mc();
        if (!inGame || !(e.gui instanceof GuiChest)) {
            return;
        }
        IInventory chest;
        if (instantShop.getValue()
                && Mouse.getEventButton() == 0
                && Mouse.getEventButtonState()
                && !GuiScreen.func_146272_n()
                && SHOPS.contains((chest = lowerChest()).func_70005_c_())) {
            Slot slot = PlayerUtils.chestSlotUnderMouse((GuiChest) e.gui, chest.func_70302_i_());
            if (slot != null) {
                mc().field_71442_b.func_78753_a(mc().field_71439_g.field_71070_bA.field_75152_c, slot.field_75222_d, 2, 3, mc().field_71439_g);
                e.setCanceled(true);
            }
        }
        if (!scrollToDepositWithdraw.getValue()) {
            return;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        chest = lowerChest();
        int size = chest.func_70302_i_();
        if (size == 27) {
            if (wheel > 0) {
                for (int i = size; i < size + 36; ++i) {
                    ItemStack stack = mc.field_71439_g.field_71070_bA.func_75139_a(i).func_75211_c();
                    if (stack == null || !RESOURCES.contains(stack.func_77973_b()))
                        continue;
                    mc().field_71442_b.func_78753_a(mc().field_71439_g.field_71070_bA.field_75152_c, i, 0, 1, mc().field_71439_g);
                    break;
                }
            } else {
                for (int i = 0; i < size; ++i) {
                    ItemStack stack = chest.func_70301_a(i);
                    if (stack == null || !RESOURCES.contains(stack.func_77973_b()))
                        continue;
                    mc().field_71442_b.func_78753_a(mc().field_71439_g.field_71070_bA.field_75152_c, i, 0, 1, mc().field_71439_g);
                    break;
                }
            }
        }
    }

    private static IInventory lowerChest() {
        return ((ContainerChest) mc().field_71439_g.field_71070_bA).func_85151_d();
    }

    public static void onGameStartChat(ClientChatReceivedEvent e, String s, boolean bedWars, boolean bedEsp, boolean autoWho) {
        for (String line : START_LINES) {
            if (!s.contains(line))
                continue;
            if (bedWars) {
                ((BedWarsModule) ModuleManager.get("BedWars")).bedGuard.onDisable();
                reset();
                for (Alert a : Alert.values()) {
                    if (!a.hasSet())
                        continue;
                    a.flagged.clear();
                }
            }
            if (bedWars || bedEsp) {
                findBedLater();
            }
            if (!autoWho)
                break;
            break;
        }
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        Minecraft mc = mc();
        if (partyMessage != null) {
            PlayerUtils.sendChatMessage("/pc " + partyMessage);
            partyMessage = null;
        }
        if (!(inGame = Hypixel.bedwars(2))) {
            return;
        }
        if (mc.field_71439_g.field_70173_aa % 3 == 0) {
            HashSet<Integer> ids = new HashSet<Integer>();
            ArrayList<EntityPlayer> list = new ArrayList<EntityPlayer>();
            for (EntityPlayer p : mc.field_71441_e.field_73010_i) {
                if (p == mc.field_71439_g || p.field_70725_aQ != 0 || Hypixel.info(p) == null)
                    continue;
                if (!ids.add(p.func_145782_y()))
                    continue;
                boolean teammate = Hypixel.sameTeam(p);
                if ((closestEnemy.getValue() != 0) && ownBed != null && !teammate && !BotCheck.isBot(p, false)) {
                    list.add(p);
                }
                int id = p.func_145782_y();
                ItemStack held = p.func_70694_bm();
                for (Alert a : Alert.values()) {
                    if (!a.hasSet())
                        continue;
                    if (a.flagged.contains(id)) {
                        if (a.permanent || a.flag(p, held, teammate) || a.permanent)
                            continue;
                        a.flagged.remove(id);
                        continue;
                    }
                    if (!a.flag(p, held, teammate))
                        continue;
                    a.flagged.add(id);
                    if (!a.shouldAlert())
                        continue;
                    alert(p, a, teammate);
                }
            }
            enemies.clear();
            if (!list.isEmpty()) {
                boolean byBed = Hypixel.state == Hypixel.State.BEDWARS_GAME_BED;
                list.sort((a, b) -> compare(byBed, a, b));
                int n = Math.min(closestEnemy.getValue().intValue(), list.size());
                for (int i = 0; i < n; ++i) {
                    EntityPlayer p = list.get(i);
                    double d = byBed ? Math.sqrt(p.func_174818_b(ownBed)) : (double) p.func_70032_d(mc.field_71439_g);
                    enemies.add(InfoHud.line(p.func_145748_c_().func_150260_c(), ChatCommands.distance(d, false), p.func_70694_bm()));
                }
            }
            if (dragon.getValue()) {
                double d = -1.0;
                for (Entity entity : mc.field_71441_e.field_72996_f) {
                    if (!(entity instanceof EntityDragon))
                        continue;
                    double d2 = mc.field_71439_g.func_70068_e(entity);
                    if (d != -1.0 && !(d2 < d))
                        continue;
                    d = d2;
                }
                dragonDistance = d;
                if (d != -1.0) {
                    dragonDistance = Math.sqrt(dragonDistance);
                }
            }
        }
        if (milkTicks != 0) {
            if (milkTicks > 0) {
                milkText = "&7Magic Milk: &a" + Strings.round((double) milkTicks / 20.0, 1) + "s";
                if (--milkTicks == 0) {
                    milkTicks = -40;
                }
            } else {
                milkText = ++milkTicks == 0 ? null : "&7Magic milk: &cFinished";
            }
        }
    }

    @Override
    public void guiButtonToggled(BooleanSetting b) {
        if (b == bedTracker && !b.getValue())
            bedGuard.onDisable();
        if (!b.getValue()) {
            if (b == dragon) {
                dragonDistance = -1.0;
            } else if (b == magicMilkTimer) {
                milkText = null;
                milkTicks = 0;
            }
        }
    }

    private static void findBedLater() {
        Executor.later(() -> {
            if (ModuleManager.enabled("BedWars") && Chat.inWorld())
                findBed();
        }, 1000);
    }

    private static void alert(EntityPlayer p, Alert a, boolean teammate) {
        Minecraft mc = mc();
        if (ignoreTeammates.getValue() && teammate) {
            return;
        }
        long now = System.currentTimeMillis();
        if (alertInterval.getValue() > 0.0) {
            HashMap<Alert, Long> map = lastAlerts.get(p.func_110124_au());
            if (map == null) {
                map = new HashMap<Alert, Long>();
            } else {
                Long last = map.get(a);
                if (last != null && (double) Math.abs(now - last) <= alertInterval.getValue() * 1000.0) {
                    return;
                }
            }
            map.put(a, now);
            lastAlerts.put(p.func_110124_au(), map);
        }
        double d = mc.field_71439_g.func_70032_d(p);
        String name = p.func_145748_c_().func_150260_c();
        String s = name + " " + a.text + " " + ChatCommands.distance(d, true);
        Chat.print(s);
        if (sendPartyChat.getValue() && now - lastParty > 1500L && Hypixel.onHypixel()) {
            partyMessage = Chat.strip(Chat.color(s));
            lastParty = now;
        }
        if (shouldPing.getValue() && now - lastPing > 1500L) {
            PlayerUtils.ping();
            lastPing = now;
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().field_71439_g) {
            reset();
        }
    }

    @Override
    public void onEnable() {
        enemies = new ArrayList<InfoHud.Line>((int) 10);
    }

    private static char distanceColor(double d) {
        if (d <= 25.0) {
            return 'c';
        }
        if (d <= 50.0) {
            return '6';
        }
        if (d <= 75.0) {
            return 'e';
        }
        if (d <= 125.0) {
            return 'a';
        }
        return '2';
    }

    private static void reset() {
        ownBed = null;
        inGame = false;
        milkTicks = 0;
        lastPing = 0;
        dragonDistance = -1.0;
        obsidianPlaced.clear();
        lastAlerts.clear();
        milkText = null;
        partyMessage = null;
        if (enemies != null) {
            enemies.clear();
        }
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        if (!(packet instanceof S23PacketBlockChange)) {
            return true;
        }
        S23PacketBlockChange p = (S23PacketBlockChange) packet;
        Minecraft mc = mc();
        if (obsidian.getValue()
                && p.func_180728_a().func_177230_c() == Blocks.field_150343_Z
                && BlockUtils.nextToBed(p.func_179827_b())) {
            obsidianPlaced.add(p.func_179827_b());
            double d = 36.0;
            EntityPlayer placer = null;
            for (EntityPlayer other : mc.field_71441_e.field_73010_i) {
                if (other == mc.field_71439_g || other.field_70725_aQ != 0)
                    continue;
                double d2 = other.func_174818_b(p.func_179827_b());
                if (!(d2 < d))
                    continue;
                d = d2;
                placer = other;
            }
            if (placer != null) {
                alert(placer, Alert.OBSIDIAN_PLACED);
            }
        }
        return true;
    }

    private static void findBed() {
        Minecraft mc = mc();
        ownBed = null;
        for (int i = 10; i >= -10; --i) {
            for (int j = -25; j <= 25; ++j) {
                for (int k = -25; k <= 25; ++k) {
                    if (mc.field_71441_e == null) {
                        return;
                    }
                    BlockPos pos = new BlockPos(mc.field_71439_g.field_70165_t + (double) j, mc.field_71439_g.field_70163_u + (double) i, mc.field_71439_g.field_70161_v + (double) k);
                    IBlockState state = mc.field_71441_e.func_180495_p(pos);
                    if (state.func_177230_c() != Blocks.field_150324_C
                            || state.func_177229_b(BlockBed.field_176472_a) != BlockBed.EnumPartType.FOOT)
                        continue;
                    ownBed = pos;
                    ((BedWarsModule) ModuleManager.get("BedWars")).bedGuard.trackBed(pos);
                    return;
                }
            }
        }
    }

    private static void renderDragon(EntityDragon dragon, int color, float partialTicks) {
        RenderManager rm = mc().func_175598_ae();
        double x = dragon.field_70142_S + (dragon.field_70165_t - dragon.field_70142_S) * (double) partialTicks - rm.field_78730_l;
        double y = dragon.field_70137_T + (dragon.field_70163_u - dragon.field_70137_T) * (double) partialTicks - rm.field_78731_m;
        double z = dragon.field_70136_U + (dragon.field_70161_v - dragon.field_70136_U) * (double) partialTicks - rm.field_78728_n;
        AxisAlignedBB bb = dragon.func_174813_aQ().func_72314_b(2.0, 0.0, 2.0);
        AxisAlignedBB box = new AxisAlignedBB(bb.field_72340_a - dragon.field_70165_t + x, bb.field_72338_b - dragon.field_70163_u + y, bb.field_72339_c - dragon.field_70161_v + z, bb.field_72336_d - dragon.field_70165_t + x, bb.field_72337_e - dragon.field_70163_u + y - 3.0, bb.field_72334_f - dragon.field_70161_v + z);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        float a = (float) (color >> 24 & 255) / 255.0f;
        float r = (float) (color >> 16 & 0xFF) / 255.0f;
        float g = (float) (color >> 8 & 0xFF) / 255.0f;
        float b = (float) (color & 0xFF) / 255.0f;
        GlStateManager.func_179131_c(r, g, b, a);
        GL11.glEnable(2848);
        GL11.glLineWidth(2.0f);
        RenderGlobal.func_181561_a(box);
        GL11.glDisable(2848);
        RenderUtils.resetColor();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
    }
}
