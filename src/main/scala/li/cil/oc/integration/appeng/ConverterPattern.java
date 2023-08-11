package li.cil.oc.integration.appeng;

import appeng.api.AEApi;
import appeng.helpers.PatternHelper;
import appeng.util.Platform;
import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class ConverterPattern implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            ItemStack is = (ItemStack) value;
            try {
                final NBTTagCompound encodedValue = is.getTagCompound();
                if (encodedValue != null) {
                    final NBTTagList inTag = encodedValue.getTagList("in", 10);
                    final NBTTagList outTag = encodedValue.getTagList("out", 10);

                    Map[] inputs = new Map[inTag.tagCount()];
                    for (int i = 0; i < inTag.tagCount(); i++) {
                        inputs[i] = new HashMap<>();
                        final NBTTagCompound tag = inTag.getCompoundTagAt(i);
                        final ItemStack inputItem = Platform.loadItemStackFromNBT(tag);
                        if (inputItem != null) {
                            inputs[i].put("name", inputItem.getItem().getItemStackDisplayName(inputItem));
                            if (tag.getLong("Cnt") > 0) {
                                inputs[i].put("count", tag.getLong("Cnt"));
                            } else {
                                inputs[i].put("count", inputItem.stackSize);
                            }
                        }
                    }

                    Map[] results = new Map[outTag.tagCount()];
                    for (int i = 0; i < outTag.tagCount(); i++) {
                        results[i] = new HashMap<>();
                        final NBTTagCompound tag = outTag.getCompoundTagAt(i);
                        final ItemStack outputItem = Platform.loadItemStackFromNBT(tag);
                        if (outputItem != null) {
                            results[i].put("name", outputItem.getItem().getItemStackDisplayName(outputItem));
                            if (tag.getLong("Cnt") > 0) {
                                results[i].put("count", tag.getLong("Cnt"));
                            } else {
                                results[i].put("count", outputItem.stackSize);
                            }
                        }
                    }
                        output.put("inputs", inputs);
                        output.put("outputs", results);
                        output.put("isCraftable", encodedValue.getBoolean("crafting"));
                }
            } catch (final Throwable ignored) {
                
            }
        }
    }
}
