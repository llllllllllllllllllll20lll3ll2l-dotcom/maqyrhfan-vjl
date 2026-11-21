package vl.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import vl.Vtorzheniye;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/**
 * Реестр сущностей мода.
 */
public final class VtorzheniyeEntities {
	/**
	 * Гость. Высокая, слишком худая фигура в чужой коже.
	 */
	public static final EntityType<Guest> GUEST = register(
		"guest",
		EntityType.Builder.<Guest>of(Guest::new, MobCategory.MONSTER)
			.sized(0.6F, 2.9F)
			.eyeHeight(2.55F)
			.clientTrackingRange(12)
			.updateInterval(1)
			.canSpawnFarFromPlayer()
	);

	private VtorzheniyeEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Vtorzheniye.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(GUEST, Guest.createAttributes());
	}
}
