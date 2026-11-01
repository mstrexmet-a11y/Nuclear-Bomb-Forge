package ru.mstrexmet.nuclearbomb;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class NuclearDeviceItem extends Item {
    public NuclearDeviceItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 target = player.position().add(player.getLookAngle().scale(5.0));
        BlockPos origin = BlockPos.containing(target);

        if (!level.isClientSide) {
            // Sound only: the visual scene is rendered locally with custom geometry.
            level.playSound(null, origin, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 3.5F, 0.62F);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCinematicEffects.trigger(
                new Vec3(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5)));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
