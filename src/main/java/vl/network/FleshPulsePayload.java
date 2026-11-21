package vl.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import vl.Vtorzheniye;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Серверный сигнал клиенту о том, что гниль «проснулась».
 *
 * <p>Пакет несёт одну переменную — значение 0..100, которое рисует {@code FleshOverlay}.
 * Никакой логики урона в нём нет: сервер только сообщает клиенту, как сильно давить на глаз.
 */
public record FleshPulsePayload(int intensity) implements CustomPacketPayload {
	public static final Type<FleshPulsePayload> TYPE = new Type<>(Vtorzheniye.id("flesh_pulse"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FleshPulsePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		FleshPulsePayload::intensity,
		FleshPulsePayload::new
	);

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}

	/**
	 * Отправить импульс одному игроку.
	 */
	public static void send(ServerPlayer player, int intensity) {
		ServerPlayNetworking.send(player, new FleshPulsePayload(intensity));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
