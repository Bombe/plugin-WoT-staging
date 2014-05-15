package plugins.WebOfTrust.identicon.face;

import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_ROUND;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

/**
 * An oval shape that can paint itself to a {@link Graphics2D} object.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
class Oval {

	private final double centerX;
	private final double centerY;
	private final double width;
	private final double height;
	private final double rotation;
	private final double lineWidth;
	private final Color lineColor;
	private final Color fillColor;

	Oval(Point2D.Double center, double width, double height, double rotation, double lineWidth, Color lineColor) {
		this(center.getX(), center.getY(), width, height, rotation, lineWidth, lineColor, null);
	}

	Oval(Point2D.Double center, double width, double height, double rotation, double lineWidth, Color lineColor, Color fillColor) {
		this(center.getX(), center.getY(), width, height, rotation, lineWidth, lineColor, fillColor);
	}

	Oval(double centerX, double centerY, double width, double height, double rotation, double lineWidth, Color lineColor, Color fillColor) {
		this.centerX = centerX;
		this.centerY = centerY;
		this.width = width;
		this.height = height;
		this.rotation = rotation;
		this.lineWidth = lineWidth;
		this.lineColor = lineColor;
		this.fillColor = fillColor;
	}

	public void draw(Graphics2D graphics, int width, int height) {
		Graphics2D rotated = (Graphics2D) graphics.create();
		rotated.setStroke(new BasicStroke((int) (lineWidth * width), CAP_ROUND, JOIN_ROUND));
		rotated.translate(centerX * width, centerY * height);
		rotated.rotate(rotation);
		if (fillColor != null) {
			rotated.setColor(fillColor);
			rotated.fillOval((int) ((-this.width / 2) * width), (int) ((-this.height / 2) * height), (int) (this.width * width), (int) (this.height * height));
		}
		rotated.setColor(lineColor);
		rotated.drawOval((int) ((-this.width / 2) * width), (int) ((-this.height / 2) * height), (int) (this.width * width), (int) (this.height * height));
	}

	public Point2D.Double getPoint(double radians) {
		double x = centerX + cos(radians) * width / 2;
		double y = centerY - sin(radians) * height / 2;
		double rotatedX = centerX + ((x - centerX) * cos(rotation) - (y - centerY) * sin(rotation));
		double rotatedY = centerY + ((x - centerX) * sin(rotation) + (y - centerY) * cos(rotation));
		return new Point2D.Double(rotatedX, rotatedY);
	}

}
