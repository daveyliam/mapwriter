package mapwriter;

import mapwriter.region.BlockColours;
import mapwriter.region.RegionManager;

public class ReloadRegionsTask extends Task {
	
	final RegionManager regionManager;
	final BlockColours blockColours;
	final int x, z, w, h, dimension;
	String msg = "";
	
	public ReloadRegionsTask(Mw mw, int x, int z, int w, int h, int dimension) {
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
		this.regionManager.reloadRegions(this.x, this.z, this.w, this.h, this.dimension);
	}
	
	@Override
	public void onComplete() {
		MwUtil.printBoth("regenerate task complete");
	}

}
