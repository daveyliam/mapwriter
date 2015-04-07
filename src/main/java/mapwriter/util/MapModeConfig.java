package mapwriter.util;

import java.util.ArrayList;
import java.util.List;

import mapwriter.gui.ModGuiConfig.ModBooleanEntry;
import mapwriter.handler.ConfigurationHandler;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.DummyConfigElement.DummyCategoryElement;
import net.minecraftforge.fml.client.config.IConfigElement;

public class MapModeConfig {
	
		public final String configCategory;
		
		public boolean enabled = true;
		public boolean rotate = true;
		public boolean circular = true;
		public boolean coordsEnabled = false;
		public int borderMode = 1;
		public int playerArrowSize = 5;
		public int markerSize = 5;
		public int trailMarkerSize = 3;
		public int alphaPercent = 100;
		
		public int marginTop = 0;
		public int marginBottom = 0;
		public int marginLeft = 0;
		public int marginRight = 0;
		public int heightPercent = -1;
		
		public MapModeConfig(String configCategory) {
			this.configCategory = configCategory;
		}
		
		public void loadConfig() {
			// get options from config file
			this.enabled = ConfigurationHandler.configuration.getBoolean("enabled", this.configCategory, this.enabled, "");
			this.playerArrowSize = ConfigurationHandler.configuration.getInt("playerArrowSize", this.configCategory, this.playerArrowSize, 1, 20, "");
			this.markerSize = ConfigurationHandler.configuration.getInt("markerSize", this.configCategory, this.markerSize, 1, 20, "");
			this.alphaPercent = ConfigurationHandler.configuration.getInt("alphaPercent", this.configCategory, this.alphaPercent, 0, 100, "");
			
			this.heightPercent = ConfigurationHandler.configuration.getInt("heightPercent",this.configCategory, this.heightPercent, 0, 100, "");
			this.marginTop = ConfigurationHandler.configuration.getInt("marginTop", this.configCategory, this.marginTop, -1, 320, "");
			this.marginBottom = ConfigurationHandler.configuration.getInt("marginBottom", this.configCategory, this.marginBottom, -1, 320, "");
			this.marginLeft = ConfigurationHandler.configuration.getInt("marginLeft", this.configCategory, this.marginLeft, -1, 320, "");
			this.marginRight = ConfigurationHandler.configuration.getInt("marginRight", this.configCategory, this.marginRight, -1, 320, "");
			
			this.rotate = ConfigurationHandler.configuration.getBoolean("rotate", this.configCategory, this.rotate, "");
			this.circular = ConfigurationHandler.configuration.getBoolean("circular", this.configCategory, this.circular, "");
			this.coordsEnabled = ConfigurationHandler.configuration.getBoolean("coordsEnabled", this.configCategory, this.coordsEnabled, "");
			this.borderMode = ConfigurationHandler.configuration.getInt("borderMode", this.configCategory, this.borderMode, 0, 1, "");
			
			this.trailMarkerSize = Math.max(1, this.markerSize - 1);
		}	
		
	    public IConfigElement categoryElement(String name, String tooltip_key) 
	    {    	
	    	ConfigurationHandler.configuration.get(this.configCategory, "rotate", this.rotate).setConfigEntryClass(ModBooleanEntry.class);
	    	return new DummyCategoryElement(name, tooltip_key,
	                new ConfigElement(ConfigurationHandler.configuration.getCategory(this.configCategory)).getChildElements());
	    }
}
