package plugins.WebOfTrust.identicon.face;

import static java.awt.Color.black;
import static java.awt.Color.white;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.lang.Math.PI;
import static java.util.Arrays.asList;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
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

		private final Oval head;
		private final Oval leftEar;
		private final Oval rightEar;

		public Head(double headWidth, Color skinColor, double earWidth, double earHeight) {
			head = new Oval(0.5, 0.5, headWidth, 0.8, 0, 1 / 40.0, black, skinColor);
			leftEar = new Oval(head.getPoint(PI), earWidth, earHeight, -PI / 10, 1 / 40.0, black, skinColor);
			rightEar = new Oval(head.getPoint(0), earWidth, earHeight, PI / 10, 1 / 40.0, black, skinColor);
		}

		public void render(Graphics2D graphics, int width, int height) {
			graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
			drawEars(graphics, width, height);
			head.draw(graphics, width, height);
		}

		private void drawEars(Graphics2D graphics, int width, int height) {
			leftEar.draw(graphics, width, height);
			rightEar.draw(graphics, width, height);
		}

		public static Head createHead(BitShiftedInputStream bitStream) throws IOException {
			int headShape = bitStream.read(2);
			int skinColor = bitStream.read(3);
			int earWidth = bitStream.read(1);
			int earHeight = bitStream.read(2);
			return new Head(
					0.5 + (0.3 * headShape / 4),
					skinColors.get(skinColor),
					0.12 + 0.04 * earWidth,
					0.18 + 0.06 * earHeight / 4
			);
		}

	}

}
