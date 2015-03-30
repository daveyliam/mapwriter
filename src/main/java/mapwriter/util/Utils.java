package mapwriter.util;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.chunk.Chunk;

public class Utils 
{
	public static int[] integerListToIntArray(List<Integer> list)
	{
		// convert List of integers to integer array
		int size = list.size();
		int[] array = new int[size];
		for (int i = 0; i < size; i++) 
		{
			array[i] = list.get(i);
		}
		return array;
	}
	
	public static String mungeString(String s) {
		s = s.replace('.', '_');
		s = s.replace('-', '_');
		s = s.replace(' ',  '_');
		s = s.replace('/',  '_');
		s = s.replace('\\',  '_');
		return Reference.patternInvalidChars.matcher(s).replaceAll("");
	}
	
	public static File getFreeFilename(File dir, String baseName, String ext) {
		int i = 0;
		File outputFile;
		if (dir != null) {
			outputFile = new File(dir, baseName + "." + ext);
		} else {
			outputFile = new File(baseName + "." + ext);
		}
		while (outputFile.exists() && (i < 1000)) {
			if (dir != null) {
				outputFile = new File(dir, baseName + "." + i + "." + ext);
			} else {
				outputFile = new File(baseName + "." + i + "." + ext);
			}
			i++;
		}
		return (i < 1000) ? outputFile : null;
	}
	
	public static void printBoth(String msg) {
		EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;
		if (thePlayer != null) {
			thePlayer.addChatMessage(new ChatComponentText(msg));
		}
		Logging.log("%s", msg);
	}
	
	public static File getDimensionDir(File worldDir, int dimension) {
		File dimDir;
		if (dimension != 0) {
			dimDir = new File(worldDir, "DIM" + dimension);
		} else {
			dimDir = worldDir;
		}
		return dimDir;
	}
	
	public static IntBuffer allocateDirectIntBuffer(int size) {
		return ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
	}
	
	// algorithm from http://graphics.stanford.edu/~seander/bithacks.html (Sean Anderson)
	// works by making sure all bits to the right of the highest set bit are 1, then
	// adding 1 to get the answer.
	public static int nextHighestPowerOf2(int v) {
		// decrement by 1 (to handle cases where v is already a power of two)
		v--;
		
		// set all bits to the right of the uppermost set bit.
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		// v |= v >> 32; // uncomment for 64 bit input values
		
		// add 1 to get the power of two result
		return v + 1;
	}
	
	public static String getCurrentDateString() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
		return dateFormat.format(new Date());
	}
	
	public static int distToChunkSq(int x, int z, Chunk chunk) {
		int dx = (chunk.xPosition << 4) + 8 - x;
		int dz = (chunk.zPosition << 4) + 8 - z;
		return (dx * dx) + (dz * dz);
	}
}
