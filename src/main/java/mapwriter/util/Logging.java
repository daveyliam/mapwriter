package mapwriter.util;

import java.util.regex.Pattern;

import mapwriter.forge.MwForge;

public class Logging 
{	
	public static void logInfo(String s, Object...args) {
		MwForge.logger.info(String.format(s, args));
	}
	
	public static void logWarning(String s, Object...args) {
		MwForge.logger.warn(String.format(s, args));
	}
	
	public static void logError(String s, Object...args) {
		MwForge.logger.error(String.format(s, args));
	}
	
	public static void debug(String s, Object...args) {
		MwForge.logger.debug(String.format(s, args));
	}
	
	public static void log(String s, Object...args) {
		logInfo(String.format(s, args));
	}
}
