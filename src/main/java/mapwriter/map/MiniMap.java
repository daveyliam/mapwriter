package mapwriter.map;

import java.util.ArrayList;
import java.util.List;

import mapwriter.Mw;
import mapwriter.map.mapmode.LargeMapMode;
import mapwriter.map.mapmode.MapMode;
import mapwriter.map.mapmode.SmallMapMode;
import mapwriter.util.Config;
import mapwriter.util.Reference;

public class MiniMap {
	private Mw mw;
	
	public MapMode smallMapMode;
	public MapMode largeMapMode;
	public MapMode guiMapMode;
	
	public MapView view;
	
	public MapRenderer smallMap;
	public MapRenderer largeMap;
	
	private List<MapRenderer> mapList;
	private MapRenderer currentMap = null;
	
	public MiniMap(Mw mw) {
		this.mw = mw;
		
		// map view shared between large and small map modes
		this.view = new MapView(mw);
		this.view.setZoomLevel(Config.zoomLevel);
		
		// small map mode
		this.smallMapMode = new SmallMapMode();
		this.smallMap = new MapRenderer(mw, this.smallMapMode, this.view);
		
		// large map mode
		this.largeMapMode = new LargeMapMode();
		this.largeMap = new MapRenderer(mw, this.largeMapMode, this.view);
		
		this.mapList = new ArrayList<MapRenderer>();
		
		// add small, large and underground map modes if they
		// are enabled.
		if (this.smallMapMode.config.enabled) {
			this.mapList.add(this.smallMap);
		}
		if (this.largeMapMode.config.enabled) {
			this.mapList.add(this.largeMap);
		}
		// add a null entry (hides the overlay when selected)
		this.mapList.add(null);
		
		// sanitize overlayModeIndex loaded from config
		this.nextOverlayMode(0);
		this.currentMap = this.mapList.get(Config.modeIndex);
	}
	
	public void close() {
		this.mapList.clear();
		this.currentMap = null;
	}
	
	// toggle between small map, underground map and no map
	public MapRenderer nextOverlayMode(int increment) {
		int size = this.mapList.size();
		Config.modeIndex = (Config.modeIndex + size + increment) % size;
		this.currentMap = this.mapList.get(Config.modeIndex);
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