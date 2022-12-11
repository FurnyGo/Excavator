package net.danczer.excavator;

import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;

public class ExcavatorEntitySpawnS2CPacket extends EntitySpawnS2CPacket {
    private final boolean isForwardFacing;

    public ExcavatorEntitySpawnS2CPacket(ExcavatorMinecartEntity entity) {
        super(entity);

        isForwardFacing = entity.isForwardFacing();
    }

    public boolean isForwardFacing() {
        return this.isForwardFacing;
    }
}
