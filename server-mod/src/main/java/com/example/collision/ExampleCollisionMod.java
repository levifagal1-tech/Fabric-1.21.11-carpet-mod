package com.example.collision;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleCollisionMod implements ModInitializer {
	public static final String MOD_ID = "examplecollision";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Example Collision Mod initializing");

		// Starting point for real collision/gameplay logic. Common ways to hook this
		// in without registering new content (so vanilla clients can still connect):
		//  - Mixin into an existing vanilla Block's entity-collision method, following
		//    the injection pattern in ExampleServerLifecycleMixin.
		//  - Use Fabric API's ServerTickEvents to poll entity/world state each tick.
		//  - Mixin into Blocks (or a specific Block subclass) to tweak an existing
		//    block's AbstractBlock.Settings, e.g. its collision shape or friction.
		ServerLifecycleEvents.SERVER_STARTED.register(server -> LOGGER.info("Example Collision Mod ready."));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
