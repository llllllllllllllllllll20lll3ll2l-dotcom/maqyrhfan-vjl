package vl.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import vl.Vtorzheniye;
import vl.entity.VtorzheniyeEntities;
import vl.network.FleshPulsePayload;

@Environment(EnvType.CLIENT)
public final class VtorzheniyeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(GuestRenderer.LAYER, GuestModel::createBodyLayer);
		EntityRendererRegistry.register(VtorzheniyeEntities.GUEST, GuestRenderer::new);

		// Оверлей кладём сразу после ванильных «миск-оверлеев»: он окажется
		// поверх виньетки, но под хотбаром и прицелом.
		HudElementRegistry.attachElementAfter(
			VanillaHudElements.MISC_OVERLAYS,
			Vtorzheniye.id("flesh"),
			FleshOverlay::extract
		);

		ClientPlayNetworking.registerGlobalReceiver(
			FleshPulsePayload.TYPE,
			(payload, context) -> FleshOverlay.accept(payload.intensity())
		);

		// При выходе из мира гниль сбрасывается: каждый новый заход — чистая кожа.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FleshOverlay.accept(0));

		Vtorzheniye.LOGGER.info("Вторжение: клиент готов смотреть.");
	}
}
