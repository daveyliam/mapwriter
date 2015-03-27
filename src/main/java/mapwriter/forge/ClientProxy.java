package mapwriter.forge;

import java.io.File;

import mapwriter.Mw;
import mapwriter.api.MwAPI;
import mapwriter.handler.ConfigurationHandler;
import mapwriter.overlay.OverlayGrid;
import mapwriter.overlay.OverlaySlime;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class ClientProxy extends CommonProxy {
	
	public void preInit(File configFile) 
	{
		ConfigurationHandler.init(configFile);
		FMLCommonHandler.instance().bus().register(new ConfigurationHandler());
	}
	
	public void load() {
		Mw mw = new Mw();
		MinecraftForge.EVENT_BUS.register(new EventHandler(mw));
		FMLCommonHandler.instance().bus().register(new MwKeyHandler());
		MinecraftForge.EVENT_BUS.register(new MwKeyHandler());
	}
	
	public void postInit() {
		MwAPI.registerDataProvider("Slime", new OverlaySlime());
		MwAPI.registerDataProvider("Grid", new OverlayGrid());
	}
}
