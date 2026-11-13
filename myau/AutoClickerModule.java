package myau;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.WorldSettings;

public class AutoClickerModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private boolean restoreAttackKey;
    private long clickCooldownMillis;
    private boolean restoreUseItemKey;
    private long blockHitHoldMillis;
    private long blockHitCycleMillis;
    public final IntegerSetting minCps = new IntegerSetting("min-cps", 10, 1, 20);
    public final IntegerSetting maxCps = new IntegerSetting("max-cps", 14, 1, 20);
    public final BooleanSetting blockHit = new BooleanSetting("block-hit", false);
    public final FloatSetting blockHitHold = new FloatSetting("block-hit-hold", 1.5f, 1.0f, 20.0f, this.blockHit::getValue);
    public final FloatSetting blockHitDelay = new FloatSetting("block-hit-delay", 0.0f, 0.0f, 20.0f, this.blockHit::getValue);
    public final IntegerSetting blockHitHurtTime = new IntegerSetting("block-hit-hurt-time", 6, 0, 10, this.blockHit::getValue);
    public final BooleanSetting weaponsOnly = new BooleanSetting("weapons-only", true);
    public final BooleanSetting allowTools = new BooleanSetting("allow-tools", false, this.weaponsOnly::getValue);
    public final BooleanSetting breakBlocks = new BooleanSetting("break-blocks", true);

    private long nextClickDelayMillis() {
        return 1000L / RandomUtils.randomLong(this.minCps.getValue().longValue(), this.maxCps.getValue().longValue());
    }

    private long blockHitHoldDurationMillis() {
        return NumericConversions.truncateFloatToLong(this.blockHitHold.getValue() * 50.0f);
    }

    private long blockHitDelayDurationMillis() {
        return NumericConversions.truncateFloatToLong(this.blockHitDelay.getValue() * 50.0f);
    }

    private boolean isLookingAtBlock() {
        MovingObjectPosition hitResult = minecraft.field_71476_x;
        return hitResult != null && hitResult.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private boolean canAutoClick() {
        if (!(!this.weaponsOnly.getValue() || ItemUtils.isHoldingWeapon() || this.allowTools.getValue() && ItemUtils.isHoldingTool())) {
            return false;
        }
        if (!this.breakBlocks.getValue() || !this.isLookingAtBlock()) {
            return true;
        }
        WorldSettings.GameType gameType = minecraft.field_71442_b.func_178889_l();
        return gameType != WorldSettings.GameType.SURVIVAL && gameType != WorldSettings.GameType.CREATIVE;
    }

    public AutoClickerModule() {
        super("AutoClicker", false, 0);
    }

    @EventHandler
    public void onPreTick(PreTickEvent event) {
        boolean hurtTimeTooHigh;
        if (this.clickCooldownMillis > 0L) {
            this.clickCooldownMillis -= 50L;
        }
        if (this.blockHitHoldMillis > 0L) {
            this.blockHitHoldMillis -= 50L;
        }
        if (this.blockHitCycleMillis > 0L) {
            this.blockHitCycleMillis -= 50L;
        }
        if (minecraft.field_71462_r != null) {
            this.restoreAttackKey = false;
            this.restoreUseItemKey = false;
            return;
        }
        if (this.restoreAttackKey) {
            this.restoreAttackKey = false;
            InputUtils.updateKeyState(minecraft.field_71474_y.field_74312_F.func_151463_i());
        }
        if (this.restoreUseItemKey) {
            this.restoreUseItemKey = false;
            InputUtils.updateKeyState(minecraft.field_71474_y.field_74313_G.func_151463_i());
        }
        if (!(this.isEnabled() && this.canAutoClick() && minecraft.field_71474_y.field_74312_F.func_151470_d())) {
            return;
        }
        if (!minecraft.field_71439_g.func_71039_bw()) {
            while (this.clickCooldownMillis <= 0L) {
                this.restoreAttackKey = true;
                this.clickCooldownMillis += this.nextClickDelayMillis();
                InputUtils.setKeyState(minecraft.field_71474_y.field_74312_F.func_151463_i(), false);
                InputUtils.onKeyTick(minecraft.field_71474_y.field_74312_F.func_151463_i());
            }
        }
        if (!(this.blockHit.getValue() && minecraft.field_71474_y.field_74313_G.func_151470_d() && ItemUtils.isHoldingSword())) {
            return;
        }
        boolean bl = hurtTimeTooHigh = MyauClient.hurtTimeTracker.remainingHurtTicks() > this.blockHitHurtTime.getValue();
        if (this.blockHitHoldMillis <= 0L || hurtTimeTooHigh) {
            this.restoreUseItemKey = true;
            InputUtils.setKeyState(minecraft.field_71474_y.field_74313_G.func_151463_i(), false);
            if (this.blockHitCycleMillis > 0L || hurtTimeTooHigh) {
                return;
            }
        }
        if (this.blockHitHoldMillis > 0L || minecraft.field_71439_g.func_71039_bw()) {
            return;
        }
        long holdDurationMillis = this.blockHitHoldDurationMillis();
        this.blockHitHoldMillis += holdDurationMillis;
        this.blockHitCycleMillis += holdDurationMillis + 50L + this.blockHitDelayDurationMillis();
        InputUtils.onKeyTick(minecraft.field_71474_y.field_74313_G.func_151463_i());
    }

    boolean isSyntheticAttack() {
        return this.isEnabled() && this.restoreAttackKey;
    }

    @EventHandler(priority = -100)
    public void onLeftClick(LeftClickEvent event) {
        if (this.isEnabled() && !event.isCancelled() && !this.restoreAttackKey) {
            this.clickCooldownMillis += this.nextClickDelayMillis();
        }
    }

    @EventHandler(priority = -100)
    public void onRightClick(RightClickEvent event) {
        if (this.isEnabled() && !event.isCancelled() && ItemUtils.isHoldingSword() && !this.restoreUseItemKey) {
            this.blockHitHoldMillis += this.blockHitHoldDurationMillis();
        }
    }

    @Override
    public void onEnable() {
        this.clickCooldownMillis = 0L;
        this.blockHitHoldMillis = 0L;
        this.blockHitCycleMillis = 0L;
    }

    @Override
    public void onSettingChanged(String name) {
        if (this.minCps.getName().equals(name)) {
            if (this.minCps.getValue() > this.maxCps.getValue()) {
                this.maxCps.setValue(this.minCps.getValue());
            }
        } else if (this.maxCps.getName().equals(name) && this.minCps.getValue() > this.maxCps.getValue()) {
            this.minCps.setValue(this.maxCps.getValue());
        }
    }

    @Override
    public String[] getHudInfo() {
        if (Objects.equals(this.minCps.getValue(), this.maxCps.getValue())) {
            return new String[] { this.minCps.getValue().toString() };
        }
        return new String[] { String.format("%d-%d", this.minCps.getValue(), this.maxCps.getValue()) };
    }
}
