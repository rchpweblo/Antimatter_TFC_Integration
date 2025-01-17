package muramasa.antimatter.client.event.forge;

import muramasa.antimatter.Ref;
import muramasa.antimatter.client.SoundHelper;
import muramasa.antimatter.client.event.ClientEvents;
import muramasa.antimatter.material.MaterialType;
import muramasa.antimatter.tool.IAntimatterTool;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.DrawSelectionEvent.HighlightBlock;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public class ClientEventsForge {

    @SubscribeEvent
    public static void onBlockHighlight(HighlightBlock event) {
        if (ClientEvents.onBlockHighlight(event.getLevelRenderer(), event.getCamera(), event.getTarget(), event.getPartialTicks(), event.getPoseStack(), event.getMultiBufferSource()))
            event.setCanceled(true);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    protected static void onTooltipAdd(final ItemTooltipEvent ev) {
        MaterialType.addTooltip(ev.getItemStack(), ev.getToolTip(), ev.getPlayer(), ev.getFlags());
        ClientEvents.onItemTooltip(ev.getItemStack(), ev.getToolTip(), ev.getPlayer(), ev.getFlags());
    }

    //TODO why is this client only?
    //Needs some work, won't work in 3rd person also, needs special ItemModel properties
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            Player player = e.player;
            if (player == null || player.getMainHandItem().isEmpty()) return;
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof IAntimatterTool)) return;
            IAntimatterTool item = (IAntimatterTool) stack.getItem();
            if (item.getAntimatterToolType().getUseAction() != UseAnim.NONE && player.swinging) {
                item.getItem().onUsingTick(stack, player, stack.getCount());
                //player.swingProgress = player.prevSwingProgress;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderDebugInfo(RenderGameOverlayEvent.Text e) {
        ClientEvents.onRenderDebugInfo(e.getLeft());
    }

    @SubscribeEvent
    public static void onGuiMouseScrollPre(ScreenEvent.MouseScrollEvent.Pre e) {
        ClientEvents.onGuiMouseScrollPre(e.getScrollDelta());
    }
    @SubscribeEvent
    public static void onGuiMouseClickPre(ScreenEvent.MouseClickedEvent.Pre e) {
        ClientEvents.onGuiMouseClickPre(e.getButton());
    }

    @SubscribeEvent
    public static void onGuiMouseReleasedPre(ScreenEvent.MouseReleasedEvent.Pre e) {
        ClientEvents.onGuiMouseReleasedPre(e.getButton());
    }

    @SubscribeEvent
    public static void worldUnload(WorldEvent.Unload ev) {
        SoundHelper.worldUnload(ev.getWorld());
    }
}
