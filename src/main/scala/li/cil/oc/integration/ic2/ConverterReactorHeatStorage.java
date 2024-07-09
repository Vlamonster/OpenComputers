package li.cil.oc.integration.ic2;

import ic2.api.reactor.IReactorComponent;
import java.util.Map;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ConverterReactorHeatStorage implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;
            final Item item = stack.getItem();
            if (item instanceof IReactorComponent) {
                final IReactorComponent reactorComponent = (IReactorComponent) item;
                int maxHeat = reactorComponent.getMaxHeat(null, stack, 0, 0);
                if (maxHeat > 0) {
                	output.put("heat", reactorComponent.getCurrentHeat(null, stack, 0, 0));
                	output.put("maxHeat", maxHeat);
                }
            }
        }
    }
}
