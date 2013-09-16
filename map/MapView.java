package mapwriter.map;

import java.util.ArrayList;

import mapwriter.Mw;
import mapwriter.map.mapmode.MapMode;

public class MapView {
	
	private int zoomLevel = 0;
	private int dimension = 0;
	private int textureSize = 2048;
	
	// the position of the centre of the 'view' of the map using game (block) coordinates
	private double x = 0.0;
	private double z = 0.0;
	
	// width and height of map to display in pixels
	private int mapW = 1;
	private int mapH = 1;
	
	// the width and height of the map in blocks at zoom level 0.
	// updated when map width, map height, or texture size changes.
	private int baseW = 1;
	private int baseH = 1;
	
	// the width and height of the map in blocks at the current
	// zoom level.
	public double w = 1.0;
	public double h = 1.0;
	
	// the last coordinate the view was updated at.
	// if the view moves 256 blocks away from this position a texture update is triggered.
	public int lastUpdateX = 0;
	public int lastUpdateZ = 0;
	
	public void setViewCentre(double vX, double vZ) {
		this.x = vX;
		this.z = vZ;
	}
	
	public double getX() {
		return this.x;
	}
	
	public double getZ() {
		return this.z;
	}
	
	public double getWidth() {
		return this.w;
	}
	
	public double getHeight() {
		return this.h;
	}
	
	public void panView(double relX, double relZ) {
		this.setViewCentre(
				this.x + (relX * this.w),
				this.z + (relZ * this.h));
	}
	
	public int setZoomLevel(int zoomLevel) {
		//MwUtil.log("MapView.setZoomLevel(%d)", zoomLevel);
		int prevZoomLevel = this.zoomLevel;
		this.zoomLevel = Math.min(Math.max(Mw.minZoom, zoomLevel), Mw.maxZoom);
		if (prevZoomLevel != this.zoomLevel) {
			this.updateZoom();
		}
		return this.zoomLevel;
	}
	
	private void updateZoom() {
		if (this.zoomLevel >= 0) {
			this.w = this.baseW << this.zoomLevel;
			this.h = this.baseH << this.zoomLevel;
		} else {
			this.w = this.baseW >> (-this.zoomLevel);
			this.h = this.baseH >> (-this.zoomLevel);
		}
	}
	
	public int adjustZoomLevel(int n) {
		return this.setZoomLevel(this.zoomLevel + n);
	}
	
	public int getZoomLevel() {
		return this.zoomLevel;
	}
	
	public int getRegionZoomLevel() {
		return Math.max(0, this.zoomLevel);
	}
	
	// bX and bZ are the coordinates of the block the zoom is centred on.
	// The relative position of the block in the view will remain the same
	// as before the zoom.
	public void zoomToPoint(int newZoomLevel, double bX, double bZ) {
		int prevZoomLevel = this.zoomLevel;
		newZoomLevel = this.setZoomLevel(newZoomLevel);
		double zF = Math.pow(2, newZoomLevel - prevZoomLevel);
    	this.setViewCentre(
				bX - ((bX - this.x) * zF),
				bZ - ((bZ - this.z) * zF));
	}
	
	public void setDimension(int dimension) {
		double scale = 8.0;
		if ((this.dimension != -1) && (dimension == -1)) {
			scale = 0.125;
		} else if ((this.dimension == -1) && (dimension != -1)) {
			scale = 8.0;
		}
		this.dimension = dimension;
		this.setViewCentre(this.x * scale, this.z * scale);
	}
	
	public void setDimensionAndAdjustZoom(int dimension) {
		int zoomLevelChange = 0;
		if ((this.dimension != -1) && (dimension == -1)) {
			zoomLevelChange = -3;
		} else if ((this.dimension == -1) && (dimension != -1)) {
			zoomLevelChange = 3;
		}
		this.setZoomLevel(this.getZoomLevel() + zoomLevelChange);
		this.setDimension(dimension);
	}
	
	public void nextDimension(ArrayList<Integer> dimensionList, int n) {
		int i = dimensionList.indexOf(this.dimension);
		i = Math.max(0,  i);
		int size = dimensionList.size();
		int dimension = dimensionList.get((i + size + n) % size);
		this.setDimensionAndAdjustZoom(dimension);
	}
	
	public int getDimension() {
		return this.dimension;
	}
	
	public void setMapWH(int w, int h) {
		if ((this.mapW != w) || (this.mapH != h)) {
			this.mapW = w;
			this.mapH = h;
			this.updateBaseWH();
		}
	}
	
	public void setMapWH(MapMode mapMode) {
		this.setMapWH(mapMode.wPixels, mapMode.hPixels);
	}
	
	public double getMinX() {
		return this.x - (this.w / 2);
	}
	
	public double getMaxX() {
		return this.x + (this.w / 2);
	}
	
	public double getMinZ() {
		return this.z - (this.h / 2);
	}
	
	public double getMaxZ() {
		return this.z + (this.h / 2);
	}
	
	public double getDimensionScaling(int playerDimension) {
		double scale;
		if ((this.dimension != -1) && (playerDimension == -1)) {
			scale = 8.0;
		} else if ((this.dimension == -1) && (playerDimension != -1)) {
			scale = 0.125;
		} else {
			scale = 1.0;
		}
		return scale;
	}
	
	public void setViewCentreScaled(double vX, double vZ, int playerDimension) {
		double scale = this.getDimensionScaling(playerDimension);
		this.setViewCentre(vX * scale, vZ * scale);
	}
	
	public void setTextureSize(int n) {
		if (this.textureSize != n) {
			this.textureSize = n;
			this.updateBaseWH();
		}
	}
	
	private void updateBaseWH() {
		int w = this.mapW;
		int h = this.mapH;
		int halfTextureSize = this.textureSize / 2;
		
		// if we cannot display the map at 1x1 pixel per block, then
		// try 2x2 pixels per block, then 4x4 and so on
		while ((w > halfTextureSize) ||  (h > halfTextureSize)) {
			w /= 2;
			h /= 2;
		}
		
		//MwUtil.log("MapView.updateBaseWH: map = %dx%d, tsize = %d, base = %dx%d", this.mapW, this.mapH, this.textureSize, w, h);
		
		this.baseW = w;
		this.baseH = h;
		
		this.updateZoom();
	}
	
	public int getPixelsPerBlock() {
		return this.mapW / this.baseW;
	}
	
	public boolean isBlockWithinView(double bX, double bZ, boolean circular) {
		boolean inside;
		if (!circular) {
			inside = (bX > this.getMinX()) || (bX < this.getMaxX()) ||
					(bZ > this.getMinZ()) || (bZ < this.getMaxZ());
		} else {
			double x = (bX - this.x);
			double z = (bZ - this.z);
			double r = this.getHeight() / 2;
			inside = ((x * x) + (z * z)) < (r * r);
		}
		return inside;
	}
}
