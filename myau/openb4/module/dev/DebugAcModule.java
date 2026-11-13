package myau.openb4.module.dev;

import java.util.Arrays;
import java.util.List;
import myau.BooleanSetting;
import myau.openb4.client.BotCheck;
import myau.openb4.client.Chat;
import myau.openb4.module.Module;
import myau.openb4.util.BlockUtils;
import myau.openb4.util.ItemUtils;
import myau.openb4.util.PacketUtils;
import myau.openb4.util.PlayerUtils;
import myau.openb4.util.Strings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

public class DebugAcModule extends Module {

    public static final BooleanSetting shouldPing = new BooleanSetting("should-ping", true);
    public static final BooleanSetting backwardsSpeed = new BooleanSetting("backwards-speed", false);
    public static final BooleanSetting blink = new BooleanSetting("blink", false);
    public static final BooleanSetting digTargets = new BooleanSetting("dig-targets", true);
    public static final BooleanSetting groundSpoof = new BooleanSetting("ground-spoof", true);
    public static final BooleanSetting incorrectHitVec = new BooleanSetting("incorrrect-hit-vec", false);
    public static final BooleanSetting missingStopSprint = new BooleanSetting("missing-stop-sprint", true);
    private static final List<Double> OFFSETS_LOW = Arrays.asList(0.0, 0.0625, 0.09999999403953552, 0.25, 0.28125, 0.3125, (double) 0.35f, 0.375, (double) 0.4f, 0.4375, 0.5, 0.625, (double) 0.7f, 0.8125, 0.875, 0.9375);
    private static final List<Double> OFFSETS_HIGH = Arrays.asList(1.0, 0.0625, 0.125, 0.1875, (double) 0.3f, 0.375, 0.5, 0.5625, (double) 0.6f, 0.625, (double) 0.65f, 0.6875, 0.75, 0.78125, (double) 0.8f, 0.8125, 0.875, (double) 0.9f, 0.9375);
    private static final List<Double> GROUND_Y = Arrays.asList(0.0, 0.015625, 0.0625, 0.125, 0.25, 0.1875, 0.3125, 0.375, 0.5, 0.5625, 0.625, 0.75, 0.8125, 0.875);
    private static final double HIT_VEC_TOLERANCE = 0.028;
    private static final double MIN_HIT_Y = -0.1f;
    private static final double MAX_HIT_Y = 1.9;
    private static final double MIN_HIT_XZ = -0.4000000134110453, MAX_HIT_XZ = 0.4000000134110453;
    private static final double BACKWARDS = 0.21586;
    private static final float MIN_YAW_STEP = 0.009521484f, TINY_YAW = 1.5E-4f;
    private static final float MIN_PITCH_STEP = 0.0096001625f, TINY_PITCH = 1.2E-5f;
    private static final long POST_WINDOW = 30L;
    private static long detections;
    private static long lastPing;
    private static long lastFlying;
    private static long lastOther;
    private static String postPacket;
    private static boolean openedInventory, closedWindow, clickedWindow, sentEntityAction, sentChat, stoppedBreak, abortedBreak, startedBreak, dug, dugAny, usedItem, placeNeedsSwing, placedBlockItem, placeInteract, placedUse, placed, interacted, interactedAt, attacked, usedEntity, swung, missingStopSneak, missingStopSprintFlag, missingSwapOrAbort, missingSwap, missingRelease, missingPlaceOrDig, missingPlace255, missingPlace, missingInteract, missingSwing, entityActionLocked, unexpectedPlace, attackLocked;
    private static int swaps, swings, packets;
    private static int[] lastClick;
    private static Entity target;
    private static Vec3 hitVec;
    private static boolean stoppedBreakLastTick;
    private static boolean sneaking, sprinting, blocking, usingItem, previousGround, ground;
    private static int totalSwaps;
    private static C07PacketPlayerDigging.Action lastDigAction;
    private static EnumFacing startBreakFacing, lastDigFacing, placeFacing;
    private static BlockPos lastDigPos, placePos;
    private static C0BPacketEntityAction.Action lastEntityAction;
    private static Integer slot;
    private static ItemStack heldStack;
    private static Item heldItem;
    private static int airC03s;
    private static double x, y, z, prevX, prevY, prevZ, lastSpeed, lastAccel, lastBackwards;
    private static float lastYaw, lastPitch;

