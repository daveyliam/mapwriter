package mapwriter.tasks;

import java.util.concurrent.atomic.AtomicBoolean;

import mapwriter.Mw;
import mapwriter.map.MapTexture;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;

public class UpdateSurfaceChunksTask extends Task
{
	MwChunk chunk;
	RegionManager regionManager;
	MapTexture mapTexture;
	public AtomicBoolean Running = new AtomicBoolean();

	public UpdateSurfaceChunksTask(Mw mw, MwChunk chunk)
	{
		this.mapTexture = mw.mapTexture;
		this.regionManager = mw.regionManager;
		this.chunk = chunk;
	}

	@Override
	public void run()
	{
		this.Running.set(true);
		// for (MwChunk chunk : this.chunkArray) {
		if (this.chunk != null)
		{
			// update the chunk in the region pixels
			this.regionManager.updateChunk(this.chunk);
			// copy updated region pixels to maptexture
			this.mapTexture.updateArea(this.regionManager, this.chunk.x << 4, this.chunk.z << 4, MwChunk.SIZE, MwChunk.SIZE, this.chunk.dimension);
		}
		// }
		this.Running.set(false);
	}

	@Override
	public void onComplete()
	{
	}

	public int getChunkX()
	{
		return this.chunk.x;
	}

	public int getChunkZ()
	{
		return this.chunk.z;
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
