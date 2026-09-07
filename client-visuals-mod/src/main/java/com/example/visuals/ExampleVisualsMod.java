package com.example.visuals;

import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleVisualsMod implements ModInitializer {
	public static final String MOD_ID = "examplevisuals";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// One entry per folder under src/main/resources/resourcepacks/ - add more packs
	// to the collection by dropping in another folder and listing its name here.
	private static final List<String> BUNDLED_PACKS = List.of("example_retexture");

	@Override
	public void onInitialize() {
		FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(container -> {
			for (String packName : BUNDLED_PACKS) {
				ResourceManagerHelper.registerBuiltinResourcePack(
					Identifier.fromNamespaceAndPath(MOD_ID, packName),
					container,
					ResourcePackActivationType.DEFAULT_ENABLED);
			}
		});

		LOGGER.info("Registered {} bundled resource pack(s)", BUNDLED_PACKS.size());
	}
}
