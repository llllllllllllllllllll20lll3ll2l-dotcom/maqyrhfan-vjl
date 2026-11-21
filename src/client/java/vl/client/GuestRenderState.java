package vl.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Состояние для отрисовки Гостя.
 */
public class GuestRenderState extends LivingEntityRenderState {
	/** Насколько сильно Гость «вытянут» по вертикали: он будто растёт, пока на него смотрят. */
	public float heightStretch = 1.0F;
	/** Медленное покачивание головы. */
	public float sway = 0.0F;
	/** Видно ли его вообще (мигает, когда рядом). */
	public float visibility = 1.0F;
}
