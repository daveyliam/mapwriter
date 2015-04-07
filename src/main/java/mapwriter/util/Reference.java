package mapwriter.util;

import java.util.regex.Pattern;

public final class Reference 
{
	public static final String MOD_ID = "MapWriter";
	public static final String MOD_NAME = "MapWriter";
	public static final String VERSION = "@MOD_VERSION@";
	public static final String MOD_GUIFACTORY_CLASS = "mapwriter.gui.ModGuiFactoryHandler";
	public static final String CLIENT_PROXY_CLASS = "mapwriter.forge.ClientProxy";
	public static final String SERVER_PROXY_CLASS = "mapwriter.forge.CommonProxy";
	
	public static final String catOptions = "options";
	public static final String catLargeMapConfig = "largemap";
	public static final String catSmallMapConfig = "smallmap";
	public static final String catFullMapConfig = "fullscreenmap";
	
	public static final String PlayerTrailName = "player";
	
	public static final Pattern patternInvalidChars = Pattern.compile("[^a-zA-Z0-9_]");
}
