package net.danczer.excavator;

import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.util.math.Vec3f;

public class ExcavatorDrill extends Item {
    private final Vec3f drillColor;
    private final MiningToolItem pickAxe;
    private final MiningToolItem shovel;

    public ExcavatorDrill(Settings settings, Vec3f drillColor, MiningToolItem pickAxe, MiningToolItem shovel) {
        super(settings);
        this.drillColor = drillColor;
        this.pickAxe = pickAxe;
        this.shovel = shovel;
    }

    public MiningToolItem getPickAxe(){
        return pickAxe;
    }
    public MiningToolItem getShovel(){
        return shovel;
    }

    public Vec3f getDrillColor(){
        return drillColor;
    }
}
