package muramasa.antimatter.pipe;

import muramasa.antimatter.pipe.types.ItemPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tesseract.Tesseract;
import tesseract.api.ITickingController;

import java.util.List;

public class BlockItemPipe<T extends ItemPipe<T>> extends BlockPipe<T> {

    public BlockItemPipe(T type, PipeSize size) {
        super(type.getId(), type, size, 0);
    }

    @Override
    public List<String> getInfo(List<String> info, Level world, BlockState state, BlockPos pos) {
        if (world.isClientSide) return info;
        ITickingController<?, ?, ?> controller = Tesseract.ITEM.getController(world, pos.asLong());
        controller.getInfo(pos.asLong(), info);
        info.add("Capacity: " + getType().getCapacity(getSize()));
        return info;
    }

//    @Override
//    public ITextComponent getDisplayName(ItemStack stack) {
//        boolean res = stack.getMetadata() > 7;
//        PipeSize size = PipeSize.VALUES[res ? stack.getMetadata() - 8 : stack.getMetadata()];
//        return (size == PipeSize.NORMAL ? "" : size.getDisplayName() + " ") + (res ? "Restrictive " : "") + material.getDisplayName() + " Item Pipe";
//    }
//
//    @Override
//    public void addInformation(ItemStack stack, @Nullable IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
//        boolean res = stack.getMetadata() > 7;
//        PipeSize size = PipeSize.VALUES[res ? stack.getMetadata() - 8 : stack.getMetadata()];
//        tooltip.add("Item Capacity: " + TextFormatting.BLUE + getSlotCount(size) + " Stacks/s");
//        tooltip.add("Routing Value: " + TextFormatting.YELLOW + getStepSize(size, res));
//    }
}
