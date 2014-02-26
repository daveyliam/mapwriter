package mapwriter.map;

import java.awt.Point;
import java.util.LinkedList;

import mapwriter.Mw;
import mapwriter.Render;
import mapwriter.map.mapmode.MapMode;

public class Trail {
	
	class TrailMarker {
		
		double x, y, z, heading;
		int alphaPercent;
		
		static final int borderColour = 0xff000000;
		static final int colour = 0xff00ffff;
		
		public TrailMarker(double x, double y, double z, double heading) {
			this.set(x, y, z, heading);
		}
		
		public void set(double x, double y, double z, double heading) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.heading = heading;
			this.alphaPercent = 100;
		}
		
		public void draw(MapMode mapMode, MapView mapView) {
			if (mapView.isBlockWithinView(this.x, this.z, mapMode.circular)) {
				Point.Double p = mapMode.blockXZtoScreenXY(mapView, this.x, this.z);
			
				// draw a coloured arrow centered on the calculated (x, y)
				Render.setColourWithAlphaPercent(borderColour, this.alphaPercent);
				Render.drawArrow(p.x, p.y, this.heading, mapMode.trailMarkerSize);
				Render.setColourWithAlphaPercent(colour, this.alphaPercent);
				Render.drawArrow(p.x, p.y, this.heading, mapMode.trailMarkerSize - 1.0);
			}
		}
	}
	
	private Mw mw;
	public LinkedList<TrailMarker> trailMarkerList = new LinkedList<TrailMarker>();
	public int maxLength = 30;
	public String name;
	public boolean enabled;
	public long lastMarkerTime = 0;
	public long intervalMillis = 5000;
	
	public Trail(Mw mw, String name) {
		this.mw = mw;
		this.name = name;
		this.enabled = this.mw.config.getOrSetBoolean(Mw.catOptions, this.name + "TrailEnabled", false);
		this.maxLength = this.mw.config.getOrSetInt(Mw.catOptions, this.name + "TrailMaxLength", this.maxLength, 1, 200);
		this.intervalMillis = (long) this.mw.config.getOrSetInt(Mw.catOptions, this.name + "TrailMarkerIntervalMillis", (int) this.intervalMillis, 100, 360000);
	}
	
	public void close() {
		this.mw.config.setBoolean(Mw.catOptions, this.name + "TrailEnabled", this.enabled);
		this.mw.config.setInt(Mw.catOptions, this.name + "TrailMaxLength", this.maxLength);
		this.mw.config.setInt(Mw.catOptions, this.name + "TrailMarkerIntervalMillis", (int) this.intervalMillis);
		this.trailMarkerList.clear();
	}
	
	// for other types of trails will need to extend Trail and override this method
	public void onTick() {
		long time = System.currentTimeMillis();
    	if ((time - this.lastMarkerTime) > this.intervalMillis) {
    		this.lastMarkerTime = time;
    		this.addMarker(this.mw.playerX, this.mw.playerY, this.mw.playerZ, this.mw.playerHeading);
    	}
	}
	
	public void addMarker(double x, double y, double z, double heading) {
		this.trailMarkerList.add(new TrailMarker(x, y, z, heading));
		// remove elements from beginning of list until the list has at most
		// maxTrailLength elements
		while (this.trailMarkerList.size() > this.maxLength) {
			this.trailMarkerList.poll();
		}
		int i = this.maxLength - this.trailMarkerList.size();
		for (TrailMarker marker : this.trailMarkerList) {
			marker.alphaPercent = (i * 100) / this.maxLength;
			i++;
		}
	}
	
	public void draw(MapMode mapMode, MapView mapView) {
		for (TrailMarker marker : this.trailMarkerList) {
			marker.draw(mapMode, mapView);
		}
	}
	
	
}
