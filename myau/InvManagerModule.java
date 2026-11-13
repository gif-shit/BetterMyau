package myau;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;
import org.apache.commons.lang3.RandomUtils;

public class InvManagerModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private int actionDelayTicks;
    private int openDelayTicks;
    private boolean inventoryOpen;
    public final IntegerSetting minDelay = new IntegerSetting("min-delay", 1, 0, 20);
    public final IntegerSetting maxDelay = new IntegerSetting("max-delay", 2, 0, 20);
    public final IntegerSetting openDelay = new IntegerSetting("open-delay", 1, 0, 20);
    public final BooleanSetting autoArmor = new BooleanSetting("auto-armor", true);
    public final BooleanSetting dropTrash = new BooleanSetting("drop-trash", false);
    public final IntegerSetting swordSlot = new IntegerSetting("sword-slot", 1, 0, 9);
    public final IntegerSetting pickaxeSlot = new IntegerSetting("pickaxe-slot", 3, 0, 9);
    public final IntegerSetting shovelSlot = new IntegerSetting("shovel-slot", 4, 0, 9);
    public final IntegerSetting axeSlot = new IntegerSetting("axe-slot", 5, 0, 9);
    public final IntegerSetting blocksSlot = new IntegerSetting("blocks-slot", 2, 0, 9);
    public final IntegerSetting blocks = new IntegerSetting("blocks", 128, 64, 2304);

    public InvManagerModule() {
        super("InvManager", false, 0);
    }

    private boolean isSupportedGameMode() {
        WorldSettings.GameType gameType = minecraft.field_71442_b.func_178889_l();
        return gameType == WorldSettings.GameType.SURVIVAL || gameType == WorldSettings.GameType.ADVENTURE;
    }

    private static int inventoryIndexToContainerSlot(int inventoryIndex) {
        if (inventoryIndex >= 36) {
            return 44 - inventoryIndex;
        }
        if (inventoryIndex > 8) {
            return inventoryIndex;
        }
        return 36 + inventoryIndex;
    }

    private void clickInventorySlot(int slot, int button, int mode) {
        minecraft.field_71442_b.func_78753_a(minecraft.field_71439_g.field_71069_bz.field_75152_c, slot, button, mode, (EntityPlayer) minecraft.field_71439_g);
    }

    private static int stackSize(int inventorySlot) {
        if (inventorySlot == -1) {
            return 0;
        }
        ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(inventorySlot);
        return stack == null ? 0 : stack.field_77994_a;
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (this.actionDelayTicks > 0) {
            --this.actionDelayTicks;
        }
        if (this.openDelayTicks > 0) {
            --this.openDelayTicks;
        }
        if (!(minecraft.field_71462_r instanceof GuiInventory)
                || !(((GuiInventory) minecraft.field_71462_r).field_147002_h instanceof ContainerPlayer)) {
            this.inventoryOpen = false;
            return;
        }
        if (!this.inventoryOpen) {
            this.inventoryOpen = true;
            this.openDelayTicks = this.openDelay.getValue() + 1;
        }
        if (this.openDelayTicks > 0 || this.actionDelayTicks > 0 || !this.isEnabled() || !this.isSupportedGameMode()) {
            return;
        }
        int[] bestArmor = new int[4];
        int[] bestArmorIncludingDamaged = new int[4];
        for (int armorType = 0; armorType < 4; ++armorType) {
            bestArmor[armorType] = ItemUtils.findBestArmorSlot(armorType, true);
            bestArmorIncludingDamaged[armorType] = ItemUtils.findBestArmorSlot(armorType, false);
        }
        int desiredSwordSlot = this.swordSlot.getValue() - 1;
        int bestSword = ItemUtils.findBestWeaponSlot(desiredSwordSlot, true);
        int bestSwordIncludingDamaged = ItemUtils.findBestWeaponSlot(desiredSwordSlot, false);
        int desiredPickaxeSlot = this.pickaxeSlot.getValue() - 1;
        int bestPickaxe = ItemUtils.findBestToolSlot("pickaxe", desiredPickaxeSlot, true);
        int bestPickaxeIncludingDamaged = ItemUtils.findBestToolSlot("pickaxe", desiredPickaxeSlot, false);
        int desiredShovelSlot = this.shovelSlot.getValue() - 1;
        int bestShovel = ItemUtils.findBestToolSlot("shovel", desiredShovelSlot, true);
        int bestShovelIncludingDamaged = ItemUtils.findBestToolSlot("shovel", desiredShovelSlot, false);
        int desiredAxeSlot = this.axeSlot.getValue() - 1;
        int bestAxe = ItemUtils.findBestToolSlot("axe", desiredAxeSlot, true);
        int bestAxeIncludingDamaged = ItemUtils.findBestToolSlot("axe", desiredAxeSlot, false);
        int desiredBlocksSlot = this.blocksSlot.getValue() - 1;
        int bestBlocks = ItemUtils.findLargestBlockStackSlot(desiredBlocksSlot);
        if (this.autoArmor.getValue() && this.equipBestArmor(bestArmor, bestArmorIncludingDamaged)) {
            return;
        }
        HashSet<Integer> reservedHotbarSlots = new HashSet<Integer>();
        if (this.moveBestItemToHotbar(desiredSwordSlot, bestSword, bestSwordIncludingDamaged, reservedHotbarSlots)) {
            return;
        }
        if (this.moveBestItemToHotbar(desiredPickaxeSlot, bestPickaxe, bestPickaxeIncludingDamaged, reservedHotbarSlots)) {
            return;
        }
        if (this.moveBestItemToHotbar(desiredShovelSlot, bestShovel, bestShovelIncludingDamaged, reservedHotbarSlots)) {
            return;
        }
        if (this.moveBestItemToHotbar(desiredAxeSlot, bestAxe, bestAxeIncludingDamaged, reservedHotbarSlots)) {
            return;
        }
        if (this.moveBestItemToHotbar(desiredBlocksSlot, bestBlocks, -1, reservedHotbarSlots)) {
            return;
        }
        if (!this.dropTrash.getValue()) {
            return;
        }
        HashSet<Integer> protectedSlots = new HashSet<Integer>();
        addSlots(protectedSlots, bestArmor);
        addSlots(protectedSlots, bestArmorIncludingDamaged);
        addSlot(protectedSlots, bestSword);
        addSlot(protectedSlots, bestSwordIncludingDamaged);
        addSlot(protectedSlots, bestPickaxe);
        addSlot(protectedSlots, bestPickaxeIncludingDamaged);
        addSlot(protectedSlots, bestShovel);
        addSlot(protectedSlots, bestShovelIncludingDamaged);
        addSlot(protectedSlots, bestAxe);
        addSlot(protectedSlots, bestAxeIncludingDamaged);
        addSlot(protectedSlots, bestBlocks);
        int retainedBlockCount = stackSize(bestBlocks);
        for (int inventorySlot = 0; inventorySlot < 36; ++inventorySlot) {
            ItemStack stack;
            if (protectedSlots.contains(inventorySlot)
                    || (stack = minecraft.field_71439_g.field_71071_by.func_70301_a(inventorySlot)) == null)
                continue;
            boolean placeableBlocks = ItemUtils.isPlaceableBlockStack(stack);
            if (ItemUtils.isDiscardableItem(stack) || placeableBlocks && retainedBlockCount >= this.blocks.getValue()) {
                this.clickInventorySlot(inventoryIndexToContainerSlot(inventorySlot), 1, 4);
                return;
            }
            if (!placeableBlocks)
                continue;
            retainedBlockCount += stack.field_77994_a;
        }
    }

    private boolean equipBestArmor(int[] bestArmor, int[] bestArmorIncludingDamaged) {
        for (int armorType = 0; armorType < 4; ++armorType) {
            int equippedInventorySlot;
            int preferred = bestArmor[armorType];
            int fallback = bestArmorIncludingDamaged[armorType];
            if (preferred == -1
                    && fallback == -1
                    || preferred == (equippedInventorySlot = 39 - armorType)
                    || fallback == equippedInventorySlot)
                continue;
            if (minecraft.field_71439_g.field_71071_by.func_70301_a(equippedInventorySlot) != null) {
                if (minecraft.field_71439_g.field_71071_by.func_70447_i() != -1) {
                    this.clickInventorySlot(inventoryIndexToContainerSlot(equippedInventorySlot), 0, 1);
                } else {
                    this.clickInventorySlot(inventoryIndexToContainerSlot(equippedInventorySlot), 1, 4);
                }
            } else {
                int source = preferred != -1 ? preferred : fallback;
                this.clickInventorySlot(inventoryIndexToContainerSlot(source), 0, 1);
            }
            return true;
        }
        return false;
    }

    private boolean moveBestItemToHotbar(int desiredHotbarSlot, int preferredSource, int fallbackSource, Set<Integer> reservedHotbarSlots) {
        if (desiredHotbarSlot < 0
                || desiredHotbarSlot > 8
                || reservedHotbarSlots.contains(desiredHotbarSlot)
                || preferredSource == -1
                && fallbackSource == -1) {
            return false;
        }
        reservedHotbarSlots.add(desiredHotbarSlot);
        if (preferredSource == desiredHotbarSlot || fallbackSource == desiredHotbarSlot) {
            return false;
        }
        int source = preferredSource != -1 ? preferredSource : fallbackSource;
        this.clickInventorySlot(inventoryIndexToContainerSlot(source), desiredHotbarSlot, 2);
        return true;
    }

    private static void addSlots(Set<Integer> slots, int[] values) {
        for (int slot : values) {
            addSlot(slots, slot);
        }
    }

    private static void addSlot(Set<Integer> slots, int slot) {
        if (slot >= 0) {
            slots.add(slot);
        }
    }

    @EventHandler
    public void onWindowClick(WindowClickEvent event) {
        this.actionDelayTicks = RandomUtils.nextInt((int) (this.minDelay.getValue() + 1), (int) (this.maxDelay.getValue() + 2));
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
