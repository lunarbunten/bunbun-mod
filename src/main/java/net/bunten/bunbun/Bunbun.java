package net.bunten.bunbun;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Bunbun implements ModInitializer {
	public static final String MOD_ID = "bunbun";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final EntityDataAccessor<Byte> RABBIT_DATA_FLAGS_ID = SynchedEntityData.defineId(Rabbit.class, EntityDataSerializers.BYTE);
	public static final EntityDataAccessor<Optional<UUID>> RABBIT_DATA_OWNERUUID_ID = SynchedEntityData.defineId(Rabbit.class, EntityDataSerializers.OPTIONAL_UUID);
	public static final EntityDataAccessor<Boolean> RABBIT_ORDERED_TO_SIT_ID = SynchedEntityData.defineId(Rabbit.class, EntityDataSerializers.BOOLEAN);

	public static final TagKey<Item> RABBIT_TAMING_ITEMS = bind("rabbit_taming_items");

	private static TagKey<Item> bind(String string) {
		return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, string));
	}

	public static void setOwnerUUID(Rabbit rabbit, @Nullable UUID uUID) {
		rabbit.getEntityData().set(RABBIT_DATA_OWNERUUID_ID, Optional.ofNullable(uUID));
	}

	public static boolean isTame(Rabbit rabbit) {
		return (rabbit.getEntityData().get(RABBIT_DATA_FLAGS_ID) & 4) != 0;
	}

	public static void setTame(Rabbit rabbit, boolean bl, boolean bl2) {
		byte b = rabbit.getEntityData().get(RABBIT_DATA_FLAGS_ID);
		if (bl) {
			rabbit.getEntityData().set(RABBIT_DATA_FLAGS_ID, (byte)(b | 4));
		} else {
			rabbit.getEntityData().set(RABBIT_DATA_FLAGS_ID, (byte)(b & -5));
		}
	}

	public static void setOrderedToSit(Rabbit rabbit, boolean bl) {
		rabbit.getEntityData().set(RABBIT_ORDERED_TO_SIT_ID, bl);
	}

	public static boolean isOrderedToSit(Rabbit rabbit) {
		return rabbit.getEntityData().get(RABBIT_ORDERED_TO_SIT_ID);
	}

	public static void setInSittingPose(Rabbit rabbit, boolean bl) {
		byte b = rabbit.getEntityData().get(RABBIT_DATA_FLAGS_ID);
		if (bl) {
			rabbit.getEntityData().set(RABBIT_DATA_FLAGS_ID, (byte)(b | 1));
		} else {
			rabbit.getEntityData().set(RABBIT_DATA_FLAGS_ID, (byte)(b & -2));
		}
	}

	public static boolean isInSittingPose(Rabbit rabbit) {
		return (rabbit.getEntityData().get(RABBIT_DATA_FLAGS_ID) & 1) != 0;
	}

	private void setupTameRabbitGoals(Rabbit rabbit) {
		rabbit.removeAllGoals(Objects::nonNull);

		rabbit.goalSelector.addGoal(1, new FloatGoal(rabbit));
		rabbit.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(rabbit, rabbit.level()));
		rabbit.goalSelector.addGoal(2, new RabbitSitGoal(rabbit));
		rabbit.goalSelector.addGoal(3, new RabbitFollowOwnerGoal(rabbit, 3.0f, 3.0f, 7.0f));
		rabbit.goalSelector.addGoal(4, new Rabbit.RabbitPanicGoal(rabbit, 2.2));
		rabbit.goalSelector.addGoal(5, new BreedGoal(rabbit, 0.8));
		rabbit.goalSelector.addGoal(6, new TemptGoal(rabbit, 1.0, stack -> stack.is(RABBIT_TAMING_ITEMS), false));
		rabbit.goalSelector.addGoal(7, new TemptGoal(rabbit, 1.0, itemStack -> itemStack.is(ItemTags.RABBIT_FOOD), false));
		rabbit.goalSelector.addGoal(8, new RabbitAvoidUntamedAnimals<>(rabbit, Wolf.class, 10.0F, 2.2, 2.2));
		rabbit.goalSelector.addGoal(8, new Rabbit.RabbitAvoidEntityGoal<>(rabbit, Monster.class, 4.0F, 2.2, 2.2));
		rabbit.goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(rabbit, 0.6));
		rabbit.goalSelector.addGoal(11, new LookAtPlayerGoal(rabbit, Player.class, 10.0F));
	}

	private void tryToTame(Rabbit rabbit, Player player) {
		if (rabbit.getRandom().nextInt(3) == 0) {
			setOwnerUUID(rabbit, player.getUUID());
			if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer, rabbit);
			setTame(rabbit, true, true);
			setOrderedToSit(rabbit, true);
			rabbit.getNavigation().stop();
			rabbit.level().broadcastEntityEvent(rabbit, (byte) 7);

			setupTameRabbitGoals(rabbit);
		} else {
			rabbit.level().broadcastEntityEvent(rabbit, (byte) 6);
		}
	}

	@Override
	public void onInitialize() {
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (entity instanceof Rabbit rabbit && ((OwnableEntity) rabbit).getOwnerUUID() == null && player.getItemInHand(hand).is(RABBIT_TAMING_ITEMS) && !world.isClientSide())
				tryToTame(rabbit, player);
            return InteractionResult.PASS;
        });
	}
}