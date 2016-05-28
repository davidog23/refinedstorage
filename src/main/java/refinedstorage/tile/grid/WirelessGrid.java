package refinedstorage.tile.grid;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import refinedstorage.RefinedStorage;
import refinedstorage.RefinedStorageUtils;
import refinedstorage.block.EnumGridType;
import refinedstorage.item.ItemWirelessGrid;
import refinedstorage.network.MessageWirelessGridCraftingStart;
import refinedstorage.network.MessageWirelessGridSettingsUpdate;
import refinedstorage.network.MessageWirelessGridStoragePull;
import refinedstorage.network.MessageWirelessGridStoragePush;
import refinedstorage.storage.ClientItemGroup;
import refinedstorage.tile.TileController;
import refinedstorage.tile.config.IRedstoneModeConfig;

import java.util.ArrayList;
import java.util.List;

public class WirelessGrid implements IGrid {
    private EnumHand hand;
    private BlockPos controller;
    private int sortingType;
    private int sortingDirection;
    private int searchBoxMode;
    private long lastUpdate;
    private List<ClientItemGroup> itemGroups = new ArrayList<ClientItemGroup>();

    public WirelessGrid(ItemStack stack, EnumHand hand) {
        this.hand = hand;
        this.controller = new BlockPos(ItemWirelessGrid.getX(stack), ItemWirelessGrid.getY(stack), ItemWirelessGrid.getZ(stack));
        this.sortingType = ItemWirelessGrid.getSortingType(stack);
        this.sortingDirection = ItemWirelessGrid.getSortingDirection(stack);
        this.searchBoxMode = ItemWirelessGrid.getSearchBoxMode(stack);
    }

    @Override
    public EnumGridType getType() {
        return EnumGridType.NORMAL;
    }

    @Override
    public List<ClientItemGroup> getItemGroups() {
        return itemGroups;
    }

    @Override
    public void setItemGroups(List<ClientItemGroup> groups) {
        this.itemGroups = groups;
        this.lastUpdate = System.currentTimeMillis();
    }

    @Override
    public BlockPos getControllerPos() {
        return controller;
    }

    @Override
    public void onItemPush(int playerSlot, boolean one) {
        RefinedStorage.NETWORK.sendToServer(new MessageWirelessGridStoragePush(controller.getX(), controller.getY(), controller.getZ(), playerSlot, one));
    }

    @Override
    public void onItemPull(int id, int flags) {
        RefinedStorage.NETWORK.sendToServer(new MessageWirelessGridStoragePull(controller.getX(), controller.getY(), controller.getZ(), id, flags));
    }

    public void onClose(EntityPlayer player) {
        TileEntity tile = player.worldObj.getTileEntity(controller);

        if (tile instanceof TileController) {
            ((TileController) tile).onCloseWirelessGrid(player);
        }
    }

    @Override
    public int getSortingType() {
        return sortingType;
    }

    @Override
    public int getSortingDirection() {
        return sortingDirection;
    }

    @Override
    public int getSearchBoxMode() {
        return searchBoxMode;
    }

    @Override
    public void onSortingTypeChanged(int type) {
        RefinedStorage.NETWORK.sendToServer(new MessageWirelessGridSettingsUpdate(RefinedStorageUtils.getIdFromHand(hand), getSortingDirection(), type, getSearchBoxMode()));

        this.sortingType = type;
    }

    @Override
    public void onSortingDirectionChanged(int direction) {
        RefinedStorage.NETWORK.sendToServer(new MessageWirelessGridSettingsUpdate(RefinedStorageUtils.getIdFromHand(hand), direction, getSortingType(), getSearchBoxMode()));

        this.sortingDirection = direction;
    }

    @Override
    public void onSearchBoxModeChanged(int searchBoxMode) {
        RefinedStorage.NETWORK.sendToServer(new MessageWirelessGridSettingsUpdate(RefinedStorageUtils.getIdFromHand(hand), getSortingDirection(), getSortingType(), searchBoxMode));

        this.searchBoxMode = searchBoxMode;
    }

    @Override
    public void onCraftingRequested(int id, int quantity) {
        RefinedStorage.NETWORK.sendToServer(new MessageWirelessGridCraftingStart(controller.getX(), controller.getY(), controller.getZ(), id, quantity));
    }

    @Override
    public IRedstoneModeConfig getRedstoneModeSetting() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return System.currentTimeMillis() - lastUpdate < 1000;
    }
}
