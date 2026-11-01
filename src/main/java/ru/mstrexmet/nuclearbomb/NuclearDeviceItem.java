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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class NuclearDeviceItem extends Item {
    public NuclearDeviceItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            Vec3 p = player.position().add(player.getLookAngle().scale(5.0));
            BlockPos origin = BlockPos.containing(p.x, p.y, p.z);
            createCinematicEffect((ServerLevel) level, origin);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientCinematicEffects::trigger);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void createCinematicEffect(ServerLevel level, BlockPos origin) {
        double x = origin.getX() + 0.5, y = origin.getY() + 0.5, z = origin.getZ() + 0.5;
        level.sendParticles(ParticleTypes.FLASH, x, y + 1, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 1, z, 1, 0, 0, 0, 0);
        level.playSound(null, origin, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, 0.55F);

        // Animated-looking radial dust halo; purely cosmetic particles.
        for (int i = 0; i < 112; i++) {
            double a = Math.PI * 2.0 * i / 112.0;
            double dx = Math.cos(a), dz = Math.sin(a);
            level.sendParticles(ParticleTypes.CLOUD, x + dx * 1.4, y + 0.18, z + dz * 1.4,
                    1, dx * 0.72, 0.06, dz * 0.72, 0.08);
            if (i % 2 == 0) level.sendParticles(ParticleTypes.POOF,
                    x + dx * 1.1, y + 0.35, z + dz * 1.1, 1,
                    dx * 0.5, 0.04, dz * 0.5, 0.04);
        }

        // Layered particle shells create a stylized volumetric cloud.
        for (int layer = 0; layer < 18; layer++) {
            double height = 1.4 + layer * 0.82;
            double radius = layer < 8 ? 0.9 + layer * 0.12
                    : 2.0 + Math.sin((layer - 7) / 10.0 * Math.PI) * 3.0;
            int points = layer < 8 ? 22 : 44;
            for (int i = 0; i < points; i++) {
                double a = Math.PI * 2.0 * i / points;
                double wobble = 0.84 + ((i * 17 + layer * 11) % 13) * 0.025;
                double px = x + Math.cos(a) * radius * wobble;
                double pz = z + Math.sin(a) * radius * wobble;
                level.sendParticles(layer % 4 == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.LARGE_SMOKE,
                        px, y + height, pz, 1, 0.12, 0.32, 0.12, 0.012);
                if (layer >= 8) level.sendParticles(ParticleTypes.CLOUD, px, y + height + 0.18, pz,
                        1, 0.16, 0.12, 0.16, 0.006);
            }
        }
        for (int i = 0; i < 48; i++) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1.5 + i * 0.34, z,
                    2, 0.42, 0.22, 0.42, 0.008);
        }
    }
}
