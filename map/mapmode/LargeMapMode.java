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
		
		this.borderWidth = 1;
		this.borderColour = 0xff000000;
		this.playerArrowSize = 5;
		this.markerSize = 5;
		
		this.loadConfig();
	}
}
