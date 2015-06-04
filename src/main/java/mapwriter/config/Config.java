package mapwriter.config;

import mapwriter.util.Reference;

public class Config {
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
	public static boolean linearTextureScaling = true;
	public static String coordsMode = coordsModeStringArray[0];
	public static boolean undergroundMode = false;
	public static boolean teleportEnabled = true;
	public static String teleportCommand = "tp";
	public static int defaultTeleportHeight = 80;
	public static int zoomOutLevels = 5;
	public static int zoomInLevels = -5;
	public static boolean useSavedBlockColours = false;
	public static int maxChunkSaveDistSq = 128 * 128;
	public static boolean mapPixelSnapEnabled = true;
	public static int configTextureSize = 2048;
	public static int maxDeathMarkers = 3;
	public static int chunksPerTick = 5;
	public static boolean portNumberInWorldNameEnabled = true;
	public static String saveDirOverride = "";
	public static boolean regionFileOutputEnabledSP = true;
	public static boolean regionFileOutputEnabledMP = true;
	public static int backgroundTextureMode = 0;
	
	//World configuration Options
	public static int overlayModeIndex = 0;
	public static int overlayZoomLevel = 0;
	
	public static MapModeConfig largeMap = new MapModeConfig(Reference.catLargeMapConfig);
	public static MapModeConfig smallMap = new MapModeConfig(Reference.catSmallMapConfig);	
	public static MapModeConfig fullScreenMap = new MapModeConfig(Reference.catFullMapConfig);	
}
