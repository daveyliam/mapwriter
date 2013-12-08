package mapwriter.region;

import java.io.File;
import java.util.Arrays;

public class UndergroundPixels extends SurfacePixels {
	
	public UndergroundPixels(Region region, File filename) {
		super(region, filename);
	}
	
	@Override
	public void updateChunk(MwChunk chunk, int y, byte[] mask) {
		int x = (chunk.x << 4);
		int z = (chunk.z << 4);
		int offset = this.region.getPixelOffset(x, z);
		int[] pixels = this.getOrAllocatePixels();
		ChunkRender.render(
			this.region.regionManager.blockColours,
			chunk, pixels, offset, Region.SIZE, y, mask
		);
		this.updateCount++;
	}
	
	@Override
	public int[] getRenderedPixels(int y) {
		int[] srcPixels = this.getPixels();
		int[] renderedPixels = null;
		if (srcPixels != null) {
			renderedPixels = new int[this.pixels.length];
			Arrays.fill(renderedPixels, 0x40000000);
			for (int i = 0; i < this.pixels.length; i++) {
				int pixel = srcPixels[i];
				int pixelY = (pixel >> 24) & 0xff;
				// only show blocks at eye level or 16 blocks below
				if ((y > pixelY) && ((y - pixelY) < 16)) {
					renderedPixels[i] = pixel;
				}
			}
		}
		return renderedPixels;
	}
}
