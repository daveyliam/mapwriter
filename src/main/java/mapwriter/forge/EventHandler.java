package mapwriter.forge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import mapwriter.Mw;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

public class EventHandler {
	
	Mw mw;
	
	public EventHandler(Mw mw) {
		this.mw = mw;
	}
	
	@ForgeSubscribe
	public void eventChunkLoad(ChunkEvent.Load event) {
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (side == Side.CLIENT) {
			this.mw.onChunkLoad(event.getChunk());
		}
	}
	
	@ForgeSubscribe
	public void eventChunkUnload(ChunkEvent.Unload event) {
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (side == Side.CLIENT) {
			this.mw.onChunkUnload(event.getChunk());
		}
	}
	
	@ForgeSubscribe
	public void eventWorldLoad(WorldEvent.Load event) {
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (side == Side.CLIENT) {
			this.mw.onWorldLoad(event.world);
		}
	}
	
	@ForgeSubscribe
	public void eventWorldUnload(WorldEvent.Unload event) {
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (side == Side.CLIENT) {
			this.mw.onWorldUnload(event.world);
		}
	}
	
	@ForgeSubscribe
	public void eventLivingDeath(LivingDeathEvent event) {
		// server side only event :(
		/*
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		Mw.log("death event for entity %s %d, side %s, server %s",
				event.entity, event.entity.entityId, side, MinecraftServer.getServer());
		/if (side == Side.CLIENT) {
			if (Minecraft.getMinecraft().thePlayer != null) {
				if (event.entity.entityId == Minecraft.getMinecraft().thePlayer.entityId) {
					this.mw.onPlayerDeath();
				}
			}
		}
		*/
	}
}
