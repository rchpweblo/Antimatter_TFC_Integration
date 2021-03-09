package muramasa.antimatter.integration.jei.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import muramasa.antimatter.machine.Tier;
import muramasa.antimatter.recipe.Recipe;
import net.minecraft.client.gui.FontRenderer;

public class RecipeInfoRenderer implements IRecipeInfoRenderer {

    public final static RecipeInfoRenderer INSTANCE = new RecipeInfoRenderer();
    @Override
    public void render(MatrixStack stack, Recipe recipe, FontRenderer fontRenderer, int guiOffsetX, int guiOffsetY) {
        if (recipe.getDuration() == 0 && recipe.getPower() == 0) return;
        String power = "Duration: " + recipe.getDuration() + " ticks";
        String euT = "EU/t: " + recipe.getPower();
        String amps = "Amps: " + recipe.getAmps();
        Tier tier = Tier.getTier((int) (recipe.getPower()/recipe.getAmps()));
        String formattedText = " (" + tier.getId().toUpperCase() + ")";
        renderString(stack, power,fontRenderer, 5, 5,guiOffsetX,guiOffsetY);
        renderString(stack, euT,fontRenderer, 5, 15,guiOffsetX,guiOffsetY);
        renderString(stack, formattedText, fontRenderer,5+stringWidth(euT,fontRenderer), 15,Tier.EV.getRarityFormatting().getColor(), guiOffsetX,guiOffsetY);
        renderString(stack, amps,fontRenderer, 5, 25,guiOffsetX,guiOffsetY);
    }
}