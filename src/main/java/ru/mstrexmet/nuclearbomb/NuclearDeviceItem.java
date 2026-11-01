package ru.mstrexmet.nuclearbomb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class NuclearDeviceItem extends Item {
    public NuclearDeviceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            Vec3 p = player.position().add(player.getLookAngle().scale(5.0));
            BlockPos origin = BlockPos.containing(p.x, p.y, p.z);
            createCinematicEffect(level, origin);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void createCinematicEffect(Level level, BlockPos origin) {
        double x = origin.getX() + 0.5;
        double y = origin.getY() + 0.5;
        double z = origin.getZ() + 0.5;

        // A bright visual flash and expanding ground-level dust ring.
        level.sendParticles(ParticleTypes.FLASH, x, y + 1, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 1, z, 1, 0, 0, 0, 0);
        level.playSound(null, origin, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, 0.55F);

        for (int i = 0; i < 72; i++) {
            double a = (Math.PI * 2.0 * i) / 72.0;
            double r = 2.0 + (i % 5) * 0.35;
            level.sendParticles(ParticleTypes.CLOUD,
                    x + Math.cos(a) * r, y + 0.15, z + Math.sin(a) * r,
                    1, 0.12, 0.08, 0.12, 0.02);
        }

        // Rising stem and layered cap create a stylized mushroom-cloud silhouette.
        for (int layer = 0; layer < 13; layer++) {
            double height = 1.5 + layer * 1.25;
            double radius = layer < 7 ? 1.4 + layer * 0.22 : 3.0 + (layer - 7) * 0.72;
            int points = layer < 7 ? 16 : 28;
            for (int i = 0; i < points; i++) {
                double a = (Math.PI * 2.0 * i) / points;
                double wobble = 0.82 + ((i * 13 + layer * 7) % 9) * 0.045;
                double px = x + Math.cos(a) * radius * wobble;
                double pz = z + Math.sin(a) * radius * wobble;
                level.sendParticles(layer % 3 == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.LARGE_SMOKE,
                        px, y + height, pz, 1, 0.18, 0.45, 0.18, 0.015);
                if (layer >= 7) {
                    level.sendParticles(ParticleTypes.CLOUD, px, y + height + 0.25, pz,
                            1, 0.3, 0.25, 0.3, 0.01);
                }
            }
        }

        // Tall central smoke column.
        for (int i = 0; i < 35; i++) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 2 + i * 0.48, z,
                    2, 0.65, 0.35, 0.65, 0.01);
        }
    }
}
