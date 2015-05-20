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
		EventHandler eventHandler = new EventHandler(Mw.getInstance());
		MinecraftForge.EVENT_BUS.register(eventHandler);
		FMLCommonHandler.instance().bus().register(eventHandler);
		
		MwKeyHandler keyEventHandler = new MwKeyHandler();
		FMLCommonHandler.instance().bus().register(keyEventHandler);
		MinecraftForge.EVENT_BUS.register(keyEventHandler);
	}
	
	public void postInit() {
		MwAPI.registerDataProvider("Slime", new OverlaySlime());
		MwAPI.registerDataProvider("Grid", new OverlayGrid());
	}
}
