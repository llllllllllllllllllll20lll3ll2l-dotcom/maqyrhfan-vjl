package vl.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import vl.entity.Guest;
import vl.entity.VtorzheniyeEntities;
import vl.network.FleshPulsePayload;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Гниль — серверный «таймер вторжения» на каждого игрока.
 *
 * <p>Мод не начинается с громкого рыка. Он начинается с того, что мир становится
 * <em>чуть-чуть неправильным</em>: звук за спиной, которого нет, стук в дверь в полночь,
 * и чувство, что кто-то стоит в углу и ждёт, когда ты отвернёшься.
 *
 * <p>Уровень гнили растёт от времени в темноте и от бессонницы. На высоких уровнях
 * Гость перестаёт прятаться, а мир начинает гнить ({@link WorldRot}).
 */
public final class FleshNetwork {
	/** Состояние гнили для одного игрока. */
	private static final Map<UUID, Integer> CORRUPTION = new HashMap<>();

	/** Максимальный уровень. */
	public static final int MAX = 100;

	private static final int MINUTES_BETWEEN_EVENTS = 3;
	private static final int TICKS_PER_MINUTE = 20 * 60;
	/** Как часто (в тиках) начисляется гниль. */
	private static final int ACCRUE_EVERY = 40;

	private static int globalTimer;

	private FleshNetwork() {
	}

