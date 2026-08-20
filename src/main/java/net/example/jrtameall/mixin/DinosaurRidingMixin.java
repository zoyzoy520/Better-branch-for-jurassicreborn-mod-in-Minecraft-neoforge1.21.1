package net.example.jrtameall.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.example.jrtameall.SeatOffsets;
import net.vit.jurassicreborn.common.entities.CrocodileDinosaurEntity;
import net.vit.jurassicreborn.common.entities.DinosaurEntity;
import net.vit.jurassicreborn.common.entities.FlyingDinosaurEntity;
import net.vit.jurassicreborn.common.entities.PenguinDinosaurEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pig-like riding for tamed dinosaurs (M1: core land riding).
 *
 * The five declared methods below are merged INTO DinosaurEntity by Mixin as
 * overrides of inherited vanilla hooks (getControllingPassenger etc.). Mixin
 * cannot @Inject into inherited methods, but it CAN add overrides of them
 * (none of the five is final). This is what activates vanilla's
 * travelRidden branch, the >=1.0 ridden step-up (auto-climb) and the
 * MOVE/JUMP/LOOK goal suppression.
 *
 * Server-side state (wake, order fix, nav stop) lives in tickRidden because
 * it runs on both sides; movement itself runs on the rider's client via
 * vanilla travelRidden -> travel (server receives positions via
 * ServerboundMoveVehiclePacket).
 */
@Mixin(DinosaurEntity.class)
public abstract class DinosaurRidingMixin extends PathfinderMob {

    /** Mixin classes extending the target's superclass still need an explicit constructor. */
    protected DinosaurRidingMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract void setFieldOrder(DinosaurEntity.Order order);

    @Shadow
    public abstract void setSleeping(boolean sleeping);

    @Shadow
    public abstract boolean isSleeping();

    @Shadow
    public abstract DinosaurEntity.Order getOrder();

    @Shadow
    public abstract boolean isOwner(Player player);

    @Shadow
    public abstract boolean isCarcass();

    @Shadow
    public abstract int getAgePercentage();

    @Shadow
    public abstract boolean isMarineCreature();

    // --- Declared overrides of inherited vanilla riding hooks ---

