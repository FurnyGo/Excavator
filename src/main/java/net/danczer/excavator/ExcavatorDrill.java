package net.danczer.excavator;

import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;

public class ExcavatorDrill extends Item {
    private final String itemPath;
    private final MiningToolItem pickAxe;
    private final MiningToolItem shovel;

    public ExcavatorDrill(Settings settings, String itemPath, MiningToolItem pickAxe, MiningToolItem shovel) {
        super(settings);
        this.itemPath = itemPath;
        this.pickAxe = pickAxe;
        this.shovel = shovel;
    }

    public MiningToolItem getPickAxe(){
        return pickAxe;
    }
    public MiningToolItem getShovel(){
        return shovel;
    }

    public String getItemPath(){
        return itemPath;
    }
}
