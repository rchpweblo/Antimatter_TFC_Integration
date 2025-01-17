package muramasa.antimatter;

import muramasa.antimatter.client.AntimatterModelManager;
import muramasa.antimatter.client.ClientData;
import muramasa.antimatter.cover.ICover;
import muramasa.antimatter.data.AntimatterDefaultTools;
import muramasa.antimatter.data.AntimatterMaterialTypes;
import muramasa.antimatter.data.AntimatterMaterials;
import muramasa.antimatter.data.AntimatterStoneTypes;
import muramasa.antimatter.datagen.AntimatterDynamics;
import muramasa.antimatter.datagen.loaders.MaterialRecipes;
import muramasa.antimatter.datagen.loaders.StoneRecipes;
import muramasa.antimatter.datagen.providers.*;
import muramasa.antimatter.event.CraftingEvent;
import muramasa.antimatter.event.ProvidersEvent;
import muramasa.antimatter.fluid.AntimatterFluid;
import muramasa.antimatter.gui.SlotType;
import muramasa.antimatter.gui.event.GuiEvents;
import muramasa.antimatter.integration.jeirei.AntimatterJEIREIPlugin;
import muramasa.antimatter.integration.kubejs.KubeJSRegistrar;
import muramasa.antimatter.item.interaction.CauldronInteractions;
import muramasa.antimatter.machine.MachineState;
import muramasa.antimatter.material.*;
import muramasa.antimatter.network.AntimatterNetwork;
import muramasa.antimatter.ore.BlockOre;
import muramasa.antimatter.ore.StoneType;
import muramasa.antimatter.proxy.ClientHandler;
import muramasa.antimatter.proxy.IProxyHandler;
import muramasa.antimatter.proxy.ServerHandler;
import muramasa.antimatter.recipe.Recipe;
import muramasa.antimatter.recipe.RecipeBuilders;
import muramasa.antimatter.recipe.container.ContainerItemShapedRecipe;
import muramasa.antimatter.recipe.container.ContainerItemShapelessRecipe;
import muramasa.antimatter.recipe.ingredient.IngredientSerializer;
import muramasa.antimatter.recipe.ingredient.PropertyIngredient;
import muramasa.antimatter.recipe.material.MaterialSerializer;
import muramasa.antimatter.recipe.serializer.AntimatterRecipeSerializer;
import muramasa.antimatter.registration.RegistrationEvent;
import muramasa.antimatter.registration.Side;
import muramasa.antimatter.tool.IAntimatterTool;
import muramasa.antimatter.util.AntimatterPlatformUtils;
import muramasa.antimatter.util.TagUtils;
import muramasa.antimatter.util.Utils;
import muramasa.antimatter.worldgen.AntimatterWorldGenerator;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static muramasa.antimatter.data.AntimatterStoneTypes.BEDROCK;

//import muramasa.antimatter.integration.kubejs.KubeJSRegistrar;


public class Antimatter extends AntimatterMod {

    public static Antimatter INSTANCE;
    public static final Logger LOGGER = LogManager.getLogger(Ref.ID);
    public static IProxyHandler PROXY;

    static {
        // AntimatterAPI.runBackgroundProviders();
    }

    public Antimatter() {
        super();
    }