    /** Returning the Player passenger activates travelRidden + auto-climb. */
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player ? player : null;
    }

    /** Constant forward (pig-style); zero input when not holding the carrot = stationary. */
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        if (!player.isHolding(Items.CARROT_ON_A_STICK)) {
            return Vec3.ZERO;
        }
        if (this.isInWater() && (this.isMarineCreature()
                || (Object) this instanceof PenguinDinosaurEntity
                || (Object) this instanceof CrocodileDinosaurEntity)) {
            // M3/M5: aquatic riding - pitch controls dive/climb (y component),
            // boosted 6.25x (2.5x of the previous 2.5x) per test feedback.
            // Penguin/crocodile ride the same vanilla water travel path (their
            // JR sink/target branches are neutralized while ridden), so the
            // same input.y gives them pitch-controlled up/down too.
            float pitch = player.getXRot() * (float) (Math.PI / 180.0);
            return new Vec3(0.0D, -Math.sin(pitch) * 6.25D, 1.0D);
        }
        if ((Object) this instanceof FlyingDinosaurEntity flying && !flying.isTouchingGround()) {
            // M4: flight riding - pitch controls climb/dive while airborne.
            // On the ground the flying branch below is used (pig-style).
            float pitch = player.getXRot() * (float) (Math.PI / 180.0);
            return new Vec3(0.0D, -Math.sin(pitch) * 6.25D, 1.0D);
        }
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    /** Ride speed = the dinosaur's configured max speed (age-interpolated x genetics). */
    protected float getRiddenSpeed(Player player) {
        // NOTE: the M3 water sprint does NOT go through this value. Vanilla
        // derives water acceleration from WATER_MOVEMENT_EFFICIENCY, which JR
        // zeroes on marine dinosaurs, so speed is ignored in water entirely.
        // The sprint acceleration lives in tickRidden instead.
        float base = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if ((Object) this instanceof FlyingDinosaurEntity flying) {
            // M4 feedback: fliers walk at half speed on the ground. In the air
            // the tickRidden acceleration drives speed, not this value.
            if (flying.isTouchingGround()) {
                return base * 0.5F;
            }
            return base;
        }
        // M5: land dinosaurs - half speed at rest, 1.25x while holding jump.
        // Jump state is client-only (LocalPlayer); the server gets the rest
        // value, which is fine because movement runs on the rider's client.
        // The isClientSide guard short-circuits before the client-only class
        // link so dedicated servers never resolve LocalPlayer.
        if (this.level().isClientSide
                && player instanceof net.minecraft.client.player.LocalPlayer local
                && local.input.jumping) {
            return base * 1.25F;
        }
        return base * 0.5F;
    }

    /** Steering sync (pig pattern) + server-side state corrections. */
    protected void tickRidden(Player player, Vec3 travelVector) {
        this.setRot(player.getYRot(), player.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        // M3/M4: aquatic + flight riding - extra forward acceleration. In
        // water the speed attribute is ignored entirely (WATER_MOVEMENT_EFFICIENCY
        // is 0.0 on JR marine dinosaurs); in the air JR's flight travel never
        // consumes ridden input, so the horizontal component is driven here too.
        // Baseline is 0.025 accel; holding jump gives 4x that (0.10). Requires
        // the carrot so a stationary rider (no carrot) does not drift. Jump state
        // exists only on the client (LocalPlayer), which is fine - travel also
        // only runs on the rider's client; the server takes positions from packets.
        // Same "aquatic ride" set as getRiddenInput: marine creatures plus
        // penguin/crocodile (they ride the same vanilla water travel path).
        boolean aquaticRide = this.isInWater() && (this.isMarineCreature()
                || (Object) this instanceof PenguinDinosaurEntity
                || (Object) this instanceof CrocodileDinosaurEntity);
        boolean airborneFlight = (Object) this instanceof FlyingDinosaurEntity flying
                && !flying.isTouchingGround() && !this.isInWater();
        if ((aquaticRide || airborneFlight)
                && player.isHolding(Items.CARROT_ON_A_STICK)) {
            // isClientSide guards the client-only LocalPlayer link so a
            // dedicated server never resolves the class.
            double accel = this.level().isClientSide
                    && player instanceof net.minecraft.client.player.LocalPlayer local
                    && local.input.jumping ? 0.10D : 0.025D;
            float yaw = this.getYRot() * (float) (Math.PI / 180.0);
            this.setDeltaMovement(this.getDeltaMovement().add(-Mth.sin(yaw) * accel, 0.0D, Mth.cos(yaw) * accel));
        }
        // M4: takeoff hop - while riding a ground-standing flier, jump lifts
        // it off. Once airborne noGravity takes over (tick() sets it because
        // shouldLand stays false while ridden), so the hop is enough to leave
        // the ground; the pitch-controlled air branch then takes over.
        if (this.level().isClientSide
                && player instanceof net.minecraft.client.player.LocalPlayer local
                && local.input.jumping
                && player.isHolding(Items.CARROT_ON_A_STICK)) {
            if ((Object) this instanceof FlyingDinosaurEntity flying
                    && flying.isTouchingGround() && !this.isInWater()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.5D, 0.0D));
            }
        }
        if (!this.level().isClientSide) {
            // Force stand/wake: ride mode overrides SIT order and sleep.
            if (this.getOrder() == DinosaurEntity.Order.SIT) {
                this.setFieldOrder(DinosaurEntity.Order.WANDER);
            }
            if (this.isSleeping()) {
                this.setSleeping(false);
            }
            // M3: aquatic riding - keep the rider's air bar full.
            if (this.isMarineCreature()) {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 300, 0, false, false));
            }
            // DinosaurMoveHelper would fight the rider's steering.
            this.getNavigation().stop();
            // M2: pure mount mode - clear any pre-ride target every tick.
            this.setTarget(null);
        }
        // M2/M4: flying dinosaurs never try to land while ridden. Runs on BOTH
        // sides - the client also reads shouldLand in tick() (idle hover builds
        // shouldLand after 60 idle ticks, which would re-enable gravity), and
        // the client's own tickRidden is the one that must suppress it.
        if ((Object) this instanceof FlyingDinosaurEntity flying) {
            flying.shouldLand = false;
        }
    }

    /**
     * Seat height: 60% of the hitbox height (the back) plus a raise plus a
     * per-species manual offset from SeatOffsets. Large dinosaurs (big
     * carnivores like rex/giga/indoraptor/spinosaurus and sauropods, plus
     * other bulky species like triceratops) keep the full raise; everything
     * smaller gets a reduced raise. Classified by hitbox height so no
     * species list has to be maintained. positionRider computes: this return
     * value MINUS the passenger's VEHICLE attachment (0, 0.6, 0), so we add
     * it back into every formula.
     */
    private static final double SEAT_SMALL_RAISE = 1.5D;
    private static final double SEAT_LARGE_RAISE = 3.0D;
    private static final double LARGE_HEIGHT_THRESHOLD = 3.4D;
    /** Unlisted small dinosaurs (bbHeight below this) also get the -1 small-type adjustment. */
    private static final double SMALL_EXTRA_THRESHOLD = 2.2D;

    public Vec3 getPassengerRidingPosition(Entity passenger) {
        double h = this.getBbHeight();
        double raise = h >= LARGE_HEIGHT_THRESHOLD ? SEAT_LARGE_RAISE : SEAT_SMALL_RAISE;
        String species = this.getType().builtInRegistryHolder().key().location().toString();
        // Explicitly listed species use their table value (0 included); unlisted
        // small dinosaurs follow the same -1 adjustment as the listed ones.
        double adj;
        if (SeatOffsets.contains(species)) {
            adj = SeatOffsets.get(species);
        } else if (h < LARGE_HEIGHT_THRESHOLD && h < SMALL_EXTRA_THRESHOLD) {
            adj = -1.0D;
        } else {
            adj = 0.0D;
        }
        double seatY = Math.max(h * 0.6D, 0.45D) + raise + adj;
        return this.position().add(0.0D, seatY, 0.0D);
    }

    // --- Mount gate: carrot-on-a-stick right click ---

    @Inject(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), cancellable = true)
    private void jr_tame_all$mountGate(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!player.getItemInHand(hand).is(Items.CARROT_ON_A_STICK)) {
            return;
        }
        if (hand != InteractionHand.MAIN_HAND || player.isCrouching() || this.isVehicle() || this.isCarcass()) {
            return;
        }
        if (this.level().isClientSide) {
            // Lenient client gate (age is synced) so the interact packet is always sent;
            // the server below is the real gate.
            if (this.getAgePercentage() > 75) {
                cir.setReturnValue(InteractionResult.sidedSuccess(true));
            }
        } else if (this.isOwner(player) && this.getAgePercentage() > 75
                // M5: penguins are a requested ride exception - they are too
                // narrow for the pig-width gate but the user rides them.
                && ((Object) this instanceof PenguinDinosaurEntity
                    || this.getBbWidth() >= EntityType.PIG.getWidth())) {
            // M3: marine creatures can only be ridden in water.
            if (this.isMarineCreature() && !this.isInWater()) {
                return;
            }
            player.startRiding(this);
            cir.setReturnValue(InteractionResult.sidedSuccess(false));
        }
    }

    // --- Death: seed the passenger's fall distance (spec: death dismount = fall damage).
    // Vanilla already delivers fall damage from the ejection height; this is belt-and-braces.

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
    private void jr_tame_all$seedPassengerFallDistance(DamageSource source, CallbackInfo ci) {
        if (!this.level().isClientSide) {
            for (Entity passenger : this.getPassengers()) {
                passenger.fallDistance = Math.max(passenger.fallDistance, this.fallDistance);
            }
        }
    }
}
