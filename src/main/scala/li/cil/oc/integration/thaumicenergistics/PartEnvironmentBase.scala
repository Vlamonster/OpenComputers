package li.cil.oc.integration.thaumicenergistics

import appeng.api.parts.IPartHost
import li.cil.oc.Settings
import li.cil.oc.api.machine.{Arguments, Context}
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ResultWrapper.result
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import thaumcraft.api.aspects.Aspect
import thaumicenergistics.common.network.IAspectSlotPart

import scala.reflect.ClassTag

trait PartEnvironmentBase extends ManagedEnvironment {
  def world: World
  def host: IPartHost

  private def resolveAspectSlot(part: IAspectSlotPart, slot: Int): Int = {
    val available = part.getAvailableAspectSlots

    if (slot < 1 || slot > available.length) {
      throw new IllegalArgumentException("invalid slot")
    }
    available(slot - 1)
  }

  // function(side:number):number
  def getSlotSize[PartType <: IAspectSlotPart : ClassTag](context: Context, args: Arguments): Array[AnyRef] = {
    val side = args.checkSideAny(0)
    host.getPart(side) match {
      case part: PartType =>
        result(part.getAvailableAspectSlots.length)
      case _ => result(Unit, "no matching part")
    }
  }

  // function(side:number[, slot:number]):string
  def getPartConfig[PartType <: IAspectSlotPart : ClassTag](context: Context, args: Arguments): Array[AnyRef] = {
    val side = args.checkSideAny(0)
    host.getPart(side) match {
      case part: PartType =>
        val slot = resolveAspectSlot(part, args.optInteger(1, 1))
        val stack = Option(part.getAspect(slot))
        result(stack match {
          case Some(aspect) => aspect.getTag
          case None => Unit
        })
      case _ => result(Unit, "no matching part")
    }
  }

  // function(side:number[, slot:number][, aspect:string]):boolean
  def setPartConfig[PartType <: IAspectSlotPart : ClassTag](context: Context, args: Arguments): Array[AnyRef] = {
    val side = args.checkSideAny(0)
    host.getPart(side) match {
      case part: PartType =>
        val noSlotArg = args.isString(1)
        val slot = resolveAspectSlot(part, if (noSlotArg) 1 else args.optInteger(1, 1))
        val aspect = if (noSlotArg || args.count > 2) {
          Aspect.getAspect(args.checkString(if (noSlotArg) 1 else 2)) match {
            case aspect: Aspect => aspect
            case _ => throw new IllegalArgumentException("invalid aspect")
          }
        }
        else null

        val fakePlayer = FakePlayerFactory.get(world.asInstanceOf[WorldServer], Settings.get.fakePlayerProfile)

        part.setAspect(slot, aspect, fakePlayer)
        context.pause(0.5)
        result(true)
      case _ => result(Unit, "no matching part")
    }
  }
}