    @Override
    public void onRegistrarInit() {
        super.onRegistrarInit();
        LOGGER.info("Loading Antimatter");
        INSTANCE = this;
        PROXY = Utils.unsafeRunForDist(() -> ClientHandler::new, () -> ServerHandler::new); // todo: scheduled to
        // change in new Forge
        if (AntimatterAPI.isModLoaded(Ref.MOD_KJS)){
            new KubeJSRegistrar();
        }
        AntimatterDynamics.clientProvider(Ref.ID,
                () -> new AntimatterBlockStateProvider(Ref.ID, Ref.NAME.concat(" BlockStates")));
        AntimatterDynamics.clientProvider(Ref.ID,
                () -> new AntimatterItemModelProvider(Ref.ID, Ref.NAME.concat(" Item Models")));
        AntimatterDynamics.clientProvider(Ref.SHARED_ID,
                () -> new AntimatterBlockStateProvider(Ref.SHARED_ID, "Antimatter Shared BlockStates"));
        AntimatterDynamics.clientProvider(Ref.SHARED_ID,
                () -> new AntimatterItemModelProvider(Ref.SHARED_ID, "Antimatter Shared Item Models"));
        AntimatterDynamics.clientProvider(Ref.ID,
                () -> new AntimatterLanguageProvider(Ref.ID, Ref.NAME.concat(" en_us Localization"), "en_us"));
        AntimatterDynamics.clientProvider(Ref.SHARED_ID,
                () -> new AntimatterLanguageProvider(Ref.SHARED_ID, Ref.NAME.concat(" en_us Localization (Shared)"), "en_us"));
        AntimatterAPI.init();
        AntimatterNetwork.register();
        AntimatterConfig.createConfig();
    }

    public void addCraftingLoaders(CraftingEvent ev) {
        ev.addLoader(StoneRecipes::loadRecipes);
        ev.addLoader(MaterialRecipes::init);
    }

    public void providers(ProvidersEvent ev) {
        final AntimatterBlockTagProvider[] p = new AntimatterBlockTagProvider[1];
        ev.addProvider(Ref.ID, () -> {
            p[0] = new AntimatterBlockTagProvider(Ref.ID, Ref.NAME.concat(" Block Tags"), false);
            return p[0];
        });
        ev.addProvider(Ref.SHARED_ID, () -> new AntimatterFluidTagProvider(Ref.SHARED_ID,
                "Antimatter Shared Fluid Tags", false));
        ev.addProvider(Ref.ID, () -> new AntimatterItemTagProvider(Ref.ID, Ref.NAME.concat(" Item Tags"),
                false, p[0]));
        ev.addProvider(Ref.ID,
                () -> new AntimatterBlockLootProvider(Ref.ID, Ref.NAME.concat(" Loot generator")));
        ev.addProvider(Ref.ID, () -> new AntimatterTagProvider<Biome>(BuiltinRegistries.BIOME, Ref.ID, Ref.NAME.concat(" Biome Tags"), "worldgen/biome") {
            @Override
            protected void processTags(String domain) {
                this.tag(TagUtils.getBiomeTag(new ResourceLocation("is_desert"))).add(Biomes.DESERT);
                this.tag(TagUtils.getBiomeTag(new ResourceLocation("is_plains"))).add(Biomes.PLAINS);
                this.tag(TagUtils.getBiomeTag(new ResourceLocation("is_savanna"))).add(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA);
                this.tag(TagUtils.getBiomeTag(new ResourceLocation("is_swamp"))).add(Biomes.SWAMP);
            }
        });
    }

