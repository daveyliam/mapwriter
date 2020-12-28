package mapwriter.forge;

import java.util.logging.Logger;

import mapwriter.Mw;
import net.minecraft.client.Minecraft;
//import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
//import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkMod;

@Mod(modid="MapWriter", name="MapWriter", version="2.1.1")
@NetworkMod(clientSideRequired=true, serverSideRequired=false)
public class MwForge {
	
	@Instance("MapWriter")
	public static MwForge instance;
	
	@SidedProxy(clientSide="mapwriter.forge.ClientProxy", serverSide="mapwriter.forge.CommonProxy")
	public static CommonProxy proxy;
	
	//public static Logger logger = LogManager.getLogger("MapWriter");
    public static Logger logger;
	
	@PreInit
	public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
        proxy.preInit(event.getSuggestedConfigurationFile());
	}
	
	@Init
	public void load(FMLInitializationEvent event) {
		proxy.load();
	}
	
	@PostInit
	public void postInit(FMLPostInitializationEvent event) {
		proxy.postInit();
	}
	
    // TODO is this neccessary anymore?
    /*@SubscribeEvent
    public void renderMap(RenderGameOverlayEvent.Post event){
        if(event.type == RenderGameOverlayEvent.ElementType.ALL){
            Mw.instance.onTick();
        }
    }*/

    // TODO is this neccessary anymore? already done on disconnect in MwConnectionHandler
    /*@SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event){
        if (event.phase == TickEvent.Phase.START){
        	// run the cleanup code when Mw is loaded and the player becomes null.
        	// a bit hacky, but simpler than checking if the connection has closed.
            if ((Mw.instance.ready) && (Minecraft.getMinecraft().thePlayer == null)) {
                Mw.instance.close();
            }
        }
    }*/
}
