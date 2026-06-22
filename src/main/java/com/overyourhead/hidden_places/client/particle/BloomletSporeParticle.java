package com.overyourhead.hidden_places.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class BloomletSporeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected BloomletSporeParticle(ClientLevel level, double x, double y, double z,
                                    double xSpeed, double ySpeed, double zSpeed,
                                    SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.friction = 0.92F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.xd = xSpeed + (this.random.nextDouble() - 0.5D) * 0.01D;
        this.yd = ySpeed + (this.random.nextDouble() - 0.5D) * 0.01D;
        this.zd = zSpeed + (this.random.nextDouble() - 0.5D) * 0.01D;
        this.lifetime = 12 + this.random.nextInt(8);
        this.quadSize *= 0.7F + this.random.nextFloat() * 0.55F;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 0.92F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }

        this.setSpriteFromAge(this.sprites);

        int fadeStart = this.lifetime / 2;
        if (this.age >= fadeStart) {
            float progress = (float) (this.age - fadeStart) / (float) Math.max(1, this.lifetime - fadeStart);
            this.alpha = Math.max(0.0F, 0.92F * (1.0F - progress));
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                 double xSpeed, double ySpeed, double zSpeed) {
            return new BloomletSporeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
