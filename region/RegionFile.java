package mapwriter.region;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/* Anvil region file reader/writer implementation.
 *
 * This code is very similar to RegionFile and RegionFileChunkBuffer from
 * Minecraft. Not sure if it would be better just to use the Minecraft code.
 * 
 */

public class RegionFile {
	
	final File file;
	int lengthInSectors = 0;
	RandomAccessFile fin = null;
	
	Section[] chunkSectionsArray = new Section[4096];
	int[] timestampArray = new int[4096];
	ArrayList<Boolean> filledSectorArray = null;
	
	private class Section {
		final int startSector;
		final int length;
		Section(int startSector, int length) {
			this.startSector = startSector;
			this.length = length;
		}
		Section(int sectorAndSize) {
			this((sectorAndSize >> 8) & 0xffffff, sectorAndSize & 0xff);
		}
		int getSectorAndSize() {
			return (this.startSector << 8) | (this.length & 0xff);
		}
	}
	
	public RegionFile(File file) {
		this.file = file;
	}
	
	public String toString() {
		return String.format("%s", this.file);
	}
	
	public boolean exists() {
		return this.file.isFile();
	}
	
	public boolean isOpen() {
		return (this.fin != null);
	}
	
	// set the corresponding bits in filledSectorArray to 'filled'
	// for 'count' sectors, starting at 'firstSector'.
	private void setFilledSectorArray(Section section, boolean filled) {
		int endSector = section.startSector + section.length;
		int sectorsToAppend = endSector + 1 - this.filledSectorArray.size();
		for (int i = 0; i < sectorsToAppend; i++) {
			this.filledSectorArray.add(Boolean.valueOf(false));
		}
		for (int i = section.startSector; i < endSector; i++) {
			this.filledSectorArray.set(i, Boolean.valueOf(filled));
		}
	}
	
	private Section getFreeSection(int requiredLength) {
		int start = 0;
		int length = 0;
		int closestStart = 0;
		int closestLength = Integer.MAX_VALUE;
		// start at 2 to skip headers
		int i;
		for (i = 2; i < this.filledSectorArray.size(); i++) {
			if (this.filledSectorArray.get(i)) {
				// sector filled
				if (length >= requiredLength) {
					// end of free section
					if (length < closestLength) {
						closestLength = length;
						closestStart = start;
						if (closestLength == requiredLength) {
							break;
						}
					}
					length = 0;
				}
			} else {
				// sector empty
				if (length == 0) {
					start = i;
				}
				length++;
			}
		}
		
		if (closestStart <= 0) {
			// append to end of file
			closestStart = i;
		}
		
		return (closestStart != 0) ? new Section(closestStart, requiredLength) : null;
	}
	
	public void printInfo() {
		int freeCount = 0;
		int filledCount = 0;
		// start at 2 to skip headers
		for (int i = 2; i < this.filledSectorArray.size(); i++) {
			if (this.filledSectorArray.get(i)) {
				filledCount++;
			} else {
				freeCount++;
			}
		}
		RegionManager.logInfo("Region File %s: filled sectors = %d, free sectors = %d", this, filledCount, freeCount);
		
		String s = "";
		int i;
		for (i = 0; i < this.filledSectorArray.size(); i++) {
			if ((i & 31) == 0) {
				s = String.format("%04x:", i);
			}
			s += this.filledSectorArray.get(i) ? '1' : '0';
			if ((i & 31) == 31) {
				RegionManager.logInfo("%s", s);
			}
		}
		if ((i & 31) != 31) {
			RegionManager.logInfo("%s", s);
		}
	}
	
	private Section getChunkSection(int x, int z) {
		return this.chunkSectionsArray[((z & 31) << 5) | (x & 31)];
	}
	
	private void addChunkSection(int chunkIndex, int sectorAndSize) {
		if (sectorAndSize != 0) {
			Section section = new Section(sectorAndSize);
			this.chunkSectionsArray[chunkIndex] = section;
			this.setFilledSectorArray(section, true);
		} else {
			this.chunkSectionsArray[chunkIndex] = null;
		}
	}
	
	private void updateChunkSection(int x, int z, Section newSection) throws IOException {
		int chunkIndex = ((z & 31) << 5) | (x & 31);
		this.fin.seek(chunkIndex * 4);
		if (newSection != null) {
			this.fin.writeInt(newSection.getSectorAndSize());
		} else {
			this.fin.writeInt(0);
		}
		
		this.chunkSectionsArray[chunkIndex] = newSection;
	}
	
