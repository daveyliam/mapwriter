package mapwriter.forge;

import mapwriter.Mw;
import mapwriter.overlay.OverlaySlime;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.event.ForgeSubscribe;

public class EventHandler {
    
    @ForgeSubscribe
    public void eventChunkLoad(ChunkEvent.Load event) {
        Side side = FMLCommonHandler.instance().getEffectiveSide();
        if (side == Side.CLIENT) {
            Mw.instance.onChunkLoad(event.getChunk());
        }
    }
    
    @ForgeSubscribe
    public void eventChunkUnload(ChunkEvent.Unload event) {
        Side side = FMLCommonHandler.instance().getEffectiveSide();
        if (side == Side.CLIENT) {
            Mw.instance.onChunkUnload(event.getChunk());
        }
    }
    
    @ForgeSubscribe
    public void eventWorldLoad(WorldEvent.Load event) {
        Side side = FMLCommonHandler.instance().getEffectiveSide();
        if (side == Side.CLIENT) {
            Mw.instance.onWorldLoad(event.world);
        }
    }
    
    @ForgeSubscribe
    public void eventWorldUnload(WorldEvent.Unload event) {
        Side side = FMLCommonHandler.instance().getEffectiveSide();
        if (side == Side.CLIENT) {
            Mw.instance.onWorldUnload(event.world);
        }
    }

    //@SubscribeEvent
    @ForgeSubscribe
    public void eventClientChat(ClientChatReceivedEvent event){
        if(OverlaySlime.seedFound || !OverlaySlime.seedAsked) return;
        try{ //I don't want to crash the game when we derp up in here
            if(event.message.startsWith("Seed: ")){ //Because bukkit...
                OverlaySlime.setSeed(Long.parseLong(event.message.substring(6)));
                event.setCanceled(true); //Don't let the player see this seed message, They didn't do /seed, we did
            }
        }catch(Exception e){
            //e.printStackTrace();
        }
    }
}
