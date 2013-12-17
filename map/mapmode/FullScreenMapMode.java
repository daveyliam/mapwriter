package mapwriter.map.mapmode;

import mapwriter.forge.MwConfig;

public class FullScreenMapMode extends MapMode {
	public FullScreenMapMode(MwConfig config) {
		super(config, "fullScreenMap");
		
		this.heightPercent = -1;
		this.marginTop = 0;
		this.marginBottom = 0;
		this.marginLeft = 0;
		this.marginRight = 0;
		
		this.borderMode = 0;
		this.playerArrowSize = 5;
		this.markerSize = 5;
		this.alphaPercent = 100;
		
		this.rotate = false;
		this.circular = false;
		this.coordsEnabled = false;
		
		this.loadConfig();
	}
}
