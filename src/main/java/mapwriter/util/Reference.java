package mapwriter.util;

import java.util.regex.Pattern;

import net.minecraft.util.ResourceLocation;

public final class Reference 
{
	public static final String MOD_ID = "MapWriter";
	public static final String MOD_NAME = "MapWriter";
	public static final String VERSION = "@MOD_VERSION@";
	public static final String MOD_GUIFACTORY_CLASS = "mapwriter.gui.ModGuiFactoryHandler";
	public static final String CLIENT_PROXY_CLASS = "mapwriter.forge.ClientProxy";
	public static final String SERVER_PROXY_CLASS = "mapwriter.forge.CommonProxy";
	
	public static final String VersionURL = "https://raw.githubusercontent.com/Vectron/Versions/master/MwVersion.json";
	
	public static final String catOptions = "options";
	public static final String catLargeMapConfig = "largemap";
	public static final String catSmallMapConfig = "smallmap";
	public static final String catFullMapConfig = "fullscreenmap";
	
	public static final String PlayerTrailName = "player";
	
	public static final Pattern patternInvalidChars = Pattern.compile("[^a-zA-Z0-9_]");
	
	public static final String catWorld = "world";
	public static final String catMarkers = "markers";
	public static final String worldDirConfigName = "mapwriter.cfg";
	public static final String blockColourSaveFileName = "MapWriterBlockColours.txt";
	public static final String blockColourOverridesFileName = "MapWriterBlockColourOverrides.txt";
	
	public static final ResourceLocation backgroundTexture = new ResourceLocation("mapwriter", "textures/map/background.png");
	public static final ResourceLocation roundMapTexture = new ResourceLocation("mapwriter", "textures/map/border_round.png");
	public static final ResourceLocation squareMapTexture = new ResourceLocation("mapwriter", "textures/map/border_square.png");
	public static final ResourceLocation playerArrowTexture = new ResourceLocation("mapwriter", "textures/map/arrow_player.png");
	public static final ResourceLocation northArrowTexture = new ResourceLocation("mapwriter", "textures/map/arrow_north.png");
	public static final ResourceLocation leftArrowTexture = new ResourceLocation("mapwriter", "textures/map/arrow_text_left.png");
	public static final ResourceLocation rightArrowTexture = new ResourceLocation("mapwriter", "textures/map/arrow_text_right.png");
	
}
