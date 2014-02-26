package mapwriter.map;

import java.util.ArrayList;
import java.util.List;

import mapwriter.BackgroundExecutor;
import mapwriter.Texture;
import mapwriter.region.Region;
import mapwriter.region.RegionManager;

import org.lwjgl.opengl.GL11;

public class MapTexture extends Texture {
	
	public int textureRegions;
	public int textureSize;
	
	private MapViewRequest loadedView = null;
	private MapViewRequest requestedView = null;
	
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
		super(textureSize, textureSize, 0x00000000, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
		
		this.setLinearScaling(linearScaling);
		
		this.textureRegions = textureSize >> Region.SHIFT;
		this.textureSize = textureSize;
		this.regionArray = new Region[this.textureRegions * this.textureRegions];
	}
	
	public void requestView(MapViewRequest req, BackgroundExecutor executor, RegionManager regionManager) {
		if ((this.requestedView == null) || (!this.requestedView.equals(req))) {
			this.requestedView = req;
			executor.addTask(new MapUpdateViewTask(this, regionManager, req));
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
	
	public void setLoaded(MapViewRequest req) {
		this.loadedView = req;
	}
	
	public boolean isLoaded(MapViewRequest req) {
		return (this.loadedView != null) && (this.loadedView.mostlyEquals(req));
	}
	
	//
	// methods below this point run in the background thread
	//
	
	public synchronized void setRGBOpaque(int x, int y, int w, int h, int[] pixels, int offset, int scanSize) {
		// TODO: Remove the need for this function. It would better if the
		// region pixels were stored as normal pixels (without the height in
		// the alpha channel). Then we could just directly copy the pixels
		// to the texture pixelBuf.
		int bufOffset = (y * this.w) + x;
		for (int i = 0; i < h; i++) {
			this.setPixelBufPosition(bufOffset + (i * this.w));
			int rowOffset = offset + (i * scanSize);
			for (int j = 0; j < w; j++) {
				int colour = pixels[rowOffset + j];
				if(colour != 0) {
					colour |= 0xff000000;
				}
				this.pixelBufPut(colour);
			}
		}
	}
	
	public void addTextureUpdate(int x, int z, int w, int h) {
		synchronized (this.textureUpdateQueue) {
			this.textureUpdateQueue.add(new Rect(x, z, w, h));
		}
	}
	
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
			this.fillRect(tx, ty, tw, th, 0x00000000);
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
		boolean loaded = false;
		int index = this.getRegionIndex(x, z, zoomLevel);
		Region currentRegion = this.regionArray[index];
		if ((currentRegion == null) || (!currentRegion.equals(x, z, zoomLevel, dimension))) {
			Region newRegion = regionManager.getRegion(x, z, zoomLevel, dimension);
			this.regionArray[index] = newRegion;
			this.updateTextureFromRegion(
				newRegion, newRegion.x, newRegion.z, newRegion.size, newRegion.size
			);
			//regionManager.logInfo("regionArray[%d] = %s", index, newRegion);
			loaded = true;
		}
		return loaded;
	}
	
	public int loadRegions(RegionManager regionManager, MapViewRequest req) {
		int size = Region.SIZE << req.zoomLevel;
		int loadedCount = 0;
		for (int z = req.zMin; z <= req.zMax; z += size) {
			for (int x = req.xMin; x <= req.xMax; x += size) {
				if (this.loadRegion(regionManager, x, z, req.zoomLevel, req.dimension)) {
					loadedCount++;
				}
			}
		}
		return loadedCount;
	}
	
	public void updateArea(RegionManager regionManager, int x, int z, int w, int h, int dimension) {
		for (int i = 0; i < this.regionArray.length; i++) {
			Region region = this.regionArray[i];
			if ((region != null) && (region.isAreaWithin(x, z, w, h, dimension))) {
				this.updateTextureFromRegion(region, x, z, w, h);
			}
		}
	}
}