	public static void init() {
		FleshPulsePayload.register();
		WorldRot.init();
		ServerTickEvents.END_SERVER_TICK.register(FleshNetwork::tickServer);

		// Ушёл из мира — гниль сбрасывается: каждый новый заход начинается с чистой кожи.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			CORRUPTION.remove(handler.getPlayer().getUUID());
		});
	}

	/**
	 * Текущий уровень гнили игрока: 0..{@link #MAX}.
	 */
	public static int corruptionOf(ServerPlayer player) {
		return CORRUPTION.getOrDefault(player.getUUID(), 0);
	}

	public static void addCorruption(ServerPlayer player, int amount) {
		int updated = Math.min(corruptionOf(player) + amount, MAX);
		CORRUPTION.put(player.getUUID(), updated);

		if (updated >= 20) {
			FleshPulsePayload.send(player, updated);
		}
	}

	public static void clearCorruption(ServerPlayer player) {
		CORRUPTION.remove(player.getUUID());
		FleshPulsePayload.send(player, 0);
	}

	private static void tickServer(MinecraftServer server) {
		globalTimer++;

		boolean accrueTick = globalTimer % ACCRUE_EVERY == 0;
		boolean eventTick = globalTimer % (TICKS_PER_MINUTE * MINUTES_BETWEEN_EVENTS) == 0;
		boolean guestTick = globalTimer % TICKS_PER_MINUTE == 0;

		if (!accrueTick && !eventTick && !guestTick) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSpectator() || player.isCreative()) {
				continue;
			}

			if (!(player.level() instanceof ServerLevel level)) {
				continue;
			}

			if (accrueTick) {
				accrue(player, level);
			}

			int corruption = corruptionOf(player);

			// Клиент рисует «мясо» по краям экрана — сила берётся отсюда.
			FleshPulsePayload.send(player, corruption);

			if (VtorzheniyeRules.enabled(level)) {
				WorldRot.tick(level, player, corruption);
			}

			boolean eventWindow = eventTick
				|| (VtorzheniyeRules.intensity(level) > 1 && globalTimer % (TICKS_PER_MINUTE * 2) == 0);

			if (eventWindow && corruption >= 15) {
				if (player.getRandom().nextFloat() < corruption / (float) MAX) {
					playQuietEvent(level, player, corruption);
				}
			}

			if (guestTick && corruption >= MAX) {
				spawnGuest(level, player);
			}
		}
	}

	/**
	 * Начисление гнили. Быстро — в темноте, медленно — всегда.
	 */
	private static void accrue(ServerPlayer player, ServerLevel level) {
		if (!VtorzheniyeRules.enabled(level)) {
			return;
		}

		BlockPos pos = player.blockPosition();
		boolean dark = level.getMaxLocalRawBrightness(pos) < 6;
		boolean night = Math.floorMod(level.getOverworldClockTime(), 24000L) > 13000L;
		boolean underground = pos.getY() < 45;

		int amount = 1;
		if (dark) {
			amount += 2;
		}
		if (underground) {
			amount += 1;
		}
		if (night) {
			amount += 1;
		}
		// Свет — единственная настоящая защита.
		if (level.getMaxLocalRawBrightness(pos) >= 12) {
			amount = 0;
		}

		// Сон отматывает гниль назад.
		if (player.isSleeping()) {
			amount = -3;
		}

		if (amount > 0) {
			addCorruption(player, amount * VtorzheniyeRules.intensity(level));
		} else if (amount < 0) {
			int updated = Math.max(0, corruptionOf(player) + amount);
			CORRUPTION.put(player.getUUID(), updated);
			FleshPulsePayload.send(player, updated);
		}
	}

	/**
	 * Одно из «тихих» событий. Ни одно из них не наносит урон — весь урон в голове.
	 */
	private static void playQuietEvent(ServerLevel level, ServerPlayer player, int corruption) {
		int roll = player.getRandom().nextInt(5);

		switch (roll) {
			case 0 -> {
				// Стук у двери. Никого там нет. Пока.
				Vec3 behind = player.position().subtract(player.getLookAngle().normalize().scale(3.0D));
				level.playSound(null, behind.x, behind.y, behind.z, VtorzheniyeSounds.KNOCK, SoundSource.HOSTILE, 1.0F, 0.8F);
			}
			case 1 -> {
				// Шёпот прямо в затылок.
				level.playSound(null, player.blockPosition(), VtorzheniyeSounds.WHISPER, SoundSource.HOSTILE, 0.9F,
					0.55F + player.getRandom().nextFloat() * 0.2F);
			}
			case 2 -> {
				// Гниль дошла до груди: сердце начинает стучать вслух.
				level.playSound(null, player.blockPosition(), VtorzheniyeSounds.HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.0F);
				int duration = 200 + corruption * 4;
				player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, duration, 0));
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 1));
			}
			case 3 -> {
				// Кто-то царапает стену — прямо у твоего уха.
				level.playSound(null, player.blockPosition(), VtorzheniyeSounds.SCRAPE, SoundSource.HOSTILE, 1.1F, 0.6F);
				level.sendParticles(
					ParticleTypes.SCULK_CHARGE_POP,
					player.getX(),
					player.getEyeY(),
					player.getZ(),
					12,
					0.35D,
					0.25D,
					0.35D,
					0.01D
				);
			}
			default -> {
				// Гул. Что-то идёт мимо. Или сквозь.
				level.playSound(null, player.blockPosition(), VtorzheniyeSounds.PRESSURE, SoundSource.AMBIENT, 0.9F, 0.5F);
			}
		}
	}

	/**
	 * Появление Гостя: далеко, вне поля зрения, за спиной.
	 */
	private static void spawnGuest(ServerLevel level, ServerPlayer player) {
		if (!level.getEntitiesOfClass(Guest.class, player.getBoundingBox().inflate(64.0D)).isEmpty()) {
			return;
		}

		Vec3 look = player.getLookAngle().normalize();
		Vec3 spot = player.position().subtract(look.scale(18.0D));

		Guest guest = VtorzheniyeEntities.GUEST.create(level, EntitySpawnReason.EVENT);
		if (guest == null) {
			return;
		}

		for (int attempt = 0; attempt < 8; attempt++) {
			double x = spot.x + (player.getRandom().nextDouble() - 0.5D) * 10.0D;
			double z = spot.z + (player.getRandom().nextDouble() - 0.5D) * 10.0D;
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));

			guest.snapTo(x, y, z, player.getYRot() + 180.0F, 0.0F);

			// Место может быть занято или внутри стены — тогда пробуем следующую точку.
			if (level.noCollision(guest)) {
				break;
			}
		}

		if (level.addFreshEntity(guest)) {
			level.playSound(null, player.blockPosition(), VtorzheniyeSounds.PRESSURE, SoundSource.AMBIENT, 1.0F, 0.35F);
		}
	}
}
