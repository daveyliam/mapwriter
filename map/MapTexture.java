package mapwriter.map;

import java.util.ArrayList;
import java.util.List;

import mapwriter.BackgroundExecutor;
import mapwriter.Texture;
import mapwriter.region.MwChunk;
import mapwriter.region.Region;
import mapwriter.region.RegionManager;

import org.lwjgl.opengl.GL11;

public class MapTexture extends Texture {
	
	public int textureRegions;
	public int textureSize;
	
	public int viewUpdateCount = 0;
	
	public int requestedMinX = 0;
	public int requestedMinZ = 0;
	public int requestedMaxX = 0;
	public int requestedMaxZ = 0;
	public int requestedZoomLevel = 0;
	public int requestedDimension = 0;
	
	public int loadedMinX = 0;
	public int loadedMinZ = 0;
	public int loadedMaxX = 0;
	public int loadedMaxZ = 0;
	public int loadedZoomLevel = 0;
	public int loadedDimension = 0;
	
	private Region[] regionArray;
	
	// accessed from both render and background thread.
	// make sure all methods using it are synchronized.
	//private int[] regionModifiedArray;
	
	private class Rect {
		final int x, y, w, h;
		Rect(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
	}
	
	private List<Rect> textureUpdateQueue = new ArrayList<Rect>();
	
	public MapTexture(int textureSize, boolean linearScaling) {
		super(textureSize, textureSize, 0xff000000, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
		
		this.setLinearScaling(linearScaling);
		
		this.textureRegions = textureSize >> Region.SHIFT;
		this.textureSize = textureSize;
		this.regionArray = new Region[this.textureRegions * this.textureRegions];
	}
	
	public void requestView(MapView view, BackgroundExecutor executor, RegionManager regionManager) {
		// round to nearest multiple of 512
		
		int zoomLevel = view.getRegionZoomLevel();
		int size = Region.SIZE << zoomLevel;
		int minX = ((int) view.getMinX()) & (-size);
		int minZ = ((int) view.getMinZ()) & (-size);
		int maxX = ((int) view.getMaxX()) & (-size);
		int maxZ = ((int) view.getMaxZ()) & (-size);
		int dimension = view.getDimension();
		if ((this.viewUpdateCount <= 0) ||
				(minX != this.requestedMinX) ||
				(minZ != this.requestedMinZ) ||
				(maxX != this.requestedMaxX) ||
				(maxZ != this.requestedMaxZ) ||
				(zoomLevel != this.requestedZoomLevel) ||
				(dimension != this.requestedDimension)) {
			this.requestedMinX = minX;
			this.requestedMinZ = minZ;
			this.requestedMaxX = maxX;
			this.requestedMaxZ = maxZ;
			this.requestedZoomLevel = zoomLevel;
			this.requestedDimension = dimension;
			this.viewUpdateCount++;
			executor.addTask(new MapUpdateViewTask(this, regionManager));
		}
	}
	
	//
	// methods below this point run in the background thread
	//
	
	public void updateTextureFromRegion(Region region, int x, int z, int w, int h) {
		int tx = (x >> region.zoomLevel) & (this.w - 1);
		int ty = (z >> region.zoomLevel) & (this.h - 1);
		int tw = (w >> region.zoomLevel);
		int th = (h >> region.zoomLevel);
		
		// make sure we don't write outside texture
		tw = Math.min(tw, this.w - tx);
		th = Math.min(th, this.h - th);
		
		//MwUtil.log("updateTextureFromRegion: region %s, %d %d %d %d -> %d %d %d %d", region, x, z, w, h, tx, ty, tw, th);
		
		int[] pixels = region.getPixels();
		if (pixels != null) {
			this.setRGBOpaque(tx, ty, tw, th, pixels, region.getPixelOffset(x, z), Region.SIZE);
		} else {
			this.fillRect(tx, ty, tw, th, 0xff000000);
		}
		
		this.addTextureUpdate(tx, ty, tw, th);
	}
	
	public int getRegionIndex(int x, int z, int zoomLevel) {
		x = (x >> (Region.SHIFT + zoomLevel)) & (this.textureRegions - 1);
		z = (z >> (Region.SHIFT + zoomLevel)) & (this.textureRegions - 1);
		return (z * this.textureRegions) + x;
	}
	
	public boolean loadRegion(RegionManager regionManager, int x, int z, int zoomLevel, int dimension) {
		//MwUtil.log("mapTexture.loadRegion %d %d %d %d", x, z, zoomLevel, dimension);
		boolean alreadyLoaded = true;
		int index = this.getRegionIndex(x, z, zoomLevel);
		Region currentRegion = this.regionArray[index];
		if ((currentRegion == null) || (!currentRegion.equals(x, z, zoomLevel, dimension))) {
			// modifying the refCount here may be causing problems unloading regions
			//if (currentRegion != null) {
			//	currentRegion.refCount--;
			//}
			Region newRegion = regionManager.getRegion(x, z, zoomLevel, dimension);
			this.regionArray[index] = newRegion;
			//newRegion.refCount++;
			this.updateTextureFromRegion(newRegion, newRegion.x, newRegion.z, newRegion.size, newRegion.size);
			//MwUtil.log("regionArray[%d] = %s", newRegion.index, newRegion);
			alreadyLoaded = false;
		}
		return alreadyLoaded;
	}
	
	//public boolean isRegionInTexture(Region region) {
	//	return region.equals(this.regionArray[this.getRegionIndex(region.x, region.z, region.zoomLevel)]);
	//}
	
	public int loadRegions(RegionManager regionManager, int minX, int minZ, int maxX, int maxZ, int zoomLevel, int dimension) {
		int size = Region.SIZE << zoomLevel;
		int loadedCount = 0;
		for (int z = minZ; z <= maxZ; z += size) {
			for (int x = minX; x <= maxX; x += size) {
				if (!this.loadRegion(regionManager, x, z, zoomLevel, dimension)) {
					loadedCount++;
				}
			}
		}
		return loadedCount;
	}
	
	public void updateChunk(RegionManager regionManager, MwChunk chunk) {
		for (int i = 0; i < this.regionArray.length; i++) {
			Region region = this.regionArray[i];
			if ((region != null) && (region.isChunkWithin(chunk))) {
				this.updateTextureFromRegion(region, chunk.x << 4, chunk.z << 4, MwChunk.SIZE, MwChunk.SIZE);
			}
		}
	}
	
	public void addTextureUpdate(int x, int z, int w, int h) {
		synchronized (this.textureUpdateQueue) {
			this.textureUpdateQueue.add(new Rect(x, z, w, h));
		}
	}
	
	public void processTextureUpdates() {
		synchronized (this.textureUpdateQueue) {
			for (Rect rect : this.textureUpdateQueue) {
					//MwUtil.log("MwMapTexture.processTextureUpdates: %d %d %d %d", rect.x, rect.y, rect.w, rect.h);
					this.updateTextureArea(rect.x, rect.y, rect.w, rect.h);
			}
			this.textureUpdateQueue.clear();
		}
	}
}
