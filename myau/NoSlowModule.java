package myau;

import com.google.common.base.CaseFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;

public class NoSlowModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private int lastFloatUseSlot = -1;
    private boolean grimSlowedThisTick = false;
    private boolean grimFreeTickAllowed = false;
    private boolean switchedAway = false;
    private boolean switchAwayTick = false;
    private boolean reblockPending = false;
    private static final int SWORD_BLINK = 4;
    private static final int FOOD_BLINK = 3;

    final NoSlowBlink blink = new NoSlowBlink(this);

    public final ModeSetting swordMode = blinkMode("sword-mode", 1, "NONE", "VANILLA", "GRIM", "SWITCH", "Blink");
    public final PercentSetting swordMotion = new PercentSetting("sword-motion", 20, this::isSwordMotionVisible);
    public final BooleanSetting swordSprint = new BooleanSetting("sword-sprint", true, this::isSwordSprintVisible);
    public final ModeSetting foodMode = blinkMode("food-mode", 0, "NONE", "VANILLA", "FLOAT", "Blink");
    public final IntegerSetting blinkTicks = new IntegerSetting("blink-ticks", 1, 1, 20, () -> swordMode.getValue() == SWORD_BLINK || foodMode.getValue() == FOOD_BLINK);
    public final PercentSetting foodMotion = new PercentSetting("food-motion", 20, this::isFoodMotionVisible);
    public final BooleanSetting foodSprint = new BooleanSetting("food-sprint", true, this::isFoodSprintVisible);
    public final ModeSetting bowMode = new ModeSetting("bow-mode", 0, new String[] { "NONE", "VANILLA", "FLOAT" });
    public final PercentSetting bowMotion = new PercentSetting("bow-motion", 20, this::isBowMotionVisible);
    public final BooleanSetting bowSprint = new BooleanSetting("bow-sprint", true, this::isBowSprintVisible);

    public NoSlowModule() {
        super("NoSlow", false, 0);
    }

    @Override
    public String[] getHudInfo() {
        ModeSetting mode = null;
        if (minecraft.field_71439_g != null) {
            if (this.isSwordModeActive())
                mode = this.swordMode;
            else if (this.isFoodModeActive())
                mode = this.foodMode;
            else if (this.isBowModeActive())
                mode = this.bowMode;
        }
        if (mode == null) {
            if (this.swordMode.getValue() != 0)
                mode = this.swordMode;
            else if (this.foodMode.getValue() != 0)
                mode = this.foodMode;
            else if (this.bowMode.getValue() != 0)
                mode = this.bowMode;
        }
        return mode == null ? super.getHudInfo() : new String[] { CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, mode.getModeName()) };
    }

    private static ModeSetting blinkMode(String name, int defaultMode, String... modes) {
        return new ModeSetting(name, defaultMode, modes) {

            @Override
            public boolean parseValue(String value) {
                return super.parseValue("Semi".equalsIgnoreCase(value) || "Legit".equalsIgnoreCase(value) || "Adjust Legit".equalsIgnoreCase(value) ? "Blink" : value);
            }
        };
    }

    boolean isSwordBlinkSelected() {
        return this.isEnabled() && minecraft.field_71439_g != null && swordMode.getValue() == SWORD_BLINK && ItemUtils.isHoldingSword();
    }

    boolean isBlinkSelected() {
        return this.isSwordBlinkSelected()
                || (this.isEnabled()
                && minecraft.field_71439_g != null
                && foodMode.getValue() == FOOD_BLINK
                && ItemUtils.isHoldingConsumable());
    }

    @EventHandler(priority = -200)
    public void onBlinkPacket(PacketSendEvent event) {
        this.blink.packet(event);
    }

    @EventHandler
    public void onBlinkPostMotion(PostPlayerUpdateEvent event) {
        this.blink.postMotion();
    }

    @EventHandler
    public void onBlinkPostTick(PostTickEvent event) {
        this.blink.watchdog();
    }

    @EventHandler
    public void onBlinkWorldLoad(WorldLoadEvent event) {
        this.blink.reset(false);
    }

    @Override
    public void onSettingChanged(String name) {
        if ("sword-mode".equals(name) || "food-mode".equals(name))
            this.blink.reset(true);
    }

    public boolean isSwordModeActive() {
        return this.swordMode.getValue() != 0 && ItemUtils.isHoldingSword();
    }

    public boolean isFoodModeActive() {
        return this.foodMode.getValue() != 0 && ItemUtils.isHoldingConsumable();
    }

    public boolean isBowModeActive() {
        return this.bowMode.getValue() != 0 && ItemUtils.isHoldingBow();
    }

    public boolean isFloatModeActive() {
        return this.foodMode.getValue() == 2
                && ItemUtils.isHoldingConsumable()
                || this.bowMode.getValue() == 2
                && ItemUtils.isHoldingBow();
    }

    public boolean shouldSuppressItemSlowdown() {
        return !this.isBlinkSelected()
                && minecraft.field_71439_g.func_71039_bw()
                && (this.isSwordModeActive()
                || this.isFoodModeActive()
                || this.isBowModeActive());
    }

    public boolean shouldAllowSprint() {
        if (this.isBlinkSelected())
            return false;
        return this.isSwordModeActive()
                && this.swordSprint.getValue()
                || this.isFoodModeActive()
                && this.foodSprint.getValue()
                || this.isBowModeActive()
                && this.bowSprint.getValue();
    }

    public int getMotionPercent() {
        if (this.isBlinkSelected())
            return PlayerUtils.isUseItemKeyDown() ? 100 : 20;
        if (ItemUtils.isHoldingSword()) {
            return this.swordMotion.getValue();
        }
        if (ItemUtils.isHoldingConsumable()) {
            return this.foodMotion.getValue();
        }
        if (ItemUtils.isHoldingBow()) {
            return this.bowMotion.getValue();
        }
        return 100;
    }

    private boolean isSwitchActive() {
        return this.isEnabled()
                && this.swordMode.getValue() == 3
                && ItemUtils.isHoldingSword()
                && minecraft.field_71439_g.func_71039_bw();
    }

    public boolean shouldHoldAttack() {
        return this.switchAwayTick;
    }

    @EventHandler(priority = 200)
    public void onRotationUpdateFirst(RotationUpdateEvent rotationUpdateEvent) {
        this.switchAwayTick = false;
        this.reblockPending = false;
        int n = minecraft.field_71439_g.field_71071_by.field_70461_c;
        if (!this.isSwitchActive()) {
            if (this.switchedAway) {
                PacketUtils.send(new C09PacketHeldItemChange(n));
                this.switchedAway = false;
            }
            return;
        }
        if (this.switchedAway) {
            PacketUtils.send(new C09PacketHeldItemChange(n));
            this.switchedAway = false;
            this.reblockPending = true;
        } else {
            PacketUtils.send(new C09PacketHeldItemChange((n + 1) % 9));
            this.switchedAway = true;
            this.switchAwayTick = true;
        }
    }

    @EventHandler(priority = -100)
    public void onRotationUpdateLast(RotationUpdateEvent rotationUpdateEvent) {
        if (!this.reblockPending) {
            return;
        }
        this.reblockPending = false;
        ItemStack itemStack = minecraft.field_71439_g.func_70694_bm();
        if (itemStack != null && this.isSwitchActive()) {
            PacketUtils.send(new C08PacketPlayerBlockPlacement(itemStack));
        }
    }

    public boolean isGrimSwordMode() {
        return this.swordMode.getValue() == 2 && ItemUtils.isHoldingSword();
    }

    private boolean isServerBlocking() {
        KillAuraModule killAuraModule = (KillAuraModule) MyauClient.moduleManager.modules.get(KillAuraModule.class);
        if (killAuraModule.isEnabled() && killAuraModule.isAutoBlockActive()) {
            return killAuraModule.isServerBlocking();
        }
        return minecraft.field_71439_g.func_71039_bw();
    }

    private void applyGrimSwordSlowdown() {
        this.grimSlowedThisTick = false;
        if (!this.isServerBlocking()) {
            return;
        }
        if (this.grimFreeTickAllowed) {
            this.grimFreeTickAllowed = false;
            return;
        }
        this.grimSlowedThisTick = true;
        minecraft.field_71439_g.field_71158_b.field_78900_b *= 0.2f;
        minecraft.field_71439_g.field_71158_b.field_78902_a *= 0.2f;
        if (!this.swordSprint.getValue()) {
            minecraft.field_71439_g.func_70031_b(false);
        }
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent packetSendEvent) {
        if (!(packetSendEvent.getPacket() instanceof C03PacketPlayer) || !this.grimSlowedThisTick) {
            return;
        }
        this.grimSlowedThisTick = false;
        if (((C03PacketPlayer) packetSendEvent.getPacket()).func_149466_j()) {
            this.grimFreeTickAllowed = true;
        }
    }

    @EventHandler
    public void onLivingUpdate(LivingUpdateEvent livingUpdateEvent) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.isGrimSwordMode()) {
            this.applyGrimSwordSlowdown();
            return;
        }
        if (this.swordMode.getValue() == 3 && ItemUtils.isHoldingSword()) {
            return;
        }
        if (!this.shouldSuppressItemSlowdown()) {
            return;
        }
        KillAuraModule killAuraModule = (KillAuraModule) MyauClient.moduleManager.modules.get(KillAuraModule.class);
        if (killAuraModule.isAutoBlockActive() && killAuraModule.autoBlockNoSlow.getValue()) {
            return;
        }
        float f = (float) this.getMotionPercent() / 100.0f;
        minecraft.field_71439_g.field_71158_b.field_78900_b *= f;
        minecraft.field_71439_g.field_71158_b.field_78902_a *= f;
        if (!this.shouldAllowSprint()) {
            minecraft.field_71439_g.func_70031_b(false);
        }
    }

    @EventHandler
    public void onPreWalkingUpdate(PreWalkingUpdateEvent preWalkingUpdateEvent) {
        if (this.isEnabled() && this.isFloatModeActive()) {
            int n = minecraft.field_71439_g.field_71071_by.field_70461_c;
            if (this.lastFloatUseSlot != n && PlayerUtils.isUseItemKeyDown()) {
                this.lastFloatUseSlot = n;
                MyauClient.groundSpoofManager.setRequest(true, GroundSpoofOwner.NO_SLOW);
            }
            return;
        }
        this.lastFloatUseSlot = -1;
        MyauClient.groundSpoofManager.setRequest(false, GroundSpoofOwner.NO_SLOW);
    }

    @EventHandler
    public void onRightClick(RightClickEvent rightClickEvent) {
        if (!this.isEnabled()) {
            return;
        }
        if (minecraft.field_71476_x != null) {
            switch(minecraft.field_71476_x.field_72313_a) {
                case BLOCK:
                    {
                        BlockPos blockPos = minecraft.field_71476_x.func_178782_a();
                        if (!BlockUtils.isInteractableAt(blockPos) || PlayerUtils.isSneakKeyDown())
                            break;
                        return;
                    }
                case ENTITY:
                    {
                        Entity entity = minecraft.field_71476_x.field_72308_g;
                        if (entity instanceof EntityVillager) {
                            return;
                        }
                        if (!(entity instanceof EntityLivingBase) || !EntityUtils.isShopNPC((EntityLivingBase) entity))
                            break;
                        return;
                    }
            }
        }
        if (!this.isFloatModeActive() || MyauClient.groundSpoofManager.isOffsetActive() || !minecraft.field_71439_g.field_70122_E) {
            return;
        }
        rightClickEvent.setCancelled(true);
        minecraft.field_71439_g.field_70181_x = 0.42f;
    }

    private boolean isBowSprintVisible() {
        return this.bowMode.getValue() != 0;
    }

    private boolean isBowMotionVisible() {
        return this.bowMode.getValue() != 0;
    }

    private boolean isFoodSprintVisible() {
        return foodMode.getValue() != 0 && foodMode.getValue() != FOOD_BLINK;
    }

    private boolean isFoodMotionVisible() {
        return foodMode.getValue() != 0 && foodMode.getValue() != FOOD_BLINK;
    }

    private boolean isSwordSprintVisible() {
        return swordMode.getValue() != 0 && swordMode.getValue() != SWORD_BLINK;
    }

    private boolean isSwordMotionVisible() {
        return this.swordMode.getValue() == 1;
    }

    @Override
    public void onDisable() {
        this.blink.reset(true);
        if (this.switchedAway && minecraft.field_71439_g != null) {
            PacketUtils.send(new C09PacketHeldItemChange(minecraft.field_71439_g.field_71071_by.field_70461_c));
        }
        this.switchedAway = false;
        this.switchAwayTick = false;
        this.reblockPending = false;
    }
}
