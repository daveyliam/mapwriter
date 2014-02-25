package mapwriter.region;

public interface IChunk {
	public int getBlockAndMetadata(int x, int y, int z);
	public int getBiome(int x, int z);
	public int getLightValue(int x, int y, int z);
	public int getMaxY();
}
