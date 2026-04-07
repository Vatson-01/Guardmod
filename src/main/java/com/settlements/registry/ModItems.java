package com.settlements.registry;

import com.settlements.SettlementsMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SettlementsMod.MOD_ID);

    public static final RegistryObject<Item> SHOP_BLOCK_ITEM = ITEMS.register(
            "shop_block",
            () -> new BlockItem(ModBlocks.SHOP_BLOCK.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> BRONZE_COIN = ITEMS.register(
            "bronze_coin",
            () -> new Item(new Item.Properties().stacksTo(50))
    );

    public static final RegistryObject<Item> SILVER_COIN = ITEMS.register(
            "silver_coin",
            () -> new Item(new Item.Properties().stacksTo(50))
    );

    public static final RegistryObject<Item> GOLD_COIN = ITEMS.register(
            "gold_coin",
            () -> new Item(new Item.Properties().stacksTo(50))
    );

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}