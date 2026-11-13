package myau;

import java.util.Objects;

class DroppedItemGroup {

    public final int itemId;
    public final double x;
    public final double y;
    public final double z;
    private final int hashCode;

    public DroppedItemGroup(int itemId, double x, double y, double z) {
        this.itemId = itemId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hashCode = Objects.hash(itemId, (int) x, (int) y, (int) z);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }
        DroppedItemGroup group = (DroppedItemGroup) other;
        return this.itemId == group.itemId
                && (int) this.x == (int) group.x
                && (int) this.y == (int) group.y
                && (int) this.z == (int) group.z;
    }

    public int hashCode() {
        return this.hashCode;
    }
}
