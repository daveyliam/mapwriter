package mapwriter.forge;

import java.io.File;

import mapwriter.Mw;
import mapwriter.api.MwAPI;
import mapwriter.overlay.OverlayGrid;
import mapwriter.overlay.OverlaySlime;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

public class ClientProxy extends CommonProxy {
	
	private MwConfig config;
	
	public void preInit(File configFile) {
		this.config = new MwConfig(configFile);
	}
	
	public void load() {
		Mw mw = new Mw(this.config);
		MinecraftForge.EVENT_BUS.register(new EventHandler());
		MinecraftForge.EVENT_BUS.register(new MwKeyHandler());
		// temporary workaround for user defined key bindings not being loaded
		// at game start. see https://github.com/MinecraftForge/FML/issues/378
		// for more info.
		Minecraft.getMinecraft().gameSettings.loadOptions();
		NetworkRegistry.instance().registerConnectionHandler(new MwConnectionHandler());
		TickRegistry.registerTickHandler(new MwTickHandler(), Side.CLIENT);
	}
	
	public void postInit() {
		MwAPI.registerDataProvider("Slime", new OverlaySlime());
		MwAPI.registerDataProvider("Grid", new OverlayGrid());
		//MwAPI.registerDataProvider("Checker", new OverlayChecker());
		//MwAPI.setCurrentDataProvider("Slime");
	}
}
