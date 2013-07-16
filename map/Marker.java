package mapwriter.map;

import java.awt.Point;

import mapwriter.Render;
import mapwriter.map.mapmode.MapMode;

public class Marker {
	public String name;
	public int x;
	public int y;
	public int z;
	public int dimension;
	public String groupName;
	public int colour;
	
	public Point.Double screenPos = new Point.Double(0, 0);
	
	private static int[] colours = new int[] {
    		0xff0000, 0x00ff00, 0x0000ff, 0xffff00, 0xff00ff, 0x00ffff,
    		0xff8000, 0x8000ff};
	// static so that current index is shared between all markers
    private static int colourIndex = 0;
	
	public Marker(String name, String groupName, int x, int y, int z, int dimension, int colour) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.colour = colour;
		this.groupName = groupName;
	}
	
	public String getString() {
		return String.format("%s %s (%d, %d, %d) %d %06x",
				this.name, this.groupName, this.x, this.y, this.z, this.dimension, this.colour & 0xffffff);
	}
	
	public static int getCurrentColour() {
    	return 0xff000000 | colours[colourIndex];
    }
	
    public void colourNext() {
    	colourIndex = (colourIndex + 1) % colours.length;
		this.colour = getCurrentColour();
    }
    
    public void colourPrev() {
    	colourIndex = (colourIndex + colours.length - 1) % colours.length;
		this.colour = getCurrentColour();
    }
    
    public void draw(MapMode mapMode, MapView mapView, int borderColour) {
		// markers are always specified by their overworld coordinates
		double scale = mapView.getDimensionScaling(this.dimension);
		Point.Double p = mapMode.getClampedScreenXY(mapView, this.x * scale, this.z * scale);
		this.screenPos.setLocation(p.x + mapMode.xTranslation, p.y + mapMode.yTranslation);
		
		// draw a coloured rectangle centered on the calculated (x, y)
		double mSize = mapMode.markerSize;
		double halfMSize = mapMode.markerSize / 2.0;
		Render.setColour(borderColour);
		Render.drawRect(p.x - halfMSize, p.y - halfMSize, mSize, mSize);
		Render.setColour(this.colour);
		Render.drawRect(p.x - halfMSize + 0.5, p.y - halfMSize + 0.5, mSize - 1.0, mSize - 1.0);
	}
}