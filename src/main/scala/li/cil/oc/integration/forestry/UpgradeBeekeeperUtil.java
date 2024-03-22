package li.cil.oc.integration.forestry;

import forestry.api.apiculture.IBeeHousing;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.util.GT_ApiaryUpgrade;
import gregtech.api.util.GT_Utility;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_IndustrialApiary;
import li.cil.oc.util.BlockPosition;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Wrapper class for UpgradeBeekeeper item. Scala doesn't seem to like GT machines,
 * so we wrap most functionality in a static java class
 * */
public final class UpgradeBeekeeperUtil {

    // First upgrade slot index of Industrial Apiaries because it's private for some reason
    private static final int UPGRADE_INDEX = 7;
    private UpgradeBeekeeperUtil() {}

    /** Returns an IBeeHousing TileEntity at position pos. Can be an Industrial Apiary */
    public static IBeeHousing getBeeHousingAt(BlockPosition pos) {
        if (pos.world().isEmpty())
            return null;

        World world = pos.world().get();
        TileEntity te = world.getTileEntity(pos.x(), pos.y(), pos.z());
        if (te == null)
            return null;
        if (te instanceof IBeeHousing)
            return (IBeeHousing)te;
        // Scala doesn't compile if these checks are of the form (a instanceof B b)
        if (!(te instanceof BaseMetaTileEntity))
            return null;
        BaseMetaTileEntity mte = (BaseMetaTileEntity)te;
        if (!(mte.getMetaTileEntity() instanceof GT_MetaTileEntity_IndustrialApiary))
            return null;
        return (IBeeHousing)mte.getMetaTileEntity();
    }

    /** Returns a Tile Entity for Industrial Apiaries at position pos, or null if none exist */
    public static GT_MetaTileEntity_IndustrialApiary getGTIApiaryAt(BlockPosition pos) {
        if (pos.world().isEmpty())
            return null;

        World world = pos.world().get();
        TileEntity te = world.getTileEntity(pos.x(), pos.y(), pos.z());
        if (te == null)
            return null;
        if (!(te instanceof BaseMetaTileEntity))
            return null;
        BaseMetaTileEntity mte = (BaseMetaTileEntity)te;
        if (!(mte.getMetaTileEntity() instanceof GT_MetaTileEntity_IndustrialApiary))
            return null;
        return (GT_MetaTileEntity_IndustrialApiary)mte.getMetaTileEntity();
    }

    public static boolean swapQueen(BlockPosition pos, IInventory hostInv, int slot) {
        IBeeHousing housing = getBeeHousingAt(pos);
        if (housing == null)
            return false;

        ItemStack newQueen = hostInv.getStackInSlot(slot);
        ItemStack oldQueen = housing.getBeeInventory().getQueen();
        housing.getBeeInventory().setQueen(newQueen);
        hostInv.setInventorySlotContents(slot, oldQueen);
        return true;
    }

    public static boolean swapDrone(BlockPosition pos, IInventory hostInv, int slot) {
        IBeeHousing housing = getBeeHousingAt(pos);
        if (housing == null)
            return false;

        ItemStack newDrone = hostInv.getStackInSlot(slot);
        ItemStack oldDrone = housing.getBeeInventory().getDrone();
        housing.getBeeInventory().setDrone(newDrone);
        hostInv.setInventorySlotContents(slot, oldDrone);
        return true;
    }

