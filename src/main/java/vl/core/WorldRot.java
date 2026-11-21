package vl.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import vl.entity.Guest;
import vl.entity.VtorzheniyeEntities;

/**
 * «Порча мира» — вторая половина вторжения.
 *
 * <p>Гость — это видимая часть. Но настоящий ужас в том, что мир вокруг
 * <em>портится сам</em>: гаснут факелы, на стенах появляются прожилки скулка,
 * а сон перестаёт быть безопасным.
 */
public final class WorldRot {
	/** Сколько тиков между шагами порчи. */
	private static final int ROT_INTERVAL = 20 * 45;

	private WorldRot() {
	}

	public static void init() {
		// Регистрация не нужна: порчу дёргает FleshNetwork через tick().
	}

	public static void tick(ServerLevel level, ServerPlayer player, int corruption) {
		if (level.getGameTime() % ROT_INTERVAL != 0) {
			return;
		}

		// В Нижнем мире и Энде и так всё плохо: портим только Верхний мир.
		if (!level.dimension().equals(Level.OVERWORLD)) {
			return;
		}

		int radius = 4 + corruption / 12;

		// 1. Огонь не хочет гореть рядом с тобой.
		if (corruption > 25) {
			snuffLights(level, player, radius);
		}

		// 2. Стены начинают «дышать» скулком.
		if (corruption > 45) {
			creepSculk(level, player, radius);
		}

		// 3. Он приходит, пока ты спишь.
		if (corruption > 60 && player.isSleeping()) {
			wakeWithGuest(level, player);
		}
	}

	/**
	 * Гасит факелы и фонари вокруг игрока, оставляя угли и дым.
	 */
	private static void snuffLights(ServerLevel level, ServerPlayer player, int radius) {
		BlockPos origin = player.blockPosition();
		int snuffed = 0;

		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -2, -radius), origin.offset(radius, 2, radius))) {
			BlockState state = level.getBlockState(pos);
			Block block = state.getBlock();

			if (block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH) {
				level.removeBlock(pos, false);
				level.playSound(null, pos, VtorzheniyeSounds.SCRAPE, SoundSource.BLOCKS, 0.6F, 1.4F);
				snuffed++;
			} else if (block == Blocks.LANTERN || block == Blocks.SOUL_LANTERN) {
				if (state.hasProperty(BlockStateProperties.HANGING)) {
					level.setBlockAndUpdate(pos, Blocks.IRON_CHAIN.defaultBlockState()
						.setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
				} else {
					level.removeBlock(pos, false);
				}
				snuffed++;
			} else if (block == Blocks.CAMPFIRE || block == Blocks.SOUL_CAMPFIRE) {
				BlockState unlit = state.setValue(BlockStateProperties.LIT, false);
				level.setBlockAndUpdate(pos, unlit);
				snuffed++;
			}

			if (snuffed >= 6) {
				return;
			}
		}
	}

	/**
	 * На горизонтальных поверхностях рядом появляются прожилки скулка.
	 */
	private static void creepSculk(ServerLevel level, ServerPlayer player, int radius) {
		BlockPos origin = player.blockPosition();
		int placed = 0;

		for (int attempt = 0; attempt < 24 && placed < 3; attempt++) {
			BlockPos pos = origin.offset(
				player.getRandom().nextInt(radius * 2 + 1) - radius,
				player.getRandom().nextInt(5) - 2,
				player.getRandom().nextInt(radius * 2 + 1) - radius
			);

			BlockPos ground = pos.below();
			BlockState groundState = level.getBlockState(ground);

			if (!groundState.isFaceSturdy(level, ground, Direction.UP) || !level.getBlockState(pos).isAir()) {
				continue;
			}

			level.setBlockAndUpdate(pos, Blocks.SCULK_VEIN.defaultBlockState());
			placed++;
		}
	}

	/**
	 * Сон прерывается: Гость уже стоит у кровати.
	 */
	private static void wakeWithGuest(ServerLevel level, ServerPlayer player) {
		player.stopSleeping();

		level.playSound(null, player.blockPosition(), VtorzheniyeSounds.BREATH, SoundSource.HOSTILE, 1.4F, 0.6F);
		level.playSound(null, player.blockPosition(), VtorzheniyeSounds.DOOR_BURST, SoundSource.HOSTILE, 0.9F, 0.8F);

		Guest guest = VtorzheniyeEntities.GUEST.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
		if (guest == null) {
			return;
		}

		BlockPos behind = player.blockPosition().relative(player.getDirection().getOpposite(), 3);
		guest.snapTo(behind.getX() + 0.5D, behind.getY(), behind.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
		level.addFreshEntity(guest);
	}
}
