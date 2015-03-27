package mapwriter.util;

public class Config {
	// configuration options
	public static boolean linearTextureScalingEnabled = true;
	public static int coordsMode = 0;
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
	//public static boolean lightingEnabled = false;
	
	//World configuration Options
	public static int modeIndex = 0;
	public static int zoomLevel = 0;
}
