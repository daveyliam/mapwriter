package mapwriter.config;

import java.io.File;

import mapwriter.util.Reference;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

	public class ConfigurationHandler
	{
		// configuration files (global and world specific)
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
			Config.linearTextureScaling = configuration.getBoolean("linearTextureScaling", Reference.catOptions, Config.linearTextureScalingDef, "mw.config.linearTextureScaling");
			Config.useSavedBlockColours = configuration.getBoolean("useSavedBlockColours", Reference.catOptions, Config.useSavedBlockColoursDef, "mw.config.useSavedBlockColours");
			Config.teleportEnabled = configuration.getBoolean("teleportEnabled", Reference.catOptions, Config.teleportEnabledDef, "mw.config.teleportEnabled");
			Config.teleportCommand = configuration.getString("teleportCommand", Reference.catOptions, Config.teleportCommandDef, "mw.config.teleportCommand");
			Config.maxChunkSaveDistSq = configuration.getInt("maxChunkSaveDistSq", Reference.catOptions, Config.maxChunkSaveDistSqDef, 1, 256 * 256, "mw.config.maxChunkSaveDistSq");
			Config.mapPixelSnapEnabled = configuration.getBoolean("mapPixelSnapEnabled", Reference.catOptions, Config.mapPixelSnapEnabledDef, "mw.config.mapPixelSnapEnabled");
			Config.maxDeathMarkers = configuration.getInt("maxDeathMarkers", Reference.catOptions, Config.maxDeathMarkersDef, 0, 1000, "mw.config.maxDeathMarkers");
			Config.chunksPerTick = configuration.getInt("chunksPerTick", Reference.catOptions, Config.chunksPerTickDef, 1, 500, "mw.config.chunksPerTick");
			Config.saveDirOverride = configuration.getString("saveDirOverride", Reference.catOptions, Config.saveDirOverrideDef, "mw.config.saveDirOverride");
			Config.portNumberInWorldNameEnabled = configuration.getBoolean("portNumberInWorldNameEnabled", Reference.catOptions, Config.portNumberInWorldNameEnabledDef, "mw.config.portNumberInWorldNameEnabled");
			Config.undergroundMode = configuration.getBoolean("undergroundMode", Reference.catOptions, Config.undergroundModeDef, "mw.config.undergroundMode");
			Config.regionFileOutputEnabledSP = configuration.getBoolean("regionFileOutputEnabledSP", Reference.catOptions, Config.regionFileOutputEnabledSPDef, "mw.config.regionFileOutputEnabledSP");
			Config.regionFileOutputEnabledMP = configuration.getBoolean("regionFileOutputEnabledMP", Reference.catOptions, Config.regionFileOutputEnabledMPDef, "mw.config.regionFileOutputEnabledMP");
			Config.backgroundTextureMode = configuration.getString("backgroundTextureMode", Reference.catOptions, Config.backgroundTextureModeDef, "mw.config.backgroundTextureModeDef", Config.backgroundModeStringArray);
			Config.zoomOutLevels = configuration.getInt("zoomOutLevels", Reference.catOptions, Config.zoomOutLevelsDef, 1, 256, "mw.config.zoomOutLevels");
			Config.zoomInLevels = -configuration.getInt("zoomInLevels", Reference.catOptions, -Config.zoomInLevelsDef, 1, 256, "mw.config.zoomInLevels");
			
			Config.configTextureSize = configuration.getInt("textureSize", Reference.catOptions, Config.configTextureSizeDef, 1024, 4096, "mw.config.textureSize");
			
			Config.overlayModeIndex = configuration.getInt("overlayModeIndex", Reference.catOptions, Config.overlayModeIndexDef, 0, 1000, "mw.config.overlayModeIndex");
			Config.overlayZoomLevel = configuration.getInt("overlayZoomLevel", Reference.catOptions, 0, Config.zoomInLevels, Config.zoomOutLevels, "mw.config.overlayZoomLevel");
			
			Config.moreRealisticMap = configuration.getBoolean("moreRealisticMap", Reference.catOptions, Config.moreRealisticMapDef, "mw.config.moreRealisticMap");
			
			Config.newMarkerDialog = configuration.getBoolean("newMarkerDialog", Reference.catOptions, Config.newMarkerDialogDef, "mw.config.newMarkerDialog");
			
			Config.fullScreenMap.loadConfig();
			Config.largeMap.loadConfig();
			Config.smallMap.loadConfig();
			
			if (configuration.hasChanged())
	        {
	            configuration.save();
	        }
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
	    	Config.fullScreenMap.setDefaults();
	    	Config.largeMap.setDefaults();
	    	Config.smallMap.setDefaults();
	    }
	}