package mapwriter.handler;

import java.io.File;

import mapwriter.Mw;
import mapwriter.util.Config;
import mapwriter.util.Reference;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

	public class ConfigurationHandler
	{
	    public static Configuration configuration;

	    public static void init(File configFile)
	    {
	        // Create the configuration object from the given configuration file
	        if (configuration == null)
	        {
	            configuration = new Configuration(configFile);
	            loadConfig();
	        }
	    }

		public static void loadConfig() 
		{
			configuration.load();
			Config.linearTextureScalingEnabled = configuration.getBoolean("linearTextureScaling", Reference.catOptions, Config.linearTextureScalingEnabled, "");
			Config.useSavedBlockColours = configuration.getBoolean("useSavedBlockColours", Reference.catOptions, Config.useSavedBlockColours, "");
			Config.teleportEnabled = configuration.getBoolean("teleportEnabled", Reference.catOptions, Config.teleportEnabled, "");
			Config.teleportCommand = configuration.getString("teleportCommand", Reference.catOptions, Config.teleportCommand, "");
			Config.coordsMode = configuration.getInt("coordsMode", Reference.catOptions, Config.coordsMode, 0, 2, "");
			Config.maxChunkSaveDistSq = configuration.getInt("maxChunkSaveDistSq", Reference.catOptions, Config.maxChunkSaveDistSq, 1, 256 * 256, "");
			Config.mapPixelSnapEnabled = configuration.getBoolean("mapPixelSnapEnabled", Reference.catOptions, Config.mapPixelSnapEnabled, "");
			Config.maxDeathMarkers = configuration.getInt("maxDeathMarkers", Reference.catOptions, Config.maxDeathMarkers, 0, 1000, "");
			Config.chunksPerTick = configuration.getInt("chunksPerTick", Reference.catOptions, Config.chunksPerTick, 1, 500, "");
			Config.saveDirOverride = configuration.getString("saveDirOverride", Reference.catOptions, Config.saveDirOverride, "");
			Config.portNumberInWorldNameEnabled = configuration.getBoolean("portNumberInWorldNameEnabled", Reference.catOptions, Config.portNumberInWorldNameEnabled, "");
			Config.undergroundMode = configuration.getBoolean("undergroundMode", Reference.catOptions, Config.undergroundMode, "");
			Config.regionFileOutputEnabledSP = configuration.getBoolean("regionFileOutputEnabledSP", Reference.catOptions, Config.regionFileOutputEnabledSP, "");
			Config.regionFileOutputEnabledMP = configuration.getBoolean("regionFileOutputEnabledMP", Reference.catOptions, Config.regionFileOutputEnabledMP, "");
			Config.backgroundTextureMode = configuration.getInt("backgroundTextureMode", Reference.catOptions, Config.backgroundTextureMode, 0, 1, "");

			Config.maxZoom = configuration.getInt("zoomOutLevels", Reference.catOptions, Config.maxZoom, 1, 256, "");
			Config.minZoom = -configuration.getInt("zoomInLevels", Reference.catOptions, -Config.minZoom, 1, 256, "");
			
			Config.configTextureSize = configuration.getInt("textureSize", Reference.catOptions, Config.configTextureSize, 1024, 8192, "");
			
	        if (configuration.hasChanged())
	        {
	            configuration.save();
	        }
	        
			//Mw.instance.setTextureSize();
		}

		public static void loadWorldConfig()
		{
			// load config file options
			Config.modeIndex = configuration.getInt("overlayModeIndex", Reference.catOptions, Config.modeIndex, 0, 1000, "");
			Config.zoomLevel = configuration.getInt("overlayZoomLevel", Reference.catOptions, 0, Config.minZoom, Config.maxZoom, "");
		}
		
		public static void SaveConfig()
		{
			//this.mw.config.setInt(Reference.catOptions, "overlayModeIndex", Config.modeIndex);
			//this.mw.config.setInt(Reference.catOptions, "overlayZoomLevel", this.view.getZoomLevel());
			
			//this.mw.config.setBoolean(Reference.catOptions, Reference.TrailName + "TrailEnabled", Config.PlayerTrailEnabled);
			//this.mw.config.setInt(Reference.catOptions, Reference.TrailName + "TrailMaxLength", Config.maxLength);
			//this.mw.config.setInt(Reference.catOptions, Reference.TrailName + "TrailMarkerIntervalMillis", (int) Config.intervalMillis);
		}

		
	    @SubscribeEvent
	    public void onConfigurationChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event)
	    {
	        if (event.modID.equalsIgnoreCase(Reference.MOD_ID))
	        {
	            loadConfig();
	        }
	    }
	}
