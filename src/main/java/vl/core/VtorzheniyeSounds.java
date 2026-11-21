package vl.core;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import vl.Vtorzheniye;

/**
 * Все звуки мода.
 *
 * <p>Звуки — половина ужаса. Мы даём каждому событию несколько «дублей»: случайный выбор из
 * нескольких файлов делает повторы незаметными.
 */
public final class VtorzheniyeSounds {
	/** Стук: кто-то стоит за дверью и ждёт. */
	public static final SoundEvent KNOCK = register("knock");
	/** Шёпот: на языке, которого нет. */
	public static final SoundEvent WHISPER = register("whisper");
	/** Влажный хрип — очень близко, за спиной. */
	public static final SoundEvent BREATH = register("breath");
	/** Скрежет: Гость царапает стену, пока ты спишь. */
	public static final SoundEvent SCRAPE = register("scrape");
	/** Стук сердца, когда гниль добирается до груди. */
	public static final SoundEvent HEARTBEAT = register("heartbeat");
	/** Скример: короткий, режущий, без права на подготовку. */
	public static final SoundEvent SCREAM = register("scream");
	/** Стук по деревянной доске — «пуск» ритуала. */
	public static final SoundEvent RITUAL_KNOCK = register("ritual_knock");
	/** Ватный, глухой гул: гниль набирает силу. */
	public static final SoundEvent PRESSURE = register("pressure");
	/** Удар молота о металл: дверь не выдержала. */
	public static final SoundEvent DOOR_BURST = register("door_burst");

	private VtorzheniyeSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = Vtorzheniye.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void register() {
		// Инициализирует статические поля — без этого класса звуки не попадут в реестр.
	}
}
