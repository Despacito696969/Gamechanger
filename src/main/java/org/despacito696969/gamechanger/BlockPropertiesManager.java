package org.despacito696969.gamechanger;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.IdentityHashMap;

public class BlockPropertiesManager {
    public static IdentityHashMap<Block, BlockPropertiesMod> propMods = new IdentityHashMap<>();

    static {
        var mod = new BlockPropertiesMod();
        mod.destroyTime = Blocks.DIRT.defaultDestroyTime() * 10;
        mod.explosionResistance = Blocks.DIRT.getExplosionResistance() * 1000;
        propMods.put(Blocks.DIRT, mod);
    }
}
