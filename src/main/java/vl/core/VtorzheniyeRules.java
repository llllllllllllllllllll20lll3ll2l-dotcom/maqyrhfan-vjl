package vl.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRule;

import org.jetbrains.annotations.Nullable;

import vl.Vtorzheniye;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;

/**
 * Игровое правило мода.
 *
 * <p>Страх — вещь субъективная, поэтому «страшность» вынесена в правило мира:
 * {@code /gamerule vtorzheniye:intensity 0|1|2}.
 * <ul>
 *     <li>{@code 0} — мод молчит полностью;</li>
 *     <li>{@code 1} — как задумано: тихо, медленно, без скримеров на каждом шагу;</li>
 *     <li>{@code 2} — гниль набирается вдвое быстрее, события чаще.</li>
 * </ul>
 */
public final class VtorzheniyeRules {
	@Nullable
	private static GameRule<Integer> intensityRule;

	private VtorzheniyeRules() {
	}

	/**
	 * Регистрирует правило. Вызывается из {@link Vtorzheniye#onInitialize()}.
	 */
	public static void register() {
		if (intensityRule != null) {
			return;
		}

		intensityRule = GameRuleBuilder.forInteger(1)
			.range(0, 2)
			.buildAndRegister(Vtorzheniye.id("intensity"));
	}

	/**
	 * Текущая интенсивность в этом мире: 0, 1 или 2.
	 */
	public static int intensity(ServerLevel level) {
		if (intensityRule == null) {
			return 1;
		}

		return level.getGameRules().get(intensityRule);
	}

	public static boolean enabled(ServerLevel level) {
		return intensity(level) > 0;
	}
}
