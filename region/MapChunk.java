package mapwriter.region;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.chunk.Chunk;

public class MapChunk {
	int[] data = new int[512];
	
	int createFromChunk(Chunk chunk) {
		int i = 256;
		int columnIndex = 0;
		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int currentBlock = 0;
				int runLength = 0;
				int yStart = 255;
				int columnStart = i;
				
				for (int y = 255; y > 0; y--) {
					
					int block = ((chunk.getBlockID(x, y, z) & 0xfff) << 4) | (chunk.getBlockMetadata(x, y, z) & 0xf);
					
					if (block == currentBlock) {
						runLength++;
					} else {
						if ((runLength > 0) && (currentBlock != 0)) {
							this.data[i++] = ((yStart & 0xff) << 24) | ((runLength & 0xff) << 16) | (currentBlock & 0xffff);
						}
						yStart = y;
						currentBlock = block;
						runLength = 1;
					}
				}
				
				int biome = chunk.getBiomeArray()[(z << 4) | x];
				this.data[columnIndex++] = ((biome & 0xff) << 24) | ((columnStart & 0xffff) << 8) | ((i - columnStart) & 0xff);
			}
		}
		
		return i;
	}
	
	int getColumnIndex(int x, int z) {
		return this.columnIndexArray[((z & 0xf) << 4) | (x & 0xf)];
	}
	
	void setColumn(int x, int z, int[] column) {
		this.compressedColumns[((z & 0xf) << 4) | (x & 0xf)] = column;
	}
	
	private int decompressColumn(int[] src, short[] dst) {
		int i = 0;
		for (int e : src) {
			int length = (e >> 16) & 0xff;
			short blockID = (short) (e & 0xffff);
			
			for(; length > 0; length--) {
				dst[i++] = blockID;
			}
		}
		
		return i;
	}
	
	private int compressColumn(short[] src, int[] dst) {
		int i = 0;
		int runLength = 0;
		short currentBlockID = 0;
		for (short blockID : src) {
			if (blockID == currentBlockID) {
				runLength++;
			} else {
				if (runLength > 0) {
					dst[i++] = ((runLength & 0xff) << 16) | (currentBlockID & 0xffff);
				}
				currentBlockID = blockID;
				runLength = 1;
			}
		}
		if (runLength > 0) {
			dst[i++] = ((runLength & 0xff) << 16) | (currentBlockID & 0xffff);
		}
		
		//int[] compressedColumn = null;
		//if (i > 0) {
		//	compressedColumn = new int[i];
		//	System.arraycopy(this.compressBuffer, 0, compressedColumn, 0, i);
		//}
		
		return i;
	}
	
	void insertSection(int x, int y, int z, short[] section) {
		int[] column = this.getColumn(x, z);
		short[] decompressedColumn = new short[256];
		if (column != null) {
			this.decompressColumn(column, decompressedColumn);
		}
		System.arraycopy(section, 0, decompressedColumn, y, section.length);
		
		int[] newColumn = null;
		int[] buffer = new int[256];
		int length = this.compressColumn(decompressedColumn, buffer);
		
		if (length > 0) {
			newColumn = new int[length];
			System.arraycopy(buffer, 0, newColumn, 0, length);
		}
		
		this.setColumn(x, z, newColumn);
	}
}
