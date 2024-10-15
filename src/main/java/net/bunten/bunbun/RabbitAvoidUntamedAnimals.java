package net.bunten.bunbun;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Rabbit;

import java.util.function.Predicate;

import static net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR;

public class RabbitAvoidUntamedAnimals<T extends LivingEntity> extends AvoidEntityGoal<T> {
    private final Rabbit rabbit;

    public static final Predicate<LivingEntity> ISNT_TAME_ANIMAL = entity -> entity instanceof TamableAnimal tamable && !tamable.isTame();

    public RabbitAvoidUntamedAnimals(Rabbit rabbit, Class<T> class_, float f, double d, double e) {
        super(rabbit, class_, livingEntity -> true, f, d, e, ISNT_TAME_ANIMAL.and(NO_CREATIVE_OR_SPECTATOR));
        this.rabbit = rabbit;
    }

    @Override
    public boolean canUse() {
        return rabbit.getVariant() != Rabbit.Variant.EVIL && super.canUse();
    }
}