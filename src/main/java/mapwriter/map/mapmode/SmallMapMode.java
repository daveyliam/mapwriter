package mapwriter.map.mapmode;


public class SmallMapMode extends MapMode {
	public SmallMapMode() {
		super("smallMap");
		
		this.heightPercent = 30;
		this.marginTop = 10;
		this.marginBottom = -1;
		this.marginLeft = -1;
		this.marginRight = 10;
		
		this.playerArrowSize = 4;
		this.markerSize = 3;
		
		this.coordsEnabled = true;
		
		this.loadConfig();
	}
}
