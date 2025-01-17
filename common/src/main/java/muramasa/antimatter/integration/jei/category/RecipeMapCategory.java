package muramasa.antimatter.integration.jei.category;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.botarium.common.fluid.base.FluidHolder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import muramasa.antimatter.Data;
import muramasa.antimatter.gui.BarDir;
import muramasa.antimatter.gui.GuiData;
import muramasa.antimatter.gui.SlotData;
import muramasa.antimatter.gui.SlotType;
import muramasa.antimatter.integration.jei.AntimatterJEIPlugin;
import muramasa.antimatter.integration.jeirei.renderer.IRecipeInfoRenderer;
import muramasa.antimatter.machine.Tier;
import muramasa.antimatter.recipe.IRecipe;
import muramasa.antimatter.recipe.ingredient.FluidIngredient;
import muramasa.antimatter.recipe.ingredient.RecipeIngredient;
import muramasa.antimatter.recipe.map.IRecipeMap;
import muramasa.antimatter.recipe.map.RecipeMap;
import muramasa.antimatter.util.AntimatterPlatformUtils;
import muramasa.antimatter.util.Utils;
import muramasa.antimatter.util.int4;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import tesseract.FluidPlatformUtils;
import tesseract.TesseractGraphWrappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static muramasa.antimatter.integration.jeirei.AntimatterJEIREIPlugin.intToSuperScript;

public class RecipeMapCategory implements IRecipeCategory<IRecipe> {

    protected static int JEI_OFFSET_X = 1, JEI_OFFSET_Y = 1;
    protected static IGuiHelper guiHelper;

    protected String title;
    protected final ResourceLocation loc;
    protected final RecipeType<IRecipe> type;
    protected IDrawable background, icon, progressBackground;
    protected IDrawableAnimated progressBar;
    protected GuiData gui;
    protected Tier guiTier;
    private final IRecipeInfoRenderer infoRenderer;

