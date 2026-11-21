package vl.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import vl.Vtorzheniye;
import vl.entity.Guest;

/**
 * Рендерер Гостя.
 *
 * <p>Он намеренно чуть выше и чуть темнее, чем должен быть, а его текстура почти не
 * отражает свет — так силуэт «проваливается» в темноту.
 */
public class GuestRenderer extends MobRenderer<Guest, GuestRenderState, GuestModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Vtorzheniye.id("guest"), "main");

	private static final Identifier TEXTURE = Vtorzheniye.id("textures/entity/guest.png");

	public GuestRenderer(EntityRendererProvider.Context context) {
		super(context, new GuestModel(context.bakeLayer(LAYER)), 0.35F);
	}

	@Override
	public Identifier getTextureLocation(GuestRenderState state) {
		return TEXTURE;
	}

	@Override
	public GuestRenderState createRenderState() {
		return new GuestRenderState();
	}

	@Override
	public void extractRenderState(Guest entity, GuestRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);

		// Чем ближе Гость, тем сильнее он «вытягивается». Это не должно быть заметно
		// сразу — только когда он уже слишком близко.
		float closeness = Mth.clamp(1.0F - ((float) Math.sqrt(this.entityRenderDispatcher.distanceToSqr(entity)) / 24.0F), 0.0F, 1.0F);

		state.heightStretch = 1.0F + closeness * 0.18F;
		state.sway = (float) Math.sin((entity.tickCount + partialTicks) * 0.02F) * 0.18F;
	}

	@Override
	protected void scale(GuestRenderState state, PoseStack poseStack) {
		// Узкий и вытянутый: 0.9 по горизонтали, до 1.18 по вертикали вблизи.
		poseStack.scale(0.88F, state.heightStretch, 0.88F);
	}
}
