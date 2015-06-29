package mapwriter.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/*
MwRender contains most of the code for drawing the overlay.
This includes:
 - loading textures from images
 - saving textures to images
 - allocating and setting up GL textures
 - drawing coloured and textured quads (using minecraft Tesselator class)
 
*/

public class Render {
	public static double zDepth = 200.0D;
	public static final double circleSteps = 30.0;
	
	public static void setColourWithAlphaPercent(int colour, int alphaPercent) {
		setColour(((((alphaPercent * 0xff) / 100) & 0xff) << 24) | (colour & 0xffffff));
	}
	
	public static void setColour(int colour) {
		
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.color(
				(float) ((colour >> 16) & 0xff) / 255.0f,
				(float) ((colour >> 8)  & 0xff) / 255.0f,
				(float) ((colour)       & 0xff) / 255.0f,
				(float) ((colour >> 24) & 0xff) / 255.0f);
		GlStateManager.disableBlend();
	}

	public static void resetColour() {
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f); 
	}
	
	public static int multiplyColours(int c1, int c2) {
		float c1A = (float) ((c1 >> 24) & 0xff);
		float c1R = (float) ((c1 >> 16) & 0xff);
		float c1G = (float) ((c1 >> 8)  & 0xff);
		float c1B = (float) ((c1 >> 0)  & 0xff);
		float c2A = (float) ((c2 >> 24) & 0xff);
		float c2R = (float) ((c2 >> 16) & 0xff);
		float c2G = (float) ((c2 >> 8)  & 0xff);
		float c2B = (float) ((c2 >> 0)  & 0xff);
		int r = (int) (c1R * c2R / 255.0f) & 0xff;
		int g = (int) (c1G * c2G / 255.0f) & 0xff;
		int b = (int) (c1B * c2B / 255.0f) & 0xff;
		int a = (int) (c1A * c2A / 255.0f) & 0xff;
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
	
	public static int getAverageOfPixelQuad(int[] pixels, int offset, int scanSize) {
		int p00 = pixels[offset];
		int p01 = pixels[offset + 1];
		int p10 = pixels[offset + scanSize];
		int p11 = pixels[offset + scanSize + 1];
		
		// ignore alpha channel
		int r = ((p00 >> 16) & 0xff) + ((p01 >> 16) & 0xff) + ((p10 >> 16) & 0xff) + ((p11 >> 16) & 0xff);
		r >>= 2;
		int g = ((p00 >>  8) & 0xff) + ((p01 >>  8) & 0xff) + ((p10 >>  8) & 0xff) + ((p11 >>  8) & 0xff);
		g >>= 2;
		int b =  (p00        & 0xff) +  (p01        & 0xff) + (p10         & 0xff) +  (p11        & 0xff);
		b >>= 2;
		return 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
	}
	
	public static int getAverageColourOfArray(int[] pixels) {
		int count = 0;
		double totalA = 0.0;
		double totalR = 0.0;
		double totalG = 0.0;
		double totalB = 0.0;
		for (int pixel : pixels) {
			double a = (double) ((pixel >> 24) & 0xff);
			double r = (double) ((pixel >> 16) & 0xff);
			double g = (double) ((pixel >> 8)  & 0xff);
			double b = (double) ((pixel >> 0)  & 0xff);
			
			totalA += a;
			totalR += r * a / 255.0;
			totalG += g * a / 255.0;
			totalB += b * a / 255.0;
			
			count++;
		}
		
		totalR = totalR * 255.0 / totalA;
		totalG = totalG * 255.0 / totalA;
		totalB = totalB * 255.0 / totalA;
		totalA = totalA / ((double) count);
		
		return ((((int) (totalA)) & 0xff) << 24) |
				((((int) (totalR)) & 0xff) << 16) |
				((((int) (totalG)) & 0xff) << 8) |
				((((int) (totalB)) & 0xff));
	}
	
	public static int adjustPixelBrightness(int colour, int brightness) {
		int r = ((colour >> 16) & 0xff);
		int g = ((colour >> 8) & 0xff);
		int b = ((colour >> 0) & 0xff);
		r = Math.min(Math.max(0, r + brightness), 0xff);
		g = Math.min(Math.max(0, g + brightness), 0xff);
		b = Math.min(Math.max(0, b + brightness), 0xff);
		return (colour & 0xff000000) | (r << 16) | (g << 8) | (b);
	}
	
	public static int getTextureWidth() {
		return GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
	}
	
	public static int getTextureHeight() {
		return GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
	}
	
	public static int getBoundTextureId() {
		return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
	}
	
	public static void printBoundTextureInfo(int texture) {
		int w = getTextureWidth();
		int h = getTextureHeight();
		int depth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL12.GL_TEXTURE_DEPTH);
		int format = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
		Logging.log("texture %d parameters: width=%d, height=%d, depth=%d, format=%08x", texture, w, h, depth, format);		
	}
	
	public static int getMaxTextureSize() {
		return GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
	}
	
	/*
	Drawing Methods
	
	Note that EntityRenderer.setupOverlayRendering must be called before drawing for the scene
	to appear correctly on the overlay.
	If these functions are called from the hookUpdateCameraAndRender method of Mw this
	will have already been done.
	*/
	
	// draw rectangle with texture stretched to fill the shape
	public static void drawTexturedRect(double x, double y, double w, double h) {
		drawTexturedRect(x, y, w, h, 0.0D, 0.0D, 1.0D, 1.0D);
	}
	
	// draw rectangle with texture UV coordinates specified (so only part of the texture fills the rectangle).
	public static void drawTexturedRect(double x, double y, double w, double h, double u1, double v1, double u2, double v2) {
		try {
			GlStateManager.enableTexture2D();
			GlStateManager.enableBlend();
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	        Tessellator tessellator = Tessellator.getInstance();
	        WorldRenderer renderer  = tessellator.getWorldRenderer();
	        renderer.startDrawingQuads();
	        renderer.addVertexWithUV(x + w, y,     zDepth, u2, v1);
	        renderer.addVertexWithUV(x,     y,     zDepth, u1, v1);
	        renderer.addVertexWithUV(x,     y + h, zDepth, u1, v2);
	        renderer.addVertexWithUV(x + w, y + h, zDepth, u2, v2);
	        //renderer.finishDrawing();
	        tessellator.draw();
	        GlStateManager.disableBlend();
		} catch (NullPointerException e) {
			Logging.log("MwRender.drawTexturedRect: null pointer exception");
		}
	}
	
	public static void drawArrow(double x, double y, double angle, double length) {
		// angle the back corners will be drawn at relative to the pointing angle
		double arrowBackAngle = 0.75D * Math.PI;
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer  = tessellator.getWorldRenderer();
        renderer.startDrawing(GL11.GL_TRIANGLE_FAN);
        renderer.addVertex(x + (length * Math.cos(angle)), y + (length * Math.sin(angle)), zDepth);
        renderer.addVertex(x + (length * 0.5D * Math.cos(angle - arrowBackAngle)), y + (length * 0.5D * Math.sin(angle - arrowBackAngle)), zDepth);
        renderer.addVertex(x + (length * 0.3D * Math.cos(angle + Math.PI)), y + (length * 0.3D * Math.sin(angle + Math.PI)), zDepth);
        renderer.addVertex(x + (length * 0.5D * Math.cos(angle + arrowBackAngle)), y + (length * 0.5D * Math.sin(angle + arrowBackAngle)), zDepth);
        //renderer.finishDrawing();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
	}
	
	public static void drawTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer  = tessellator.getWorldRenderer();
        renderer.startDrawing(GL11.GL_TRIANGLES);
        renderer.addVertex(x1, y1, zDepth);
        renderer.addVertex(x2, y2, zDepth);
        renderer.addVertex(x3, y3, zDepth);
        //renderer.finishDrawing();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
	}
	
	public static void drawRect(double x, double y, double w, double h) {
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer  = tessellator.getWorldRenderer();
        renderer.startDrawingQuads();
        renderer.addVertex(x + w, y,     zDepth);
        renderer.addVertex(x,     y,     zDepth);
        renderer.addVertex(x,     y + h, zDepth);
        renderer.addVertex(x + w, y + h, zDepth);
        //renderer.finishDrawing();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
	}
	
	public static void drawCircle(double x, double y, double r) {
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer  = tessellator.getWorldRenderer();
        renderer.startDrawing(GL11.GL_TRIANGLE_FAN);
        renderer.addVertex(x, y, zDepth);
        // for some the circle is only drawn if theta is decreasing rather than ascending
        double end = Math.PI * 2.0;
        double incr = end / circleSteps;
        for (double theta = -incr; theta < end; theta += incr) {
        	renderer.addVertex(x + (r * Math.cos(-theta)), y + (r * Math.sin(-theta)), zDepth);
        }
       //renderer.finishDrawing();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
	}
	
	public static void drawCircleBorder(double x, double y, double r, double width) {
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer  = tessellator.getWorldRenderer();
        renderer.startDrawing(GL11.GL_TRIANGLE_STRIP);
        // for some the circle is only drawn if theta is decreasing rather than ascending
        double end = Math.PI * 2.0;
        double incr = end / circleSteps;
        double r2 = r + width;
        for (double theta = -incr; theta < end; theta += incr) {
        	renderer.addVertex(x + (r * Math.cos(-theta)), y + (r * Math.sin(-theta)), zDepth);
        	renderer.addVertex(x + (r2 * Math.cos(-theta)), y + (r2 * Math.sin(-theta)), zDepth);
        }
        //renderer.finishDrawing();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
	}
	
	public static void drawRectBorder(double x, double y, double w, double h, double bw) {
		// top border
		Render.drawRect(x - bw, y - bw, w + bw + bw, bw);
		// bottom border
		Render.drawRect(x - bw, y + h,  w + bw + bw, bw);
		// left border
		Render.drawRect(x - bw, y, bw, h);
		// right border
		Render.drawRect(x + w,  y, bw, h);
	}
	
	public static void drawString(int x, int y, int colour, String formatString, Object...args) {
		Minecraft mc = Minecraft.getMinecraft();
		//mc.renderEngine.resetBoundTexture();
		FontRenderer fr = mc.fontRendererObj;
		String s = String.format(formatString, args);
		fr.drawStringWithShadow(s, x, y, colour);
	}
	
	public static void drawCentredString(int x, int y, int colour, String formatString, Object...args) {
		Minecraft mc = Minecraft.getMinecraft();
		//mc.renderEngine.resetBoundTexture();
		FontRenderer fr = mc.fontRendererObj;
		String s = String.format(formatString, args);
		int w = fr.getStringWidth(s);
		fr.drawStringWithShadow(s, x - (w / 2), y, colour);
	}
	
	public static void setCircularStencil(double x, double y, double r) {
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		// disable drawing to the color buffer.
		// circle will only be drawn to depth buffer.
		GlStateManager.colorMask(false, false, false, false);
		// enable writing to depth buffer
		GlStateManager.depthMask(true);
		
		// Clearing the depth buffer causes problems with shader mods.
		// I guess we just have to hope that the rest of the depth buffer
		// contains z values greater than 2000 at this stage in the frame
		// render.
		// It would be much easier to use the stencil buffer instead, but it is
		// not specifically requested in the Minecraft LWJGL display setup code.
		// So the stencil buffer is only available on GL implementations that
		// set it up by default.
		
		// clear depth buffer to z = 3000.0
		//GlStateManager.clearDepth(3000.0);
		//GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
		
		// always write to depth buffer
		GlStateManager.depthFunc(GL11.GL_ALWAYS);
		
		// draw stencil pattern (filled circle at z = 1000.0)
		Render.setColour(0xffffffff);
		Render.zDepth = 1000.0;
		Render.drawCircle(x, y, r);
		Render.zDepth = 200.0;
		
		// re-enable drawing to colour buffer
		GlStateManager.colorMask(true, true, true, true);
		// disable drawing to depth buffer
		GlStateManager.depthMask(false);
		// only draw pixels with z values that are greater
		// than the value in the depth buffer.
		// The overlay is drawn at 2000 so this will pass inside
		// the circle (2000 > 1000) but not outside (2000 <= 3000).
		GlStateManager.depthFunc(GL11.GL_GREATER);
	}
	
	public static void disableStencil() {
		GlStateManager.depthMask(true);
		GlStateManager.depthFunc(GL11.GL_LEQUAL);
		GlStateManager.disableDepth();
		GL11.glDisable(GL11.GL_DEPTH_TEST);
	}
	
	// A better implementation of a circular stencil using the stencil buffer
	// rather than the depth buffer can be found below. It works only on GL
	// implementations that attach a stencil buffer by default (e.g. Intel, but
	// not on Nvidia).
	//
	// To fix this we would need to change the display create line in
	// 'Minecraft.java' file from:
	//   Display.create((new PixelFormat()).withDepthBits(24));
	// to:
	//   Display.create((new PixelFormat()).withDepthBits(24).withStencilBits(8));
	//
	// Then we could use the stencil buffer and the the circular map would have
	// far less problems.
	//
	// I suppose it would also be possible to detect the number of stencil bits
	// available at runtime using GL11.glGetInteger(GL11.GL_STENCIL_BITS) and
	// only use the depth buffer stencil algorithm if it returns 0. But this
	// doesn't solve the problem of the stencil buffer not being initialized by
	// default on some systems.
	
	/*public static void setCircularStencil(double x, double y, double r) {
		GL11.glEnable(GL11.GL_STENCIL_TEST);
		// disable drawing to the color and depth buffers.
		// circle will only be drawn to stencil buffer.
		GL11.glColorMask(false, false, false, false);
		GL11.glDepthMask(false);
		// set up stencil func and op so that a 1 is always written to the stencil buffer
		// whenever a pixel is drawn.
		GL11.glStencilFunc(GL11.GL_NEVER, 1, 0x01);
		// replace stencil buffer value with 1 whenever stencil test fails.
		// keep stencil buffer value otherwise.
		GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_KEEP, GL11.GL_KEEP);
		// enable writing to 8 bits of the stencil buffer
		GL11.glStencilMask(0x01);
		// clear stencil buffer, with mask 0xff
		GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
		// draw stencil pattern
		Render.setColour(0xffffffff);
		Render.drawCircle(x, y, r);
		
		// re-enable drawing to colour and depth buffers
		GL11.glColorMask(true, true, true, true);
		// probably shouldn't enable? -> GL11.glDepthMask(true);
		// disable writing to stencil buffer
		GL11.glStencilMask(0x00);
		// draw only when stencil buffer value == 1 (inside circle)
		GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0x01);
	}
	
	public static void disableStencil() {
		GL11.glDisable(GL11.GL_STENCIL_TEST);
	}*/
}