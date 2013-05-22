package mapwriter.region;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import mapwriter.Mw;
import mapwriter.MwUtil;

/* Anvil region file reader/writer implementation.
 * Not currently used as the Minecraft RegionFile and RegionFileCache classes
 * seem to work well, without being too tied up with other Minecraft code.
 */

public class RegionFile {
	
	byte[] dataArray;
	ByteBuffer dataBuffer;
	
	boolean loaded = false;
	int x;
	int z;
	int dimension;
	
	static final int DEFLATE_BUFFER_SIZE = 1024 * 256;
	static final int CHUNKS_PER_REGION = 32 * 32;
	static final int MAX_REGION_FILE_SIZE = 16 * 1024 * 1024;
	
	public RegionFile() {
		this.dataArray = new byte[MAX_REGION_FILE_SIZE];
		this.dataBuffer = ByteBuffer.wrap(dataArray, 0, dataArray.length);
	}
	
	public boolean equals(int x, int z, int dimension) {
		x &= Mw.REGION_MASK;
		z &= Mw.REGION_MASK;
		return (x == this.x) && (z == this.z) && (dimension == this.dimension);
	}
	
	public boolean load(File worldDir, int x, int z, int dimension) {
		boolean error = true;
		FileInputStream fin = null;
		
		x &= Mw.REGION_MASK;
		z &= Mw.REGION_MASK;
		this.x = x;
		this.z = z;
		this.dimension = dimension;
		
		File dimensionDir = MwUtil.getDimensionDir(worldDir, dimension);
		File regionDir = new File(dimensionDir, "region");
		
		File regionFilePath = new File(regionDir, String.format("r.%d.%d.mca", x >> Mw.REGION_SHIFT, z >> Mw.REGION_SHIFT));
		if (regionFilePath.isFile()) {
			try {
				MwUtil.log("MapWriter: reading region file '%s'", regionFilePath.getPath());
				fin = new FileInputStream(regionFilePath);
				int n = fin.read(this.dataArray);
				error = (n <= 0);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (fin != null) {
					try { fin.close(); }
					catch (Exception e) {}
				}
			}
		}
		
		this.loaded = !error;
		
		return error;
	}
	
	private int getChunkSectorAndSize(int x, int z) {
		int sectorAndSize = 0;
		if ((dataArray != null) && (this.loaded)) {
			try {
				// byte | 0     1     2 |         3       |
				//      | first_sector  | size_in_sectors |
				// first_sector and size_in_sectors in units of 4096 byte sectors from start of file.
				sectorAndSize = this.dataBuffer.getInt(((z * 32) + x) * 4);
			} catch (Exception e) {
				System.out.format("error: %s while getting header for chunk (%d, %d), is region file corrupt?\n", e, x, z);
			}
		}
		return sectorAndSize;
	}
	
	public boolean getChunkData(int x, int z, byte[] chunkData) {
		boolean error = true;
		
		int sectorAndSize = getChunkSectorAndSize(x, z);
		
		if (sectorAndSize != 0) {
			int offset = (sectorAndSize & 0xffffff00) << 4;
			Inflater inflater = null;
			try {
				int length = this.dataBuffer.getInt(offset);
				byte version = this.dataBuffer.get(offset + 4);
				// inflate (decompress) chunk data
				if (version == 2) {
					inflater = new Inflater();
					inflater.setInput(this.dataArray, offset + 5, length - 1);
					inflater.inflate(chunkData, 0, chunkData.length);
					error = !inflater.finished();
					if (error) {
						MwUtil.log("error: %d input bytes remaining while decompressing chunk (%d, %d)", inflater.getRemaining(), x, z);
					}
				} else {
					MwUtil.log("error: unsupported chunk version %d", version);
				}
			} catch (Exception e) {
				MwUtil.log("error: %s while decompressing chunk (%d, %d)", e, x, z);
			} finally {
				if (inflater != null) {
					inflater.end();
				}
				inflater = null;
			}
		}
		return error;
	}
	
	public boolean writeChunkData(int x, int z, byte[] chunkData) {
		Deflater deflater = null;
		boolean error = true;
		byte[] deflateBuffer = new byte[DEFLATE_BUFFER_SIZE];
		int length = 0;
		try {
			deflater = new Deflater();
			deflater.setInput(chunkData);
			length = deflater.deflate(deflateBuffer);
			deflater.finish();
			error = !deflater.finished();
			if (error) {
				MwUtil.log("error compressing chunk (%d, %d)", x, z);
			}
		} catch (Exception e) {
			
		} finally {
			if (deflater != null) {
				deflater.end();
			}
			deflater = null;
		}
		
		int sectorAndSize = getChunkSectorAndSize(x, z);
		
		if (sectorAndSize != 0) {
			// we are replacing an existing chunk
			int size = (sectorAndSize & 0xff) << 12;
			int offset = (sectorAndSize & 0xffffff00) << 4;
			
			if ((length + 5) <= size) {
				// enough room to replace existing chunk in place
				try {
					this.dataBuffer.position(offset);
					this.dataBuffer.putInt(length);
					this.dataBuffer.put((byte) 2);
					this.dataBuffer.put(deflateBuffer, 0, length);
				} catch (Exception e) {
					MwUtil.log("exception %s while writing chunk (%d, %d)", e, x, z);
				}
			}
		} else {
			// chunk not in region file, add to end
			
		}
		
		return error;
	}
}