package com.settlements.registry;

import com.settlements.SettlementsMod;
import com.settlements.world.block.ShopBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SettlementsMod.MOD_ID);

    public static final RegistryObject<Block> SHOP_BLOCK = BLOCKS.register(
            "shop_block",
            () -> new ShopBlock(BlockBehaviour.Properties.of().strength(2.0F, 6.0F))
    );

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}