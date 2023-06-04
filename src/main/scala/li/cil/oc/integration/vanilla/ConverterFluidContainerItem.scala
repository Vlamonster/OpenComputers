package li.cil.oc.integration.vanilla

import java.util

import li.cil.oc.{Settings, api}
import li.cil.oc.server.driver.Registry
import net.minecraft.item
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids
import net.minecraftforge.fluids.FluidStack

import scala.collection.convert.WrapAsScala._

object ConverterFluidContainerItem extends api.driver.Converter  {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]): Unit =
    value match {
      case stack: item.ItemStack => stack.getItem match {
        case fc: fluids.IFluidContainerItem =>
          output += "capacity" -> Int.box(fc.getCapacity(stack))
          val fluidStack  = fc.getFluid(stack)
          if (fluidStack != null) {
            val fluidData = Registry.convert(Array[AnyRef](fluidStack))
            if (fluidData.nonEmpty) {
              output += "fluid" -> fluidData(0)
            }
          }
          if (!output.containsKey("fluid")) {
            val fluidMap = new util.HashMap[AnyRef, AnyRef]()
            fluidMap += "amount" -> Int.box(0)
            output += "fluid" -> fluidMap
          }

          // old GT:NH tag names
          if (fluidStack != null && fluidStack.getFluid != null) {
            val fluid = fluidStack.getFluid
            if (Settings.get.insertIdsInConverters)
              output += "fluid_id" -> Int.box(fluid.getID)
            output += "fluid_hasTag" -> Boolean.box(fluidStack.tag != null)
            if (fluid != null) {
              output += "fluid_name" -> fluid.getName
              output += "fluid_label" -> fluid.getLocalizedName(fluidStack)
            }
          }
        case _ =>
      }
      case _ =>
    }
}
