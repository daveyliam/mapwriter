package mapwriter.map;

import java.util.ArrayList;

import mapwriter.Mw;
import mapwriter.region.Region;
import mapwriter.region.RegionManager;

import org.lwjgl.opengl.GL11;

public class MapTexture extends Texture {
	
	public int textureRegions;
	public int textureSize;
	
	class Rect {
		final int x, y, w, h;
		Rect(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
	}
	
	private Region[] regionArray;
	private ArrayList<Rect> textureUpdateQueue = new ArrayList<Rect>();
	
	public MapTexture(int textureSize) {
		super(textureSize, textureSize, 0xff000000, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
		
		this.textureRegions = textureSize >> Mw.REGION_SHIFT;
		this.textureSize = textureSize;
		this.regionArray = new Region[this.textureRegions * this.textureRegions];
	}
	
	public int getRegionIndex(int x, int z, int zoomLevel) {
		x = (x >> (Mw.REGION_SHIFT + zoomLevel)) & (this.textureRegions - 1);
		z = (z >> (Mw.REGION_SHIFT + zoomLevel)) & (this.textureRegions - 1);
		return (z * this.textureRegions) + x;
	}
	
	public void requestRegion(RegionManager regionManager, int x, int z, int zoomLevel, int dimension) {
		int index = this.getRegionIndex(x, z, zoomLevel);
		Region currentRegion = this.regionArray[index];
		if ((currentRegion == null) || (!currentRegion.equals(x, z, zoomLevel, dimension))) {
			Region newRegion = regionManager.getRegion(x, z, zoomLevel, dimension);
			this.regionArray[index] = newRegion;
			this.fillAndUpdateRegionArea(newRegion, 0xff000000);
			newRegion.addLoadTask(this);
			//MwUtil.log("regionArray[%d] = %s", newRegion.index, newRegion);
		}
	}
	
	public boolean isRegionInTexture(Region region) {
		return region.equals(this.regionArray[this.getRegionIndex(region.x, region.z, region.zoomLevel)]);
	}
	
	public void update(RegionManager regionManager, MapView mapView) {
		this.processTextureUpdates();
		
		int zoomLevel = mapView.getRegionZoomLevel();
		int x = (int) mapView.getMinX();
		int z = (int) mapView.getMinZ();
		int dimension = mapView.getDimension();
		
		int rS = Mw.REGION_SIZE << zoomLevel;
		for (int j = 0; j < this.textureRegions; j++) {
			for (int i = 0; i < this.textureRegions; i++) {
				this.requestRegion(regionManager,
						x + (i << (zoomLevel + Mw.REGION_SHIFT)),
						z + (j << (zoomLevel + Mw.REGION_SHIFT)),
						zoomLevel, dimension);
			}
		}
	}
	
	public boolean rectWithinTexture(int tx, int ty, int tw, int th) {
		return (tx >= 0) && ((tx + tw) <= this.textureSize) &&
				(ty >= 0) && ((ty + th) <= this.textureSize);
	}
	
	public void fillAndUpdateRegionArea(Region region, int colour) {
		int tx = (region.x >> region.zoomLevel) & (this.textureSize - 1);
		int ty = (region.z >> region.zoomLevel) & (this.textureSize - 1);
		int tw = (region.size >> region.zoomLevel);
		int th = (region.size >> region.zoomLevel);
		this.fillRect(tx, ty, tw, th, colour);
		this.updateTextureArea(tx, ty, tw, th);
	}
	
	public void updateFromRegion(Region region, int x, int z, int w, int h) {
		int tx = (x >> region.zoomLevel) & (this.textureSize - 1);
		int ty = (z >> region.zoomLevel) & (this.textureSize - 1);
		int tw = (w >> region.zoomLevel);
		int th = (h >> region.zoomLevel);
		int[] pixels = region.getPixels();
		if (pixels != null) {
			//MwUtil.log("updating maptexture from region %s, %d %d -> %d %d %d %d", region, x, z, tx, ty, tw, th);
			this.setRGBOpaque(tx, ty, tw, th, pixels, region.getPixelOffset(x, z), Mw.REGION_SIZE);
		}
		// not needed if area filled upon region request
		//else {
		//	this.fillRect(tx, ty, tw, th, 0xff000000);
		//}
		this.addTextureUpdate(tx, ty, tw, th);
	}
	
	public void addTextureUpdate(int x, int z, int w, int h) {
		synchronized (this.textureUpdateQueue) {
			this.textureUpdateQueue.add(new Rect(x, z, w, h));
		}
	}
	
	public void processTextureUpdates() {
		synchronized (this.textureUpdateQueue) {
			for (Rect rect : this.textureUpdateQueue) {
				if (this.rectWithinTexture(rect.x, rect.y, rect.w, rect.h)) {
					//MwUtil.log("MwMapTexture.processTextureUpdates: %d %d %d %d", rect.x, rect.y, rect.w, rect.h);
					this.updateTextureArea(rect.x, rect.y, rect.w, rect.h);
				}
			}
			this.textureUpdateQueue.clear();
		}
	}
}
