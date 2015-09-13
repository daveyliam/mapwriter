package mapwriter.tasks;

import java.util.concurrent.atomic.AtomicBoolean;

import mapwriter.Mw;
import mapwriter.map.MapTexture;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;
import mapwriter.util.Logging;

public class UpdateSurfaceChunksTask extends Task {
	MwChunk chunk;
	RegionManager regionManager;
	MapTexture mapTexture;
	public AtomicBoolean Running = new AtomicBoolean();
	
	public UpdateSurfaceChunksTask(Mw mw, MwChunk chunk) {
		this.mapTexture = mw.mapTexture;
		this.regionManager = mw.regionManager;
		this.chunk = chunk;
	}
	
	@Override
	public void run() {
		Running.set(true);;
		//for (MwChunk chunk : this.chunkArray) {
			if (chunk != null) {
				// update the chunk in the region pixels
				this.regionManager.updateChunk(chunk);
				// copy updated region pixels to maptexture
				this.mapTexture.updateArea(
					this.regionManager,
					chunk.x << 4, chunk.z << 4,
					MwChunk.SIZE, MwChunk.SIZE, chunk.dimension
				);
			}
		//}
			Running.set(false);
	}
	
	@Override
	public void onComplete() {
	}
	
	public int getChunkX()
	{
		return chunk.x;
	}
	
	public int getChunkZ()
	{
		return chunk.z;
	}

	public void UpdateChunkData(MwChunk chunk)
	{
		this.chunk = chunk;
	}
	public MwChunk getChunk()
	{
		return this.chunk;
	}
}
