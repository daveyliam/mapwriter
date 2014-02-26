package mapwriter.forge;

import cpw.mods.fml.common.FMLCommonHandler;
import mapwriter.Mw;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
	public void init(MwConfig config) {
		Mw mw = new Mw(config);
		FMLCommonHandler.instance().bus().register(new MwKeyHandler());
		MinecraftForge.EVENT_BUS.register(new EventHandler(mw));
	}
}
