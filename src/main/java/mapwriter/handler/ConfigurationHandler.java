package mapwriter.handler;

import java.io.File;

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
	            setMapModeDefaults();
	            loadConfig();
	        }
	    }

		public static void loadConfig() 
		{
			Config.linearTextureScalingEnabled = configuration.getBoolean("linearTextureScaling", Reference.catOptions, Config.linearTextureScalingEnabled, "");
			Config.useSavedBlockColours = configuration.getBoolean("useSavedBlockColours", Reference.catOptions, Config.useSavedBlockColours, "");
			Config.teleportEnabled = configuration.getBoolean("teleportEnabled", Reference.catOptions, Config.teleportEnabled, "");
			Config.teleportCommand = configuration.getString("teleportCommand", Reference.catOptions, Config.teleportCommand, "");
			Config.coordsMode = configuration.getString("coordsMode", Reference.catOptions, Config.coordsMode, "", Config.coordsModeStringArray);
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
			
			Config.configTextureSize = configuration.getInt("textureSize", Reference.catOptions, Config.configTextureSize, 1024, 4096, "");
			
			Config.fullScreenMap.loadConfig();
			Config.largeMap.loadConfig();
			Config.smallMap.loadConfig();
			
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

	    @SubscribeEvent
	    public void onConfigurationChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event)
	    {
	        if (event.modID.equalsIgnoreCase(Reference.MOD_ID))
	        {
	            loadConfig();
	        }
	    }

	    public static void setMapModeDefaults()
	    {
	    	Config.fullScreenMap.heightPercent = -1;
	    	Config.fullScreenMap.marginTop = 0;
	    	Config.fullScreenMap.marginBottom = 0;
	    	Config.fullScreenMap.marginLeft = 0;
	    	Config.fullScreenMap.marginRight = 0;
	    	Config.fullScreenMap.borderMode = 0;
	    	Config.fullScreenMap.playerArrowSize = 5;
	    	Config.fullScreenMap.markerSize = 5;
	    	Config.fullScreenMap.alphaPercent = 100;
	    	Config.fullScreenMap.rotate = false;
	    	Config.fullScreenMap.circular = false;
			Config.fullScreenMap.coordsEnabled = false;
			
			ConfigurationHandler.configuration.get(Reference.catFullMapConfig, "heightPercent", Config.fullScreenMap.heightPercent).setShowInGui(false);
			ConfigurationHandler.configuration.get(Reference.catFullMapConfig, "marginTop", Config.fullScreenMap.marginTop).setShowInGui(false);
			ConfigurationHandler.configuration.get(Reference.catFullMapConfig, "marginLeft", Config.fullScreenMap.marginLeft).setShowInGui(false);
			ConfigurationHandler.configuration.get(Reference.catFullMapConfig, "marginRight", Config.fullScreenMap.marginRight).setShowInGui(false);
			ConfigurationHandler.configuration.get(Reference.catFullMapConfig, "borderMode", Config.fullScreenMap.borderMode).setShowInGui(false);
			ConfigurationHandler.configuration.get(Reference.catFullMapConfig, "rotate", Config.fullScreenMap.rotate).setShowInGui(false);
			ConfigurationHandler.configuration.get(Reference.catFullMapConfig, "circular", Config.fullScreenMap.circular).setShowInGui(false);
			ConfigurationHandler.configuration.get(Reference.catFullMapConfig, "coordsEnabled", Config.fullScreenMap.coordsEnabled).setShowInGui(false);
			
			Config.largeMap.heightPercent = -1;
			Config.largeMap.marginTop = 10;
			Config.largeMap.marginBottom = 40;
			Config.largeMap.marginLeft = 40;
			Config.largeMap.marginRight = 40;
			Config.largeMap.playerArrowSize = 5;
			Config.largeMap.markerSize = 5;
			Config.largeMap.coordsEnabled = true;	
			
			ConfigurationHandler.configuration.get(Reference.catLargeMapConfig, "heightPercent", Config.largeMap.heightPercent).setShowInGui(false);			
			
			Config.smallMap.heightPercent = 30;
			Config.smallMap.marginTop = 10;
			Config.smallMap.marginBottom = -1;
			Config.smallMap.marginLeft = -1;
			Config.smallMap.marginRight = 10;
			Config.smallMap.playerArrowSize = 4;
			Config.smallMap.markerSize = 3;
			Config.smallMap.coordsEnabled = true;
	    }
	}