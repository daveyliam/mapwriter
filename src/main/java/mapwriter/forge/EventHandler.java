package mapwriter.forge;

import mapwriter.Mw;
import mapwriter.overlay.OverlaySlime;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class EventHandler {
	
	Mw mw;
	
	public EventHandler(Mw mw) {
		this.mw = mw;
	}
	
	@SubscribeEvent
	public void eventChunkLoad(ChunkEvent.Load event){
		if(event.world.isRemote){
			this.mw.onChunkLoad(event.getChunk());
		}
	}
	
	@SubscribeEvent
	public void eventChunkUnload(ChunkEvent.Unload event){
		if(event.world.isRemote){
			this.mw.onChunkUnload(event.getChunk());
		}
	}
	
	@SubscribeEvent
	public void eventWorldLoad(WorldEvent.Load event){
		if(event.world.isRemote){
			this.mw.onWorldLoad(event.world);
		}
	}

    @SubscribeEvent
    public void eventWorldUnload(WorldEvent.Unload event){
        if(event.world.isRemote){
            this.mw.onWorldUnload(event.world);
        }
    }

    @SubscribeEvent
    public void onClientChat(ClientChatReceivedEvent event){
        if(OverlaySlime.seedFound || !OverlaySlime.seedAsked) return;
        try{ //I don't want to crash the game when we derp up in here
            if(event.message instanceof ChatComponentTranslation){
                ChatComponentTranslation component = (ChatComponentTranslation) event.message;
                if(component.getKey().equals("commands.seed.success")){
                    OverlaySlime.setSeed((Long) component.getFormatArgs()[0]);
                    event.setCanceled(true); //Don't let the player see this seed message, They didn't do /seed, we did
                }
            }else if(event.message instanceof ChatComponentText){
                ChatComponentText component = (ChatComponentText) event.message;
                String msg = component.getUnformattedText();
                if(msg.startsWith("Seed: ")){ //Because bukkit...
                    OverlaySlime.setSeed(Long.parseLong(msg.substring(6)));
                    event.setCanceled(true); //Don't let the player see this seed message, They didn't do /seed, we did
                }
            }
        }catch(Exception e){
            //e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onTextureStitchEventPost(TextureStitchEvent.Post event){ 
    	if (event.map.getTextureType() == 0)
    	{
    		mw.reloadBlockColours();
    	}
    }
}
