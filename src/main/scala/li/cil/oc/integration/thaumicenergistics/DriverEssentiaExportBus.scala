package li.cil.oc.integration.thaumicenergistics

import appeng.api.parts.IPartHost
import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ResultWrapper._
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import thaumicenergistics.api.ThEApi
import thaumicenergistics.common.parts.PartEssentiaExportBus

object DriverEssentiaExportBus extends driver.SidedBlock {
  override def worksWith(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case container: IPartHost => ForgeDirection.VALID_DIRECTIONS.map(container.getPart).filter(obj => { obj != null }).exists(_.isInstanceOf[PartEssentiaExportBus])
      case _ => false
    }

  override def createEnvironment(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) = new Environment(world, world.getTileEntity(x, y, z).asInstanceOf[IPartHost])

  final class Environment(val world: World, val host: IPartHost) extends ManagedTileEntityEnvironment[IPartHost](host, "essentia_exportbus") with NamedBlock with PartEnvironmentBase {
    override def preferredName = "essentia_exportbus"

    override def priority = 2

    @Callback(doc = "function(side:number[, slot:number]):string -- Get the configuration of the export bus pointing in the specified direction.")
    def getExportConfiguration(context: Context, args: Arguments): Array[AnyRef] = getPartConfig[PartEssentiaExportBus](context, args)

    @Callback(doc = "function(side:number[, slot:number][, aspect:string]):boolean -- Configure the export bus pointing in the specified direction to export essentia matching the specified type.")
    def setExportConfiguration(context: Context, args: Arguments): Array[AnyRef] = setPartConfig[PartEssentiaExportBus](context, args)

    @Callback(doc = "function(side:number):number -- Get the number of valid slots in this export bus.")
    def getExportSlotSize(context: Context, args: Arguments): Array[AnyRef] = getSlotSize[PartEssentiaExportBus](context, args)

    @Callback(doc = "function(side:number):boolean -- Get whether or not essentia exported into a void jar will allow voiding")
    def getVoidAllowed(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSideAny(0)
      host.getPart(side) match {
        case part: PartEssentiaExportBus =>
          result(part.isVoidAllowed)
        case _ => result(Unit, "no essentia export bus")
      }
    }

    @Callback(doc = "function(side:number, allowed:boolean):boolean -- Set void mode")
    def setVoidAllowed(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSideAny(0)
      val mode = args.checkBoolean(1)
      host.getPart(side) match {
        case part: PartEssentiaExportBus =>
          var didSomething = false
          if (mode != part.isVoidAllowed) {
            part.toggleVoidMode()
            didSomething = true
          }

          result(didSomething)
        case _ => result(Unit, "no essentia export bus")
      }
    }

  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (ThEApi.instance.parts.Essentia_ExportBus.getStack.isItemEqual(stack))
        classOf[Environment]
      else null
  }
}
