package mapwriter.map;

import java.awt.Point;

public class Marker {
	public String name;
	public int x;
	public int y;
	public int z;
	public String groupName;
	public int colour;
	
	public Point.Double screenPos = new Point.Double(0, 0);
	
	private static int[] colours = new int[] {
    		0xff0000, 0x00ff00, 0x0000ff, 0xffff00, 0xff00ff, 0x00ffff,
    		0xff8000, 0x8000ff};
	// static so that current index is shared between all markers
    private static int colourIndex = 0;
	
	public Marker(String name, String groupName, int x, int y, int z, int colour) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.colour = colour;
		this.groupName = groupName;
	}
	
	public String getString() {
		return String.format("%s %s (%d, %d, %d) %06x",
				this.name, this.groupName, this.x, this.y, this.z, this.colour & 0xffffff);
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
}