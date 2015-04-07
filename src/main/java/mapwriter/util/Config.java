package mapwriter.util;

import java.util.ArrayList;
import java.util.List;

import mapwriter.gui.ModGuiConfig.ModBooleanEntry;
import mapwriter.handler.ConfigurationHandler;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.IConfigElement;

public class Config {
	public static final String[] miniMapPositionStringArray = {
		"unchanged",
		"top right",
		"top left",
		"bottom right",
		"bottom left"
	};
	public static final String[] coordsModeStringArray = {
		"disabled",
		"small",
		"large"
	};
	public static final String[] backgroundModeStringArray = {
		"none",
		"static",
		"panning"
	};
	
	// configuration options
	public static boolean linearTextureScalingEnabled = true;
	public static String coordsMode = coordsModeStringArray[0];
	public static boolean undergroundMode = false;
	public static boolean teleportEnabled = true;
	public static String teleportCommand = "tp";
	public static int defaultTeleportHeight = 80;
	public static int maxZoom = 5;
	public static int minZoom = -5;
	public static boolean useSavedBlockColours = false;
	public static int maxChunkSaveDistSq = 128 * 128;
	public static boolean mapPixelSnapEnabled = true;
	public static int textureSize = 2048;
	public static int configTextureSize = 2048;
	public static int maxDeathMarkers = 3;
	public static int chunksPerTick = 5;
	public static boolean portNumberInWorldNameEnabled = true;
	public static String saveDirOverride = "";
	public static boolean regionFileOutputEnabledSP = true;
	public static boolean regionFileOutputEnabledMP = true;
	public static int backgroundTextureMode = 0;
	
	//World configuration Options
	public static int modeIndex = 0;
	public static int zoomLevel = 0;
	
	public static MapModeConfig largeMap = new MapModeConfig(Reference.catLargeMapConfig);
	public static MapModeConfig smallMap = new MapModeConfig(Reference.catSmallMapConfig);	
	public static MapModeConfig fullScreenMap = new MapModeConfig(Reference.catFullMapConfig);	
}
