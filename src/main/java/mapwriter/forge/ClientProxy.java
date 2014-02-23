package mapwriter.forge;

import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import mapwriter.Mw;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
	public void init(MwConfig config) {
		Mw mw = new Mw(config);
		MinecraftForge.EVENT_BUS.register(new EventHandler(mw));
		NetworkRegistry.instance().registerConnectionHandler(new MwConnectionHandler(mw));
		TickRegistry.registerTickHandler(new MwTickHandler(mw), Side.CLIENT);
		KeyBindingRegistry.registerKeyBinding(new MwKeyHandler(mw));
	}
}
