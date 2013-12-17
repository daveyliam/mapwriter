package mapwriter.map.mapmode;

import mapwriter.forge.MwConfig;

public class LargeMapMode extends MapMode {
	public LargeMapMode(MwConfig config) {
		super(config, "largeMap");
		
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
