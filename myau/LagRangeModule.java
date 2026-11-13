package myau;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import myau.mixin.AccessorPlayerControllerMP;
import myau.mixin.AccessorRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class LagRangeModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private int delayTicks = -1;
    private long remainingDelayMillis;
    private boolean delaying;
    private Vec3 previousPosition;
    private Vec3 currentPosition;
    public final IntegerSetting delay = new IntegerSetting("delay", 100, 0, 1000);
    public final FloatSetting range = new FloatSetting("range", 10.0f, 3.0f, 100.0f);
    public final BooleanSetting weaponsOnly = new BooleanSetting("weapons-only", true);
    public final BooleanSetting allowTools = new BooleanSetting("allow-tools", false, this.weaponsOnly::getValue);
    public final BooleanSetting botCheck = new BooleanSetting("bot-check", true);
    public final BooleanSetting teams = new BooleanSetting("teams", true);
    public final ModeSetting showPosition = new ModeSetting("show-position", 0, new String[] { "NONE", "DEFAULT", "HUD" });

    public LagRangeModule() {
        super("LagRange", false, 0);
    }

    private boolean isTarget(EntityPlayer player) {
        if (player == minecraft.field_71439_g
                || player == minecraft.field_71439_g.field_70154_o
                || player == minecraft.func_175606_aa()
                || player == minecraft.func_175606_aa().field_70154_o) {
            return false;
        }
        if (player.field_70725_aQ > 0 || EntityUtils.isFriend(player)) {
            return false;
        }
        if (EntityUtils.isEnemy(player)) {
            return true;
        }
        if (this.teams.getValue() && EntityUtils.isTeammate(player)) {
            return false;
        }
        return !this.botCheck.getValue() || !EntityUtils.isBot(player);
    }

    private boolean shouldFlush(Packet packet) {
        if (packet instanceof C02PacketUseEntity) {
            return true;
        }
        if (packet instanceof C07PacketPlayerDigging) {
            return ((C07PacketPlayerDigging) packet).func_180762_c() != C07PacketPlayerDigging.Action.RELEASE_USE_ITEM;
        }
        if (packet instanceof C08PacketPlayerBlockPlacement) {
            ItemStack stack = ((C08PacketPlayerBlockPlacement) packet).func_149574_g();
            return stack == null || !(stack.func_77973_b() instanceof ItemSword);
        }
        return false;
    }

    @EventHandler(priority = -50)
    public void onPreTick(PreTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        MyauClient.packetDelayManager.setDelayTicks(0);
        this.delaying = false;
        BedNukerModule conflictingModule = (BedNukerModule) MyauClient.moduleManager.modules.get(BedNukerModule.class);
        if (conflictingModule.isEnabled() && conflictingModule.isControllingInteractions()) {
            this.delayTicks = -1;
            return;
        }
        if (((AccessorPlayerControllerMP) minecraft.field_71442_b).getIsHittingBlock()
                || minecraft.field_71439_g.func_71039_bw()
                && !minecraft.field_71439_g.func_70632_aY()
                || this.weaponsOnly.getValue()
                && !ItemUtils.isHoldingWeapon()
                && (!this.allowTools.getValue()
                || !ItemUtils.isHoldingTool())) {
            this.delayTicks = -1;
            return;
        }
        List<EntityPlayer> targets = minecraft.field_71441_e.field_72996_f.stream().filter(entity -> entity instanceof EntityPlayer).map(EntityPlayer.class::cast).filter(this::isTarget).collect(Collectors.toList());
        if (targets.isEmpty()) {
            this.delayTicks = -1;
            return;
        }
        double eyeHeight = minecraft.field_71439_g.func_70047_e();
        Vec3 bufferedEyes = MyauClient.packetDelayManager.lastReleasedPosition().func_72441_c(0.0, eyeHeight, 0.0);
        Vec3 previousEyes = new Vec3(minecraft.field_71439_g.field_70142_S, minecraft.field_71439_g.field_70137_T + eyeHeight, minecraft.field_71439_g.field_70136_U);
        Vec3 currentEyes = new Vec3(minecraft.field_71439_g.field_70165_t, minecraft.field_71439_g.field_70163_u + eyeHeight, minecraft.field_71439_g.field_70161_v);
        for (EntityPlayer target : targets) {
            double currentDistance = RotationUtils.distanceToEntityFrom((Entity) target, currentEyes);
            if (currentDistance > (double) this.range.getValue())
                continue;
            double previousDistance = RotationUtils.distanceToEntityFrom((Entity) target, previousEyes);
            double bufferedDistance = RotationUtils.distanceToEntityFrom((Entity) target, bufferedEyes);
            if (!(currentDistance < previousDistance) && !(currentDistance < bufferedDistance))
                continue;
            this.delayTicks = 0;
            this.remainingDelayMillis += (long) this.delay.getValue();
            while (this.remainingDelayMillis > 0L) {
                ++this.delayTicks;
                this.remainingDelayMillis -= 50L;
            }
            MyauClient.packetDelayManager.setDelayTicks(this.delayTicks);
            this.delaying = true;
            break;
        }
    }

    @EventHandler(priority = -50)
    public void onPostTick(PostTickEvent event) {
        if (this.isEnabled()) {
            Vec3 position = MyauClient.packetDelayManager.lastReleasedPosition();
            this.previousPosition = this.currentPosition == null ? position : this.currentPosition;
            this.currentPosition = position;
        }
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent event) {
        boolean flush = this.isEnabled();
        if (!flush || event.isCancelled()) {
            return;
        }
        flush = this.shouldFlush(event.getPacket());
        if (flush) {
            MyauClient.packetDelayManager.setDelayTicks(0);
            this.delayTicks = -1;
        }
    }

    @EventHandler(priority = 50)
    public void onRenderWorld(RenderWorldEvent event) {
        boolean draw = this.isEnabled();
        if (!draw || this.showPosition.getValue() == 0 || minecraft.field_71474_y.field_74320_O == 0) {
            return;
        }
        draw = this.delaying;
        if (!draw || this.previousPosition == null) {
            return;
        }
        if (this.currentPosition == null) {
            return;
        }
        Color positionColor = new Color(-1);
        switch(this.showPosition.getValue()) {
            case 1:
                {
                    positionColor = EntityUtils.teamColor((EntityPlayer) minecraft.field_71439_g, 1.0f);
                    break;
                }
            case 2:
                {
                    positionColor = ((HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class)).getColor(System.currentTimeMillis());
                }
        }
        double x = MathUtils.lerpDouble(this.previousPosition.field_72450_a, this.currentPosition.field_72450_a, event.getPartialTicks());
        double y = MathUtils.lerpDouble(this.previousPosition.field_72448_b, this.currentPosition.field_72448_b, event.getPartialTicks());
        double z = MathUtils.lerpDouble(this.previousPosition.field_72449_c, this.currentPosition.field_72449_c, event.getPartialTicks());
        float border = minecraft.field_71439_g.func_70111_Y();
        AxisAlignedBB bounds = new AxisAlignedBB(x - (double) minecraft.field_71439_g.field_70130_N / 2.0, y, z - (double) minecraft.field_71439_g.field_70130_N / 2.0, x + (double) minecraft.field_71439_g.field_70130_N / 2.0, y + (double) minecraft.field_71439_g.field_70131_O, z + (double) minecraft.field_71439_g.field_70130_N / 2.0).func_72314_b((double) border, (double) border, (double) border).func_72317_d(-((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosX(), -((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosY(), -((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosZ());
        RenderUtils.beginOverlay();
        RenderUtils.fillBounds(bounds, positionColor.getRed(), positionColor.getGreen(), positionColor.getBlue(), 63);
        RenderUtils.endOverlay();
    }

    @Override
    public void onDisable() {
        MyauClient.packetDelayManager.setDelayTicks(0);
        this.delayTicks = -1;
        this.remainingDelayMillis = 0L;
        this.delaying = false;
        this.previousPosition = null;
        this.currentPosition = null;
    }

    @Override
    public String[] getHudInfo() {
        return new String[] { String.format("%dms", this.delay.getValue()) };
    }
}
