package mapwriter.tasks;

import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;

public class SaveChunkTask extends Task {
	private final MwChunk chunk;
	private final RegionManager regionManager;
	
	public SaveChunkTask(MwChunk chunk, RegionManager regionManager) {
		this.chunk = chunk;
		this.regionManager = regionManager;
	}

	@Override
	public void run() {
		this.chunk.write(this.regionManager.regionFileCache);
	}
	
	@Override
	public void onComplete() {
	}
}
