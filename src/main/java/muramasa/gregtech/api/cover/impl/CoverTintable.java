package muramasa.gregtech.api.cover.impl;

import muramasa.gregtech.api.cover.Cover;
import muramasa.gregtech.client.render.ModelUtils;
import net.minecraft.client.renderer.block.model.BakedQuad;

import java.util.List;

public abstract class CoverTintable extends Cover {

    public static final int TINTED_COVER_LAYER = 5;

    public abstract int getRGB();

    @Override
    public List<BakedQuad> onRender(List<BakedQuad> quads, int side) {
        return ModelUtils.tint(quads, getRGB());
    }
}