    public DebugAcModule(int key) {
        super("DebugAC", key);
    }

    @Override
    public void link() {
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.field_71439_g) {
            reset();
        }
    }

    @Override
    public void onEnable() {
        detections = 0L;
        reset();
    }

    private static void clearTick() {
        openedInventory = false;
        closedWindow = false;
        clickedWindow = false;
        sentEntityAction = false;
        sentChat = false;
        stoppedBreak = false;
        abortedBreak = false;
        startedBreak = false;
        dug = false;
        dugAny = false;
        usedItem = false;
        placeNeedsSwing = false;
        placedBlockItem = false;
        placeInteract = false;
        placedUse = false;
        placed = false;
        interacted = false;
        interactedAt = false;
        attacked = false;
        usedEntity = false;
        swung = false;
        missingStopSneak = false;
        missingStopSprintFlag = false;
        missingSwapOrAbort = false;
        missingSwap = false;
        missingRelease = false;
        missingPlaceOrDig = false;
        missingPlace255 = false;
        missingPlace = false;
        missingInteract = false;
        missingSwing = false;
        entityActionLocked = false;
        unexpectedPlace = false;
        attackLocked = false;
        swaps = 0;
        swings = 0;
        packets = 0;
        lastClick = null;
        target = null;
        hitVec = null;
        postPacket = null;
    }

    private static void flag(String s) {
        Chat.print("&7Detected #&b" + ++detections + " &7(t:&b" + mc.field_71439_g.field_70173_aa + "&7): &d" + s);
        long now = System.currentTimeMillis();
        if (shouldPing.getValue() && Math.abs(now - lastPing) >= 1000L) {
            PlayerUtils.ping();
            lastPing = now;
        }
    }

    private static Object checkTarget(BlockPos pos, EnumFacing facing, double x, double y, double z, float yaw, float pitch) {
        MovingObjectPosition hit = PlayerUtils.rayTraceFrom(PlayerUtils.blockReach(), x, y, z, yaw, pitch);
        if (hit == null || hit.field_72313_a != MovingObjectPosition.MovingObjectType.BLOCK) {
            return null;
        }
        if (!BlockUtils.samePos(hit.func_178782_a(), pos)) {
            return hit.func_178782_a();
        }
        Block block;
        if (hit.field_178784_b != facing
                && !((block = BlockUtils.getBlock(pos)) instanceof BlockDoor)
                && !(block instanceof BlockTrapDoor)) {
            return hit.field_178784_b;
        }
        return 0;
    }

    private static void reset() {
        clearTick();
        stoppedBreakLastTick = false;
        sneaking = false;
        sprinting = false;
        blocking = false;
        usingItem = false;
        previousGround = false;
        ground = false;
        totalSwaps = 0;
        lastDigAction = null;
        startBreakFacing = null;
        lastDigFacing = null;
        placeFacing = null;
        lastDigPos = null;
        placePos = null;
        lastEntityAction = null;
        slot = mc.field_71439_g.field_71071_by.field_70461_c;
        updateHeldItem(slot);
    }

    public static void updateHeldItem(int n) {
        try {
            heldStack = mc.field_71439_g.field_71071_by.func_70301_a(n);
        } catch (ArrayIndexOutOfBoundsException e) {
            heldStack = null;
        }
        heldItem = heldStack == null ? null : heldStack.func_77973_b();
    }

    private static boolean grace() {
        return mc.field_71439_g != null
                && mc.field_71439_g.field_70173_aa <= 5
                || Math.abs(System.currentTimeMillis() - PacketUtils.lastPosLook) < 300L;
    }

    private static String v(Object o) {
        return "&b" + o + "&d";
    }

    public static void onSent(Packet<?> packet) {
        if (mc.func_71356_B() && packet.getClass().getSimpleName().charAt(0) == 'S') {
            return;
        }
        if (packet instanceof C03PacketPlayer) {
            onFlying((C03PacketPlayer) packet);
            return;
        }
        if (!(packet instanceof C0FPacketConfirmTransaction
                || packet instanceof C00PacketKeepAlive
                || packet instanceof C01PacketPing
                || packet instanceof FMLProxyPacket)) {
            ++packets;
            lastOther = System.currentTimeMillis();
            long l;
            if (!grace() && (l = lastOther - lastFlying) < POST_WINDOW) {
                postPacket = "post packet " + v(l) + "ms " + v(packet.getClass().getSimpleName());
            }
        }
        if (packet instanceof C0APacketAnimation) {
            swung = true;
            missingSwing = false;
            if (++swings > 3) {
                flag("too many swings " + v(swings));
            }
            if (blocking) {
                flag("swing while blocking");
            }
            if (placeInteract && !placeNeedsSwing && !startedBreak && !PlayerUtils.isCreative()) {
                flag("swing after regular place");
            }
        } else if (packet instanceof C0BPacketEntityAction) {
            onEntityAction((C0BPacketEntityAction) packet);
        } else if (packet instanceof C0DPacketCloseWindow) {
            if (openedInventory) {
                flag("closed window too soon");
            }
            if (closedWindow) {
                flag("duplicate close window");
            }
            closedWindow = true;
            blocking = false;
            usingItem = false;
        } else if (packet instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow p = (C0EPacketClickWindow) packet;
            if (openedInventory) {
                flag("clicked window too soon");
            }
            clickedWindow = true;
            if (lastClick != null) {
                if (p.func_149544_d() != lastClick[0]) {
                    if (p.func_149542_h() != 0
                            && p.func_149542_h() != 3
                            && p.func_149542_h() != 5
                            && (p.func_149542_h() != 1
                            || p.func_149544_d() < lastClick[0])) {
                        flag("impossible slot order " + v(lastClick[0]) + " -> " + v(p.func_149544_d()));
                    }
                } else if (p.func_149543_e() == lastClick[1]
                        && p.func_149542_h() == lastClick[2]
                        && p.func_149542_h() != 0
                        && p.func_149542_h() != 3) {
                    flag("duplicate window click mode " + v(p.func_149542_h()) + " button " + v(p.func_149543_e()));
                }
            }
            lastClick = new int[] { p.func_149544_d(), p.func_149543_e(), p.func_149542_h() };
        } else if (packet instanceof C01PacketChatMessage) {
            if (sentChat) {
                flag("duplicate chat");
            }
            sentChat = true;
        } else if (packet instanceof C02PacketUseEntity) {
            onUseEntity((C02PacketUseEntity) packet);
        } else if (packet instanceof C07PacketPlayerDigging) {
            onDig((C07PacketPlayerDigging) packet);
        } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            onPlace((C08PacketPlayerBlockPlacement) packet);
        } else if (packet instanceof C09PacketHeldItemChange) {
            int n = ((C09PacketHeldItemChange) packet).func_149614_c();
            if (n < 0 || n > 8) {
                flag("invalid swap slot " + v(n));
            }
            if (slot != null && n == slot) {
                flag("duplicate swap slot " + v(n));
            }
            blocking = false;
            usingItem = false;
            missingSwapOrAbort = false;
            missingSwap = false;
            ++totalSwaps;
            if (++swaps > 2) {
                flag("too many swaps " + v(swaps));
            } else if (swaps == 2) {
                if (startedBreak) {
                    missingSwing = true;
                } else {
                    missingPlaceOrDig = true;
                }
            }
            if (placed) {
                flag("swapped to " + v(n) + " after place");
            }
            slot = n;
            updateHeldItem(slot);
        } else if (packet instanceof C16PacketClientStatus
                && ((C16PacketClientStatus) packet).func_149435_c() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
            if (openedInventory) {
                flag("duplicate open inventory");
            }
            openedInventory = true;
        }
    }

    private static void onFlying(C03PacketPlayer p) {
        double ox;
        double oy;
        double oz;
        long now = System.currentTimeMillis();
        long l = now - lastFlying;
        if (l > 5L) {
            if (postPacket != null) {
                flag(postPacket);
            }
        } else if (blink.getValue() && !grace()) {
            flag("blink " + v(l) + "ms");
        }
        lastFlying = now;
        airC03s = p.func_149465_i() ? 0 : ++airC03s;
        if (p.func_149466_j()) {
            ox = prevX = x;
            oy = prevY = y;
            oz = prevZ = z;
            x = p.func_149464_c();
            y = p.func_149467_d();
            z = p.func_149472_e();
            double dx = x - prevX;
            double dz = z - prevZ;
            double speed = Math.sqrt(dx * dx + dz * dz);
            double accel = speed - lastSpeed;
            lastSpeed = speed;
            lastAccel = accel;
            if (backwardsSpeed.getValue()) {
                double yaw = p.func_149463_k() ? (double) p.func_149462_g() : (double) lastYaw;
                double pitch = p.func_149463_k() ? (double) p.func_149470_h() : (double) lastPitch;
                double rad = Math.toRadians(yaw);
                double along = dx * -Math.sin(rad) + dz * Math.cos(rad);
                if (Math.abs(along) > BACKWARDS
                        && Math.abs(along) > Math.abs(lastBackwards)
                        && (PlayerUtils.speedLevel() == 0
                        || placed
                        && pitch > 65.0
                        && PlayerUtils.holdingBlock())) {
                    if (-along > BACKWARDS) {
                        if (p.func_149465_i() && ground && previousGround) {
                            flag("backwards speed #1 " + v(Strings.round(-along, 3)) + " > " + v(Strings.round(BACKWARDS, 3)));
                        } else if (!p.func_149465_i() && !ground && previousGround) {
                            flag("backwards speed #2 " + v(Strings.round(-along, 3)) + " > " + v(Strings.round(BACKWARDS, 3)));
                        }
                    } else if (along > BACKWARDS * 2.0 && lastBackwards < -BACKWARDS / 2.0 && !p.func_149465_i() && ground) {
                        flag("backwards speed #3 " + v(Strings.round(along, 3)) + " > " + v(Strings.round(BACKWARDS * 2.0, 3)));
                    } else if (along > BACKWARDS && lastBackwards < -BACKWARDS / 2.0 && p.func_149465_i() && ground && previousGround) {
                        flag("backwards speed #4 " + v(Strings.round(along, 3)) + " > " + v(Strings.round(BACKWARDS, 3)));
                    }
                }
                lastBackwards = along;
            }
            if (groundSpoof.getValue()) {
                if (p.func_149465_i()) {
                    boolean valid = GROUND_Y.contains(y % 1.0);
                    if (!valid) {
                        flag("ground spoof, expected " + v(valid) + " with " + v(y));
                    }
                } else if (!mc.field_71439_g.field_71075_bZ.field_75100_b && !grace()) {
                    boolean valid;
                    Block block = BlockUtils.getState(BlockUtils.below(0.5)).func_177230_c();
                    if (!(block instanceof BlockStairs
                            || block instanceof BlockSlab
                            || (block = BlockUtils.getState(BlockUtils.below(1.0)).func_177230_c()) instanceof BlockStairs
                            || block instanceof BlockSlab)) {
                        block = null;
                    }
                    if (block == null && (valid = GROUND_Y.contains(y % 1.0))) {
                        flag("ground spoof, expected " + v(valid) + " with " + v(y));
                    }
                }
            }
        } else {
            ox = x;
            oy = y;
            oz = z;
        }
        if (p.func_149463_k()) {
            float yaw = p.func_149462_g();
            float pitch = p.func_149470_h();
            float dy = yaw - lastYaw;
            float dp = pitch - lastPitch;
            float ay = Math.abs(dy);
            float ap = Math.abs(dp);
            if (lastYaw != 0.0f && lastPitch != 0.0f && !grace()) {
                if (ay > 0.0f) {
                    if (ay < MIN_YAW_STEP && ay > TINY_YAW) {
                        flag("too small yaw diff " + v(dy));
                    }
                    if (dy % 10.0f == 0.0f) {
                        flag("very robotic yaw diff " + v(dy));
                    } else if (ay > 10.0f && dy % 1.0f == 0.0f) {
                        flag("robotic yaw diff " + v(dy));
                    }
                    if (ay > 400.0f) {
                        flag("unlikely fast yaw diff " + v(dy));
                    }
                }
                if (ap > 0.0f) {
                    if (ap < MIN_PITCH_STEP && ap > TINY_PITCH) {
                        flag("too small pitch diff " + v(dp));
                    }
                    if (ap != 180.0f && dp % 1.0f == 0.0f) {
                        flag("robotic pitch diff " + v(dp));
                    }
                    if (ap > 155.0f) {
                        flag("unlikely fast pitch diff " + v(dp));
                    }
                }
            }
            lastYaw = yaw;
            lastPitch = pitch;
            if (lastPitch > 90.0f || lastPitch < -90.0f) {
                flag("invalid pitch " + v(lastPitch));
            }
        }
        previousGround = ground;
        ground = p.func_149465_i();
        if (missingSwing) {
            flag("missing swing");
        }
        if (missingInteract) {
            flag("missing interact");
        }
        if (missingPlace) {
            flag("missing place");
        }
        if (missingPlace255) {
            flag("missing place 255");
        }
        if (missingPlaceOrDig) {
            flag("missing place or dig");
        }
        if (missingRelease) {
            flag("missing release");
        }
        if (missingSwap) {
            flag("missing swap");
        }
        if (missingSwapOrAbort) {
            flag("missing swap or abort");
        }
        if (missingStopSprintFlag && missingStopSprint.getValue()) {
            flag("missing stop sprint");
        }
        if (missingStopSneak) {
            flag("missing stop sneak");
        }
        if (startedBreak && !swung) {
            flag("start break without swinging");
        }
        if (target != null && interactedAt) {
            MovingObjectPosition hit = BotCheck.rayTrace(target, PlayerUtils.vanillaReach() + 0.2, ox, oy, oz, lastYaw, lastPitch);
            if (hit == null || hit.field_72313_a != MovingObjectPosition.MovingObjectType.ENTITY) {
                flag("interact unexpected target " + v(hit == null ? "null" : hit.field_72313_a.name()) + " expected " + v(target.func_70005_c_()));
            } else if (incorrectHitVec.getValue()) {
                Vec3 rel = new Vec3(hit.field_72307_f.field_72450_a - hit.field_72308_g.field_70165_t, hit.field_72307_f.field_72448_b - hit.field_72308_g.field_70163_u, hit.field_72307_f.field_72449_c - hit.field_72308_g.field_70161_v);
                if (Math.abs(rel.field_72450_a - hitVec.field_72450_a) > HIT_VEC_TOLERANCE
                        || Math.abs(rel.field_72448_b - hitVec.field_72448_b) > HIT_VEC_TOLERANCE
                        || Math.abs(rel.field_72449_c - hitVec.field_72449_c) > HIT_VEC_TOLERANCE) {
                    flag("incorrect hit vec " + v(Math.abs(rel.field_72450_a - hitVec.field_72450_a) + " " + Math.abs(rel.field_72448_b - hitVec.field_72448_b) + " " + Math.abs(rel.field_72449_c - hitVec.field_72449_c)));
                }
            }
        }
        if (placeInteract && !placedBlockItem) {
            Object o = checkTarget(placePos, placeFacing, ox, oy, oz, lastYaw, lastPitch);
            if (o == null) {
                flag("place interact unexpected target");
            } else if (o instanceof BlockPos) {
                flag("place interact incorrect position");
            } else if (o instanceof EnumFacing) {
                flag("place interact incorrect facing, expected " + v(placeFacing == null ? "null" : placeFacing.name()) + " got " + v(((EnumFacing) o).name()));
            }
        }
        if (dug && startedBreak && !PlayerUtils.isCreative() && digTargets.getValue()) {
            Object o = checkTarget(lastDigPos, startBreakFacing, ox, oy, oz, lastYaw, lastPitch);
            if (o == null) {
                flag("start dig unexpected target");
            } else if (o instanceof BlockPos) {
                flag("start dig incorrect position");
            } else if (o instanceof EnumFacing) {
                flag("start dig incorrect facing, expected " + v(startBreakFacing == null ? "null" : startBreakFacing.name()) + " got " + v(((EnumFacing) o).name()));
            }
        }
        if (sentChat && (packets > 2 || packets == 2 && !sentEntityAction)) {
            flag("impossible chat " + v(packets));
        }
        if (clickedWindow && sprinting) {
            flag("clicked window while sprinting");
        }
        if (closedWindow) {
            if (swung || usedEntity || placed || dugAny || swaps > 0) {
                flag("impossible packets while closing window");
            }
            if (p instanceof C03PacketPlayer.C05PacketPlayerLook || p instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
                flag("rotated while closing window");
            }
        }
        stoppedBreakLastTick = stoppedBreak;
        clearTick();
        updateHeldItem(slot);
    }

    private static void onEntityAction(C0BPacketEntityAction p) {
        if (entityActionLocked) {
            flag("unexpected entity action");
        }
        if (sentEntityAction) {
            entityActionLocked = true;
        }
        switch(p.func_180764_b()) {
            case START_SPRINTING:
                if (sprinting) {
                    flag("started sprinting while sprinting");
                }
                if (sentEntityAction && lastEntityAction == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                    flag("instant start sprint");
                }
                if (usedItem && missingStopSprint.getValue()) {
                    flag("start sprint after using item");
                }
                if (sneaking) {
                    missingStopSneak = true;
                }
                sprinting = true;
                break;
            case STOP_SPRINTING:
                if (!sprinting) {
                    flag("stopped sprinting while not sprinting");
                }
                if (sentEntityAction && lastEntityAction == C0BPacketEntityAction.Action.START_SPRINTING) {
                    flag("instant stop sprint");
                }
                missingStopSprintFlag = false;
                sprinting = false;
                break;
            case START_SNEAKING:
                if (sneaking) {
                    flag("started sneaking while sneaking");
                }
                if (sentEntityAction && lastEntityAction == C0BPacketEntityAction.Action.STOP_SNEAKING) {
                    flag("instant start sneak");
                }
                if (sprinting) {
                    flag("started sprinting while sneaking");
                }
                sneaking = true;
                break;
            case STOP_SNEAKING:
                if (!sneaking) {
                    flag("stopped sneaking while sneaking");
                }
                if (sentEntityAction && lastEntityAction == C0BPacketEntityAction.Action.START_SNEAKING) {
                    flag("instant stop sneak");
                }
                missingStopSneak = false;
                sneaking = false;
                break;
            default:
                break;
        }
        sentEntityAction = true;
        lastEntityAction = p.func_180764_b();
    }

    private static void onUseEntity(C02PacketUseEntity p) {
        Entity entity = p.func_149564_a(mc.field_71441_e);
        usedEntity = true;
        if (blocking) {
            flag("interact while blocking");
        }
        if (placed) {
            flag("interact after placing");
        }
        if (dugAny) {
            flag("interact after digging");
        }
        switch(p.func_149565_c()) {
            case ATTACK:
                if (attackLocked) {
                    flag("unexpected attack");
                }
                if (attacked) {
                    attackLocked = true;
                    if (target != entity) {
                        flag("duplicate attack on different target");
                    }
                }
                attacked = true;
                if (!swung) {
                    flag("attack without swing");
                }
                target = entity;
                break;
            case INTERACT_AT:
                if (interactedAt) {
                    flag("duplicate interact_at");
                }
                interactedAt = true;
                if (attacked) {
                    if (target == null) {
                        flag("invalid target on interact_at");
                    } else if (target != entity) {
                        flag("incorrect target on interact_at");
                    }
                } else {
                    target = entity;
                }
                hitVec = p.func_179712_b();
                if (target instanceof EntityPlayer) {
                    if (hitVec.field_72448_b < MIN_HIT_Y) {
                        flag("invalid y hit vec " + v(hitVec.field_72448_b));
                    } else if (hitVec.field_72448_b > MAX_HIT_Y) {
                        flag("invalid y hit vec " + v(hitVec.field_72448_b));
                    }
                    if (hitVec.field_72450_a < MIN_HIT_XZ || hitVec.field_72450_a > MAX_HIT_XZ) {
                        flag("invalid x hit vec " + v(hitVec.field_72450_a));
                    }
                    if (hitVec.field_72449_c < MIN_HIT_XZ || hitVec.field_72449_c > MAX_HIT_XZ) {
                        flag("invalid z hit vec " + v(hitVec.field_72449_c));
                    }
                }
                if (target instanceof EntityArmorStand)
                    break;
                missingInteract = true;
                break;
            case INTERACT:
                if (interacted) {
                    flag("duplicate interact");
                }
                interacted = true;
                missingInteract = false;
                if (interactedAt) {
                    if (target == null) {
                        flag("invalid target on interact");
                    } else if (target != entity) {
                        flag("incorrect target on interact");
                    }
                }
                if (!interactedAt) {
                    flag("missing interact_at before interact");
                }
                if (heldStack == null
                        || heldItem == Items.field_151063_bx
                        || entity instanceof EntityItemFrame
                        || !mc.field_71439_g.func_70093_af()
                        && (entity instanceof EntityVillager
                        || entity instanceof EntityArmorStand)) {
                    unexpectedPlace = true;
                    break;
                }
                missingPlace255 = true;
                break;
        }
        if (target == null) {
            flag("null target on interact");
        } else if (target == mc.field_71439_g) {
            flag("interacted with self");
        }
    }

    private static void onDig(C07PacketPlayerDigging p) {
        C07PacketPlayerDigging.Action action = p.func_180762_c();
        EnumFacing facing = p.func_179714_b();
        BlockPos pos = p.func_179715_a();
        switch(action) {
            case RELEASE_USE_ITEM:
                if (!usingItem && swaps == 0) {
                    flag("released while not using");
                }
                blocking = false;
                usingItem = false;
                missingRelease = false;
                if (facing != EnumFacing.DOWN) {
                    flag("incorrect facing on release " + v(facing.name()));
                }
                if (pos.func_177958_n() == 0 && pos.func_177956_o() == 0 && pos.func_177952_p() == 0)
                    break;
                flag("incorrect position on release " + v(pos.func_177958_n() + " " + pos.func_177956_o() + " " + pos.func_177952_p()));
                break;
            case START_DESTROY_BLOCK:
                startedBreak = true;
                startBreakFacing = facing;
                if (!abortedBreak)
                    break;
                if (facing != lastDigFacing) {
                    flag("incorrect facing on start break, expected " + v(lastDigFacing) + " got " + v(facing));
                }
                if (swaps != 0 || !pos.equals(lastDigPos))
                    break;
                missingSwapOrAbort = true;
                break;
            case STOP_DESTROY_BLOCK:
                stoppedBreak = true;
                if (PlayerUtils.isCreative()) {
                    flag("stop break in creative mode");
                }
                if (startedBreak) {
                    flag("instant break");
                }
                if (lastDigAction == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
                    flag("duplicate stop break");
                } else if (lastDigAction == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK && !pos.equals(lastDigPos)) {
                    flag("incorrect last position on stop break " + v(pos.func_177958_n() + " " + pos.func_177956_o() + " " + pos.func_177952_p()));
                }
                if (!digTargets.getValue())
                    break;
                Object o = checkTarget(pos, facing, x, y, z, lastYaw, lastPitch);
                if (o == null) {
                    flag("stop dig unexpected target");
                    break;
                }
                if (!(o instanceof BlockPos))
                    break;
                flag("stop dig incorrect position");
                break;
            case ABORT_DESTROY_BLOCK:
                abortedBreak = true;
                missingSwapOrAbort = false;
                if (lastDigAction != C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK)
                    break;
                flag("duplicate abort break");
                break;
            default:
                break;
        }
        if (action != C07PacketPlayerDigging.Action.DROP_ITEM && action != C07PacketPlayerDigging.Action.DROP_ALL_ITEMS) {
            if (action.ordinal() <= 2) {
                missingPlaceOrDig = false;
                dug = true;
                lastDigAction = action;
                lastDigFacing = facing;
                lastDigPos = pos;
                if (PlayerUtils.isAdventure()) {
                    flag("dig in adventure mode");
                }
            }
            dugAny = true;
        }
    }

    private static void onPlace(C08PacketPlayerBlockPlacement p) {
        int n = p.func_149568_f();
        Item item = p.func_149574_g() == null ? null : p.func_149574_g().func_77973_b();
        BlockPos pos = p.func_179724_a();
        Vec3 offset = new Vec3(p.func_149573_h(), p.func_149569_i(), p.func_149575_j());
        placed = true;
        if (unexpectedPlace) {
            flag("unexpected place");
        }
        if (missingPlace255 && n != 255) {
            flag("expected place was wrong direction");
        }
        if (!(!attacked || interactedAt && interacted)) {
            flag("placed while attacking without interact");
        }
        if (!(!dugAny || PlayerUtils.isCreative() || startedBreak && n == 255)) {
            flag("placed after dig");
        }
        if (totalSwaps > 0 && item != heldItem && item != Items.field_151069_bo) {
            flag("incorrect item, expected " + v(ItemUtils.name(heldItem)) + " got " + v(ItemUtils.name(item)));
        }
        if (n == 255) {
            EnumAction action;
            missingPlace255 = false;
            placedUse = true;
            if (item != null && (action = item.func_77661_b(p.func_149574_g())) != EnumAction.NONE) {
                if (action == EnumAction.BLOCK) {
                    if (blocking && !PlayerUtils.isCreative()) {
                        flag("blocked sword while blocked");
                    }
                    blocking = true;
                }
                usedItem = true;
                usingItem = true;
                if (sprinting && !PlayerUtils.isCreative()) {
                    missingStopSprintFlag = true;
                }
            }
            if (item == null) {
                flag("invalid item on place " + v(ItemUtils.name(item)));
            }
            if (pos.func_177958_n() != -1 || pos.func_177956_o() != -1 || pos.func_177952_p() != -1) {
                flag("invalid position on place " + v(pos.func_177958_n() + " " + pos.func_177956_o() + " " + pos.func_177952_p()));
            }
            if (offset.field_72450_a != 0.0 || offset.field_72448_b != 0.0 || offset.field_72449_c != 0.0) {
                flag("invalid offset on place " + v(offset.field_72450_a + " " + offset.field_72448_b + " " + offset.field_72449_c));
            }
        } else if (n >= 0 && n <= 5) {
            if (placeInteract) {
                flag("duplicate place interact");
            }
            placeInteract = true;
            placePos = pos;
            placeFacing = EnumFacing.func_82600_a(n);
            if (heldItem instanceof ItemBlock) {
                placedBlockItem = true;
            }
            if (placedBlockItem || ItemUtils.isPlaceable(heldItem) || BlockUtils.isInteractable(BlockUtils.getBlock(pos))) {
                placeNeedsSwing = true;
                if (!(stoppedBreakLastTick || mc.field_71439_g.func_70093_af() || PlayerUtils.isAdventure())) {
                    missingSwing = true;
                }
            } else if (heldItem == null) {
                unexpectedPlace = true;
            } else {
                missingPlace255 = true;
            }
            if (pos.func_177956_o() < 0 || pos.func_177956_o() > 255) {
                flag("invalid position y " + v(pos.func_177956_o()));
            }
            if (n == 4
                    && !OFFSETS_LOW.contains(offset.field_72450_a)
                    || n == 5
                    && !OFFSETS_HIGH.contains(offset.field_72450_a)
                    || offset.field_72450_a < 0.0
                    || offset.field_72450_a > 1.0) {
                flag("incorrect offset x " + v(offset.field_72450_a));
            }
            if (n == 0
                    && !OFFSETS_LOW.contains(offset.field_72448_b)
                    || n == 1
                    && !OFFSETS_HIGH.contains(offset.field_72448_b)
                    || offset.field_72448_b < 0.0
                    || offset.field_72448_b > 1.0) {
                flag("incorrect offset y " + v(offset.field_72448_b));
            }
            if (n == 2
                    && !OFFSETS_LOW.contains(offset.field_72449_c)
                    || n == 3
                    && !OFFSETS_HIGH.contains(offset.field_72449_c)
                    || offset.field_72449_c < 0.0
                    || offset.field_72449_c > 1.0) {
                flag("incorrect offset z " + v(offset.field_72449_c));
            }
        } else {
            flag("invalid facing " + v(n));
        }
        missingPlaceOrDig = false;
        missingPlace = false;
    }
}
