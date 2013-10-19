package mapwriter.region;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/* Bitmap Image writer implementation.
 * 
 * Used to write out very large images without being limited by
 * available memory.
 */

public class BitmapFile {
	
	private final File file;
	private RandomAccessFile raf = null;
	private FileChannel fc = null;
	private MappedByteBuffer buffer = null;
	
	private static final int BMP_HEADER_SIZE = 14;
	private static final int DIB_HEADER_SIZE = 40;
	
	int w = 0;
	int h = 0;
	int pixelsStartOffset = BMP_HEADER_SIZE + DIB_HEADER_SIZE;
	int rowLength = 0;
	
	public BitmapFile(File file) {
		this.file = file;
	}
	
	public boolean open(int w, int h) {
		
		// w * 3 padded to a multiple of 4 bytes
		this.w = w;
		this.h = h;
		this.rowLength = ((w * 3) + 3) & (-4);
		int pixelsLength = rowLength * h;
		int fileSize = this.pixelsStartOffset + pixelsLength;
		
		File dir = this.file.getParentFile();
		if (dir != null) {
			if (dir.exists()) {
				if (!dir.isDirectory()) {
					RegionManager.logError("path %s exists and is not a directory", dir);
					return true;
				}
			} else {
				if (!dir.mkdirs()) {
					RegionManager.logError("could not create directory %s", dir);
					return true;
				}
			}
		}
		
		try {
			this.raf = new RandomAccessFile(this.file, "rw"); 
			FileChannel fc = raf.getChannel();
			this.buffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
			this.buffer.order(ByteOrder.LITTLE_ENDIAN);
			
			// seek to start 
			this.buffer.position(0);
			
			// BMP Header
			
			// write "BM" ID field
			this.buffer.put((byte) 0x42);
			this.buffer.put((byte) 0x4D);
			// write file size
			this.buffer.putInt(fileSize);
			// reserved values
			this.buffer.putShort((short) 0);
			this.buffer.putShort((short) 0);
			// start of pixel data offset
			this.buffer.putInt(this.pixelsStartOffset);
			
			// DIB Header
			
			// length of DIB section
			this.buffer.putInt(DIB_HEADER_SIZE);
			// width and height
			this.buffer.putInt(w);
			this.buffer.putInt(h);
			// planes and bpp
			this.buffer.putShort((short) 1);
			this.buffer.putShort((short) 24);
			// compression (none)
			this.buffer.putInt(0);
			// length of pixel data
			this.buffer.putInt(pixelsLength);
			// horizontal and vertical pixels per meter
			this.buffer.putInt(2835);
			this.buffer.putInt(2835);
			// colours in palette
			this.buffer.putInt(0);
			// important colours in palette (0 = all)
			this.buffer.putInt(0);
			
			// initialize pixels
			for (int i = 0; i < pixelsLength; i++) {
				this.buffer.put((byte) 0);
			}
			
		} catch (IOException e) {
			this.raf = null;
			this.buffer = null;
			this.fc = null;
			RegionManager.logError("exception when opening bitmap file '%s': %s", this.file, e);
		}
		
		return this.buffer == null;
	}
	
	public void close() {
		if (this.buffer != null) {
			this.buffer.force();
		}
		if (this.fc != null) {
			try { this.fc.close();}
			catch (IOException e) {}
		}
		if (this.raf != null) {
			try { this.raf.close();}
			catch (IOException e) {}
		}
		this.buffer = null;
		this.raf = null;
	}
	
	public void setRGB(int x, int y, int w, int h, int[] pixels, int offset, int scanSize) {
		// first pixel in the BMP is the bottom left corner, so need to mirror vertically
		y = this.h - y;
		int bmpOffset = this.pixelsStartOffset + (y * this.rowLength) + (x * 3);
		for (int i = 0; i < h; i++) {
			this.buffer.position(bmpOffset - (i * this.rowLength));
			for(int j = 0; j < w; j++) {
				int pixel = pixels[offset + (i * scanSize) + j];
				this.buffer.put((byte) (pixel & 0xff)); // blue
				this.buffer.put((byte) ((pixel >> 8) & 0xff)); // green
				this.buffer.put((byte) ((pixel >> 16) & 0xff)); // red
			}
		}
	}
}
