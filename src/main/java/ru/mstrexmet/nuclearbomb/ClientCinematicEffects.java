package ru.mstrexmet.nuclearbomb;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Client-side procedural cinematic effect. All geometry is generated locally;
 * it does not use Minecraft's stock smoke/explosion particle sprites.
 */
@Mod.EventBusSubscriber(modid = NuclearBombMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientCinematicEffects {
    private static int ticksRemaining;
    private static int totalTicks;
    private static Vec3 origin = Vec3.ZERO;

    private ClientCinematicEffects() {}

    public static void trigger(Vec3 position) {
        origin = position;
        ticksRemaining = 240;
        totalTicks = 240;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticksRemaining > 0) ticksRemaining--;
    }

    @SubscribeEvent
    public static void onCameraAngles(ComputeCameraAngles event) {
        if (ticksRemaining <= 0) return;
        float age = totalTicks - ticksRemaining;
        float impact = (float)Math.exp(-age / 18.0);
        float rumble = (float)Math.sin(age * 1.75F) * impact;
        event.setYaw(event.getYaw() + rumble * 1.25F);
        event.setPitch(event.getPitch() + (float)Math.cos(age * 1.31F) * impact * 0.8F);
        event.setRoll(event.getRoll() + rumble * 0.45F);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (ticksRemaining <= 0) return;
        float age = totalTicks - ticksRemaining;
        float flash = Math.max(0.0F, 1.0F - age / 9.0F);
        int alpha = (int)(flash * flash * 235.0F);
        if (alpha > 0) {
            GuiGraphics gui = event.getGuiGraphics();
            gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), (alpha << 24) | 0x00FFF1D6);
        }
    }

    @SubscribeEvent
    public static void renderWorld(RenderLevelStageEvent event) {
        if (ticksRemaining <= 0 || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float partial = event.getPartialTick();
        float age = (totalTicks - ticksRemaining + partial) / 20.0F;
        float growth = smooth(Math.min(1.0F, age / 4.5F));
        float fade = Math.min(1.0F, ticksRemaining / 55.0F);
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = pose.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        double x = origin.x, y = origin.y, z = origin.z;
        // A soft, expanding ground-hugging torus: custom triangle geometry.
        float ringRadius = 1.0F + growth * 13.0F;
        float ringY = 0.18F + growth * 0.12F;
        float ringAlpha = (float)(Math.sin(Math.min(1.0, age / 3.2) * Math.PI) * 0.36 * fade);
        if (ringAlpha > 0.002F) torus(buffer, matrix, x, y + ringY, z, ringRadius, 0.12F + growth * 0.2F, ringAlpha, 0.82F, 0.72F, 0.58F);

        // Rising stem, made from stacked, subtly wobbling volumetric rings.
        float stemHeight = 2.0F + growth * 18.0F;
        for (int i = 0; i < 18; i++) {
            float t = i / 17.0F;
            float h = 0.5F + t * stemHeight;
            float radius = (0.7F + 0.55F * t) * (0.72F + 0.28F * growth);
            float wobble = (float)Math.sin(age * 1.4 + t * 7.0) * (0.12F + t * 0.2F);
            float a = (0.27F + 0.12F * (float)Math.sin(t * Math.PI)) * fade;
            if (age < 0.45F) a *= age / 0.45F;
            ellipsoid(buffer, matrix, x + wobble, y + h, z - wobble * 0.45, radius, 0.72F + t * 0.22F, radius, a, 0.24F, 0.23F, 0.22F, 18, 10);
        }

        // Mushroom cap: overlapping custom mesh lobes create a scalloped,
        // volumetric crown rather than a flat particle billboard.
        float capY = y + 0.55F + stemHeight;
        float capRadius = 2.1F + growth * 7.2F;
        int lobes = 13;
        for (int i = 0; i < lobes; i++) {
            double angle = Math.PI * 2.0 * i / lobes + age * 0.055;
            float uneven = 0.82F + 0.18F * (float)Math.sin(i * 12.31 + 0.7);
            double lx = x + Math.cos(angle) * capRadius * 0.66F * uneven;
            double lz = z + Math.sin(angle) * capRadius * 0.66F * uneven;
            float pulse = 1.0F + 0.035F * (float)Math.sin(age * 2.0 + i);
            float size = capRadius * (0.43F + (i % 3) * 0.035F) * pulse;
            float alpha = (0.22F + 0.08F * (float)Math.sin(i * 3.1)) * fade;
            ellipsoid(buffer, matrix, lx, capY + (float)Math.sin(angle * 2.0) * 0.35F, lz,
                    size, size * 0.48F, size, alpha, 0.31F, 0.30F, 0.29F, 20, 12);
        }
        // Bright, warm inner billows under the crown.
        for (int i = 0; i < 7; i++) {
            double a = i * Math.PI * 2.0 / 7.0 - age * 0.08;
            double bx = x + Math.cos(a) * capRadius * 0.28;
            double bz = z + Math.sin(a) * capRadius * 0.28;
            ellipsoid(buffer, matrix, bx, capY - 0.75, bz, capRadius * 0.32F, 1.0F, capRadius * 0.32F,
                    0.12F * fade, 0.66F, 0.43F, 0.25F, 18, 10);
        }

        // Long, thin atmospheric wisps drifting outward from the cap.
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2.0 / 24.0 + age * 0.025;
            float r = capRadius * (0.82F + 0.13F * (float)Math.sin(age + i));
            double wx = x + Math.cos(a) * r;
            double wz = z + Math.sin(a) * r;
            ellipsoid(buffer, matrix, wx, capY + 0.4 + Math.sin(age * 0.7 + i) * 0.35, wz,
                    0.75F + growth, 0.35F, 0.75F + growth, 0.075F * fade,
                    0.42F, 0.40F, 0.37F, 12, 8);
        }

        BufferUploader.drawWithShader(buffer.end());
        pose.popPose();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static float smooth(float v) { return v * v * (3.0F - 2.0F * v); }

    private static void torus(BufferBuilder b, Matrix4f m, double x, double y, double z,
                              float major, float minor, float alpha, float r, float g, float bl) {
        int around = 96, sides = 10;
        for (int i = 0; i < around; i++) {
            double a0 = i * Math.PI * 2.0 / around, a1 = (i + 1) * Math.PI * 2.0 / around;
            for (int j = 0; j < sides; j++) {
                double s0 = j * Math.PI * 2.0 / sides, s1 = (j + 1) * Math.PI * 2.0 / sides;
                vertex(b,m,x+(major+minor*Math.cos(s0))*Math.cos(a0),y+minor*Math.sin(s0),z+(major+minor*Math.cos(s0))*Math.sin(a0),r,g,bl,alpha);
                vertex(b,m,x+(major+minor*Math.cos(s0))*Math.cos(a1),y+minor*Math.sin(s0),z+(major+minor*Math.cos(s0))*Math.sin(a1),r,g,bl,alpha);
                vertex(b,m,x+(major+minor*Math.cos(s1))*Math.cos(a1),y+minor*Math.sin(s1),z+(major+minor*Math.cos(s1))*Math.sin(a1),r,g,bl,alpha);
                vertex(b,m,x+(major+minor*Math.cos(s0))*Math.cos(a0),y+minor*Math.sin(s0),z+(major+minor*Math.cos(s0))*Math.sin(a0),r,g,bl,alpha);
                vertex(b,m,x+(major+minor*Math.cos(s1))*Math.cos(a1),y+minor*Math.sin(s1),z+(major+minor*Math.cos(s1))*Math.sin(a1),r,g,bl,alpha);
                vertex(b,m,x+(major+minor*Math.cos(s1))*Math.cos(a0),y+minor*Math.sin(s1),z+(major+minor*Math.cos(s1))*Math.sin(a0),r,g,bl,alpha);
            }
        }
    }

    private static void ellipsoid(BufferBuilder b, Matrix4f m, double x, double y, double z,
                                  float rx, float ry, float rz, float alpha, float r, float g, float bl,
                                  int slices, int stacks) {
        for (int i = 0; i < stacks; i++) {
            double p0 = -Math.PI/2 + Math.PI*i/stacks, p1 = -Math.PI/2 + Math.PI*(i+1)/stacks;
            for (int j = 0; j < slices; j++) {
                double t0 = Math.PI*2*j/slices, t1 = Math.PI*2*(j+1)/slices;
                point(b,m,x,y,z,rx,ry,rz,p0,t0,r,g,bl,alpha);
                point(b,m,x,y,z,rx,ry,rz,p1,t0,r,g,bl,alpha);
                point(b,m,x,y,z,rx,ry,rz,p1,t1,r,g,bl,alpha);
                point(b,m,x,y,z,rx,ry,rz,p0,t0,r,g,bl,alpha);
                point(b,m,x,y,z,rx,ry,rz,p1,t1,r,g,bl,alpha);
                point(b,m,x,y,z,rx,ry,rz,p0,t1,r,g,bl,alpha);
            }
        }
    }

    private static void point(BufferBuilder b, Matrix4f m, double x, double y, double z,
                              float rx,float ry,float rz,double p,double t,float r,float g,float bl,float a) {
        double cp=Math.cos(p);
        vertex(b,m,x+rx*cp*Math.cos(t),y+ry*Math.sin(p),z+rz*cp*Math.sin(t),r,g,bl,a);
    }

    private static void vertex(BufferBuilder b, Matrix4f m, double x, double y, double z,
                               float r,float g,float bl,float a) {
        b.vertex(m,(float)x,(float)y,(float)z).color(r,g,bl,a).endVertex();
    }
}
