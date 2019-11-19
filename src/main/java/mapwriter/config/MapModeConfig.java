package mapwriter.config;

import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.DummyConfigElement.DummyCategoryElement;
import net.minecraftforge.fml.client.config.IConfigElement;

public class MapModeConfig
{
	public final String configCategory;
	public static final String[] coordsModeStringArray =
	{
			"mw.config.map.coordsMode.disabled",
			"mw.config.map.coordsMode.small",
			"mw.config.map.coordsMode.large"
	};

	public static final String[] miniMapPositionStringArray =
	{
			"mw.config.map.position.topRight",
			"mw.config.map.position.topLeft",
			"mw.config.map.position.botRight",
			"mw.config.map.position.botLeft"
	};

	public boolean enabledDef = true;
	public boolean enabled = this.enabledDef;
	public boolean rotateDef = false;
	public boolean rotate = this.rotateDef;
	public boolean circularDef = false;
	public boolean circular = this.circularDef;
	public String coordsModeDef = coordsModeStringArray[0];
	public String coordsMode = this.coordsModeDef;
	public boolean borderModeDef = false;
	public boolean borderMode = this.borderModeDef;
	public int playerArrowSizeDef = 5;
	public int playerArrowSize = this.playerArrowSizeDef;
	public int markerSizeDef = 5;
	public int markerSize = this.markerSizeDef;
	public int trailMarkerSizeDef = 3;
	public int trailMarkerSize = this.trailMarkerSizeDef;
	public int alphaPercentDef = 100;
	public int alphaPercent = this.alphaPercentDef;
	public int heightPercentDef = -1;
	public int heightPercent = this.heightPercentDef;
	public String PositionDef = "FullScreen";
	public String Position = this.PositionDef;
	public String biomeModeDef = coordsModeStringArray[0];
	public String biomeMode = this.biomeModeDef;

	public MapModeConfig(String configCategory)
	{
		this.configCategory = configCategory;
	}

	public void loadConfig()
	{
		// get options from config file
		this.playerArrowSize = ConfigurationHandler.configuration.getInt("playerArrowSize", this.configCategory, this.playerArrowSizeDef, 1, 20, "", "mw.config.map.playerArrowSize");
		this.markerSize = ConfigurationHandler.configuration.getInt("markerSize", this.configCategory, this.markerSizeDef, 1, 20, "", "mw.config.map.markerSize");
		this.alphaPercent = ConfigurationHandler.configuration.getInt("alphaPercent", this.configCategory, this.alphaPercentDef, 0, 100, "", "mw.config.map.alphaPercent");

		this.trailMarkerSize = Math.max(1, this.markerSize - 1);
	}

	public void setDefaults()
	{
	}

	public IConfigElement categoryElement(String name, String tooltip_key)
	{
		return new DummyCategoryElement(name, tooltip_key, new ConfigElement(ConfigurationHandler.configuration.getCategory(this.configCategory)).getChildElements());
	}
}