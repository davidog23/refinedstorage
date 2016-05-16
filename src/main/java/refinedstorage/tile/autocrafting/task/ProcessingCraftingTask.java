package refinedstorage.tile.autocrafting.task;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.text.TextFormatting;
import refinedstorage.RefinedStorageUtils;
import refinedstorage.tile.TileController;
import refinedstorage.tile.autocrafting.CraftingPattern;
import refinedstorage.tile.autocrafting.TileCrafter;

public class ProcessingCraftingTask implements ICraftingTask {
    public static final int ID = 1;

    public static final String NBT_INSERTED = "Inserted";
    public static final String NBT_MISSING = "Missing";
    public static final String NBT_SATISFIED = "Satisfied";

    private CraftingPattern pattern;
    private boolean inserted[];
    private boolean missing[];
    private boolean satisfied[];

    public ProcessingCraftingTask(CraftingPattern pattern) {
        this.pattern = pattern;
        this.inserted = new boolean[pattern.getInputs().length];
        this.missing = new boolean[pattern.getInputs().length];
        this.satisfied = new boolean[pattern.getOutputs().length];
    }

    public ProcessingCraftingTask(NBTTagCompound tag) {
        this.pattern = CraftingPattern.readFromNBT(tag.getCompoundTag(CraftingPattern.NBT));
        this.inserted = RefinedStorageUtils.readBooleanArray(tag, NBT_INSERTED);
        this.missing = RefinedStorageUtils.readBooleanArray(tag, NBT_MISSING);
        this.satisfied = RefinedStorageUtils.readBooleanArray(tag, NBT_SATISFIED);
    }

    @Override
    public CraftingPattern getPattern() {
        return pattern;
    }

    @Override
    public boolean update(TileController controller) {
        TileCrafter crafter = pattern.getCrafter(controller.getWorld());
        TileEntity crafterFacing = crafter.getWorld().getTileEntity(crafter.getPos().offset(crafter.getDirection()));

        if (crafterFacing instanceof IInventory) {
            for (int i = 0; i < inserted.length; ++i) {
                if (!inserted[i]) {
                    ItemStack input = pattern.getInputs()[i];
                    ItemStack took = controller.take(input);

                    if (took != null) {
                        missing[i] = false;

                        ItemStack remaining = TileEntityHopper.putStackInInventoryAllSlots((IInventory) crafterFacing, took, crafter.getDirection().getOpposite());

                        if (remaining == null) {
                            inserted[i] = true;
                        } else {
                            controller.push(took);
                        }
                    } else {
                        missing[i] = true;
                    }
                }
            }
        } else {
            return true;
        }

        for (int i = 0; i < satisfied.length; ++i) {
            if (!satisfied[i]) {
                return false;
            }
        }

        return true;
    }

    public boolean onInserted(ItemStack inserted) {
        for (int i = 0; i < pattern.getOutputs().length; ++i) {
            if (!satisfied[i] && RefinedStorageUtils.compareStackNoQuantity(inserted, pattern.getOutputs()[i])) {
                satisfied[i] = true;

                return true;
            }
        }

        return false;
    }

    @Override
    public void onDone(TileController controller) {
        // NO OP
    }

    @Override
    public void onCancelled(TileController controller) {
        // NO OP
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagCompound patternTag = new NBTTagCompound();
        pattern.writeToNBT(patternTag);
        tag.setTag(CraftingPattern.NBT, patternTag);

        RefinedStorageUtils.writeBooleanArray(tag, NBT_INSERTED, satisfied);
        RefinedStorageUtils.writeBooleanArray(tag, NBT_MISSING, missing);
        RefinedStorageUtils.writeBooleanArray(tag, NBT_SATISFIED, satisfied);

        tag.setInteger("Type", ID);
    }

    @Override
    public String getInfo() {
        StringBuilder builder = new StringBuilder();

        builder.append(TextFormatting.YELLOW).append("{missing_items}").append(TextFormatting.RESET).append("\n");

        int missingItems = 0;

        for (int i = 0; i < pattern.getInputs().length; ++i) {
            ItemStack input = pattern.getInputs()[i];

            if (missing[i]) {
                builder.append("- ").append(input.getDisplayName()).append("\n");

                missingItems++;
            }
        }

        if (missingItems == 0) {
            builder.append(TextFormatting.GRAY).append(TextFormatting.ITALIC).append("{none}").append(TextFormatting.RESET).append("\n");
        }

        builder.append(TextFormatting.YELLOW).append("{items_processing}").append(TextFormatting.RESET).append("\n");

        int itemsProcessing = 0;

        for (int i = 0; i < pattern.getInputs().length; ++i) {
            ItemStack input = pattern.getInputs()[i];

            if (inserted[i] && !satisfied[i]) {
                builder.append("- ").append(input.getDisplayName()).append("\n");

                itemsProcessing++;
            }
        }

        if (itemsProcessing == 0) {
            builder.append(TextFormatting.GRAY).append(TextFormatting.ITALIC).append("{none}").append(TextFormatting.RESET).append("\n");
        }

        return builder.toString();
    }
}