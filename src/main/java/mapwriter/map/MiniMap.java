package mapwriter.map;

import java.util.ArrayList;
import java.util.List;

import mapwriter.Mw;
import mapwriter.map.mapmode.LargeMapMode;
import mapwriter.map.mapmode.MapMode;
import mapwriter.map.mapmode.SmallMapMode;

public class MiniMap {
	private Mw mw;
	
	public static final String catSmallMap = "smallMap";
	public static final String catLargeMap = "largeMap";
	public static final String catUndergroundMap = "undergroundMap";
	
	public MapMode smallMapMode;
	public MapMode largeMapMode;
	public MapMode guiMapMode;
	
	public MapView view;
	
	public MapRenderer smallMap;
	public MapRenderer largeMap;
	
	private List<MapRenderer> mapList;
	private MapRenderer currentMap = null;
	
	public int modeIndex = 0;
	
	public MiniMap(Mw mw) {
		this.mw = mw;
		
		// load config file options
		this.modeIndex = mw.config.getOrSetInt(Mw.catOptions, "overlayModeIndex", this.modeIndex, 0, 1000);
		int zoomLevel = mw.config.getOrSetInt(Mw.catOptions, "overlayZoomLevel", 0, mw.minZoom, mw.maxZoom);
		
		// map view shared between large and small map modes
		this.view = new MapView(mw);
		this.view.setZoomLevel(zoomLevel);
		
		// small map mode
		this.smallMapMode = new SmallMapMode(this.mw.config);
		this.smallMap = new MapRenderer(mw, this.smallMapMode, this.view);
		
		// large map mode
		this.largeMapMode = new LargeMapMode(this.mw.config);
		this.largeMap = new MapRenderer(mw, this.largeMapMode, this.view);
		
		this.mapList = new ArrayList<MapRenderer>();
		
		// add small, large and underground map modes if they
		// are enabled.
		if (this.smallMapMode.enabled) {
			this.mapList.add(this.smallMap);
		}
		if (this.largeMapMode.enabled) {
			this.mapList.add(this.largeMap);
		}
		// add a null entry (hides the overlay when selected)
		this.mapList.add(null);
		
		// sanitize overlayModeIndex loaded from config
		this.nextOverlayMode(0);
		this.currentMap = this.mapList.get(this.modeIndex);
	}
	
	public void close() {
		this.mapList.clear();
		this.currentMap = null;
		
		this.smallMapMode.close();
		this.largeMapMode.close();
		
		this.mw.config.setInt(Mw.catOptions, "overlayModeIndex", this.modeIndex);
		this.mw.config.setInt(Mw.catOptions, "overlayZoomLevel", this.view.getZoomLevel());
	}
	
	// toggle between small map, underground map and no map
	public MapRenderer nextOverlayMode(int increment) {
		int size = this.mapList.size();
		this.modeIndex = (this.modeIndex + size + increment) % size;
		this.currentMap = this.mapList.get(this.modeIndex);
		return this.currentMap;
	}
	
	public void toggleRotating() {
		boolean rotate = this.smallMapMode.toggleRotating();
		this.largeMapMode.setRotating(rotate);
	}
	
	// draw the map overlay, player arrow, and markers
	public void drawCurrentMap() {
		if (this.currentMap != null) {
			this.currentMap.draw();
		}
	}
}