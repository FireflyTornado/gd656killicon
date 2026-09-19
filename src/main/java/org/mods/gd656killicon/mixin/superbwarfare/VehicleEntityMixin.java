package org.mods.gd656killicon.mixin.superbwarfare;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.mods.gd656killicon.server.logic.superbwarfare.SuperbWarfareEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity", remap = false)
public abstract class VehicleEntityMixin {
    @Unique
    private float gd656killicon$healthBeforeHurt;

    @Shadow
    public abstract float getHealth();

    // 方法名同时写 dev(Mojmap: hurt) 与 prod(SRG: m_6469_) 两个名字,
    // 服务器上发布 jar 的 VehicleEntity.hurt 已被重映射为 m_6469_(remap=false 按字面匹配)
    @Inject(method = {"hurt", "m_6469_"}, at = @At("HEAD"))
    private void gd656killicon$capturePreDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        gd656killicon$healthBeforeHurt = this.getHealth();
    }

    @Inject(method = {"hurt", "m_6469_"}, at = @At("RETURN"))
    private void gd656killicon$recordActualVehicleDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if ((Object) this instanceof Entity entity) {
            SuperbWarfareEventHandler.onVehicleDamageApplied(entity, source, gd656killicon$healthBeforeHurt);
        }
    }
}
