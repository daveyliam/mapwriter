package mapwriter.map;

import mapwriter.region.RegionManager;
import mapwriter.tasks.Task;

public class MapUpdateViewTask extends Task {
	final MapViewRequest req;
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
		this.mapTexture.loadRegions(this.regionManager, this.req);
	}
	
	@Override
	public void onComplete() {
		// set currentView in mapTexture to requestedView
		this.mapTexture.setLoaded(this.req);
	}
}
