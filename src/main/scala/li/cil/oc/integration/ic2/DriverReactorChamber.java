package li.cil.oc.integration.ic2;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;
import ic2.api.reactor.IReactorComponent;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashMap;

public final class DriverReactorChamber extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IReactorChamber.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(
            final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IReactorChamber) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IReactorChamber> implements NamedBlock {
        public Environment(final IReactorChamber tileEntity) {
            super(tileEntity, "reactor_chamber");
        }

        @Override
        public String preferredName() {
            return "reactor_chamber";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function(active:boolean): boolean -- activate or deactivate the reactor")
        public Object[] setActive(final Context context, final Arguments args) {
            TileEntityNuclearReactorElectric reactor = (TileEntityNuclearReactorElectric) tileEntity.getReactor();
            if(reactor != null) {
                reactor.setRedstoneSignal(args.optBoolean(0, false));
                return new Object[]{reactor.receiveredstone()};
            }
            return new Object[]{false};
        }
        @Callback(doc = "function():number -- Get the reactor's heat.")
        public Object[] getHeat(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();
            if (reactor != null) {
                return new Object[] {reactor.getHeat()};
            } else {
                return new Object[] {0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's maximum heat before exploding.")
        public Object[] getMaxHeat(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();
            if (reactor != null) {
                return new Object[] {tileEntity.getReactor().getMaxHeat()};
            } else {
                return new Object[] {0};
            }
        }

        @Callback(
                doc = "function():number -- Get the reactor's energy output. Not multiplied with the base EU/t value.")
        public Object[] getReactorEnergyOutput(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();
            if (reactor != null) {
                return new Object[] {tileEntity.getReactor().getReactorEnergyOutput()};
            } else {
                return new Object[] {0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's base EU/t value.")
        public Object[] getReactorEUOutput(final Context context, final Arguments args) {
            return new Object[] {tileEntity.getReactor().getReactorEUEnergyOutput()};
        }

        @Callback(doc = "function():boolean -- Get whether the reactor is active and supposed to produce energy.")
        public Object[] producesEnergy(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();
            if (reactor != null) {
                return new Object[] {tileEntity.getReactor().produceEnergy()};
            } else {
                return new Object[] {false};
            }
        }

        @Callback(doc = "function(x:int,y:int):table -- Get information about the item stored in the given reactor slot.")
        public Object[] getSlotInfo(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();

            if (reactor == null) {
                return null;
            }

            final int x = args.optInteger(0, -1);
            final int y = args.optInteger(1, -1);

            final ItemStack stack = reactor.getItemAt(x, y);

            if (stack == null) {
                return null;
            }

            final Item item = stack.getItem();

            final HashMap<String, Object> outputMap = new HashMap<String, Object> ();

            outputMap.put("item", stack);

            if (item instanceof IReactorComponent) {
                final IReactorComponent component = (IReactorComponent) item;
                outputMap.put("canStoreHeat", component.canStoreHeat(reactor, stack, x, y));
                outputMap.put("heat", component.getCurrentHeat(reactor, stack, x, y));
                outputMap.put("maxHeat", component.getMaxHeat(reactor, stack, x, y));
            }

            return new Object[] {
                outputMap
            };
        }
    }
}
