package myau;

import myau.BooleanSetting;
import myau.EntityUtils;
import myau.ItemUtils;
import myau.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class GhostHandModule
extends Module {
    public final BooleanSetting teamOnly = new BooleanSetting("team-only", true);
    public final BooleanSetting ignoreWeapons = new BooleanSetting("ignore-weapons", false);

    public GhostHandModule() {
        super("GhostHand", false, 0);
    }

    public boolean shouldIgnoreEntity(Entity entity) {
        boolean player = entity instanceof EntityPlayer;
        if (!player) {
            return false;
        }
        if (EntityUtils.isBot((EntityPlayer)entity)) {
            return false;
        }
        if (((Boolean)this.teamOnly.getValue()).booleanValue() && !EntityUtils.isTeammate((EntityPlayer)entity)) {
            return false;
        }
        return (Boolean)this.ignoreWeapons.getValue() == false || !ItemUtils.isHoldingWeapon();
    }
}
