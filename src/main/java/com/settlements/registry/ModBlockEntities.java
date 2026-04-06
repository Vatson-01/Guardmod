package com.settlements.registry;

import com.settlements.SettlementsMod;
import com.settlements.world.blockentity.ShopBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SettlementsMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ShopBlockEntity>> SHOP_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "shop_block_entity",
            () -> BlockEntityType.Builder.of(ShopBlockEntity::new, ModBlocks.SHOP_BLOCK.get()).build(null)
    );

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}