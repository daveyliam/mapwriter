package mapwriter.map;

import java.util.ArrayList;

import mapwriter.Mw;
import mapwriter.map.mapmode.LargeMapMode;
import mapwriter.map.mapmode.MapMode;
import mapwriter.map.mapmode.SmallMapMode;
import mapwriter.map.mapmode.UndergroundMapMode;
import net.minecraft.client.Minecraft;

public class OverlayManager {
	private Minecraft mc;
	private Mw mw;
	private MapTexture mapTexture;
	
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
	
	private int mapListIndex;
	private ArrayList<MapRenderer> mapList;
	private MapRenderer currentMap = null;
	
	public OverlayManager(Mw mw, MapTexture mapTexture) {
		this.mw = mw;
		this.mapTexture = mapTexture;
		
		// small map mode
		this.smallMapMode = new SmallMapMode(this.mw.config);
		this.overlayView = new MapView();
		this.overlayView.setZoomLevel(-1);
		this.smallMap = new StandardMapRenderer(mw, this.mapTexture, this.mw.markerManager, this.smallMapMode, this.overlayView);
		
		// large map mode
		this.largeMapMode = new LargeMapMode(this.mw.config);
		this.largeMap = new StandardMapRenderer(mw, this.mapTexture, this.mw.markerManager, this.largeMapMode, this.overlayView);
		
		// undergound map mode
		this.undergroundMapMode = new UndergroundMapMode(this.mw.config);
		this.undergroundMap = new UndergroundMapRenderer(mw, this.undergroundMapMode);
		
		this.mapList = new ArrayList<MapRenderer>();
		this.mapList.add(null);
		this.mapList.add(this.smallMap);
		this.mapList.add(this.largeMap);
		this.mapList.add(this.undergroundMap);
		
		this.mapListIndex = 0;
		this.nextOverlayMode(1);
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
	}
	
	// toggle between small map, underground map and no map
	public MapRenderer nextOverlayMode(int increment) {
		int size = this.mapList.size();
		this.mapListIndex = (this.mapListIndex + size + increment) % size;
		this.currentMap = this.mapList.get(this.mapListIndex);
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