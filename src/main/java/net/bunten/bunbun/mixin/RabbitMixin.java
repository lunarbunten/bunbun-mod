package net.bunten.bunbun.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

import static net.bunten.bunbun.Bunbun.*;

@Mixin(Rabbit.class)
public abstract class RabbitMixin extends Animal implements OwnableEntity {

	protected RabbitMixin(EntityType<? extends Animal> entityType, Level level) {
		super(entityType, level);
	}

	@Unique
	private final Rabbit rabbit = (Rabbit) (Object) this;

	@Unique
	@Override
	public boolean handleLeashAtDistance(Entity entity, float f) {
		if (isInSittingPose((rabbit))) {
			if (f > 10.0F) {
				this.dropLeash(true, true);
			}

			return false;
		} else {
			return super.handleLeashAtDistance(entity, f);
		}
	}

	@Override
	@Unique
	public boolean hurt(DamageSource damageSource, float f) {
		if (isInvulnerableTo(damageSource)) {
			return false;
		} else {
			if (!level().isClientSide()) setOrderedToSit(rabbit, false);

			return super.hurt(damageSource, f);
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand interactionHand) {
		InteractionResult interactionResult = super.mobInteract(player, interactionHand);
		if (!interactionResult.consumesAction() && player == getOwner()) {
			setOrderedToSit(rabbit, !isOrderedToSit(rabbit));
			jumping = false;
			navigation.stop();
			setTarget(null);
			return InteractionResult.SUCCESS_NO_ITEM_USED;
		} else {
			return interactionResult;
		}
	}

	@Inject(at = @At("RETURN"), method = "registerGoals")
	protected void registerGoals(CallbackInfo info) {
		goalSelector.addGoal(2, new TemptGoal(this, 1.0, stack -> stack.is(RABBIT_TAMING_ITEMS), false));
	}

	@Inject(at = @At("RETURN"), method = "createAttributes", cancellable = true)
	private static void init(CallbackInfoReturnable<AttributeSupplier.Builder> info) {
		info.setReturnValue(info.getReturnValue().add(Attributes.SAFE_FALL_DISTANCE, 8.0f).add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.5f));
	}

    @Nullable
    public UUID getOwnerUUID() {
        return entityData.get(RABBIT_DATA_OWNERUUID_ID).orElse(null);
    }

	@Inject(at = @At("TAIL"), method = "defineSynchedData")
	protected void defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(RABBIT_DATA_FLAGS_ID, (byte)0);
		builder.define(RABBIT_DATA_OWNERUUID_ID, Optional.empty());
		builder.define(RABBIT_ORDERED_TO_SIT_ID, false);
	}

	@Inject(at = @At("TAIL"), method = "addAdditionalSaveData")
	public void addAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
		if (getOwnerUUID() != null) {
			compoundTag.putUUID("Owner", getOwnerUUID());
		}

		compoundTag.putBoolean("Sitting", isOrderedToSit(rabbit));
	}

	@Inject(at = @At("TAIL"), method = "readAdditionalSaveData")
	public void readAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
		UUID uUID;
		if (compoundTag.hasUUID("Owner")) {
			uUID = compoundTag.getUUID("Owner");
		} else {
			String string = compoundTag.getString("Owner");
			uUID = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), string);
		}

		if (uUID != null) {
			try {
				setOwnerUUID(rabbit, uUID);
				setTame(rabbit, true, false);
			} catch (Throwable var4) {
				setTame(rabbit, false, true);
			}
		}

		setOrderedToSit(rabbit, compoundTag.getBoolean("Sitting"));
		setInSittingPose(rabbit, isOrderedToSit(rabbit));
	}
}