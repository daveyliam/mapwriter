package mapwriter.map;

import java.util.ArrayList;
import java.util.List;

import mapwriter.Mw;
import mapwriter.map.mapmode.LargeMapMode;
import mapwriter.map.mapmode.MapMode;
import mapwriter.map.mapmode.SmallMapMode;
import mapwriter.map.mapmode.UndergroundMapMode;

public class OverlayManager {
	private Mw mw;
	
	public static final String catSmallMap = "smallMap";
	public static final String catLargeMap = "largeMap";
	public static final String catUndergroundMap = "undergroundMap";
	
	public MapMode smallMapMode;
	public MapMode largeMapMode;
	public MapMode undergroundMapMode;
	public MapMode guiMapMode;
	
	public MapView overlayView;
	public MapView guiView;
	
	public StandardMapRenderer smallMap;
	public StandardMapRenderer largeMap;
	public UndergroundMapRenderer undergroundMap;
	public StandardMapRenderer guiMap;
	
	private List<MapRenderer> mapList;
	private MapRenderer currentMap = null;
	
	public int modeIndex = 0;
	
	public OverlayManager(Mw mw) {
		this.mw = mw;
		
		// load config file options
		this.modeIndex = this.mw.config.getOrSetInt(Mw.catOptions, "overlayModeIndex", this.modeIndex, 0, 1000);
		int zoomLevel = this.mw.config.getOrSetInt(Mw.catOptions, "overlayZoomLevel", 0, Mw.minZoom, Mw.maxZoom);
		
		// map view shared between large and small map modes
		this.overlayView = new MapView();
		this.overlayView.setZoomLevel(zoomLevel);
		
		// small map mode
		this.smallMapMode = new SmallMapMode(this.mw.config);
		this.smallMap = new StandardMapRenderer(mw, this.smallMapMode, this.overlayView);
		
		// large map mode
		this.largeMapMode = new LargeMapMode(this.mw.config);
		this.largeMap = new StandardMapRenderer(mw, this.largeMapMode, this.overlayView);
		
		// undergound map mode
		this.undergroundMapMode = new UndergroundMapMode(this.mw.config);
		this.undergroundMap = new UndergroundMapRenderer(mw, this.undergroundMapMode);
		
		this.mapList = new ArrayList<MapRenderer>();
		
		// add small, large and underground map modes if they
		// are enabled.
		if (this.smallMapMode.enabled) {
			this.mapList.add(this.smallMap);
		}
		if (this.largeMapMode.enabled) {
			this.mapList.add(this.largeMap);
		}
		if (this.undergroundMapMode.enabled) {
			this.mapList.add(this.undergroundMap);
		}
		// add a null entry (hides the overlay when selected)
		this.mapList.add(null);
		
		// sanitize overlayModeIndex loaded from config
		this.nextOverlayMode(0);
		this.currentMap = this.mapList.get(this.modeIndex);
	}
	
	public void close() {
		for (MapRenderer map : this.mapList) {
			if (map != null) {
				map.close();
			}
		}
		this.mapList.clear();
		this.currentMap = null;
		
		this.smallMapMode.close();
		this.largeMapMode.close();
		this.undergroundMapMode.close();
		
		this.mw.config.setInt(Mw.catOptions, "overlayModeIndex", this.modeIndex);
		this.mw.config.setInt(Mw.catOptions, "overlayZoomLevel", this.overlayView.getZoomLevel());
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
		this.undergroundMapMode.setRotating(rotate);
	}
	
	// draw the map overlay, player arrow, and markers
	public void drawCurrentMap() {
		if (this.currentMap != null) {
			this.currentMap.update();
			this.currentMap.draw();
		}
	}
}