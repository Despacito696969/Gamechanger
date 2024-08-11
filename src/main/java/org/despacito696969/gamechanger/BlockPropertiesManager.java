package org.despacito696969.gamechanger;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.IdentityHashMap;

public class BlockPropertiesManager {
    public static IdentityHashMap<Block, BlockPropertiesMod> propMods = new IdentityHashMap<>();

    static {
        var mod = new BlockPropertiesMod();
        mod.destroyTime = Blocks.DIRT.defaultDestroyTime() * 10;
        mod.explosionResistance = Blocks.DIRT.getExplosionResistance() * 1000;
        propMods.put(Blocks.DIRT, mod);
    }

    public static BlockPropertiesMod getOrCreateProperties(Block block) {
        var props_1 = propMods.get(block);
        if (props_1 != null) {
            return props_1;
        }

        var props_2 = new BlockPropertiesMod();
        BlockPropertiesManager.propMods.put(block, props_2);
        return props_2;
    }
}
