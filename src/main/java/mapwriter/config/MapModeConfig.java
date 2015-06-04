package mapwriter.config;

import mapwriter.gui.ModGuiConfig.ModBooleanEntry;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.DummyConfigElement.DummyCategoryElement;
import net.minecraftforge.fml.client.config.IConfigElement;

public class MapModeConfig {	
		public final String configCategory;
		
		public static final String[] miniMapPositionStringArray = {
			"top right",
			"top left",
			"bottom right",
			"bottom left"
		};
		
		public boolean Changed = false;
		
		public boolean enabled = true;
		public boolean rotate = true;
		public boolean circular = true;
		public boolean coordsEnabled = false;
		public boolean borderMode = true;
		public int playerArrowSize = 5;
		public int markerSize = 5;
		public int trailMarkerSize = 3;
		public int alphaPercent = 100;
		public int heightPercent = -1;
		public String Position = miniMapPositionStringArray[0];

		
		
		public MapModeConfig(String configCategory) {
			this.configCategory = configCategory;
		}
		
		public void loadConfig() {
			// get options from config file
			this.enabled = ConfigurationHandler.configuration.getBoolean("enabled", this.configCategory, this.enabled, "mw.config.map.enabled");
			this.playerArrowSize = ConfigurationHandler.configuration.getInt("playerArrowSize", this.configCategory, this.playerArrowSize, 1, 20, "mw.config.map.playerArrowSize");
			this.markerSize = ConfigurationHandler.configuration.getInt("markerSize", this.configCategory, this.markerSize, 1, 20, "mw.config.map.markerSize");
			this.alphaPercent = ConfigurationHandler.configuration.getInt("alphaPercent", this.configCategory, this.alphaPercent, 0, 100, "mw.config.map.alphaPercent");
			
			this.heightPercent = ConfigurationHandler.configuration.getInt("heightPercent",this.configCategory, this.heightPercent, 0, 100, "mw.config.map.heightPercent");

			this.rotate = ConfigurationHandler.configuration.getBoolean("rotate", this.configCategory, this.rotate, "mw.config.map.rotate");
			this.circular = ConfigurationHandler.configuration.getBoolean("circular", this.configCategory, this.circular, "mw.config.map.circular");
			this.coordsEnabled = ConfigurationHandler.configuration.getBoolean("coordsEnabled", this.configCategory, this.coordsEnabled, "mw.config.map.coordsEnabled");
			this.borderMode = ConfigurationHandler.configuration.getBoolean("borderMode", this.configCategory, this.borderMode, "mw.config.map.borderMode");
			
			this.Position = ConfigurationHandler.configuration.getString("Position", this.configCategory, miniMapPositionStringArray[0], "mw.config.map.Position", miniMapPositionStringArray);
			
			this.trailMarkerSize = Math.max(1, this.markerSize - 1);
			
			this.Changed = true;
		}	
		
	    public IConfigElement categoryElement(String name, String tooltip_key) 
	    {    	
	    	ConfigurationHandler.configuration.get(this.configCategory, "rotate", this.rotate).setConfigEntryClass(ModBooleanEntry.class);
	    	
	    	return new DummyCategoryElement(name, tooltip_key,
	                new ConfigElement(ConfigurationHandler.configuration.getCategory(this.configCategory)).getChildElements());
	    }
}
