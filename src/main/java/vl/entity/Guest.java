package vl.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import vl.core.VtorzheniyeSounds;

/**
 * Гость — центральное существо мода.
 *
 * <p>Не бегает. Не прыгает. Не рычит. Просто идёт к тебе, и с каждым шагом звук
 * становится ближе, чем ты готов. Если игрок смотрит прямо на него — Гость
 * замирает: он не любит, когда его видят. Отвернулся — он снова в пути.
 */
public class Guest extends Mob {
	/** Сколько тиков Гость стоит неподвижно, пока на него смотрят. */
	private static final int STARE_FREEZE_TICKS = 30;
	/** Максимальная дистанция «взгляда в упор». */
	private static final double STARE_RANGE = 32.0D;
	private static final double STARE_COS = Math.cos(Math.toRadians(28.0D));

	private int stareTicks;
	private int breathCooldown;

	public Guest(EntityType<? extends Guest> type, Level level) {
		super(type, level);
		this.xpReward = 30;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 60.0D)
			.add(Attributes.MOVEMENT_SPEED, 0.13D)
			.add(Attributes.ATTACK_DAMAGE, 7.0D)
			.add(Attributes.FOLLOW_RANGE, 96.0D)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
			.add(Attributes.STEP_HEIGHT, 1.2D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();

		if (this.level().isClientSide()) {
			return;
		}

		boolean observed = this.isObserved();
		if (observed) {
			this.stareTicks = Math.min(this.stareTicks + 1, STARE_FREEZE_TICKS * 4);
		} else {
			this.stareTicks = Math.max(this.stareTicks - 2, 0);
		}

		// Пока на него смотрят — он не идёт. Но и не отступает: просто стоит.
		if (this.stareTicks >= STARE_FREEZE_TICKS) {
			this.getNavigation().stop();
			this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
		}

		this.updateBreathing(observed);
	}

	/**
	 * Смотрит ли кто-нибудь из игроков прямо на Гостя.
	 */
	private boolean isObserved() {
		for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(STARE_RANGE))) {
			if (!player.isAlive() || player.isSpectator()) {
				continue;
			}

			Vec3 look = player.getLookAngle().normalize();
			Vec3 toGuest = this.position().add(0.0D, this.getBbHeight() * 0.85D, 0.0D)
				.subtract(player.getEyePosition())
				.normalize();

			if (look.dot(toGuest) >= STARE_COS) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Влажный хрип, который становится громче, когда Гость подходит.
	 */
	private void updateBreathing(boolean observed) {
		if (--this.breathCooldown > 0) {
			return;
		}

		Player nearest = this.level().getNearestPlayer(this, 24.0D);
		if (nearest == null) {
			this.breathCooldown = 60;
			return;
		}

		double distance = this.distanceTo(nearest);
		float volume = (float) Mth.clamp(1.6D - (distance / 16.0D), 0.25D, 1.6D);
		float pitch = observed ? 1.25F : 0.65F;

		this.level().playSound(
			null,
			this.blockPosition(),
			VtorzheniyeSounds.BREATH,
			SoundSource.HOSTILE,
			volume,
			pitch
		);

		this.breathCooldown = observed ? 34 : 70;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Гость не любит, когда по нему бьют: он отвечает, но не отступает.
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			this.stareTicks = 0;
			this.breathCooldown = 0;

			if (source.getEntity() instanceof LivingEntity attacker) {
				this.setTarget(attacker);
			}
		}

		return hurt;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			level.playSound(
				null,
				this.blockPosition(),
				VtorzheniyeSounds.SCREAM,
				SoundSource.HOSTILE,
				1.2F,
				0.55F
			);
			level.sendParticles(
				ParticleTypes.SCULK_CHARGE_POP,
				this.getX(),
				this.getY() + this.getBbHeight() * 0.7D,
				this.getZ(),
				24,
				0.4D,
				0.9D,
				0.4D,
				0.02D
			);
		}

		return hit;
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		// Он не чувствует падений. Ни своих, ни чужих.
		return false;
	}

	@Nullable
	@Override
	protected SoundEvent getAmbientSound() {
		return VtorzheniyeSounds.WHISPER;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return VtorzheniyeSounds.SCREAM;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return VtorzheniyeSounds.PRESSURE;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 220;
	}

	@Override
	public float getVoicePitch() {
		return 0.55F;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Nullable
	@Override
	public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
		net.minecraft.world.level.ServerLevelAccessor level,
		net.minecraft.world.DifficultyInstance difficulty,
		EntitySpawnReason reason,
		@Nullable net.minecraft.world.entity.SpawnGroupData spawnData
	) {
		this.setPersistenceRequired();
		return super.finalizeSpawn(level, difficulty, reason, spawnData);
	}
}
