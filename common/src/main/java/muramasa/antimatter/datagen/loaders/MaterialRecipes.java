package muramasa.antimatter.datagen.loaders;

import com.google.common.collect.ImmutableMap;
import muramasa.antimatter.Ref;
import muramasa.antimatter.data.AntimatterDefaultTools;
import muramasa.antimatter.data.AntimatterMaterialTypes;
import muramasa.antimatter.data.AntimatterMaterials;
import muramasa.antimatter.datagen.builder.AntimatterCookingRecipeBuilder;
import muramasa.antimatter.datagen.providers.AntimatterRecipeProvider;
import muramasa.antimatter.material.Material;
import muramasa.antimatter.material.MaterialTags;
import muramasa.antimatter.material.MaterialType;
import muramasa.antimatter.material.MaterialTypeItem;
import muramasa.antimatter.recipe.ingredient.RecipeIngredient;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static muramasa.antimatter.data.AntimatterMaterialTypes.INGOT;
import static muramasa.antimatter.material.MaterialTags.*;

public class MaterialRecipes {
    public static void init(Consumer<FinishedRecipe> consumer, AntimatterRecipeProvider provider) {
        final CriterionTriggerInstance in = provider.hasSafeItem(AntimatterDefaultTools.WRENCH.getTag());
        AntimatterMaterialTypes.DUST.all().forEach(m -> {
            provider.addStackRecipe(consumer, Ref.ID, m.getId() + "_dust_small", "antimatter_dusts", AntimatterMaterialTypes.DUST.get(m, 1), of('D', AntimatterMaterialTypes.DUST_SMALL.getMaterialTag(m)), "DD", "DD");
            provider.addStackRecipe(consumer, Ref.ID, m.getId() + "_dust_tiny", "antimatter_dusts", AntimatterMaterialTypes.DUST.get(m, 1), of('D', AntimatterMaterialTypes.DUST_TINY.getMaterialTag(m)), "DDD", "DDD", "DDD");
        });
        AntimatterMaterialTypes.INGOT.all().forEach(m -> {
            if (m.has(AntimatterMaterialTypes.NUGGET) && m != AntimatterMaterials.Iron && m != AntimatterMaterials.Gold){
                provider.addItemRecipe(consumer, Ref.ID, m.getId() + "_ingot", "ingots", AntimatterMaterialTypes.INGOT.get(m), ImmutableMap.of('I', AntimatterMaterialTypes.NUGGET.getMaterialTag(m)), "III", "III", "III");
                provider.shapeless(consumer,"nugget_" + m.getId() + "_from_ingot", "ingots", AntimatterMaterialTypes.NUGGET.get(m, 9), AntimatterMaterialTypes.INGOT.getMaterialTag(m));
            }
        });
        AntimatterMaterialTypes.RAW_ORE.all().stream().filter(m -> !m.has(HAS_CUSTOM_SMELTING) && SMELT_INTO.getMapping(m).has(AntimatterMaterialTypes.INGOT)).forEach(m -> {
            if (m != AntimatterMaterials.Iron && m != AntimatterMaterials.Copper && m != AntimatterMaterials.Gold) {
                addSmeltingRecipe(consumer, provider, AntimatterMaterialTypes.RAW_ORE, AntimatterMaterialTypes.INGOT, 1, m, SMELT_INTO.getMapping(m));
            }
            addSmeltingRecipe(consumer, provider, AntimatterMaterialTypes.ORE, AntimatterMaterialTypes.INGOT, 1, m, SMELT_INTO.getMapping(m));
        });
        AntimatterMaterialTypes.CRUSHED.all().stream().filter(m -> !m.has(HAS_CUSTOM_SMELTING) && SMELT_INTO.getMapping(m).has(AntimatterMaterialTypes.INGOT)).forEach(m -> {
            if (m != SMELT_INTO.getMapping(m) || !m.has(AntimatterMaterialTypes.NUGGET)) return;
            addSmeltingRecipe(consumer, provider, AntimatterMaterialTypes.CRUSHED, AntimatterMaterialTypes.NUGGET, 12, m);
            addSmeltingRecipe(consumer, provider, AntimatterMaterialTypes.DUST_IMPURE, INGOT, 1, m);
            addSmeltingRecipe(consumer, provider, AntimatterMaterialTypes.CRUSHED_PURIFIED, AntimatterMaterialTypes.NUGGET, 11, m);
            addSmeltingRecipe(consumer, provider, AntimatterMaterialTypes.DUST_PURE, INGOT, 1, m);
            addSmeltingRecipe(consumer, provider, AntimatterMaterialTypes.CRUSHED_REFINED, AntimatterMaterialTypes.NUGGET, 10, m);
        });
        AntimatterMaterialTypes.DUST.all().forEach(m -> {
            if (m.has(MaterialTags.HAS_CUSTOM_SMELTING)) return;
            if (!DIRECT_SMELT_INTO.getMapping(m).has(AntimatterMaterialTypes.INGOT)) return;
            addSmeltingRecipe(consumer, provider, AntimatterMaterialTypes.DUST, AntimatterMaterialTypes.INGOT, 1, m, DIRECT_SMELT_INTO.getMapping(m));
        });
    }

    public static void addSmeltingRecipe(Consumer<FinishedRecipe> consumer, AntimatterRecipeProvider provider, MaterialType<?> input, MaterialTypeItem<?> output, int amount, Material in){
        addSmeltingRecipe(consumer, provider, input, output, amount, in, in);
    }

    public static void addSmeltingRecipe(Consumer<FinishedRecipe> consumer, AntimatterRecipeProvider provider, MaterialType<?> input, MaterialTypeItem<?> output, int amount, Material in, Material out){
        AntimatterCookingRecipeBuilder.blastingRecipe(RecipeIngredient.of(input.getMaterialTag(in), 1), new ItemStack(output.get(out), MaterialTags.SMELTING_MULTI.getInt(in) * amount), 2.0F, 100)
                .addCriterion("has_material_" + in.getId(), provider.hasSafeItem(output.getMaterialTag(out)))
                .build(consumer, provider.fixLoc(Ref.ID, in.getId().concat("_" + input.getId() + "_to_" + output.getId())));
        AntimatterCookingRecipeBuilder.smeltingRecipe(RecipeIngredient.of(input.getMaterialTag(in), 1), new ItemStack(output.get(out), MaterialTags.SMELTING_MULTI.getInt(in) * amount), 2.0F, 200)
                .addCriterion("has_material_" + in.getId(), provider.hasSafeItem(output.getMaterialTag(out)))
                .build(consumer, provider.fixLoc(Ref.ID, in.getId().concat("_" + input.getId() + "_to_" + output.getId() + "_smelting")));
    }
}
