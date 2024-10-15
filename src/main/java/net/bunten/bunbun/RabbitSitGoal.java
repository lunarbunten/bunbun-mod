package net.bunten.bunbun;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Rabbit;

import java.util.EnumSet;

import static net.bunten.bunbun.Bunbun.*;

public class RabbitSitGoal extends Goal {
    private final Rabbit rabbit;

    public RabbitSitGoal(Rabbit tamableAnimal) {
        rabbit = tamableAnimal;
        setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
    }

    @Override
    public boolean canContinueToUse() {
        return isOrderedToSit(rabbit);
    }

    @Override
    public boolean canUse() {
        if (!isTame(rabbit)) {
            return false;
        } else if (rabbit.isInWaterOrBubble()) {
            return false;
        } else if (!rabbit.onGround()) {
            return false;
        } else {
            LivingEntity livingEntity = ((OwnableEntity) rabbit).getOwner();
            if (livingEntity == null) {
                return true;
            } else {
                return (!(rabbit.distanceToSqr(livingEntity) < 144.0) || livingEntity.getLastHurtByMob() == null) && isOrderedToSit(rabbit);
            }
        }
    }

    @Override
    public void start() {
        rabbit.getNavigation().stop();
        setInSittingPose(rabbit, true);
    }

    @Override
    public void stop() {
        setInSittingPose(rabbit, false);
    }
}