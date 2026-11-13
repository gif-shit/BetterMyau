package myau;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.WorldSettings;
import org.apache.commons.lang3.RandomUtils;

public class ChestStealerModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private int stealDelayTicks;
    private int openDelayTicks;
    private boolean chestOpen;
    private boolean inventoryFullWarningShown;
    public final IntegerSetting minDelay = new IntegerSetting("min-delay", 1, 0, 20);
    public final IntegerSetting maxDelay = new IntegerSetting("max-delay", 2, 0, 20);
    public final IntegerSetting openDelay = new IntegerSetting("open-delay", 1, 0, 20);
    public final BooleanSetting autoClose = new BooleanSetting("auto-close", false);
    public final BooleanSetting nameCheck = new BooleanSetting("name-check", true);
    public final BooleanSetting skipTrash = new BooleanSetting("skip-trash", true);

    public ChestStealerModule() {
        super("ChestStealer", false, 0);
    }

    private boolean isSupportedGameMode() {
        WorldSettings.GameType gameType = minecraft.field_71442_b.func_178889_l();
        return gameType == WorldSettings.GameType.SURVIVAL || gameType == WorldSettings.GameType.ADVENTURE;
    }

    private void shiftClickSlot(int windowId, int slot) {
        minecraft.field_71442_b.func_78753_a(windowId, slot, 0, 1, (EntityPlayer) minecraft.field_71439_g);
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        String inventoryName;
        if (this.stealDelayTicks > 0) {
            --this.stealDelayTicks;
        }
        if (this.openDelayTicks > 0) {
            --this.openDelayTicks;
        }
        if (!(minecraft.field_71462_r instanceof GuiChest)) {
            this.chestOpen = false;
            return;
        }
        Container container = ((GuiChest) minecraft.field_71462_r).field_147002_h;
        if (!(container instanceof ContainerChest)) {
            this.chestOpen = false;
            return;
        }
        if (!this.chestOpen) {
            this.chestOpen = true;
            this.inventoryFullWarningShown = false;
            this.openDelayTicks = this.openDelay.getValue() + 1;
        }
        if (this.openDelayTicks > 0 || this.stealDelayTicks > 0 || !this.isEnabled() || !this.isSupportedGameMode()) {
            return;
        }
        IInventory chestInventory = ((ContainerChest) container).func_85151_d();
        if (this.nameCheck.getValue()
                && !(inventoryName = chestInventory.func_70005_c_()).isEmpty()
                && !inventoryName.equals(I18n.func_135052_a((String) "container.chest", (Object[]) new Object[0]))
                && !inventoryName.equals(I18n.func_135052_a((String) "container.chestDouble", (Object[]) new Object[0]))) {
            return;
        }
        if (minecraft.field_71439_g.field_71071_by.func_70447_i() == -1) {
            if (!this.inventoryFullWarningShown) {
                ChatUtils.addFormattedMessage(String.format("%s%s: &cYour inventory is full!&r", "&7[&bMyau&7] &r", this.getName()));
                this.inventoryFullWarningShown = true;
            }
            if (this.autoClose.getValue()) {
                minecraft.field_71439_g.func_71053_j();
            }
            return;
        }
        if (this.skipTrash.getValue() && this.stealBestUpgrade(container, chestInventory)) {
            return;
        }
        for (int slotIndex = 0; slotIndex < chestInventory.func_70302_i_(); ++slotIndex) {
            Slot slot = container.func_75139_a(slotIndex);
            if (!slot.func_75216_d())
                continue;
            ItemStack stack = slot.func_75211_c();
            if (this.skipTrash.getValue() && ItemUtils.isDiscardableItem(stack))
                continue;
            this.shiftClickSlot(container.field_75152_c, slotIndex);
            return;
        }
        if (this.autoClose.getValue()) {
            minecraft.field_71439_g.func_71053_j();
        }
    }

    private boolean stealBestUpgrade(Container container, IInventory chestInventory) {
        double currentSwordScore;
        int bestSwordSlot = -1;
        double bestSwordScore = 0.0;
        int[] bestArmorSlots = new int[] { -1, -1, -1, -1 };
        double[] bestArmorScores = new double[] { 0.0, 0.0, 0.0, 0.0 };
        int bestPickaxeSlot = -1;
        float bestPickaxeScore = 1.0f;
        int bestShovelSlot = -1;
        float bestShovelScore = 1.0f;
        int bestAxeSlot = -1;
        float bestAxeScore = 1.0f;
        for (int slotIndex = 0; slotIndex < chestInventory.func_70302_i_(); ++slotIndex) {
            Slot slot = container.func_75139_a(slotIndex);
            if (!slot.func_75216_d())
                continue;
            ItemStack stack = slot.func_75211_c();
            Item item = stack.func_77973_b();
            if (item instanceof ItemSword) {
                double score = ItemUtils.weaponScore(stack);
                if (bestSwordSlot != -1 && !(score > bestSwordScore))
                    continue;
                bestSwordSlot = slotIndex;
                bestSwordScore = score;
                continue;
            }
            if (item instanceof ItemArmor) {
                int armorType = ((ItemArmor) item).field_77881_a;
                double score = ItemUtils.armorScore(stack);
                if (bestArmorSlots[armorType] != -1 && !(score > bestArmorScores[armorType]))
                    continue;
                bestArmorSlots[armorType] = slotIndex;
                bestArmorScores[armorType] = score;
                continue;
            }
            if (item instanceof ItemPickaxe) {
                float score = ItemUtils.toolEfficiencyScore(stack);
                if (bestPickaxeSlot != -1 && !(score > bestPickaxeScore))
                    continue;
                bestPickaxeSlot = slotIndex;
                bestPickaxeScore = score;
                continue;
            }
            if (item instanceof ItemSpade) {
                float score = ItemUtils.toolEfficiencyScore(stack);
                if (bestShovelSlot != -1 && !(score > bestShovelScore))
                    continue;
                bestShovelSlot = slotIndex;
                bestShovelScore = score;
                continue;
            }
            if (!(item instanceof ItemAxe))
                continue;
            float score = ItemUtils.toolEfficiencyScore(stack);
            if (bestAxeSlot != -1 && !(score > bestAxeScore))
                continue;
            bestAxeSlot = slotIndex;
            bestAxeScore = score;
        }
        int currentSwordSlot = ItemUtils.findBestWeaponSlot(0, true);
        double d = currentSwordScore = currentSwordSlot != -1 ? ItemUtils.weaponScore(minecraft.field_71439_g.field_71071_by.func_70301_a(currentSwordSlot)) : 0.0;
        if (bestSwordSlot != -1 && bestSwordScore > currentSwordScore) {
            this.shiftClickSlot(container.field_75152_c, bestSwordSlot);
            return true;
        }
        for (int armorType = 0; armorType < 4; ++armorType) {
            double currentArmorScore;
            int currentArmorSlot = ItemUtils.findBestArmorSlot(armorType, true);
            double d2 = currentArmorScore = currentArmorSlot != -1 ? ItemUtils.armorScore(minecraft.field_71439_g.field_71071_by.func_70301_a(currentArmorSlot)) : 0.0;
            if (bestArmorSlots[armorType] == -1 || !(bestArmorScores[armorType] > currentArmorScore))
                continue;
            this.shiftClickSlot(container.field_75152_c, bestArmorSlots[armorType]);
            return true;
        }
        if (isBetterTool("pickaxe", bestPickaxeSlot, bestPickaxeScore)) {
            this.shiftClickSlot(container.field_75152_c, bestPickaxeSlot);
            return true;
        }
        if (isBetterTool("shovel", bestShovelSlot, bestShovelScore)) {
            this.shiftClickSlot(container.field_75152_c, bestShovelSlot);
            return true;
        }
        if (isBetterTool("axe", bestAxeSlot, bestAxeScore)) {
            this.shiftClickSlot(container.field_75152_c, bestAxeSlot);
            return true;
        }
        return false;
    }

    private static boolean isBetterTool(String toolClass, int chestSlot, float chestScore) {
        if (chestSlot == -1) {
            return false;
        }
        int currentSlot = ItemUtils.findBestToolSlot(toolClass, 0, true);
        float currentScore = currentSlot != -1 ? ItemUtils.toolEfficiencyScore(minecraft.field_71439_g.field_71071_by.func_70301_a(currentSlot)) : 1.0f;
        return chestScore > currentScore;
    }

    @EventHandler
    public void onWindowClick(WindowClickEvent event) {
        this.stealDelayTicks = RandomUtils.nextInt((int) (this.minDelay.getValue() + 1), (int) (this.maxDelay.getValue() + 2));
    }

    @Override
    public void onSettingChanged(String name) {
        if ("min-delay".equals(name) && this.minDelay.getValue() > this.maxDelay.getValue()) {
            this.maxDelay.setValue(this.minDelay.getValue());
        } else if ("max-delay".equals(name) && this.minDelay.getValue() > this.maxDelay.getValue()) {
            this.minDelay.setValue(this.maxDelay.getValue());
        }
    }
}
