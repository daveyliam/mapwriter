package mapwriter;

import java.nio.IntBuffer;

import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class Texture {
	
	private int id;
	public final int w;
	public final int h;
	private final IntBuffer pixelBuf;
	
	// allocate new texture and fill from IntBuffer
	public Texture(int w, int h, int fillColour, int minFilter, int maxFilter, int textureWrap) {
		this.id = GL11.glGenTextures();
		this.w = w;
		this.h = h;
		this.pixelBuf = MwUtil.allocateDirectIntBuffer(w * h);
		this.fillRect(0, 0, w, h, fillColour);
		this.pixelBuf.position(0);
		this.bind();
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, this.pixelBuf);
		this.setTexParameters(minFilter, maxFilter, textureWrap);
	}
	
	public Texture(int w, int h, int fillColour) {
		this(w, h, fillColour, GL11.GL_LINEAR, GL11.GL_NEAREST, GL12.GL_CLAMP_TO_EDGE);
	}
	
	// create from existing texture
	public Texture(int id) {
		this.id = id;
		this.bind();
		this.w = Render.getTextureWidth();
		this.h = Render.getTextureHeight();
		this.pixelBuf = MwUtil.allocateDirectIntBuffer(this.w * this.h);
		this.getPixelsFromExistingTexture();
		MwUtil.log("created new MwTexture from GL texture id %d (%dx%d) (%d pixels)", this.id, this.w, this.h, this.pixelBuf.limit());
	}
	
	// free up the resources used by the GL texture
	public synchronized void close() {
		if (this.id != 0) {
			try {
				GL11.glDeleteTextures(this.id);
			} catch (NullPointerException e) {
				MwUtil.log("MwTexture.close: null pointer exception (texture %d)", this.id);
			}
			this.id = 0;
		}
	}
	
	public void setPixelBufPosition(int i) {
		this.pixelBuf.position(i);
	}
	
	public void pixelBufPut(int pixel) {
		this.pixelBuf.put(pixel);
	}
	
	public synchronized void fillRect(int x, int y, int w, int h, int colour) {
		int offset = (y * this.w) + x;
		for (int j = 0; j < h; j++) {
			this.pixelBuf.position(offset + (j * this.w));
			for (int i = 0; i < w; i++) {
				this.pixelBuf.put(colour);
			}
		}
	}
	
	// Copy a rectangular sub-region of dimensions 'w' x 'h' from the pixel buffer to the array 'pixels'.
	public synchronized void getRGB(int x, int y, int w, int h, int[] pixels, int offset, int scanSize, IIcon icon) {
		int bufOffset = (y * this.w) + x;
		for (int i = 0; i < h; i++) {
			try 
			{
			this.pixelBuf.position(bufOffset + (i * this.w));
			this.pixelBuf.get(pixels, offset + (i * scanSize), w);
			}
			catch (IllegalArgumentException e)
			{
				MwUtil.log("MwTexture.getRGB: IllegalArgumentException (icon name: %s; height: %d; width: %d; MaxU: %f; MinU: %f; MaxV: %f; MinV: %f)", icon.getIconName(), icon.getIconHeight(), icon.getIconWidth(), icon.getMaxU(),icon.getMinU(), icon.getMaxV(),icon.getMinV());
				MwUtil.log("MwTexture.getRGB: IllegalArgumentException (pos: %d)", bufOffset + (i * this.w));
				MwUtil.log("MwTexture.getRGB: IllegalArgumentException (buffersize: %d)", this.pixelBuf.limit());
			}
		}
	}
	
	// Copy a rectangular sub-region of dimensions 'w' x 'h' from the array 'pixels' to the pixel buffer.
	public synchronized void setRGB(int x, int y, int w, int h, int[] pixels, int offset, int scanSize) {
		int bufOffset = (y * this.w) + x;
		for (int i = 0; i < h; i++) {
			this.pixelBuf.position(bufOffset + (i * this.w));
			this.pixelBuf.put(pixels, offset + (i * scanSize), w);
		}
	}
	
	public synchronized void setRGB(int x, int y, int colour) {
		this.pixelBuf.put((y * this.w) + x, colour);
	}
	
	public synchronized int getRGB(int x, int y) {
		return this.pixelBuf.get((y * this.w) + x);
	}
	
	public void bind() {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.id);
	}
	
	// set texture scaling and wrapping parameters
	public void setTexParameters(int minFilter, int maxFilter, int textureWrap) {
		this.bind();
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, textureWrap);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, textureWrap);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, maxFilter);
	}
	
	public void setLinearScaling(boolean enabled) {
		this.bind();
		if (enabled) {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		} else {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		}
	}
	
	// update texture from pixels in pixelBuf
	public synchronized void updateTextureArea(int x, int y, int w, int h) {
		try {
			this.bind();
			GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, this.w);
			this.pixelBuf.position((y * this.w) + x);
			GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, w, h, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, this.pixelBuf);
			GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
		} catch (NullPointerException e) {
			MwUtil.log("MwTexture.updatePixels: null pointer exception (texture %d)", this.id);
		}
	}
	
	public synchronized void updateTexture() {
		this.bind();
		this.pixelBuf.position(0);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, this.w, this.h, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, this.pixelBuf);
	}
	
	// copy pixels from GL texture to pixelBuf
	private synchronized void getPixelsFromExistingTexture() {
		try {
			this.bind();
			this.pixelBuf.clear();
			GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, this.pixelBuf);
			// getTexImage does not seem to advance the buffer position, so flip does not work here
			// this.pixelBuf.flip()
			this.pixelBuf.limit(this.w * this.h);
		} catch (NullPointerException e) {
			MwUtil.log("MwTexture.getPixels: null pointer exception (texture %d)", this.id);
		}
	}
}
