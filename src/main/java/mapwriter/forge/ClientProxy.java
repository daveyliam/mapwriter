package mapwriter.forge;

import mapwriter.Mw;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
	public void init(MwConfig config) {
		Mw mw = new Mw(config);
		MinecraftForge.EVENT_BUS.register(new EventHandler(mw));
	}
}
