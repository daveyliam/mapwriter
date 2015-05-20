package mapwriter.forge;

import mapwriter.Mw;
import mapwriter.overlay.OverlaySlime;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class EventHandler {

	Mw mw;

	public EventHandler(Mw mw) {
		this.mw = mw;
	}

	@SubscribeEvent
	public void eventChunkLoad(ChunkEvent.Load event) {
		if (event.world.isRemote) {
			this.mw.onChunkLoad(event.getChunk());
		}
	}

	@SubscribeEvent
	public void eventChunkUnload(ChunkEvent.Unload event) {
		if (event.world.isRemote) {
			this.mw.onChunkUnload(event.getChunk());
		}
	}

	@SubscribeEvent
	public void onClientChat(ClientChatReceivedEvent event) {
		if (OverlaySlime.seedFound || !OverlaySlime.seedAsked)
			return;
		try { // I don't want to crash the game when we derp up in here
			if (event.message instanceof ChatComponentTranslation) {
				ChatComponentTranslation component = (ChatComponentTranslation) event.message;
				if (component.getKey().equals("commands.seed.success")) {
					OverlaySlime.setSeed((Long) component.getFormatArgs()[0]);
					event.setCanceled(true); // Don't let the player see this
												// seed message, They didn't do
												// /seed, we did
				}
			} else if (event.message instanceof ChatComponentText) {
				ChatComponentText component = (ChatComponentText) event.message;
				String msg = component.getUnformattedText();
				if (msg.startsWith("Seed: ")) { // Because bukkit...
					OverlaySlime.setSeed(Long.parseLong(msg.substring(6)));
					event.setCanceled(true); // Don't let the player see this
												// seed message, They didn't do
												// /seed, we did
				}
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void eventPlayerDeath(LivingDeathEvent event) {
		if (!event.isCanceled()) {
			if (event.entityLiving.getEntityId() == net.minecraft.client.Minecraft
					.getMinecraft().thePlayer.getEntityId()) {
				this.mw.onPlayerDeath((EntityPlayerMP)event.entityLiving);
			}
		}
	}
	
    //@SubscribeEvent
    //public void renderMap(RenderGameOverlayEvent.Post event){
    //   if(event.type == RenderGameOverlayEvent.ElementType.ALL){
    //        Mw.getInstance().onTick();
    //    }
    //}
    
	//using the FML render tick event because RenderGameOverlayEvent.Post creates flickering 
    @SubscribeEvent
    public void onTick(RenderTickEvent event)
    {
        if (event.phase.equals(Phase.START))
        {
        	return;
        }
        if (Mw.getInstance().ready)
        {
        	Mw.getInstance().onTick();
        }
    }
}
