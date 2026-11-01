package ru.mstrexmet.nuclearbomb;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeCameraAngles;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NuclearBombMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientCinematicEffects {
    private static int ticksRemaining;
    private static int totalTicks;

    private ClientCinematicEffects() {}

    public static void trigger() {
        ticksRemaining = 34;
        totalTicks = 34;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticksRemaining > 0) ticksRemaining--;
    }

    @SubscribeEvent
    public static void onCameraAngles(ComputeCameraAngles event) {
        if (ticksRemaining <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float strength = (ticksRemaining / (float) totalTicks) * 1.35F;
        float wave = (float) Math.sin((totalTicks - ticksRemaining) * 2.4F);
        event.setYaw(event.getYaw() + wave * strength);
        event.setPitch(event.getPitch() + (float) Math.cos((totalTicks - ticksRemaining) * 1.8F) * strength * 0.65F);
        event.setRoll(event.getRoll() + wave * strength * 0.35F);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (ticksRemaining <= 0) return;
        float progress = 1.0F - ticksRemaining / (float) totalTicks;
        int alpha = (int) (Math.max(0.0F, 1.0F - progress * 3.8F) * 185.0F);
        if (alpha > 0) {
            GuiGraphics gui = event.getGuiGraphics();
            gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), (alpha << 24) | 0x00FFFFFF);
        }
    }
}
