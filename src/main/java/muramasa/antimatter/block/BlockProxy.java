package muramasa.antimatter.block;

import muramasa.antimatter.AntimatterAPI;
import muramasa.antimatter.Ref;
import muramasa.antimatter.client.AntimatterModelManager;
import muramasa.antimatter.datagen.builder.AntimatterBlockModelBuilder;
import muramasa.antimatter.datagen.providers.AntimatterBlockStateProvider;
import muramasa.antimatter.registration.IRegistryEntryProvider;
import muramasa.antimatter.tile.TileEntityFakeBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.Collections;

public class BlockProxy extends BlockBasic implements IRegistryEntryProvider {

    public TileEntityType<?> TYPE;

    public BlockProxy(String domain, String id, Properties properties) {
        super(domain, id, properties);
        AntimatterAPI.register(IRegistryEntryProvider.class, getId(), this);
    }

    public BlockProxy(String domain, String id) {
        super(domain, id);
    }

    @Override
    public void onBlockModelBuild(Block block, AntimatterBlockStateProvider prov) {
        AntimatterBlockModelBuilder builder = prov.getBuilder(block);
        builder.loader(AntimatterModelManager.LOADER_PROXY);
        prov.state(block, builder);
    }

    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
        TileEntityFakeBlock tile = (TileEntityFakeBlock) world.getTileEntity(pos);
        return tile.getState().getBlock().asItem().getDefaultInstance();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRegistryBuild(IForgeRegistry<?> registry) {
        if (registry == ForgeRegistries.TILE_ENTITIES) {
           // ((IForgeRegistry<Block>)registry).register(this);
            TYPE = new TileEntityType<>(() -> new TileEntityFakeBlock(this), Collections.singleton(this), null).setRegistryName(new ResourceLocation(Ref.ID, "proxy"));
            AntimatterAPI.register(TileEntityType.class, getId(), TYPE);
        }
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TileEntityFakeBlock(this);
    }
}