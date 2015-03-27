package mapwriter.map.mapmode;


public class LargeMapMode extends MapMode {
	public LargeMapMode() {
		super("largeMap");
		
		this.heightPercent = -1;
		this.marginTop = 10;
		this.marginBottom = 40;
		this.marginLeft = 40;
		this.marginRight = 40;
		
		this.playerArrowSize = 5;
		this.markerSize = 5;
		
		this.coordsEnabled = true;
		
		this.loadConfig();
	}
}