	public boolean open() {
		File dir = this.file.getParentFile();
		if (dir.exists()) {
			if (!dir.isDirectory()) {
				RegionManager.logError("region directory %s exists and is not a directory", dir);
				return true;
			}
		} else {
			if (!dir.mkdirs()) {
				RegionManager.logError("could not create directory %s", dir);
				return true;
			}
		}
		try {
			this.fin = new RandomAccessFile(this.file, "rw");
			
			// seek to start 
			this.fin.seek(0);
			
			this.lengthInSectors = (int) ((this.fin.length() + 4095L) / 4096L);
			
			this.filledSectorArray = new ArrayList<Boolean>();
			
			Arrays.fill(this.chunkSectionsArray, null);
			Arrays.fill(this.timestampArray, 0);
			
			if (this.lengthInSectors < 3) {
				// no chunk data
				// fill chunk and timestamp tables with 0's
				for (int i = 0; i < 2048; i++) {
					this.fin.writeInt(0);
				}
			} else {
				for (int i = 0; i < 1024; i++) {
					this.addChunkSection(i, this.fin.readInt());
				}
				for (int i = 0; i < 1024; i++) {
					this.timestampArray[i] = this.fin.readInt();
				}
			}
			
			this.printInfo();
			
		} catch (Exception e) {
			this.fin = null;
			RegionManager.logError("exception when opening region file '%s': %s", this.file, e);
			
		}
		
		return this.fin == null;
	}
	
	public void close() {
		if (this.fin != null) {
			try { this.fin.close(); }
			catch (IOException e) {}
		}
	}
	
	public DataInputStream getChunkDataInputStream(int x, int z) {
		DataInputStream dis = null;
		if (this.fin != null) {
			Section section = getChunkSection(x, z);
			if (section != null) {
				int offset = section.startSector * 4096;
				try {
					// read length in sectors and compressed data version
					this.fin.seek(offset);
					int length = this.fin.readInt();
					byte version = this.fin.readByte();
					// version 1 = gzip compressed, version 2 = zlib/inflater compressed
					if ((length > 0) && ((length + 4) < (section.length * 4096)) && (version == 2)) {
						// read the compressed data
						byte[] compressedChunkData = new byte[length - 1];
						this.fin.read(compressedChunkData);
						// create a buffered inflater stream on the compressed data
						dis = new DataInputStream(new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(compressedChunkData))));
					} else {
						RegionManager.logError("error: unsupported chunk version %d", version);
					}
				} catch (Exception e) {
					RegionManager.logError("error: %s while reading chunk (%d, %d)", e, x, z);
					dis = null;
				}
			}
		}
		return dis;
	}
	
	// basically an in memory byte array that writes its contents
	// to a file when it is closed.
	private class RegionFileChunkBuffer extends ByteArrayOutputStream {
	    private final int x;
	    private final int z;
	    private final RegionFile regionFile;

	    public RegionFileChunkBuffer(RegionFile regionFile, int x, int z) {
	        super(8096);
	        this.regionFile = regionFile;
	        this.x = x;
	        this.z = z;
	    }

	    public void close() {
	    	this.regionFile.writeCompressedChunk(this.x, this.z, this.buf, this.count);
	    }
	}
	
	public DataOutputStream getChunkDataOutputStream(int x, int z) {
		return new DataOutputStream(new DeflaterOutputStream(new RegionFileChunkBuffer(this, x, z)));
	}
	
	/*private int padToSectorSize() throws IOException {
		// pad with 0 so that the file length is a multiple of 4096 bytes
		long paddedLength = (this.length + 4095L) & (-4096L);
		this.fin.seek(this.length);
		for (long i = this.length; i < paddedLength; i++) {
			this.fin.writeByte(0);
		}
		this.length = paddedLength;
		return (int) (paddedLength / 4096);
	}*/
	
	private void writeChunkDataToSection(Section section, byte[] compressedChunkData, int length) throws IOException {
		this.fin.seek(((long) section.startSector) * 4096L);
		// write version and length
		this.fin.writeInt(length);
		this.fin.writeByte(2);
		// write compressed data
		this.fin.write(compressedChunkData, 0, length);
		
		int endSector = section.startSector + section.length;
		if ((endSector + 1) > this.lengthInSectors) {
			this.lengthInSectors = endSector + 1;
		}
	}
	
	private boolean writeCompressedChunk(int x, int z, byte[] compressedChunkData, int length) {
		// if larger than the existing chunk data or chunk does not exist then need to find the
		// first possible file position to write to. This will either be a contiguous strip of
		// free sectors longer than the length of the chunk data, or the end of the file (append).
		
		// free the section this chunk currently occupies
		Section currentSection = this.getChunkSection(x, z);
		if (currentSection != null) {
			this.setFilledSectorArray(currentSection, false);
		}
		
		int requiredSectors = (length + 5 + 4095) / 4096;
		Section newSection;
		
		if ((currentSection != null) && (requiredSectors <= currentSection.length)) {
			// if the chunk still fits in it's current location don't move
			//RegionManager.logInfo("chunk (%d, %d) fits in current location %d", x, z, currentSection.startSector);
			newSection = new Section(currentSection.startSector, requiredSectors);
		} else {
			// otherwise find a free section large enough to hold the chunk data
			newSection = getFreeSection(requiredSectors);
		}
			
		try {
			//RegionManager.logInfo("writing %d bytes to sector %d for chunk (%d,  %d)", length, newSection.startSector, x, z);
			this.writeChunkDataToSection(newSection, compressedChunkData, length);
			// update the header
			this.updateChunkSection(x, z, newSection);
		} catch (IOException e) {
			RegionManager.logError("error: could not write chunk to region file: %s", e);
		}
			
		// if the chunk was appended then this won't do anything as the
		// filledSectorArray is based on the file size on open.
		this.setFilledSectorArray(newSection, true);
		
		return false;
	}
}
