package plugins.WebOfTrust.identicon.face;

import static java.awt.Color.black;
import static java.awt.Color.white;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.util.Arrays.asList;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import plugins.WebOfTrust.identicon.BitShiftedInputStream;

/**
 * Alternativ identicon implementation that draws faces from the bits of a
 * routing key.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Identiface {

	private final byte[] data;

	public Identiface(byte[] data) {
		this.data = data;
	}

	public RenderedImage render(int width, int height) {
		BufferedImage background = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D backgroundGraphcs = (Graphics2D) background.getGraphics();
		backgroundGraphcs.setColor(white);
		backgroundGraphcs.fillRect(0, 0, width, height);
		BitShiftedInputStream bitStream = new BitShiftedInputStream(new ByteArrayInputStream(data), 8);
		try {
			Head head = Head.createHead(bitStream);
			head.render(backgroundGraphcs, width, height);
		} catch (IOException ioe1) {
			/* should never happen. */
			throw new RuntimeException(ioe1);
		}
		return background;
	}

	private static class Head {

		private static final List<Color> skinColors = asList(
				new Color(0xff, 0xf8, 0xf0),
				new Color(0xff, 0xf0, 0xe0),
				new Color(0xff, 0xe8, 0xd0),
				new Color(0xff, 0xe0, 0xc0),
				new Color(0xff, 0xd0, 0xc0),
				new Color(0xe0, 0xd0, 0x80),
				new Color(0xd0, 0xb0, 0x80),
				new Color(0xb0, 0x80, 0x50)
		);

		private final Color skinColor;
		private final Rectangle2D.Double head;
		private final Rectangle2D.Double leftEar;
		private final Rectangle2D.Double rightEar;

		public Head(double headWidth, Color skinColor, double earWidth, double earHeight) {
			this.skinColor = skinColor;
			head = new Rectangle2D.Double(0.5 - headWidth / 2, 0.1, headWidth, 0.8);
			leftEar = new Rectangle2D.Double(0.5 - headWidth / 2 - earWidth / 2, 0.5 - earHeight / 2, earWidth, earHeight);
			rightEar = new Rectangle2D.Double(0.5 + headWidth / 2 - earWidth / 2, 0.5 - earHeight / 2, earWidth, earHeight);
		}

		public void render(Graphics2D graphics, int width, int height) {
			graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
			graphics.setStroke(new BasicStroke(width / 40.0f));
			drawEars(graphics, width, height);
			drawAndFillHead(graphics, width, height);
		}

		private void fillOval(Graphics2D graphics, Rectangle2D.Double rectangle, int width, int height) {
			graphics.fillOval((int) (rectangle.getX() * width), (int) (rectangle.getY() * height), (int) (rectangle.getWidth() * width), (int) (rectangle.getHeight() * height));
		}

		private void drawOval(Graphics2D graphics, Rectangle2D.Double rectangle, int width, int height) {
			graphics.drawOval((int) (rectangle.getX() * width), (int) (rectangle.getY() * height), (int) (rectangle.getWidth() * width), (int) (rectangle.getHeight() * height));
		}

		private void drawAndFillHead(Graphics2D graphics, int width, int height) {
			graphics.setColor(skinColor);
			fillOval(graphics, head, width, height);
			graphics.setColor(black);
			drawOval(graphics, head, width, height);
		}

		private void drawEars(Graphics2D graphics, int width, int height) {
			drawLeftEar(graphics, width, height);
			drawRightEar(graphics, width, height);
		}

		private void drawLeftEar(Graphics2D graphics, int width, int height) {
			graphics.setColor(skinColor);
			fillOval(graphics, leftEar, width, height);
			graphics.setColor(black);
			drawOval(graphics, leftEar, width, height);
		}

		private void drawRightEar(Graphics2D graphics, int width, int height) {
			graphics.setColor(skinColor);
			fillOval(graphics, rightEar, width, height);
			graphics.setColor(black);
			drawOval(graphics, rightEar, width, height);
		}

		public static Head createHead(BitShiftedInputStream bitStream) throws IOException {
			int headShape = bitStream.read(2);
			int skinColor = bitStream.read(3);
			int earWidth = bitStream.read(1);
			int earHeight = bitStream.read(2);
			return new Head(
					0.5 + (0.3 * headShape / 4),
					skinColors.get(skinColor),
					0.12 + 0.05 * earWidth,
					0.17 + 0.06 * earHeight / 4
			);
		}

	}

}
