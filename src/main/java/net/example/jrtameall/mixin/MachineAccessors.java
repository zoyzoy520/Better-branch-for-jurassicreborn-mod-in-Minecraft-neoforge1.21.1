package net.example.jrtameall.mixin;

import net.vit.jurassicreborn.common.blocks.entities.Embryoncis.EmbryonicMachine.EmbryonicMachineBlockEntity;
import net.vit.jurassicreborn.common.blocks.entities.grinder.FossilGrinderBlockEntity;
import net.vit.jurassicreborn.common.blocks.entities.incubator.IncubatorBlockEntity;
import net.vit.jurassicreborn.common.blocks.inventory.IncubatorItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor interfaces for private/protected machine fields. The speed mixins'
 * handlers are static (the tick methods are static), so they reach the
 * instance fields through these interfaces.
 */
public final class MachineAccessors {

    private MachineAccessors() {
    }

    @Mixin(FossilGrinderBlockEntity.class)
    public interface Grinder {
        @Accessor("grindTime")
        int jr_tame_all$getGrindTime();

        @Accessor("grindTime")
        void jr_tame_all$setGrindTime(int value);
    }

    @Mixin(EmbryonicMachineBlockEntity.class)
    public interface Embryonic {
        @Accessor("processTime")
        int jr_tame_all$getProcessTime();

        @Accessor("processTime")
        void jr_tame_all$setProcessTime(int value);
    }

    @Mixin(IncubatorBlockEntity.class)
    public interface Incubator {
        @Accessor("eggIncubationTime")
        int[] jr_tame_all$getEggIncubationTime();

        @Accessor("machineItemStackHandler")
        IncubatorItemHandler jr_tame_all$getItemHandler();
    }
}
