package mapwriter.map;

import mapwriter.Task;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;

public class MapUpdateViewTask extends Task {
	final MapViewRequest req;
	int loadedCount = 0;
	MwChunk[] chunksToUpdate;
	RegionManager regionManager;
	MapTexture mapTexture;
	
	// chunkmanager will need to keep a list of chunks to update.
	// it should also keep a 2D chunkSum array so that only modified chunks are updated.
	public MapUpdateViewTask(MapTexture mapTexture, RegionManager regionManager, MapViewRequest req) {
		
		this.mapTexture = mapTexture;
		this.regionManager = regionManager;
		this.req = req;
	}
	
	@Override
	public void run() {
		// load regions for view
		this.loadedCount = this.mapTexture.loadRegions(this.regionManager, this.req);
		
		// update region pixels with chunk data
		// copy updated pixels to maptexture
		// unload least accessed regions
		this.regionManager.pruneRegions();
	}
	
	@Override
	public void onComplete() {
		// set currentView in mapTexture to requestedView
		this.mapTexture.setLoaded(this.req);
		
		//MwUtil.log("MapUpdateViewTask: loaded %d regions", this.loadedCount);
	}
}
