package muramasa.antimatter.tool;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import muramasa.antimatter.AntimatterAPI;
import muramasa.antimatter.Ref;
import muramasa.antimatter.behaviour.IBehaviour;
import muramasa.antimatter.behaviour.IDestroySpeed;
import muramasa.antimatter.capability.energy.ItemEnergyHandler;
import muramasa.antimatter.data.AntimatterDefaultTools;
import muramasa.antimatter.item.IContainerItem;
import muramasa.antimatter.material.Material;
import muramasa.antimatter.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tesseract.TesseractCapUtils;
import tesseract.api.context.TesseractItemContext;
import tesseract.api.gt.IEnergyHandlerItem;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static muramasa.antimatter.data.AntimatterDefaultTools.KNIFE;

//@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MaterialTool extends DiggerItem implements IAntimatterTool, IContainerItem {

    protected final String domain;
    protected final AntimatterToolType type;
    protected final AntimatterItemTier itemTier;

    /**
     * -- GETTER --
     *  Returns -1 if its not a powered tool
     */
    @Getter
    protected final int energyTier;
    protected final long maxEnergy;

    public MaterialTool(String domain, AntimatterToolType type, AntimatterItemTier tier, Properties properties) {
        super(type.getBaseAttackDamage(), type.getBaseAttackSpeed(), tier, type.getToolType(), properties);
        this.domain = domain;
        this.type = type;
        this.itemTier = tier;
        this.energyTier = -1;
        this.maxEnergy = -1;
        AntimatterAPI.register(IAntimatterTool.class, this);
    }

    public MaterialTool(String domain, AntimatterToolType type, AntimatterItemTier tier, Properties properties, int energyTier) {
        super(type.getBaseAttackDamage(), type.getBaseAttackSpeed(), tier, type.getToolType(), properties);
        this.domain = domain;
        this.type = type;
        this.itemTier = tier;
        this.energyTier = energyTier;
        this.maxEnergy = type.getBaseMaxEnergy() * energyTier;
        AntimatterAPI.register(IAntimatterTool.class, this);
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public String getId() {
        if (type.isSimple()) return type.isPowered() ? String.join("_", itemTier.getPrimary().getId(), type.getId(), Ref.VN[energyTier].toLowerCase(Locale.ENGLISH)) : String.join("_", itemTier.getPrimary().getId(),type.getId());;
        return type.isPowered() ? String.join("_", type.getId(), Ref.VN[energyTier].toLowerCase(Locale.ENGLISH)) : type.getId();
    }

    @NotNull
    @Override
    public AntimatterToolType getAntimatterToolType() {
        return type;
    }

    @Override
    public AntimatterItemTier getAntimatterItemTier() {
        return itemTier;
    }

    /*
    @NotNull
    @Override
    public Set<Tag<Block>> getToolTypes(ItemStack stack) {
        return getToolTypes();
    }*/

    @NotNull
    @Override
    public ItemStack asItemStack(@NotNull Material primary, @NotNull Material secondary) {
        return resolveStack(primary, secondary, 0, maxEnergy);
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> list) {
        onGenericFillItemGroup(group, list, maxEnergy);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return Utils.doesStackHaveToolTypes(stack, AntimatterDefaultTools.WRENCH, AntimatterDefaultTools.SCREWDRIVER, AntimatterDefaultTools.CROWBAR, AntimatterDefaultTools.WIRE_CUTTER); // ???
    }

    //fabric method
    public boolean isSuitableFor(ItemStack stack, BlockState state) {
        return this.genericIsCorrectToolForDrops(stack, state);
    }

    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state){
        return genericIsCorrectToolForDrops(stack, state);
    }

    @Override
    public void onUseTick(Level p_41428_, LivingEntity p_41429_, ItemStack p_41430_, int p_41431_) {
        super.onUseTick(p_41428_, p_41429_, p_41430_, p_41431_);
    }



    /*
    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        return getPrimaryMaterial(stack).getDisplayName().appendSibling(new StringTextComponent(type.getId()));
    }
     */

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        onGenericAddInformation(stack, tooltip, flag);
        super.appendHoverText(stack, world, tooltip, flag);
    }

    //TODO figure out why I wrote the below todo
    //TODO figure this out
    //@Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return type.getUseAction();
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return type.getUseAction() == UseAnim.NONE ? super.getUseDuration(stack) : 72000;
    }

    /*
    @Override
    public boolean canHarvestBlock(ItemStack stack, BlockState state) {
        return Utils.isToolEffective(this, state) && getTier(stack).getLevel() >= state.getHarvestLevel();
    }*/

    /*
    @Override
    public int getHarvestLevel(ItemStack stack, ToolType tool, @Nullable Player player, @Nullable BlockState blockState) {
        return getToolTypes().contains(tool) ? getTier(stack).getLevel() : -1;
    }*/

    @Override
    public int getMaxDamage(ItemStack stack) {
        return (int) (getTier(stack).getUses() * getAntimatterToolType().getDurabilityMultiplier());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return onGenericHitEntity(stack, target, attacker, 0.75F, 0.75F);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float destroySpeed = genericIsCorrectToolForDrops(stack, state) ? getDefaultMiningSpeed(stack) : 1.0F;
        if (type.isPowered() && getCurrentEnergy(stack)  == 0){
            destroySpeed = 0.0f;
        }
        for (Map.Entry<String, IBehaviour<IBasicAntimatterTool>> e : getAntimatterToolType().getBehaviours().entrySet()) {
            IBehaviour<?> b = e.getValue();
            if (!(b instanceof IDestroySpeed destroySpeed1)) continue;
            float i = destroySpeed1.getDestroySpeed(this, destroySpeed, stack, state);
            if (i > 0){
                destroySpeed = i;
                break;
            }
        }
        return destroySpeed;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity entity) {
        return onGenericBlockDestroyed(stack, world, state, pos, entity);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return onGenericItemUse(ctx);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        return genericInteractLivingEntity(stack, player, interactionTarget, usedHand);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        InteractionResultHolder<ItemStack> result = onGenericRightclick(level, player, usedHand);
        if (result.getResult().shouldAwardStats()){
            return result;
        }
        return super.use(level, player, usedHand);
    }

    public void handleRenderHighlight(Player entity, LevelRenderer levelRenderer, Camera camera, HitResult target, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource) {
        onGenericHighlight(entity, levelRenderer, camera, target, partialTicks, poseStack, multiBufferSource);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player player) {
        return type.getBlockBreakability();
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return type.getToolTypes().contains(BlockTags.MINEABLE_WITH_AXE);
    }

    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slotType, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        if (slotType == EquipmentSlot.MAINHAND) {
            modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", type.getBaseAttackDamage() + getTier(stack).getAttackDamageBonus(), AttributeModifier.Operation.ADDITION));
            modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", type.getBaseAttackSpeed(), AttributeModifier.Operation.ADDITION));
        }
        return modifiers;
    }

    //fabric method
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, EquipmentSlot slotType) {
        return this.getAttributeModifiers(slotType, stack);
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        if (!type.isPowered()) {
            return amount;
        }
        if (entity instanceof Player && ((Player) entity).isCreative()) {
            return 0;
        }
        return damage(stack, amount);
    }
    @Override
    public int getEnchantability(ItemStack stack) {
        return getTier(stack).getEnchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return !type.isPowered() && getTier(toRepair).getRepairIngredient().test(repair);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (type.getBlacklistedEnchantments().contains(enchantment)) return false;
        if ((type.getToolTypes().contains(BlockTags.MINEABLE_WITH_AXE) || type == KNIFE) && enchantment.category == EnchantmentCategory.WEAPON) {
            return true;
        }
        return (!type.isPowered() || (enchantment != Enchantments.UNBREAKING && enchantment != Enchantments.MENDING)) && enchantment.category.canEnchant(stack.getItem());
    }

    public boolean hasContainerItem(ItemStack stack) {
        return type.hasContainer();
    }

    public ItemStack getContainerItem(ItemStack oldStack) {
        return getGenericContainerItem(oldStack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (type.isPowered()) return true;
        return super.isBarVisible(stack);
    }

    @Override
    public IEnergyHandlerItem createEnergyHandler(TesseractItemContext context) {
        return new ItemEnergyHandler(context, maxEnergy, 8 * (int) Math.pow(4, this.energyTier), 8 * (int) Math.pow(4, this.energyTier), 1, 1);
    }

    private Optional<ItemEnergyHandler> getCastedHandler(ItemStack stack) {
        return TesseractCapUtils.INSTANCE.getEnergyHandlerItem(stack).map(e -> (ItemEnergyHandler) e);
    }
}