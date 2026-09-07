package com.example.collision.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Template injection pattern - retarget the @Mixin class/method to whatever vanilla
// hook your real collision logic needs (e.g. a specific Block's entity-collision method).
@Mixin(MinecraftServer.class)
public class ExampleServerLifecycleMixin {
	@Inject(at = @At("HEAD"), method = "loadLevel")
	private void onLoadLevel(CallbackInfo info) {
	}
}
