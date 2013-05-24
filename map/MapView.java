package mapwriter.map;

import java.util.ArrayList;

import mapwriter.Mw;
import mapwriter.map.mapmode.MapMode;

public class MapView {
	
	private double aspect = 1.0;
	private int zoomLevel = 0;
	private int dimension = 0;
	private int textureSize = 2048;
	
	// the position of the centre of the 'view' of the map using game (block) coordinates
	private double x = 0.0;
	private double z = 0.0;
	
	private int aspectW = 1;
	private int aspectH = 1;
	
	// calculated before every frame drawn by updateView
	public double w = 1.0;
	public double h = 1.0;
	
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
		int prevZoomLevel = this.zoomLevel;
		this.zoomLevel = Math.min(Math.max(Mw.minZoom, zoomLevel), Mw.maxZoom);
		if (prevZoomLevel != this.zoomLevel) {
			this.update();
		}
		return this.zoomLevel;
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
		int zoomLevelChange = 0;
		double scale = 1.0;
		if ((this.dimension != -1) && (dimension == -1)) {
			zoomLevelChange = -3;
			scale = 0.125;
		} else if ((this.dimension == -1) && (dimension != -1)) {
			zoomLevelChange = 3;
			scale = 8.0;
		}
		this.dimension = dimension;
		this.setZoomLevel(this.getZoomLevel() + zoomLevelChange);
		this.setViewCentre(this.x * scale, this.z * scale);
	}
	
	public void nextDimension(ArrayList<Integer> dimensionList, int n) {
		int i = dimensionList.indexOf(this.dimension);
		i = Math.max(0,  i);
		int size = dimensionList.size();
		int dimension = dimensionList.get((i + size + n) % size);
		this.setDimension(dimension);
	}
	
	public int getDimension() {
		return this.dimension;
	}
	
	public void setAspect(int w, int h) {
		this.aspect = (double) w / (double) h;
		if ((this.aspectW != w) || (this.aspectH != h)) {
			this.update();
		}
		this.aspectW = w;
		this.aspectH = h;
	}
	
	public void setAspect(MapMode mapMode) {
		this.setAspect(mapMode.w, mapMode.h);
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
			this.update();
		}
	}
	
	public void update() {
		double viewSize;
		if (this.zoomLevel >= 0) {
			viewSize = (this.textureSize >> 1) << (this.zoomLevel);
		} else {
			viewSize = (this.textureSize >> 1) >> (-this.zoomLevel);
		}
		this.w = (this.aspect < 1.0) ? viewSize * this.aspect : viewSize;
		this.h = (this.aspect > 1.0) ? viewSize / this.aspect : viewSize;
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
