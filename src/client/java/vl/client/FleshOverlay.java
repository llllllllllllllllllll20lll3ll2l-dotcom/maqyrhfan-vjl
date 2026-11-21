package vl.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import vl.core.VtorzheniyeSounds;

/**
 * «Мясной» оверлей: чем выше гниль, тем больше экран затягивает.
 *
 * <p>Никакой рендер-магии: только полупрозрачные полосы по краям, которые слегка пульсируют
 * в такт сердцу. Эффект рассчитан на то, чтобы его замечали не сразу.
 */
public final class FleshOverlay {
	/** Последняя полученная интенсивность от сервера. */
	private static float target;
	private static float current;
	private static long lastPulse = -1L;

	private FleshOverlay() {
	}

	public static void accept(int intensity) {
		target = Mth.clamp(intensity, 0, 100) / 100.0F;
	}

	public static float current() {
		return current;
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		boolean hudHidden = client.gameRenderer.getGameRenderState().guiRenderState.isHudHidden;
		if (client.player == null || client.level == null || hudHidden) {
			target = 0.0F;
			current = 0.0F;
			return;
		}

		current = Mth.lerp(0.02F, current, target);
		if (current < 0.01F) {
			return;
		}

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();

		long gameTime = client.level.getGameTime();

		// Пульс: медленный «вдох-выдох» с частотой сердца.
		float pulse = 0.55F + 0.45F * (float) Math.sin(gameTime * 0.11D);
		float strength = current * pulse;

		int edge = (int) (Math.min(width, height) * 0.22F * current);
		int alpha = (int) (Mth.clamp(strength * 190.0F, 0.0F, 190.0F));

		// Цвет — тёмно-красный, ближе к запёкшейся крови, без ярких акцентов.
		int color = (alpha << 24) | 0x2A0707;

		graphics.fill(0, 0, width, edge, color);
		graphics.fill(0, height - edge, width, height, color);
		graphics.fill(0, 0, edge, height, color);
		graphics.fill(width - edge, 0, width, height, color);

		// «Дыхание» в центре: почти незаметная пелена, будто экран запотевает изнутри.
		if (current > 0.55F) {
			int inner = (int) (Mth.clamp((current - 0.55F) / 0.45F, 0.0F, 1.0F) * 42.0F);
			int innerColor = (inner << 24) | 0x2A0707;
			graphics.fill(edge, edge / 2, width - edge, height - edge / 2, innerColor);
		}

		// Стук сердца на пике: клиентский звук, чтобы игрок слышал свои собственные вены.
		if (current > 0.85F && lastPulse != gameTime && gameTime % 14L == 0L) {
			lastPulse = gameTime;
			client.level.playLocalSound(
				client.player.getX(),
				client.player.getY(),
				client.player.getZ(),
				VtorzheniyeSounds.HEARTBEAT,
				SoundSource.PLAYERS,
				0.35F,
				0.9F + client.level.getRandom().nextFloat() * 0.15F,
				false
			);
		}
	}
}
