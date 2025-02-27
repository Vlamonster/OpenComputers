package li.cil.oc.integration.gregtech

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import gregtech.api.interfaces.IDamagableItem
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.items.MetaGeneratedTool
import li.cil.oc.api.event.{GeolyzerEvent, RobotUsedToolEvent}
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection
import scala.collection.convert.WrapAsScala._

object EventHandlerGregTech {
  @SubscribeEvent
  def onGeolyzerAnalyze(e: GeolyzerEvent.Analyze) {
    val world = e.host.world
    world.getTileEntity(e.x, e.y, e.z) match {
      case tile : IGregTechTileEntity =>
        e.data += "facing" -> tile.getFrontFacing.name
        e.data += "sensorInformation" -> tile.getInfoData()
      case _ =>
    }
  }

  @SubscribeEvent
  def onRobotApplyDamageRate(e: RobotUsedToolEvent.ApplyDamageRate) {
    (e.toolBeforeUse.getItem, e.toolAfterUse.getItem) match {
      case (itemBefore: IDamagableItem, itemAfter: IDamagableItem) =>
        val damage = MetaGeneratedTool.getToolDamage(e.toolAfterUse) - MetaGeneratedTool.getToolDamage(e.toolBeforeUse)
        if (damage > 0) {
          val actualDamage = damage * e.getDamageRate
          val repairedDamage =
            if (e.agent.player.getRNG.nextDouble() > 0.5)
              damage - math.floor(actualDamage).toInt
            else
              damage - math.ceil(actualDamage).toInt
          MetaGeneratedTool.setToolDamage(e.toolAfterUse, MetaGeneratedTool.getToolDamage(e.toolAfterUse) - repairedDamage)
        }
      case _ =>
    }
  }

  def getDurability(stack: ItemStack): Double = {
    stack.getItem match {
      case item: IDamagableItem => 1.0 - MetaGeneratedTool.getToolDamage(stack).toDouble / MetaGeneratedTool.getToolMaxDamage(stack).toDouble
      case _ => Double.NaN
    }
  }
}
