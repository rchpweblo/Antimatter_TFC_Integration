package muramasa.antimatter.blockentity.multi;

import muramasa.antimatter.blockentity.BlockEntityMachine;
import muramasa.antimatter.capability.ComponentHandler;
import muramasa.antimatter.capability.machine.HatchComponentHandler;
import muramasa.antimatter.capability.machine.MachineCoverHandler;
import muramasa.antimatter.capability.machine.MachineEnergyHandler;
import muramasa.antimatter.cover.CoverOutput;
import muramasa.antimatter.cover.ICover;
import muramasa.antimatter.machine.event.ContentEvent;
import muramasa.antimatter.machine.event.IMachineEvent;
import muramasa.antimatter.machine.event.MachineEvent;
import muramasa.antimatter.machine.types.HatchMachine;
import muramasa.antimatter.structure.IComponent;
import muramasa.antimatter.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Collections;
import java.util.Optional;

import static muramasa.antimatter.Data.COVERDYNAMO;
import static muramasa.antimatter.Data.COVERENERGY;
import static muramasa.antimatter.machine.MachineFlag.*;

public class BlockEntityHatch<T extends BlockEntityHatch<T>> extends BlockEntityMachine<T> implements IComponent {

    public final Optional<HatchComponentHandler<T>> componentHandler;
    public final HatchMachine hatchMachine;

    public BlockEntityHatch(HatchMachine type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.hatchMachine = type;
        componentHandler = Optional
                .of(new HatchComponentHandler<>((T)this));
        if (type.has(ENERGY)) {
            energyHandler.set(() -> new MachineEnergyHandler<T>((T) this, 0, getMachineTier().getVoltage() * 66L,
                    type.getOutputCover() == COVERENERGY ? tier.getVoltage() : 0,
                    type.getOutputCover() == COVERDYNAMO ? tier.getVoltage() : 0,
                    type.getOutputCover() == COVERENERGY ? 2 : 0, type.getOutputCover() == COVERDYNAMO ? 1 : 0) {
                @Override
                public boolean canInput(Direction direction) {
                    ICover out = tile.coverHandler.map(MachineCoverHandler::getOutputCover).orElse(null);
                    if (out == null)
                        return false;
                    return out.isEqual(COVERENERGY) && direction == out.side();
                }

                @Override
                protected boolean checkVoltage(long voltage) {
                    boolean flag = true;
                    if (type.getOutputCover() == COVERDYNAMO) {
                        flag = voltage <= getOutputVoltage();
                    } else if (type.getOutputCover() == COVERENERGY) {
                        flag = voltage <= getInputVoltage();
                    }
                    if (!flag) {
                        Utils.createExplosion(tile.getLevel(), tile.getBlockPos(), 4.0F, Explosion.BlockInteraction.BREAK);
                    }
                    return flag;
                }

                @Override
                public boolean canOutput(Direction direction) {
                    ICover out = tile.coverHandler.map(MachineCoverHandler::getOutputCover).orElse(null);
                    if (out == null)
                        return false;
                    return out.isEqual(COVERDYNAMO) && direction == out.side();
                }
            });
        }
    }

    @Override
    public boolean wrenchMachine(Player player, BlockHitResult res, boolean crouch) {
        return setFacing(player, Utils.getInteractSide(res)) && setOutputFacing(player, Utils.getInteractSide(res));
    }

    @Override
    public Optional<HatchComponentHandler<T>> getComponentHandler() {
        return componentHandler;
    }

    @Override
    public void onMachineEvent(IMachineEvent event, Object... data) {
        if (isClientSide())
            return;
        super.onMachineEvent(event, data);
        if (event instanceof ContentEvent) {
            componentHandler.map(ComponentHandler::getControllers).orElse(Collections.emptyList())
                    .forEach(controller -> {
                        switch ((ContentEvent) event) {
                            case ITEM_INPUT_CHANGED, ITEM_OUTPUT_CHANGED, ITEM_CELL_CHANGED, FLUID_INPUT_CHANGED, FLUID_OUTPUT_CHANGED ->
                                    controller.onMachineEvent(event, data);
                        }
                    });
        } else if (event instanceof MachineEvent) {
            componentHandler.map(ComponentHandler::getControllers).orElse(Collections.emptyList())
                    .forEach(controller -> {
                        switch ((MachineEvent) event) {
                            // Forward energy event to controller.
                            case ENERGY_DRAINED, ENERGY_INPUTTED, HEAT_INPUTTED, HEAT_DRAINED -> controller.onMachineEvent(event, data);
                            default -> {
                            }
                        }
                    });
        }
    }

    @Override
    public void onFirstTick() {
        super.onFirstTick();
        coverHandler.ifPresent(t -> {
            ICover cover = t.getOutputCover();
            if (!(cover instanceof CoverOutput))
                return;
            ((CoverOutput) cover).setEjects(has(FLUID), has(ITEM));
        });
    }

    @Override
    public ResourceLocation getGuiTexture() {
        return new ResourceLocation(getMachineType().getDomain(), "textures/gui/machine/hatch.png");
    }
}