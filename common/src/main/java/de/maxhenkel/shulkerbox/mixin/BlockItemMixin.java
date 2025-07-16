package de.maxhenkel.shulkerbox.mixin;

import de.maxhenkel.shulkerbox.AdvancedShulkerboxesMod;
import de.maxhenkel.shulkerbox.menu.AdvancedShulkerboxMenu;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.item.BlockItem.getBlockEntityData;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Shadow private final Block block;

    protected BlockItemMixin(Block block) {
        this.block = block;
    }


    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    public void useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(getBlock() instanceof ShulkerBoxBlock)) {
            return;
        }
        //TODO Check for fake players
        //TODO Check if this works client side
        if (!(AdvancedShulkerboxesMod.CONFIG.sneakPlace.get() ^ context.getPlayer().isShiftKeyDown())) {
            return;
        }
        if (context.getItemInHand().getCount() != 1) {
            return;
        }

        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
            AdvancedShulkerboxMenu.open(serverPlayer, context.getItemInHand());
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = "onDestroyed", at = @At(value = "TAIL"))
    public void onDestroyed(ItemEntity itemEntity, CallbackInfo ci) {
        if (this.block instanceof ShulkerBoxBlock) {
            ItemStack itemStack = itemEntity.getItem();
            CompoundTag compoundTag = getBlockEntityData(itemStack);
            if (compoundTag != null && compoundTag.contains("LootTable")) {
                LootTable lootTable = itemEntity.level.getServer().getLootTables().get(new ResourceLocation(compoundTag.getString("LootTable")));
                LootContext.Builder builder = (new LootContext.Builder((ServerLevel) itemEntity.level))
                        .withParameter(LootContextParams.ORIGIN, itemEntity.position())
                        .withOptionalRandomSeed(compoundTag.getLong("LootTableSeed"));

                ObjectArrayList<ItemStack> objectArrayList = lootTable.getRandomItems(builder.create(LootContextParamSets.CHEST));
                NonNullList<ItemStack> items = NonNullList.withSize(objectArrayList.size(), ItemStack.EMPTY);
                for (int i = 0; i < objectArrayList.size(); i++) {
                    items.set(i, objectArrayList.get(i));
                }
                ContainerHelper.saveAllItems(compoundTag, items);
                ListTag listTag = compoundTag.getList("Items", 10);
                ItemUtils.onContainerDestroyed(itemEntity, listTag.stream().map(CompoundTag.class::cast).map(ItemStack::of));
            }
        }
    }

    @Shadow
    public abstract Block getBlock();

}
