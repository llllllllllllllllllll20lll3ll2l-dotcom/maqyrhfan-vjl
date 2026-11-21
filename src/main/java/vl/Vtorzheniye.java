package vl;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vl.core.FleshNetwork;
import vl.core.VtorzheniyeRules;
import vl.core.VtorzheniyeSounds;
import vl.entity.VtorzheniyeEntities;

/**
 * Точка входа мода.
 *
 * <p>«Вторжение» — телесный хоррор без нового измерения: всё происходит в обычном мире,
 * средь бела дня, пока игрок занимается своими делами.
 */
public final class Vtorzheniye implements ModInitializer {
	public static final String MOD_ID = "vtorzheniye";
	public static final Logger LOGGER = LoggerFactory.getLogger("Вторжение");

	@Override
	public void onInitialize() {
		// Правило мира: регистрируется до первого тика уровней.
		VtorzheniyeRules.register();
		VtorzheniyeEntities.register();
		VtorzheniyeSounds.register();
		FleshNetwork.init();

		LOGGER.info("Вторжение: мир начал чесаться под кожей.");
	}

	/**
	 * Идентификатор ресурса в пространстве имён мода.
	 */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
