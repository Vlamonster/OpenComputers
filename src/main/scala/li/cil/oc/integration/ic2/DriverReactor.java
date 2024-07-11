package li.cil.oc.integration.ic2;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;
import ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric;
import java.util.HashMap;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverReactor extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IReactor.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(
            final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IReactor) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IReactor> implements NamedBlock {
        public Environment(final IReactor tileEntity) {
            super(tileEntity, "reactor");
        }

        @Override
        public String preferredName() {
            return "reactor";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function(active:boolean): boolean -- activate or deactivate the reactor")
        public Object[] setActive(final Context context, final Arguments args) {
            TileEntityNuclearReactorElectric reactor = (TileEntityNuclearReactorElectric) tileEntity;
            if(reactor != null) {
                reactor.setRedstoneSignal(args.optBoolean(0, false));
                return new Object[]{reactor.receiveredstone()};
            }
            return new Object[]{false};
        }

        @Callback(doc = "function():number -- Get the reactor's heat.")
        public Object[] getHeat(final Context context, final Arguments args) {
            return new Object[] {tileEntity.getHeat()};
        }

        @Callback(doc = "function():number -- Get the reactor's maximum heat before exploding.")
        public Object[] getMaxHeat(final Context context, final Arguments args) {
            return new Object[] {tileEntity.getMaxHeat()};
        }

        @Callback(
                doc = "function():number -- Get the reactor's energy output. Not multiplied with the base EU/t value.")
        public Object[] getReactorEnergyOutput(final Context context, final Arguments args) {
            return new Object[] {tileEntity.getReactorEnergyOutput()};
        }

        @Callback(doc = "function():number -- Get the reactor's base EU/t value.")
        public Object[] getReactorEUOutput(final Context context, final Arguments args) {
            return new Object[] {tileEntity.getReactorEUEnergyOutput()};
        }

        @Callback(doc = "function():boolean -- Get whether the reactor is active and supposed to produce energy.")
        public Object[] producesEnergy(final Context context, final Arguments args) {
            return new Object[] {tileEntity.produceEnergy()};
        }

        @Callback(doc = "function(x:int,y:int):table -- Get information about the item stored in the given reactor slot.")
        public Object[] getSlotInfo(final Context context, final Arguments args) {
            final int x = args.optInteger(0, -1);
            final int y = args.optInteger(1, -1);

            final ItemStack stack = tileEntity.getItemAt(x, y);

            if (stack == null) {
                return null;
            }

            final Item item = stack.getItem();

            final HashMap<String, Object> outputMap = new HashMap<String, Object> ();

            outputMap.put("item", stack);

            if (item instanceof  IReactorComponent) {
                final IReactorComponent component = (IReactorComponent) item;
                outputMap.put("canStoreHeat", component.canStoreHeat(tileEntity, stack, x, y));
                outputMap.put("heat", component.getCurrentHeat(tileEntity, stack, x, y));
                outputMap.put("maxHeat", component.getMaxHeat(tileEntity, stack, x, y));
            }

            return new Object[] {
                outputMap
            };
        }
    }
}
