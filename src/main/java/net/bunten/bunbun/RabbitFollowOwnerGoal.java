package net.bunten.bunbun;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class RabbitFollowOwnerGoal extends Goal {
    private final Rabbit rabbit;
    @Nullable
    private LivingEntity owner;
    private final double speedModifier;
    private final PathNavigation navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float startDistance;
    private float oldWaterCost;

    public RabbitFollowOwnerGoal(Rabbit rabbit, double speedModifier, float startDistance, float stopDistance) {
        this.rabbit = rabbit;
        this.speedModifier = speedModifier;
        this.navigation = rabbit.getNavigation();
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        if (!(rabbit.getNavigation() instanceof GroundPathNavigation) && !(rabbit.getNavigation() instanceof FlyingPathNavigation)) {
            throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
        }
    }

    public final boolean unableToMoveToOwner(Rabbit rabbit) {
        return rabbit.isPassenger() || rabbit.mayBeLeashed() || ((OwnableEntity) rabbit).getOwner() != null && ((OwnableEntity) rabbit).getOwner().isSpectator();
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = ((OwnableEntity) rabbit).getOwner();
        if (owner == null) {
            return false;
        } else if (unableToMoveToOwner(rabbit)) {
            return false;
        } else if (rabbit.distanceToSqr(owner) < (double)(startDistance * startDistance)) {
            return false;
        } else {
            this.owner = owner;
            return true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (navigation.isDone()) {
            return false;
        } else {
            return !unableToMoveToOwner(rabbit) && !(rabbit.distanceToSqr(owner) <= (double) (stopDistance * stopDistance));
        }
    }

    @Override
    public void start() {
        timeToRecalcPath = 0;
        oldWaterCost = rabbit.getPathfindingMalus(PathType.WATER);
        rabbit.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    @Override
    public void stop() {
        owner = null;
        navigation.stop();
        rabbit.setPathfindingMalus(PathType.WATER, oldWaterCost);
        rabbit.setSpeedModifier(0.0f);
    }

    @Override
    public void tick() {
        rabbit.getLookControl().setLookAt(owner, 10.0F, (float)rabbit.getMaxHeadXRot());

        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = adjustedTickDelay(10);
            rabbit.setSpeedModifier(speedModifier);
            navigation.moveTo(owner, 1.0f);
        }
    }
}