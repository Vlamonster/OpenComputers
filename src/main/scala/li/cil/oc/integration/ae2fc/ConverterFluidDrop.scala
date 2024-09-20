package li.cil.oc.integration.ae2fc

import com.glodblock.github.common.item.ItemFluidDrop
import li.cil.oc.api.driver.Converter
import net.minecraft.item.ItemStack

import java.util
import scala.collection.convert.WrapAsScala._

object ConverterFluidDrop extends Converter {

  override def convert(value: Any, output: util.Map[AnyRef, AnyRef]): Unit = value match {
    case stack: ItemStack if ItemFluidDrop.isFluidStack(stack) =>
      output += "fluidDrop" -> ItemFluidDrop.getFluidStack(stack)
    case _ =>
  }
}
