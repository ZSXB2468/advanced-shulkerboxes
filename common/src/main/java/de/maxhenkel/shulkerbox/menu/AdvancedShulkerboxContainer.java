package de.maxhenkel.shulkerbox.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import javax.annotation.Nullable;

public class AdvancedShulkerboxContainer implements Container {

    protected NonNullList<ItemStack> items;
    protected ItemStack shulkerBox;
    protected int invSize;

    public AdvancedShulkerboxContainer(@Nullable ServerPlayer player, ItemStack shulkerBox, int invSize) {
        this.shulkerBox = shulkerBox;
        this.invSize = invSize;
        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);

        CompoundTag compoundTag = BlockItem.getBlockEntityData(shulkerBox);
        if (compoundTag != null) {
            if (compoundTag.contains("LootTable")) {
                setChanged();
                fillWithLoot(player, compoundTag);
            }

            if (compoundTag.contains("Items", 9)) {
                ContainerHelper.loadAllItems(compoundTag, items);
            }
        }
    }

    public void fillWithLoot(@Nullable ServerPlayer player, CompoundTag compoundTag) {
        if (player == null || !compoundTag.contains("LootTable")) {
            return;
        }
        LootTable lootTable = player.server.getLootTables().get(new ResourceLocation(compoundTag.getString("LootTable")));
        LootContext.Builder builder = (new LootContext.Builder((ServerLevel) player.level))
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withOptionalRandomSeed(compoundTag.getLong("LootTableSeed"))
                .withLuck(player.getLuck())
                .withParameter(LootContextParams.THIS_ENTITY, player);

        lootTable.fill(this, builder.create(LootContextParamSets.CHEST));
        setChanged();
    }

    @Override
    public int getContainerSize() {
        return invSize;
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack itemstack = ContainerHelper.removeItem(items, index, count);
        setChanged();
        return itemstack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = ContainerHelper.takeItem(items, index);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setChanged() {
        // 使用 NBT 保存物品数据
        CompoundTag compoundTag = new CompoundTag();
        ContainerHelper.saveAllItems(compoundTag, items);
        BlockItem.setBlockEntityData(shulkerBox, BlockEntityType.SHULKER_BOX, compoundTag);
    }

    @Override
    public void startOpen(Player player) {
        player.level.playSound(null, player.getX(), player.getY(), player.getZ(), getOpenSound(), SoundSource.BLOCKS, 0.5F, getVariatedPitch(player.level));
    }

    @Override
    public void stopOpen(Player player) {
        setChanged();
        player.level.playSound(null, player.getX(), player.getY(), player.getZ(), getCloseSound(), SoundSource.BLOCKS, 0.5F, getVariatedPitch(player.level));
    }

    protected static float getVariatedPitch(Level world) {
        return world.random.nextFloat() * 0.1F + 0.9F;
    }

    protected SoundEvent getOpenSound() {
        return SoundEvents.SHULKER_BOX_OPEN;
    }

    protected SoundEvent getCloseSound() {
        return SoundEvents.SHULKER_BOX_CLOSE;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().contains(shulkerBox);
    }
}