    public static int addIndustrialUpgrade(BlockPosition pos, IInventory hostInv, int slot, int amount) {
        GT_MetaTileEntity_IndustrialApiary iapiary = getGTIApiaryAt(pos);
        if (iapiary == null)
            return 0;

        ItemStack stackToInstall = hostInv.getStackInSlot(slot);
        if (stackToInstall == null || !GT_ApiaryUpgrade.isUpgrade(stackToInstall))
            return 0;
        amount = Math.min(amount, stackToInstall.stackSize);

        for (int i = UPGRADE_INDEX; i < UPGRADE_INDEX + 4; i++) {
            // isItemValidForSlot ensures merging existing stacks
            if (!iapiary.isItemValidForSlot(i, stackToInstall))
                continue;

            int maxStackSize = GT_ApiaryUpgrade.getUpgrade(stackToInstall).getMaxNumber();
            ItemStack stack = iapiary.getStackInSlot(i);
            // Push into empty slot
            if (stack == null) {
                amount = Math.min(amount, maxStackSize);
                iapiary.setInventorySlotContents(i, stackToInstall.splitStack(amount));
                if (stackToInstall.stackSize <= 0)
                    hostInv.setInventorySlotContents(slot, null);
                return amount;
            }
            // Merge stacks
            if (!GT_Utility.areStacksEqual(stackToInstall, stack))
                continue;
            amount = Math.max(Math.min(amount, maxStackSize - stack.stackSize), 0);
            if (amount == 0)
                return 0;
            stack.stackSize += amount;
            stackToInstall.stackSize -= amount;
            if (stackToInstall.stackSize <= 0)
                hostInv.setInventorySlotContents(slot, null);
            return amount;
        }
        return 0;
    }

    public static ItemStack getIndustrialUpgrade(BlockPosition pos, int index){
        if (index < 1 || index > 4)
            return null;
        GT_MetaTileEntity_IndustrialApiary iapiary = getGTIApiaryAt(pos);
        if (iapiary == null)
            return null;
        return iapiary.getStackInSlot(index - 1 + UPGRADE_INDEX);
    }

    public static int removeIndustrialUpgrade(BlockPosition pos, IInventory hostInv, int slot, int index, int amount) {
        if (index < 1 || index > 4)
            return 0;
        GT_MetaTileEntity_IndustrialApiary iapiary = getGTIApiaryAt(pos);
        if (iapiary == null)
            return 0;

        index = index - 1 + UPGRADE_INDEX;
        ItemStack stack = iapiary.getStackInSlot(index);
        if (stack == null)
            return 0;
        amount = Math.min(amount, stack.stackSize);
        int moved = insertIntoHostInv(hostInv, slot, stack.splitStack(amount));
        // If less items were moved than planned, move the unmoved items back
        stack.stackSize += (amount - moved);
        if (stack.stackSize <= 0)
            iapiary.setInventorySlotContents(index, null);
        return moved;
    }

    private static int insertIntoHostInv(IInventory hostInv, int slot, ItemStack stack) {
        if (stack == null)
            return 0;

        final int initialStackSize = stack.stackSize;

        // Try putting in selected slot first
        insertIntoSlot(hostInv, slot, stack);

        if (stack.stackSize <= 0)
            return initialStackSize;

        // Find any stacks to merge with
        for (int i = 0; i < hostInv.getSizeInventory(); i++) {
            ItemStack stackInSlot = hostInv.getStackInSlot(i);
            if (!GT_Utility.areStacksEqual(stack, stackInSlot))
                continue;
            insertIntoSlot(hostInv, i, stack);

            if (stack.stackSize <= 0)
                return initialStackSize;
        }

        // Try pushing any remaining items
        for (int i = 0; i < hostInv.getSizeInventory(); i++) {
            insertIntoSlot(hostInv, i, stack);

            if (stack.stackSize <= 0)
                return initialStackSize;
        }

        return initialStackSize - stack.stackSize;
    }

    private static void insertIntoSlot(IInventory inv, int slot, ItemStack stack) {
        ItemStack stackInSlot = inv.getStackInSlot(slot);
        int maxStackSize = Math.min(inv.getInventoryStackLimit(), stack.getMaxStackSize());
        if (stackInSlot == null) {
            inv.setInventorySlotContents(slot, stack.splitStack(Math.min(maxStackSize, stack.stackSize)));
        } else if (GT_Utility.areStacksEqual(stack, stackInSlot)) {
            int toMove = Math.min(stack.stackSize, Math.max(0, maxStackSize - stackInSlot.stackSize));
            if (toMove > 0) {
                stackInSlot.stackSize += toMove;
                stack.stackSize -= toMove;
            }
        }
    }

}
