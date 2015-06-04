package mapwriter.config;

import mapwriter.gui.ModGuiConfig.ModBooleanEntry;
import mapwriter.util.Reference;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.config.DummyConfigElement.DummyCategoryElement;

public class largeMapModeConfig extends MapModeConfig
{
	public largeMapModeConfig(String configCategory) {
		super(configCategory);
	}
	
	@Override
	public void loadConfig()
	{
		super.loadConfig();
		this.enabled = ConfigurationHandler.configuration.getBoolean("enabled", this.configCategory, this.enabledDef, "mw.config.map.enabled");
		this.rotate = ConfigurationHandler.configuration.getBoolean("rotate", this.configCategory, this.rotateDef, "mw.config.map.rotate");
		this.circular = ConfigurationHandler.configuration.getBoolean("circular", this.configCategory, this.circularDef, "mw.config.map.circular");
		this.coordsMode = ConfigurationHandler.configuration.getString("coordsMode", this.configCategory, this.coordsModeDef, "mw.config.map.coordsMode", coordsModeStringArray);
		this.borderMode = ConfigurationHandler.configuration.getBoolean("borderMode", this.configCategory, this.borderModeDef, "mw.config.map.borderMode");
	}
	
	@Override
	public void setDefaults()
	{
		this.rotateDef = true;
		this.circularDef = true;
		this.coordsModeDef = coordsModeStringArray[1];	
		this.borderModeDef = true;
		this.heightPercentDef = -1;
		this.PositionDef = "Large";
	
		ConfigurationHandler.configuration.get(Reference.catLargeMapConfig, "enabled", this.enabled).setRequiresWorldRestart(true);
	}
	
	@Override
    public IConfigElement categoryElement(String name, String tooltip_key) 
    {    	
    	ConfigurationHandler.configuration.get(this.configCategory, "rotate", this.rotate).setConfigEntryClass(ModBooleanEntry.class);
    	
    	return new DummyCategoryElement(name, tooltip_key,
                new ConfigElement(ConfigurationHandler.configuration.getCategory(this.configCategory)).getChildElements());
    }

}
