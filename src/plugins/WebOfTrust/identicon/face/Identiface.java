package plugins.WebOfTrust.identicon.face;

import static java.awt.Color.black;
import static java.awt.Color.white;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.lang.Math.PI;
import static java.lang.Math.toRadians;
import static plugins.WebOfTrust.identicon.face.Face.LeftEarRing.bottom;
import static plugins.WebOfTrust.identicon.face.Face.LeftEarRing.doubleBottom;
import static plugins.WebOfTrust.identicon.face.Face.LeftEarRing.top;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Alternativ identicon implementation that draws faces from the bits of a
 * routing key.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Identiface {

	private final Face face;

	public Identiface(Face face) {
		this.face = face;
	}

	public RenderedImage render(int width, int height) {
		BufferedImage background = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D backgroundGraphcs = (Graphics2D) background.getGraphics();

		backgroundGraphcs.setColor(white);
		backgroundGraphcs.fillRect(0, 0, width, height);

		backgroundGraphcs.setColor(face.getBackgroundColor().getColor());
		face.getBackgroundPattern().paint((Graphics2D) backgroundGraphcs.create(), width, height);

		Head head = Head.createHead(face);
		head.render(backgroundGraphcs, width, height);

		return background;
	}

	private static class Head {

		private final Oval head;
		private final Oval leftEar;
		private final Oval rightEar;
		private final List<Oval> leftEarRings = new ArrayList<Oval>();
		private final Oval leftEye;
		private final Oval rightEye;

		public Head(double headWidth, Color skinColor, double earWidth, double earHeight, boolean leftBottomEarRing, boolean leftBottomDoubleEarRing, boolean leftTopEarRing, Color leftEyeColor, Color rightEyeColor) {
			head = new Oval(0.5, 0.5, headWidth, 0.8, 0, 1 / 40.0, black, skinColor);
			leftEar = new Oval(head.getPoint(PI), earWidth, earHeight, -PI / 10, 1 / 40.0, black, skinColor);
			rightEar = new Oval(head.getPoint(0), earWidth, earHeight, PI / 10, 1 / 40.0, black, skinColor);
			if (leftBottomEarRing) {
				leftEarRings.add(new Oval(leftEar.getPoint(toRadians(225)), 0.025, 0.05, PI / 10, 1 / 40.0, new Color(0xe0, 0xe0, 0x00)));
			}
			if (leftBottomDoubleEarRing) {
				leftEarRings.add(new Oval(leftEar.getPoint(toRadians(235)), 0.025, 0.05, PI * 0.1, 1 / 40.0, new Color(0xe0, 0xe0, 0x00)));
				leftEarRings.add(new Oval(leftEar.getPoint(toRadians(210)), 0.025, 0.05, PI * 0.21, 1 / 40.0, new Color(0xe0, 0xe0, 0x00)));
			}
			if (leftTopEarRing) {
				leftEarRings.add(new Oval(leftEar.getPoint(toRadians(120)), 0.025, 0.05, -PI * 0.25, 1 / 40.0, new Color(0xe0, 0xe0, 0x00)));
			}
			leftEye = new Oval(0.5 - ((headWidth / 2) * 0.35), 0.35, 0.12, 0.07, 0, 1 / 80, black, leftEyeColor);
			rightEye = new Oval(0.5 + ((headWidth / 2) * 0.35), 0.35, 0.12, 0.07, 0, 1 / 80, black, leftEyeColor);
		}

		public void render(Graphics2D graphics, int width, int height) {
			graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
			drawEars(graphics, width, height);
			head.draw(graphics, width, height);
			leftEye.draw(graphics, width, height);
			rightEye.draw(graphics, width, height);
		}

		private void drawEars(Graphics2D graphics, int width, int height) {
			drawLeftEar(graphics, width, height);
			rightEar.draw(graphics, width, height);
		}

		private void drawLeftEar(Graphics2D graphics, int width, int height) {
			for (Oval leftEarRing : leftEarRings) {
				leftEarRing.drawArc(graphics, width, height, toRadians(255), toRadians(180));
			}
			leftEar.draw(graphics, width, height);
			for (Oval leftEarRing : leftEarRings) {
				leftEarRing.drawArc(graphics, width, height, toRadians(75), toRadians(180));
			}
		}

		public static Head createHead(Face face) {
			return new Head(
					face.getHeadShape().getWidth(),
					face.getSkinColor().getColor(),
					face.getEarWidth().getWidth(),
					face.getEarHeight().getHeight(),
					face.getLeftEarRing() == bottom,
					face.getLeftEarRing() == doubleBottom,
					face.getLeftEarRing() == top,
					face.getEyeColor().getLeftEyeColor(),
					face.getEyeColor().getRightEyeColor()
			);
		}

	}

}
