package mapwriter.config;

import mapwriter.gui.ModGuiConfig.ModBooleanEntry;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.DummyConfigElement.DummyCategoryElement;
import net.minecraftforge.fml.client.config.IConfigElement;

public class MapModeConfig {	
		public final String configCategory;	
		public static final String[] coordsModeStringArray = {
			"disabled",
			"small",
			"large"
		};
		
		public static final String[] miniMapPositionStringArray = {
			"top right",
			"top left",
			"bottom right",
			"bottom left"
		};

		public boolean enabledDef = true;
		public boolean enabled = enabledDef;
		public boolean rotateDef = false;		
		public boolean rotate = rotateDef;	
		public boolean circularDef = false;
		public boolean circular = circularDef;
		public String coordsModeDef = coordsModeStringArray[0];
		public String coordsMode = coordsModeDef;
		public boolean borderModeDef = false;
		public boolean borderMode = borderModeDef;
		public int playerArrowSizeDef = 5;
		public int playerArrowSize = playerArrowSizeDef;
		public int markerSizeDef = 5;
		public int markerSize = markerSizeDef;
		public int trailMarkerSizeDef = 3;
		public int trailMarkerSize = trailMarkerSizeDef;
		public int alphaPercentDef = 100;
		public int alphaPercent = alphaPercentDef;
		public int heightPercentDef = -1;
		public int heightPercent = heightPercentDef;
		public String PositionDef = "FullScreen";
		public String Position = PositionDef;
		
		public MapModeConfig(String configCategory) {
			this.configCategory = configCategory;
		}
		
		public void loadConfig() {
			// get options from config file
			this.playerArrowSize = ConfigurationHandler.configuration.getInt("playerArrowSize", this.configCategory, this.playerArrowSizeDef, 1, 20, "mw.config.map.playerArrowSize");
			this.markerSize = ConfigurationHandler.configuration.getInt("markerSize", this.configCategory, this.markerSizeDef, 1, 20, "mw.config.map.markerSize");
			this.alphaPercent = ConfigurationHandler.configuration.getInt("alphaPercent", this.configCategory, this.alphaPercentDef, 0, 100, "mw.config.map.alphaPercent");

			this.trailMarkerSize = Math.max(1, this.markerSize - 1);
		}	
		
		public void setDefaults()
		{
		}
		
	    public IConfigElement categoryElement(String name, String tooltip_key) 
	    {  
	    	return new DummyCategoryElement(name, tooltip_key,
	                new ConfigElement(ConfigurationHandler.configuration.getCategory(this.configCategory)).getChildElements());
	    }
}