    @Override
    public void onRegistrationEvent(RegistrationEvent event, Side side) {
        if (event == RegistrationEvent.DATA_INIT) {
            Recipe.init();

            SlotType.init();
            RecipeBuilders.init();
            MachineState.init();
            AntimatterMaterials.init();
            AntimatterMaterialTypes.init();
            AntimatterDefaultTools.init(side);
            AntimatterStoneTypes.init();
            Data.init(side);
            ICover.init();
            SubTag.init();
            AntimatterWorldGenerator.preinit();
            GuiEvents.init();
            MaterialSerializer.init();
            ContainerItemShapedRecipe.init();
            ContainerItemShapelessRecipe.init();
            AntimatterRecipeSerializer.init();
            IngredientSerializer.init();
            PropertyIngredient.Serializer.init();
        } else if (event == RegistrationEvent.WORLDGEN_INIT) {
            AntimatterWorldGenerator.init();
            AntimatterDefaultTools.postInit();
        } else if (event == RegistrationEvent.DATA_READY) {
            CauldronInteractions.init();
            if (AntimatterAPI.isModLoaded(Ref.MOD_JEI) || AntimatterAPI.isModLoaded(Ref.MOD_REI)){
                AntimatterJEIREIPlugin.registerMissingMaps();
            }
            AntimatterJEIREIPlugin.addItemsToHide(l -> {
                if (!AntimatterConfig.SHOW_ALL_ORES.get()){
                    AntimatterAPI.all(StoneType.class, s -> {
                        if (s != AntimatterStoneTypes.STONE && s != AntimatterStoneTypes.SAND && s.doesGenerateOre()){
                            AntimatterMaterialTypes.ORE.all().forEach(m -> {
                                Block ore = AntimatterMaterialTypes.ORE.get().get(m, s).asBlock();
                                if (ore instanceof BlockOre){
                                    l.add(ore);
                                }
                            });
                            AntimatterMaterialTypes.ORE_SMALL.all().forEach(m -> {
                                Block ore = AntimatterMaterialTypes.ORE_SMALL.get().get(m, s).asBlock();
                                if (ore instanceof BlockOre){
                                    l.add(ore);
                                }
                            });
                        }
                    });

                }
                if (!AntimatterConfig.SHOW_ROCKS.get()){
                    AntimatterMaterialTypes.BEARING_ROCK.all().forEach(m -> {
                        AntimatterAPI.all(StoneType.class, s -> {
                            if (s.doesGenerateOre() && s != BEDROCK) {
                                l.add(AntimatterMaterialTypes.BEARING_ROCK.get().get(m, s).asBlock());
                            }
                        });
                    });
                }
                AntimatterAPI.all(MaterialTypeItem.class, t -> {
                    if (!t.hidden()) return;
                    List<ItemLike> stacks = (List<ItemLike>) t.all().stream().map(obj -> t.get((Material)obj)).collect(Collectors.toList());
                    if (stacks.isEmpty()) return;
                    l.addAll(stacks);
                });
                AntimatterAPI.all(IAntimatterTool.class).stream().filter(t -> t.getAntimatterToolType() == AntimatterDefaultTools.WRENCH_ALT).forEach(tool -> l.add(tool.getItem()));
                AntimatterAPI.all(AntimatterFluid.class).forEach(t -> l.add(t.getFluidBlock()));
            });
            AntimatterAPI.all(Material.class).forEach(m -> {
                Map<MaterialType<?>, Integer> map = MaterialTags.FURNACE_FUELS.getMap(m);
                if (map != null){
                    map.forEach((t, i) -> {
                        if (t instanceof MaterialTypeItem<?> typeItem){
                            AntimatterPlatformUtils.setBurnTime(typeItem.get(m), i);
                        } else if (t instanceof MaterialTypeBlock<?> typeBlock && typeBlock.get() instanceof MaterialTypeBlock.IBlockGetter blockGetter){
                            AntimatterPlatformUtils.setBurnTime(blockGetter.get(m).asItem(), i);
                        }
                    });
                }

            });
        } else if (event == RegistrationEvent.CLIENT_DATA_INIT){
            AntimatterModelManager.init();
            ClientData.init();
            if (AntimatterConfig.OVERRIDE_BASALT_TEXTURE.get()){
                try {
                    AntimatterDynamics.DYNAMIC_RESOURCE_PACK.addTexture(new ResourceLocation("block/basalt_top"), readImage("block/stone/basalt/stone"));
                    AntimatterDynamics.DYNAMIC_RESOURCE_PACK.addTexture(new ResourceLocation("block/basalt_side"), readImage("block/stone/basalt/stone"));
                    AntimatterDynamics.DYNAMIC_RESOURCE_PACK.addTexture(new ResourceLocation("block/smooth_basalt"), readImage("block/stone/basalt/smooth"));
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private static BufferedImage readImage(String imagePath) throws IOException {
        InputStream in = Antimatter.class.getResourceAsStream("/assets/" + Ref.ID + "/textures/" + imagePath + ".png");
        return ImageIO.read(in);
    }

    @Override
    public String getId() {
        return Ref.ID;
    }
}
