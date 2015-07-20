package mapwriter.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import net.minecraftforge.fml.common.Loader;

import org.apache.commons.io.IOUtils;

public class VersionCheck implements Runnable {
//TODO: add https://github.com/Dynious/VersionChecker suport
	private static boolean isLatestVersion = true;
	private static String latestVersion = "";
	private static String updateURL = "";

	/**
	 * @author jabelar
	 * @link 
	 *       http://jabelarminecraft.blogspot.nl/p/minecraft-forge-1721710-making
	 *       -mod.html
	 */

	@Override
	public void run() {
		InputStream in = null;
		try {
			in = new URL(Reference.VersionURL).openStream();
		} catch (MalformedURLException e) {
		} catch (IOException e) {
		}

		try {
			List<String> list = IOUtils.readLines(in);
			int index = -1;
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i)
						.contains(Loader.instance().getMCVersionString())) {
					index = i;
					break;
				}
			}

			String version = list.get(index + 1);
			version = version.replace("\"modVersion\":\"", "");
			version = version.replace("\",", "");
			version = version.replace(" ", "");
			latestVersion = version;

			String updateURL = list.get(index + 3);
			updateURL = updateURL.replace("\"updateURL\":\"", "");
			updateURL = updateURL.replace("\",", "");
			updateURL = updateURL.replace(" ", "");
			VersionCheck.updateURL = updateURL;

			isLatestVersion = Reference.VERSION.equals(version);
		} catch (IOException e) {
		}

	}

	public static boolean isLatestVersion() {
		return isLatestVersion;
	}

	public static String getLatestVersion() {
		return latestVersion;
	}
	
	public static String getUpdateURL()
	{
		return updateURL;
	}
}
