package li.cil.oc.integration.ic2;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;
import ic2.api.reactor.IReactorComponent;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric;
import ic2.core.block.reactor.tileentity.TileEntityReactorChamberElectric;
import ic2.core.block.reactor.tileentity.TileEntityReactorRedstonePort;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashMap;

public final class DriverReactorRedstonePort extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityReactorRedstonePort.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(
            final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((TileEntityReactorRedstonePort) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityReactorRedstonePort>
            implements NamedBlock {
        public Environment(final TileEntityReactorRedstonePort tileEntity) {
            super(tileEntity, "reactor_redstone_port");
        }

        @Override
        public String preferredName() {
            return "reactor_redstone_port";
        }

        @Override
        public int priority() {
            return 0;
        }

        private IReactor getReactor() {
            final TileEntity reactorInventory = tileEntity.getReactor();

            if (reactorInventory instanceof IReactor) {
                return (IReactor) reactorInventory;
            } else {
                return ((IReactorChamber) reactorInventory).getReactor();
            }
        }

        @Callback(doc = "function(active:boolean): boolean -- activate or deactivate the reactor")
        public Object[] setActive(final Context context, final Arguments args) {
            TileEntityReactorChamberElectric reactorChamberElectric = (TileEntityReactorChamberElectric) tileEntity.getReactor();
            TileEntityNuclearReactorElectric reactor = reactorChamberElectric.getReactor();
            if(reactor != null) {
                reactor.setRedstoneSignal(args.optBoolean(0, false));
                return new Object[]{reactor.receiveredstone()};
            }
            return new Object[]{false};
        }

        @Callback(doc = "function():number -- Get the reactor's heat.")
        public Object[] getHeat(final Context context, final Arguments args) {
            final IReactor reactor = getReactor();
            if (reactor != null) {
                return new Object[] {reactor.getHeat()};
            } else {
                return new Object[] {0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's maximum heat before exploding.")
        public Object[] getMaxHeat(final Context context, final Arguments args) {
            final IReactor reactor = getReactor();
            if (reactor != null) {
                return new Object[] {reactor.getMaxHeat()};
            } else {
                return new Object[] {0};
            }
        }

        @Callback(
                doc = "function():number -- Get the reactor's energy output. Not multiplied with the base EU/t value.")
        public Object[] getReactorEnergyOutput(final Context context, final Arguments args) {
            final IReactor reactor = getReactor();
            if (reactor != null) {
                return new Object[] {reactor.getReactorEnergyOutput()};
            } else {
                return new Object[] {0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's base EU/t value.")
        public Object[] getReactorEUOutput(final Context context, final Arguments args) {
            return new Object[] {getReactor().getReactorEUEnergyOutput()};
        }

        @Callback(doc = "function():boolean -- Get whether the reactor is active and supposed to produce energy.")
        public Object[] producesEnergy(final Context context, final Arguments args) {
            final IReactor reactor = getReactor();
            if (reactor != null) {
                return new Object[] {reactor.produceEnergy()};
            } else {
                return new Object[] {false};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's emitted heat. Useful for fluid reactors.")
        public Object[] getEmitHeat(final Context context, final Arguments args) {
            final IReactor reactor = getReactor();
            if(reactor instanceof TileEntityNuclearReactorElectric) {
                TileEntityNuclearReactorElectric fluidReactor = (TileEntityNuclearReactorElectric) reactor;
                return new Object[] {fluidReactor.EmitHeat};
            } else {
                return new Object[] {0};
            }
        }

        @Callback(doc = "function(x:int,y:int):table -- Get information about the item stored in the given reactor slot.")
        public Object[] getSlotInfo(final Context context, final Arguments args) {
            final IReactor reactor = getReactor();

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
