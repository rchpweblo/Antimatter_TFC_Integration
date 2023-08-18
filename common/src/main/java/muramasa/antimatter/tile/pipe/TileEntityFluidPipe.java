package muramasa.antimatter.tile.pipe;

import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.botarium.common.fluid.base.FluidContainer;
import earth.terrarium.botarium.common.fluid.base.FluidHolder;
import earth.terrarium.botarium.common.fluid.base.PlatformFluidHandler;
import muramasa.antimatter.Ref;
import muramasa.antimatter.capability.Dispatch;
import muramasa.antimatter.capability.FluidHandler;
import muramasa.antimatter.capability.fluid.FluidTank;
import muramasa.antimatter.capability.fluid.PipeFluidHandlerSidedWrapper;
import muramasa.antimatter.capability.pipe.PipeFluidHandler;
import muramasa.antimatter.cover.ICover;
import muramasa.antimatter.gui.GuiInstance;
import muramasa.antimatter.gui.IGuiElement;
import muramasa.antimatter.gui.widget.InfoRenderWidget;
import muramasa.antimatter.integration.jeirei.renderer.IInfoRenderer;
import muramasa.antimatter.pipe.PipeSize;
import muramasa.antimatter.pipe.types.FluidPipe;
import muramasa.antimatter.util.AntimatterPlatformUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import tesseract.FluidPlatformUtils;
import tesseract.TesseractCapUtils;
import tesseract.TesseractGraphWrappers;
import tesseract.api.fluid.PipeFluidHolder;
import tesseract.api.fluid.IFluidPipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TileEntityFluidPipe<T extends FluidPipe<T>> extends TileEntityPipe<T> implements IFluidPipe, Dispatch.Sided<FluidContainer>, IInfoRenderer<InfoRenderWidget.TesseractFluidWidget> {

    protected Optional<PipeFluidHandler> fluidHandler;
    private PipeFluidHolder holder;
    Direction[] lastSide;
    long transferredAmount = 0;
    long mTemperature = 293;

    public TileEntityFluidPipe(T type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        int count = getPipeSize() == PipeSize.QUADRUPLE ? 4 : getPipeSize() == PipeSize.NONUPLE ? 9 : 1;
        fluidHandler = Optional.of(new PipeFluidHandler(this, type.getPressure(getPipeSize()) * 2, type.getPressure(getPipeSize()), count, 0));
        pipeCapHolder.set(() -> this);
        lastSide = new Direction[count];
    }

    @Override
    public void onLoad() {
        holder = new PipeFluidHolder(this);
        super.onLoad();
    }

    @Override
    public void onBlockUpdate(BlockPos neighbour) {
        super.onBlockUpdate(neighbour);
        TesseractGraphWrappers.FLUID.blockUpdate(getLevel(), getBlockPos().asLong(), neighbour.asLong());
    }


    @Override
    protected void register() {
        TesseractGraphWrappers.FLUID.registerConnector(getLevel(), getBlockPos().asLong(), this, isConnector());
    }

    @Override
    protected boolean deregister() {
        return TesseractGraphWrappers.FLUID.remove(getLevel(), getBlockPos().asLong());
    }


    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(Ref.KEY_MACHINE_FLUIDS))
            fluidHandler.ifPresent(t -> t.deserialize(tag.getCompound(Ref.KEY_MACHINE_FLUIDS)));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        fluidHandler.ifPresent(t -> tag.put(Ref.KEY_MACHINE_FLUIDS, t.serialize(new CompoundTag())));
    }

    @Override
    public void onRemove() {
        fluidHandler.ifPresent(FluidHandler::onRemove);
        super.onRemove();
    }

    @Override
    public void addWidgets(GuiInstance instance, IGuiElement parent) {
        super.addWidgets(instance, parent);
        instance.addWidget(InfoRenderWidget.TesseractFluidWidget.build().setPos(10, 10));
    }


    @Override
    public boolean isGasProof() {
        return getPipeType().isGasProof();
    }

    @Override
    public PipeFluidHolder getHolder() {
        return holder;
    }

    @Override
    public int getCapacity() {
        return getPipeType().getCapacity(getPipeSize());
    }

    @Override
    public long getPressure() {
        return getPipeType().getPressure(getPipeSize());
    }

    @Override
    public int getTemperature() {
        return getPipeType().getTemperature();
    }

    @Override
    public boolean connects(Direction direction) {
        return canConnect(direction.get3DDataValue());
    }

    @Override
    public boolean validate(Direction dir) {
        if (!super.validate(dir)) return false;
        return TesseractCapUtils.getFluidHandler(level, getBlockPos().relative(dir), dir.getOpposite()).isPresent();
    }

    public void setLastSide(Direction lastSide, int tank) {
        this.lastSide[tank] = lastSide;
    }

    @Override
    protected void serverTick(Level level, BlockPos pos, BlockState state) {
        super.serverTick(level, pos, state);
        this.getHolder().tick(getLevel().getGameTime());
    }

    public void onServerTickPre(Level level, BlockPos pos, BlockState state, boolean aFirst) {
        transferredAmount = 0;

        PlatformFluidHandler adjacentFluidHandlers[] = new PlatformFluidHandler[6];
        PipeFluidHandler pipeFluidHandler = fluidHandler.orElse(null);
        if (pipeFluidHandler == null) return;

        for (Direction tSide : Direction.values()) {
            if (connects(tSide)) {
                PlatformFluidHandler fluidHandler1 = TesseractCapUtils.getFluidHandler(level, pos.relative(tSide), tSide.getOpposite()).orElse(null);
                if (fluidHandler1 != null) {
                    adjacentFluidHandlers[tSide.get3DDataValue()] = fluidHandler1;
                }
            }
        }

        boolean tCheckTemperature = true;

        for (int i = 0; i < pipeFluidHandler.getInputTanks().getSize(); i++){
            FluidTank tTank = pipeFluidHandler.getInputTanks().getTank(i);
            FluidHolder tFluid = tTank.getStoredFluid();
            if (!tFluid.isEmpty()){
                mTemperature = (tCheckTemperature ? FluidPlatformUtils.getFluidTemperature(tFluid.getFluid()) : Math.max(mTemperature, FluidPlatformUtils.getFluidTemperature(tFluid.getFluid())));
                tCheckTemperature = false;


                if (!isGasProof() && FluidPlatformUtils.isFluidGaseous(tFluid.getFluid())) {
                    transferredAmount += tTank.extractFluid(tFluid.copyWithAmount(8 * TesseractGraphWrappers.dropletMultiplier), false).getFluidAmount();
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
                    /*try {
                        for (Entity tEntity : (List<Entity>)worldObj.getEntitiesWithinAABB(Entity.class, box(-2, -2, -2, +3, +3, +3))) {
                            UT.Entities.applyTemperatureDamage(tEntity, mTemperature, 2.0F, 10.0F);
                        }
                    } catch(Throwable e) {e.printStackTrace(ERR);}*/
                }
            }
            if (mTemperature > getTemperature()) {
                burn(level, pos.getX(), pos.getY(), pos.getZ());
                if (level.random.nextInt(100) == 0) {
                    pipeFluidHandler.clearContent();
                    level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                    return;
                }
            }

            if (!tTank.getStoredFluid().isEmpty()) distribute(level, tTank, i, adjacentFluidHandlers);

            lastSide[i] = null;
        }
    }

    @SuppressWarnings("rawtypes")
    public void distribute(Level level, FluidTank aTank, int i, PlatformFluidHandler[] fluidHandlers) {
        // Top Priority is filling Cauldrons and other specialties.
        for (Direction tSide : Direction.values()) {
            if (fluidHandlers[tSide.get3DDataValue()] != null) {
                // Covers let distribution happen, right?


            }
        }
        // Check if we are empty.
        if (aTank.isEmpty()) return;
        // Compile all possible Targets into one List.
        List<PlatformFluidHandler> tTanks = new ArrayList<>();
        List<PlatformFluidHandler> tPipes = new ArrayList<>();
        // Amount to check for Distribution
        long tAmount = aTank.getStoredFluid().getFluidAmount();
        // Count all Targets. Also includes THIS for even distribution, thats why it starts at 1.
        int tTargetCount = 1;
        // Put Targets into Lists.
        for (Direction tSide : Direction.values()) {

            // Don't you dare flow backwards!
            if (tSide == lastSide[i]) continue;
            // Are we even connected to this Side? (Only gets checked due to the Cover check being slightly expensive)
            if (!connects(tSide)) continue;
            // Covers let distribution happen, right?
            ICover cover = coverHandler.map(c -> c.get(tSide)).orElse(ICover.empty);
            if (!cover.isEmpty() && cover.blocksOutput(FluidContainer.class, tSide)) continue;
            // No Tank? Nothing to do then.
            if (fluidHandlers[tSide.get3DDataValue()] == null) continue;
            // Check if the Tank can be filled with this Fluid.
            long insert = fluidHandlers[tSide.get3DDataValue()].insertFluid(aTank.getStoredFluid().copyWithAmount(Integer.MAX_VALUE), true);
            if (insert > 0) {
                if (fluidHandlers[tSide.get3DDataValue()] instanceof PipeFluidHandlerSidedWrapper){
                    tTanks.add(level.random.nextInt(tTanks.size()+1), fluidHandlers[tSide.get3DDataValue()]);
                } else {
                    // Add to a random Position in the List.
                    tTanks.add(level.random.nextInt(tTanks.size()+1), fluidHandlers[tSide.get3DDataValue()]);
                }
                // One more Target.
                tTargetCount++;
                // Done everything.
                continue;
            }
        }
        // No Targets? Nothing to do then.
        if (tTargetCount <= 1) return;
        // Amount to distribute normally.
        tAmount = divup(tAmount, tTargetCount);
        // Distribute to Pipes first.
        for (PlatformFluidHandler tPipe : tPipes) transferredAmount += tPipe.extractFluid(tPipe.add(aTank.amount(tAmount-tPipe.amount()), aTank.get()));
        // Check if we are empty.
        if (aTank.isEmpty()) return;
        // Distribute to Tanks afterwards.
        for (DelegatorTileEntity tTank : tTanks) transferredAmount += aTank.remove(FL.fill(tTank, aTank.get(tAmount), T));
        // Check if we are empty.
        if (aTank.isEmpty()) return;
        // No Targets? Nothing to do then.
        if (tPipes.isEmpty()) return;
        // And then if there still is pressure, distribute to Pipes again.
        tAmount = (aTank.amount() - mCapacity/2) / tPipes.size();
        if (tAmount > 0) for (FluidTankGT tPipe : tPipes) transferredAmount += aTank.remove(tPipe.add(aTank.amount(tAmount), aTank.get()));
    }

    /** Divides but rounds up. */
    public static long divup(long aNumber, long aDivider) {
        return aNumber / aDivider + (aNumber % aDivider == 0 ? 0 : 1);
    }

    public static void burn(Level aWorld, int aX, int aY, int aZ) {
        BlockPos pos = new BlockPos(aX, aY, aZ);
        for (Direction tSide : Direction.values()) {
            fire(aWorld, pos.relative(tSide), false);
        }
    }

    public static boolean fire(Level aWorld, BlockPos pos, boolean aCheckFlammability) {
        BlockState tBlock = aWorld.getBlockState(pos);
        if (tBlock.getMaterial() == Material.LAVA || tBlock.getMaterial() == Material.FIRE) return false;
        if (tBlock.getMaterial() == Material.CLOTH_DECORATION || tBlock.getCollisionShape(aWorld, pos).isEmpty()) {
            if (AntimatterPlatformUtils.getFlammability(tBlock, aWorld, pos, Direction.NORTH) > 0) return aWorld.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
            if (aCheckFlammability) {
                for (Direction tSide : Direction.values()) {
                    BlockState tAdjacent = aWorld.getBlockState(pos.relative(tSide));
                    if (tAdjacent.getBlock() == Blocks.CHEST || tAdjacent.getBlock() == Blocks.TRAPPED_CHEST) return aWorld.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                    if (AntimatterPlatformUtils.getFlammability(tAdjacent, aWorld, pos.relative(tSide), tSide.getOpposite()) > 0) return aWorld.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                }
            } else {
                return aWorld.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
        return false;
    }

    @Override
    public Class<?> getCapClass() {
        return FluidContainer.class;
    }

    @Override
    public Optional<? extends FluidContainer> forSide(Direction side) {
        if (fluidHandler.isEmpty()) {
            fluidHandler = Optional.of(new PipeFluidHandler(this, type.getPressure(getPipeSize()) * 2, type.getPressure(getPipeSize()), 1, 0));
        }
        return Optional.of(new PipeFluidHandlerSidedWrapper(fluidHandler.get(), this, side));
        /*if (FluidController.SLOOSH) {

        } else {
            return Optional.of(new PipeFluidHandlerSidedWrapper(new TesseractFluidCapability<>(this, side, !isConnector(), (stack, in, out, simulate) ->
            this.coverHandler.ifPresent(t -> t.onTransfer(stack, in, out, simulate))), this, side));
        }*/
    }

    @Override
    public Optional<? extends FluidContainer> forNullSide() {
        return forSide(null);
    }

    @Override
    public int drawInfo(InfoRenderWidget.TesseractFluidWidget instance, PoseStack stack, Font renderer, int left, int top) {
        renderer.draw(stack, "Pressure used: " + instance.stack.getFluidAmount(), left, top, 16448255);
        renderer.draw(stack, "Pressure total: " + getPressure()*20, left, top + 8, 16448255);
        renderer.draw(stack, "Fluid: " + FluidPlatformUtils.getFluidId(instance.stack.getFluid()).toString(), left, top + 16, 16448255);
        renderer.draw(stack, "(Above only in intersection)", left, top + 24, 16448255);
        //renderer.draw(stack, "Frame average: " + instance.holderPressure / 20, left, top + 32, 16448255);
        return 32;
    }

    @Override
    public List<String> getInfo() {
        List<String> list = super.getInfo();
        fluidHandler.ifPresent(t -> {
            for (int i = 0; i < t.getSize(); i++) {
                FluidHolder stack = t.getFluidInTank(i);
                list.add(FluidPlatformUtils.getFluidId(stack.getFluid()).toString() + " " + (stack.getFluidAmount() / TesseractGraphWrappers.dropletMultiplier) + " mb.");
            }
        });
        list.add("Pressure: " + getPipeType().getPressure(getPipeSize()));
        list.add("Capacity: " + getPipeType().getCapacity(getPipeSize()));
        list.add("Max temperature: " + getPipeType().getTemperature());
        list.add(getPipeType().isGasProof() ? "Gas proof." : "Cannot handle gas.");
        return list;
    }

    public static boolean even(int... aCoords) {
        int i = 0;
        for (int tCoord : aCoords) {
            if (tCoord % 2 == 0) i++;
        }
        return i % 2 == 0;
    }
}
