package li.cil.oc.integration.forestry

import java.util
import forestry.api.apiculture.{BeeManager, IBeeHousing}
import forestry.plugins.PluginApiculture
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{EnvironmentHost, Node, Visibility}
import li.cil.oc.api.{Network, internal, prefab}
import li.cil.oc.server.component.result
import li.cil.oc.server.component.traits.{NetworkAware, SideRestricted, WorldAware}
import li.cil.oc.util.{BlockPosition, InventoryUtils}
import li.cil.oc.util.ExtendedArguments.extendedArguments
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._

class UpgradeBeekeeper(val host: EnvironmentHost with internal.Agent) extends prefab.ManagedEnvironment with DeviceInfo with WorldAware with SideRestricted with NetworkAware {
  override val node: Node = Network.newNode(this, Visibility.Network).
    withComponent("beekeeper", Visibility.Neighbors).
    withConnector().
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "BeeKeeper",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Breeding bees for you (almost)"
  )

  private final val defaultStackSize = 64

  override def getDeviceInfo: util.Map[String, String] = deviceInfo
  override def position: BlockPosition = BlockPosition(host)
  override protected def checkSideForAction(args: Arguments, n: Int): ForgeDirection = args.checkSideAny(n)


  @Callback(doc = """function(side:number):boolean -- Swap the queen from the selected slot with the apiary at the specified side.""")
  def swapQueen(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val pos = position.offset(facing)
    result(UpgradeBeekeeperUtil.swapQueen(pos, host.mainInventory(), host.selectedSlot()))
  }

  @Callback(doc = """function(side:number):boolean -- Swap the drone from the selected slot with the apiary at the specified side.""")
  def swapDrone(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val pos = position.offset(facing)
    result(UpgradeBeekeeperUtil.swapDrone(pos, host.mainInventory(), host.selectedSlot()))
  }

  @Callback(doc = """function(side:number):number -- Get current progress percent for the apiary at the specified side.""")
  def getBeeProgress(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val housing = UpgradeBeekeeperUtil.getBeeHousingAt(position.offset(facing))
    if (housing == null)
      return result(false, "No bee housing found")
    result(housing.getBeekeepingLogic.getBeeProgressPercent)
  }

  @Callback(doc = """function(side:number):boolean -- Checks if current bee in the apiary at the specified side can work now.""")
  def canWork(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val housing = UpgradeBeekeeperUtil.getBeeHousingAt(position.offset(facing))
    if (housing == null)
      return result(false, "No bee housing found")
    result(housing.getBeekeepingLogic.canWork)
  }

  @Callback(doc = """function(honeyslot:number):boolean -- Analyzes bee in selected slot, uses honey from the specified slot.""")
  def analyze(context: Context, args: Arguments): Array[AnyRef] = {
    val inventory = host.mainInventory
    val specimenSlot = host.selectedSlot
    val specimen = inventory.getStackInSlot(specimenSlot)
    if (!BeeManager.beeRoot.isMember(specimen))
      return result(false, "Not a bee")

    val honeySlot = args.checkSlot(inventory, 0)
    val honeyStack = inventory.getStackInSlot(honeySlot)
    if (honeyStack== null || honeyStack.stackSize == 0 || (honeyStack.getItem != PluginApiculture.items.honeydew && honeyStack.getItem != PluginApiculture.items.honeyDrop))
      return result(false, "No honey!")

    val individual = BeeManager.beeRoot.getMember(specimen)
    if (!individual.isAnalyzed) {
      individual.analyze
      val nbttagcompound = new NBTTagCompound
      individual.writeToNBT(nbttagcompound)
      specimen.setTagCompound(nbttagcompound)
      inventory.setInventorySlotContents(specimenSlot, specimen)
      honeyStack.stackSize -= 1
      inventory.setInventorySlotContents(honeySlot, honeyStack)
    }
    result(true)
  }

  @Callback(doc = """function(side:number[,amount:number]):number -- Tries to add amount many or all industrial upgrades from the selected slot to industrial apiary at the given side.""")
  def addIndustrialUpgrade(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val pos = position.offset(facing)
    var amount = defaultStackSize
    if (args.count() > 1)
      amount = args.checkInteger(1)
    result(UpgradeBeekeeperUtil.addIndustrialUpgrade(pos, host.mainInventory(), host.selectedSlot(), amount))
  }
  @Callback(doc = """function(side:number, slot: number):table -- Get industrial upgrade in the given slot of the industrial apiary at the given side.""")
  def getIndustrialUpgrade(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val pos = position.offset(facing)
    val slot = args.checkInteger(1)
    val maxIndex = UpgradeBeekeeperUtil.getMaxIndustrialUpgradeCount
    if (slot < 1 || slot > maxIndex)
      return result(Unit, "Wrong slot index (should be 1-" + maxIndex + ")")
    result(UpgradeBeekeeperUtil.getIndustrialUpgrade(pos, slot))
  }
  @Callback(doc = """function(side:number, slot: number[, amount: number]):boolean -- Remove industrial upgrade from the given slot of the industrial apiary at the given side.""")
  def removeIndustrialUpgrade(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val pos = position.offset(facing)
    val slot = args.checkInteger(1)
    val maxIndex = UpgradeBeekeeperUtil.getMaxIndustrialUpgradeCount
    var amount = defaultStackSize
    if (args.count() > 2)
      amount = args.checkInteger(2)
    if (slot < 1 || slot > maxIndex)
      return result(false, "Wrong slot index (should be 1-" + maxIndex + ")")
    result(UpgradeBeekeeperUtil.removeIndustrialUpgrade(pos, host.mainInventory(), host.selectedSlot(), slot, amount))
  }
}
