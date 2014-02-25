package mapwriter.forge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import mapwriter.Mw;
import mapwriter.api.MwAPI;
import mapwriter.overlay.OverlayGrid;
import mapwriter.overlay.OverlaySlime;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

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

        FMLCommonHandler.instance().bus().register(new MwKeyHandler());
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
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

    @SubscribeEvent
    public void renderMap(RenderGameOverlayEvent.Post event){
        if(event.type == RenderGameOverlayEvent.ElementType.ALL){
            Mw.instance.onTick();
        }
    }

    @SubscribeEvent
    public void onConnected(FMLNetworkEvent.ClientConnectedToServerEvent event){
    	if (!event.isLocal) {
    		InetSocketAddress address = (InetSocketAddress) event.manager.getSocketAddress();
    		Mw.instance.setServerDetails(address.getHostName(), address.getPort());
    	}
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event){
        if (event.phase == TickEvent.Phase.START){
        	// run the cleanup code when Mw is loaded and the player becomes null.
        	// a bit hacky, but simpler than checking if the connection has closed.
            if ((Mw.instance.ready) && (Minecraft.getMinecraft().thePlayer == null)) {
                Mw.instance.close();
            }
        }
    }
}
