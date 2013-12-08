package mapwriter.tasks;

import mapwriter.Mw;
import mapwriter.MwUtil;
import mapwriter.region.BlockColours;
import mapwriter.region.RegionManager;

public class RebuildRegionsTask extends Task {
	
	final RegionManager regionManager;
	final BlockColours blockColours;
	final int x, z, w, h, dimension;
	String msg = "";
	
	public RebuildRegionsTask(Mw mw, int x, int z, int w, int h, int dimension) {
		this.regionManager = mw.regionManager;
		this.blockColours = mw.blockColours;
		this.x = x;
		this.z = z;
		this.w = w;
		this.h = h;
		this.dimension = dimension;
	}
	
	@Override
	public void run() {
		this.regionManager.blockColours = blockColours;
		this.regionManager.rebuildRegions(this.x, this.z, this.w, this.h, this.dimension);
	}
	
	@Override
	public void onComplete() {
		MwUtil.printBoth("rebuild task complete");
	}

}
