package mapwriter.map;

import mapwriter.Task;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;

public class MapUpdateViewTask extends Task {
	final int minX, minZ, maxX, maxZ, zoomLevel, dimension;
	int loadedCount = 0;
	MwChunk[] chunksToUpdate;
	RegionManager regionManager;
	MapTexture mapTexture;
	
	// chunkmanager will need to keep a list of chunks to update.
	// it should also keep a 2D chunkSum array so that only modified chunks are updated.
	public MapUpdateViewTask(MapTexture mapTexture, RegionManager regionManager) {
		
		this.mapTexture = mapTexture;
		this.regionManager = regionManager;
		this.minX = mapTexture.requestedMinX;
		this.minZ = mapTexture.requestedMinZ;
		this.maxX = mapTexture.requestedMaxX;
		this.maxZ = mapTexture.requestedMaxZ;
		this.zoomLevel = mapTexture.requestedZoomLevel;
		this.dimension = mapTexture.requestedDimension;
	}
	
	@Override
	public void run() {
		// load regions for view
		this.loadedCount = this.mapTexture.loadRegions(this.regionManager,
				this.minX, this.minZ, this.maxX, this.maxZ, this.zoomLevel, this.dimension);
		
		// update region pixels with chunk data
		// copy updated pixels to maptexture
		// unload least accessed regions
		this.regionManager.pruneRegions();
	}
	
	@Override
	public void onComplete() {
		// set currentView in mapTexture to requestedView
		this.mapTexture.loadedMinX = this.minX;
		this.mapTexture.loadedMinZ = this.minZ;
		this.mapTexture.loadedMaxX = this.maxX;
		this.mapTexture.loadedMaxZ = this.maxZ;
		this.mapTexture.loadedZoomLevel = this.zoomLevel;
		this.mapTexture.loadedDimension = this.dimension;
		
		// update GL texture of mapTexture if updated
		this.mapTexture.updateGLTexture();
		
		//MwUtil.log("MapUpdateViewTask: loaded %d regions", this.loadedCount);
	}
}