    public RecipeMapCategory(IRecipeMap map, RecipeType<IRecipe> type, GuiData gui, Tier defaultTier, ResourceLocation iconId) {
        loc = map.getLoc();
        this.type = type;
        this.guiTier = map.getGuiTier() == null ? defaultTier : map.getGuiTier();
        title = map.getDisplayName().getString();
        int4 area = gui.getArea(), progress = new int4(0, gui.getMachineData().getProgressSize().y, gui.getMachineData().getProgressSize().x, gui.getMachineData().getProgressSize().y);
        background = guiHelper.drawableBuilder(gui.getTexture(guiTier, "machine"), area.x, area.y, area.z, area.w).addPadding(0, (map.getInfoRenderer().getRows() <= 0 ? 0 : 7 + (10 *map.getInfoRenderer().getRows())), 0, 0).build();
        progressBar = guiHelper.drawableBuilder(gui.getMachineData().getProgressTexture(this.guiTier), progress.x, progress.y, progress.z, progress.w).setTextureSize(progress.z, progress.w * 2).buildAnimated(50, fromDir(gui.getMachineData().getDir()), !gui.getMachineData().doesBarFill());
        progressBackground = guiHelper.drawableBuilder(gui.getMachineData().getProgressTexture(this.guiTier), 0, 0, progress.z, progress.w).setTextureSize(progress.z, progress.w * 2).build();
        Object icon = map.getIcon();
        if (icon != null) {
            if (icon instanceof ItemStack itemStack) {
                this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, itemStack);
            }
            if (icon instanceof ItemLike item) {
                this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(item));
            }
        } else {
            Item item = iconId == null ? Data.DEBUG_SCANNER : AntimatterPlatformUtils.getItemFromID(iconId);
            if (item == Items.AIR) item = Data.DEBUG_SCANNER;
            this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(item, 1));
        }
        this.gui = gui;
        this.infoRenderer = map.getInfoRenderer();
    }

    private IDrawableAnimated.StartDirection fromDir(BarDir dir){
        return switch (dir){
            case TOP -> IDrawableAnimated.StartDirection.TOP;
            case BOTTOM -> IDrawableAnimated.StartDirection.BOTTOM;
            case LEFT -> IDrawableAnimated.StartDirection.LEFT;
            case RIGHT -> IDrawableAnimated.StartDirection.RIGHT;
        };
    }

    @Override
    public RecipeType<IRecipe> getRecipeType() {
        return type;
    }

    @Override
    public ResourceLocation getUid() {
        return loc;
    }

    @Override
    public Component getTitle() {
        return Utils.literal(title);
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }


    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IRecipe recipe, IFocusGroup focuses) {
        List<List<ItemStack>> inputs = recipe.hasInputItems() ? recipe.getInputItems().stream().map(t -> Arrays.asList(t.getItems())).toList() : Collections.emptyList();
        List<ItemStack> outputs = recipe.hasOutputItems() ? Arrays.stream(recipe.getOutputItems(false)).toList() : Collections.emptyList();
        List<SlotData<?>> slots;
        int groupIndex = 0, slotCount;
        int offsetX = gui.getArea().x + JEI_OFFSET_X, offsetY = gui.getArea().y + JEI_OFFSET_Y;
        int inputItems = 0, inputFluids = 0;
        if (recipe.hasInputItems()) {
            slots = gui.getSlots().getSlots(SlotType.IT_IN, guiTier);
            slotCount = slots.size();
            if (slotCount > 0) {
                int s = 0;
                if (inputs.size() > 0) {
                    slotCount = Math.min(slotCount, inputs.size());
                    for (; s < slotCount; s++) {
                        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, slots.get(s).getX() - (offsetX - 1), slots.get(s).getY() - (offsetY - 1));
                        List<ItemStack> input = inputs.get(s);
                        if (input.size() == 0) {
                            List<ItemStack> st = new ObjectArrayList<>(1);
                            st.add(new ItemStack(Data.DEBUG_SCANNER, 1));
                            slot.addIngredients(VanillaTypes.ITEM_STACK, st);
                        } else {
                            slot.addIngredients(VanillaTypes.ITEM_STACK, input);
                            final int ss = s;
                            slot.addTooltipCallback((ing, list) -> {
                                if (recipe.getInputItems().get(ss) instanceof RecipeIngredient ri) {
                                    if (ri.ignoreConsume()) {
                                        list.add(Utils.literal("Does not get consumed in the process.").withStyle(ChatFormatting.WHITE));
                                    }
                                    if (ri.ignoreNbt()) {
                                        list.add(Utils.literal("Ignores NBT.").withStyle(ChatFormatting.WHITE));
                                    }
                                    Ingredient i = recipe.getInputItems().get(ss);
                                    if (RecipeMap.isIngredientSpecial(i)) {
                                        list.add(Utils.literal("Special ingredient. Class name: ").withStyle(ChatFormatting.GRAY).append(Utils.literal(i.getClass().getSimpleName()).withStyle(ChatFormatting.GOLD)));
                                    }
                                }
                                if (recipe.hasInputChances()) {
                                    if (recipe.getInputChances()[ss] < 10000) {
                                        list.add(Utils.literal("Consumption Chance: " + ((float)recipe.getInputChances()[ss] / 100) + "%").withStyle(ChatFormatting.WHITE));
                                    }
                                }
                            });
                            inputItems++;
                        }
                    }
                }
            }
        }
        if (recipe.hasOutputItems()) {
            slots = gui.getSlots().getSlots(SlotType.IT_OUT, guiTier);
            slotCount = slots.size();
            if (slotCount > 0) {
                slotCount = Math.min(slotCount, outputs.size());
                for (int s = 0; s < slotCount; s++) {
                    IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, slots.get(s).getX() - (offsetX - 1), slots.get(s).getY() - (offsetY - 1));
                    slot.addIngredient(VanillaTypes.ITEM_STACK, outputs.get(s));
                    final int ss = s;
                    slot.addTooltipCallback((ing, list) -> {
                        if (recipe.hasOutputChances()) {
                            if (recipe.getOutputChances()[ss] < 10000) {
                                list.add(Utils.literal("Output Chance: " + ((float)recipe.getOutputChances()[ss] / 100) + "%").withStyle(ChatFormatting.WHITE));
                            }
                        }
                    });
                }
            }
        }

        if (recipe.hasInputFluids()) {
            slots = gui.getSlots().getSlots(SlotType.FL_IN, guiTier);
            slotCount = slots.size();
            if (slotCount > 0) {
                List<FluidIngredient> fluids = recipe.getInputFluids();
                slotCount = Math.min(slotCount, fluids.size());
                for (int s = 0; s < slotCount; s++) {
                    IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, slots.get(s).getX() - (offsetX - 1), slots.get(s).getY() - (offsetY - 1));
                    AntimatterJEIPlugin.addFluidIngredients(slot, Arrays.asList(fluids.get(s).getStacks()));
                    slot.setFluidRenderer((int)fluids.get(s).getAmount(), true, 16, 16);
                    int finalS = s;
                    slot.addTooltipCallback((ing, list) -> {
                        FluidHolder stack = fluids.get(finalS).getStacks()[0];
                        createFluidTooltip(ing, list, stack);
                    });
                    inputFluids++;
                }
            }
        }
        if (recipe.hasOutputFluids()) {
            slots = gui.getSlots().getSlots(SlotType.FL_OUT, guiTier);
            slotCount = slots.size();
            if (slotCount > 0) {
                FluidHolder[] fluids = recipe.getOutputFluids();
                slotCount = Math.min(slotCount, fluids.length);
                for (int s = 0; s < slotCount; s++) {
                    IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, slots.get(s).getX() - (offsetX - 1), slots.get(s).getY() - (offsetY - 1));
                    slot.setFluidRenderer((int)fluids[s].getFluidAmount(), true, 16, 16);
                    AntimatterJEIPlugin.addFluidIngredients(slot, Collections.singletonList(fluids[s]));
                    int finalS = s;
                    slot.addTooltipCallback((ing, list) -> {
                        FluidHolder stack = fluids[finalS];
                        createFluidTooltip(ing, list, stack);
                    });
                }
            }
        }
    }

    private void createFluidTooltip(IRecipeSlotView ing, List<Component> list, FluidHolder stack) {
        Component component = list.get(2);
        list.remove(2);
        list.remove(1);
        long mb = (stack.getFluidAmount() / TesseractGraphWrappers.dropletMultiplier);
        if (AntimatterPlatformUtils.isFabric()){
            list.add(Utils.translatable("antimatter.tooltip.fluid.amount", Utils.literal(mb + " " + intToSuperScript(stack.getFluidAmount() % 81L) + "/₈₁ L")).withStyle(ChatFormatting.BLUE));
        } else {
            list.add(Utils.translatable("antimatter.tooltip.fluid.amount", mb + " L").withStyle(ChatFormatting.BLUE));
        }
        list.add(Utils.translatable("antimatter.tooltip.fluid.temp", FluidPlatformUtils.INSTANCE.getFluidTemperature(stack.getFluid())).withStyle(ChatFormatting.RED));
        String liquid = !FluidPlatformUtils.INSTANCE.isFluidGaseous(stack.getFluid()) ? "liquid" : "gas";
        list.add(Utils.translatable("antimatter.tooltip.fluid." + liquid).withStyle(ChatFormatting.GREEN));
        if (Utils.hasNoConsumeTag(AntimatterJEIPlugin.getIngredient(ing.getDisplayedIngredient().get())))
            list.add(Utils.literal("Does not get consumed in the process").withStyle(ChatFormatting.WHITE));
        list.add(component);
    }

    /*
    private static IRecipeSlotTooltipCallback itemCallback(Recipe recipe, boolean input) {
        return (a,b) ->
            if (input) {
                if (recipe.hasInputItems()) {
                    a.getDisplayedIngredient().flatMap(ing -> {
                        Ingredient i = ing.getIngredient();
                    })
                    if (recipe.getInputItems().get(index).ignoreConsume()) {
                        tooltip.add(Utils.literal("Does not get consumed in the process.").withStyle(ChatFormatting.WHITE));
                    }
                    if (recipe.getInputItems().size() >= index && recipe.getInputItems().get(index).ignoreNbt()) {
                        tooltip.add(Utils.literal("Ignores NBT.").withStyle(ChatFormatting.WHITE));
                    }
                    if (recipe.getInputItems().size() >= index) {
                        Ingredient i = recipe.getInputItems().get(index).get();
                        if (RecipeMap.isIngredientSpecial(i)) {
                            tooltip.add(Utils.literal("Special ingredient. Class name: ").withStyle(ChatFormatting.GRAY).append(Utils.literal(i.getClass().getSimpleName()).withStyle(ChatFormatting.GOLD)));
                        }
                    }
                }
            }
            if (recipe.hasChances() && !input) {
                int chanceIndex = index - finalInputItems;
                if (recipe.getChances()[chanceIndex] < 100) {
                    tooltip.add(Utils.literal("Chance: " + recipe.getChances()[chanceIndex] + "%").withStyle(ChatFormatting.WHITE));
                }
            }
        }
    }*/

    @Override
    public Class getRecipeClass() {
        return IRecipe.class;
    }

    @Override
    public void draw(IRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
        if (progressBackground != null){
            progressBackground.draw(stack, gui.getMachineData().getProgressPos().x + gui.getArea().x, gui.getMachineData().getProgressPos().y + gui.getArea().y);
        }
        if (progressBar != null)
            progressBar.draw(stack, gui.getMachineData().getProgressPos().x + gui.getArea().x, gui.getMachineData().getProgressPos().y + gui.getArea().y);
        gui.getSlots().getRecipeSlots(this.guiTier).forEach(s -> {
            IDrawable drawable = guiHelper.drawableBuilder(s.getTexture(), 0, 0, 18, 18).setTextureSize(18, 18).build();
            drawable.draw(stack, s.getX() - 4,s.getY() - 4);
        });
        infoRenderer.render(stack, recipe, Minecraft.getInstance().font, JEI_OFFSET_X, gui.getArea().y + JEI_OFFSET_Y + gui.getArea().z / 2);

        int offsetX = gui.getArea().x + JEI_OFFSET_X, offsetY = gui.getArea().y + JEI_OFFSET_Y;
        //Draw chance overlay.
        if (recipe.hasOutputChances()) {
            List<IRecipeSlotView> views = recipeSlotsView.getSlotViews(RecipeIngredientRole.OUTPUT);
            List<SlotData<?>> slots = gui.getSlots().getSlots(SlotType.IT_OUT, guiTier);
            for (int i = 0; i < recipe.getOutputChances().length; i++) {
                if (recipe.getOutputChances()[i] < 10000) {
                    if (i >= slots.size()) break;
                    RenderSystem.disableBlend();
                    RenderSystem.disableDepthTest();
                    stack.pushPose();
                    stack.scale(0.5f, 0.5f, 1);
                    String ch = (recipe.getOutputChances()[i] / 100) + "%";
                    Minecraft.getInstance().font.drawShadow(stack, ch, 2*((float)slots.get(i).getX() - (offsetX - 1)), 2*((float) slots.get(i).getY() - (offsetY - 1)), 0xFFFF00);

                    stack.popPose();
                    RenderSystem.enableBlend();
                    RenderSystem.enableDepthTest();
                }
            }
        }
        if (recipe.hasInputChances()) {
            List<IRecipeSlotView> views = recipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT);
            List<SlotData<?>> slots = gui.getSlots().getSlots(SlotType.IT_IN, guiTier);
            for (int i = 0; i < recipe.getInputChances().length; i++) {
                if (recipe.getInputChances()[i] < 10000) {
                    if (i >= slots.size()) break;
                    RenderSystem.disableBlend();
                    RenderSystem.disableDepthTest();
                    stack.pushPose();
                    stack.scale(0.5f, 0.5f, 1);
                    String ch = (recipe.getInputChances()[i] / 100) + "%";
                    Minecraft.getInstance().font.drawShadow(stack, ch, 2*((float)slots.get(i).getX() - (offsetX - 1)), 2*((float) slots.get(i).getY() - (offsetY - 1)), 0xFFFF00);

                    stack.popPose();
                    RenderSystem.enableBlend();
                    RenderSystem.enableDepthTest();
                }
            }
        }
    }

    public static void setGuiHelper(IGuiHelper helper) {
        guiHelper = helper;
    }


}
