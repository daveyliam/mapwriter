package mapwriter.forge;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import mapwriter.api.MwAPI;
import mapwriter.overlay.OverlayGrid;
import mapwriter.overlay.OverlaySlime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid="MapWriter", name="MapWriter", version="2.0")
public class MwForge {
	
	private MwConfig config;
	
	@Instance("MapWriter")
	public static MwForge instance;
	
	@SidedProxy(clientSide="mapwriter.forge.ClientProxy", serverSide="mapwriter.forge.CommonProxy")
	public static CommonProxy proxy;
	
	public static Logger logger = LogManager.getLogger("MapWriter");
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger.info("FML Event: preInit");
		this.config = new MwConfig(event.getSuggestedConfigurationFile());
	}
	
	@EventHandler
	public void load(FMLInitializationEvent event) {
		logger.info("FML Event: load");
		proxy.init(this.config);
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		logger.info("FML Event: postInit");

		MwAPI.registerDataProvider("Slime", new OverlaySlime());
		MwAPI.registerDataProvider("Grid", new OverlayGrid());
		//MwAPI.registerDataProvider("Checker", new OverlayChecker());
		//MwAPI.setCurrentDataProvider("Slime");
	}
}
