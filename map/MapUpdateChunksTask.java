package mapwriter.map;

import mapwriter.Task;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;

public class MapUpdateChunksTask extends Task {
	MwChunk[] chunkArray;
	RegionManager regionManager;
	MapTexture mapTexture;
	
	// chunkmanager will need to keep a list of chunks to update.
	// it should also keep a 2D chunkSum array so that only modified chunks are updated.
	public MapUpdateChunksTask(MapTexture mapTexture, RegionManager regionManager, MwChunk[] chunkArray) {
		this.mapTexture = mapTexture;
		this.regionManager = regionManager;
		
		// copy the chunks that need to be updated
		this.chunkArray = new MwChunk[chunkArray.length];
		System.arraycopy(chunkArray, 0, this.chunkArray, 0, chunkArray.length);
	}
	
	@Override
	public void run() {
		for (MwChunk chunk : this.chunkArray) {
			if (chunk != null) {
				this.regionManager.updateChunk(chunk);
				this.mapTexture.updateChunk(this.regionManager, chunk);
			}
		}
		// copy updated pixels to maptexture
		// unload least accessed regions
		this.regionManager.pruneRegions();
	}
	
	@Override
	public void onComplete() {
		// update GL texture of mapTexture if updated
		this.mapTexture.updateGLTexture();
	}
}
