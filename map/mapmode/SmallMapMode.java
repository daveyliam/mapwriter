package mapwriter.map.mapmode;

import mapwriter.forge.MwConfig;

public class SmallMapMode extends MapMode {
	public SmallMapMode(MwConfig config) {
		super(config, "smallMap");
		
		this.heightPercent = 30;
		this.marginTop = 10;
		this.marginBottom = -1;
		this.marginLeft = -1;
		this.marginRight = 10;
		
		this.borderWidth = 1;
		this.borderColour = 0xff000000;
		this.playerArrowSize = 4;
		this.markerSize = 3;
		
		this.loadConfig();
	}
}
