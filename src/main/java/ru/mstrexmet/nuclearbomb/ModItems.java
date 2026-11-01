package ru.mstrexmet.nuclearbomb;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, NuclearBombMod.MOD_ID);

    public static final RegistryObject<Item> NUCLEAR_DEVICE = ITEMS.register(
            "nuclear_device",
            () -> new NuclearDeviceItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {}
}
