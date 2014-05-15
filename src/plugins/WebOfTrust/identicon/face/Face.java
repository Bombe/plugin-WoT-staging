package plugins.WebOfTrust.identicon.face;

import static plugins.WebOfTrust.identicon.Bits.decodeBits;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Definition of a face.
 *
 * @author <a href="mailto:d.roden@xplosion.de">David Roden</a>
 */
public class Face {

	public enum BackgroundColor {

		darkGrey(new Color(0x80, 0x80, 0x80)),
		red(new Color(0xff, 0xc0, 0xc0)),
		green(new Color(0xc0, 0xff, 0xc0)),
		yellow(new Color(0xff, 0xff, 0xc0)),
		blue(new Color(0xc0, 0xc0, 0xff)),
		pink(new Color(0xff, 0xc0, 0xff)),
		turqois(new Color(0xc0, 0xff, 0xff)),
		lightGray(new Color(0xc0, 0xc0, 0xc0));

		private final Color color;

		private BackgroundColor(Color color) {
			this.color = color;
		}

		public Color getColor() {
			return color;
		}

	}

	public enum BackgroundPattern {

		solid {
			@Override
			public void paint(Graphics2D graphics, int width, int height) {
				graphics.fillRect(0, 0, width, height);
			}
		},
		checkered {
			private static final int ROWS = 4;
			private static final int COLUMNS = 4;

			@Override
			public void paint(Graphics2D graphics, int width, int height) {
				for (int x = 0; x < COLUMNS; ++x) {
					for (int y = 0; y < ROWS; ++y) {
						if (((x + y) & 0x01) == 0x01) {
							graphics.fillRect(x * width / COLUMNS, y * height / ROWS, width / COLUMNS, height / ROWS);
						}
					}
				}
			}
		},
		verticallyStriped {
			private static final int COLUMNS = 4;

			@Override
			public void paint(Graphics2D graphics, int width, int height) {
				for (int x = 0; x < COLUMNS; x += 2) {
					graphics.fillRect(x * width / COLUMNS, 0, width / COLUMNS, height);
				}
			}
		},
		horizontallyStriped {
			private static final int ROWS = 4;

			@Override
			public void paint(Graphics2D graphics, int width, int height) {
				for (int y = 0; y < ROWS; y += 2) {
					graphics.fillRect(0, y * width / ROWS, width, height / ROWS);
				}
			}
		},
		slantedLeft {
			private static final int COLUMNS = 4;

			@Override
			public void paint(Graphics2D graphics, int width, int height) {
				graphics.rotate(Math.PI / 4);
				for (int x = 0; x < COLUMNS * 2; x += 2) {
					graphics.fillRect(x * width / COLUMNS, -height, width / COLUMNS, 2 * height);
				}
			}
		},
		slantedRight {
			private static final int COLUMNS = 4;

			@Override
			public void paint(Graphics2D graphics, int width, int height) {
				graphics.rotate(-Math.PI / 4);
				for (int x = 0; x < COLUMNS * 2; x += 2) {
					graphics.fillRect((x - 2) * width / COLUMNS, 0, width / COLUMNS, 2 * height);
				}
			}
		},
		dotted {
			private static final int ROWS = 4;
			private static final int COLUMNS = 4;

			@Override
			public void paint(Graphics2D graphics, int width, int height) {
				for (int x = 0; x < COLUMNS; ++x) {
					for (int y = 0; y < ROWS; ++y) {
						graphics.fillOval((int) ((x + 0.2) * width / COLUMNS), (int) ((y + 0.2) * height / ROWS), (int) (0.6 * (width / COLUMNS)), (int) (0.6 * (height / ROWS)));
					}
				}
			}
		},
		squared {
			private static final int ROWS = 4;
			private static final int COLUMNS = 4;

			@Override
			public void paint(Graphics2D graphics, int width, int height) {
				for (int x = 0; x < COLUMNS; ++x) {
					for (int y = 0; y < ROWS; ++y) {
						graphics.fillRect((int) ((x + 0.25) * width / COLUMNS), (int) ((y + 0.25) * height / ROWS), (int) (0.5 * width / COLUMNS), (int) (0.5 * height / ROWS));
					}
				}
			}
		};

		public abstract void paint(Graphics2D graphics, int width, int height);

	}

	public enum HeadShape {

		wide(0.8),
		normal(0.7),
		thin(0.6),
		thinner(0.5);

		private final double width;

		private HeadShape(double width) {
			this.width = width;
		}

		public double getWidth() {
			return width;
		}

	}

	public enum SkinColor {

		pale(new Color(0xff, 0xf8, 0xf0)),
		normal(new Color(0xff, 0xf0, 0xe0)),
		lightTan(new Color(0xff, 0xe8, 0xd0)),
		strongTan(new Color(0xff, 0xe0, 0xc0)),
		pink(new Color(0xff, 0xd0, 0xc0)),
		yellow(new Color(0xe0, 0xd0, 0x80)),
		lightBrown(new Color(0xd0, 0xb0, 0x80)),
		darkBrown(new Color(0xb0, 0x80, 0x50));

		private final Color color;

		private SkinColor(Color color) {
			this.color = color;
		}

		public Color getColor() {
			return color;
		}

	}

	public enum EarWidth {

		normal(0.16),
		thin(0.12);

		private final double width;

		private EarWidth(double width) {
			this.width = width;
		}

		public double getWidth() {
			return width;
		}

	}

	public enum EarHeight {

		large(0.225),
		normal(0.21),
		small(0.195),
		smaller(0.18);

		private final double height;

		private EarHeight(double height) {
			this.height = height;
		}

		public double getHeight() {
			return height;
		}

	}

	public enum LeftEarRing {

		none(null),
		gold(new Color(0xe0, 0xe0, 0x00));

		private final Color color;

		private LeftEarRing(Color color) {
			this.color = color;
		}

		public Color getColor() {
			return color;
		}

	}

	private final BackgroundColor backgroundColor;
	private final BackgroundPattern backgroundPattern;
	private final HeadShape headShape;
	private final SkinColor skinColor;
	private final EarWidth earWidth;
	private final EarHeight earHeight;
	private final LeftEarRing leftEarRing;

	public Face(BackgroundColor backgroundColor, BackgroundPattern backgroundPattern, HeadShape headShape, SkinColor skinColor, EarWidth earWidth, EarHeight earHeight, LeftEarRing leftEarRing) {
		this.backgroundColor = backgroundColor;
		this.backgroundPattern = backgroundPattern;
		this.headShape = headShape;
		this.skinColor = skinColor;
		this.earWidth = earWidth;
		this.earHeight = earHeight;
		this.leftEarRing = leftEarRing;
	}

	public BackgroundColor getBackgroundColor() {
		return backgroundColor;
	}

	public BackgroundPattern getBackgroundPattern() {
		return backgroundPattern;
	}

	public HeadShape getHeadShape() {
		return headShape;
	}

	public SkinColor getSkinColor() {
		return skinColor;
	}

	public EarWidth getEarWidth() {
		return earWidth;
	}

	public EarHeight getEarHeight() {
		return earHeight;
	}

	public LeftEarRing getLeftEarRing() {
		return leftEarRing;
	}

	public static Face createFace(byte[] data) {
		BackgroundColor backgroundColor = BackgroundColor.values()[(int) decodeBits(data, 0, 3)];
		BackgroundPattern backgroundPattern = BackgroundPattern.values()[(int) decodeBits(data, 3, 3)];
		HeadShape headShape = HeadShape.values()[(int) decodeBits(data, 6, 2)];
		SkinColor skinColor = SkinColor.values()[(int) decodeBits(data, 8, 3)];
		EarWidth earWidth = EarWidth.values()[(int) decodeBits(data, 11, 1)];
		EarHeight earHeight = EarHeight.values()[(int) decodeBits(data, 12, 2)];
		LeftEarRing leftEarRing = LeftEarRing.values()[(int) decodeBits(data, 14, 1)];
		return new Face(backgroundColor, backgroundPattern, headShape, skinColor, earWidth, earHeight, leftEarRing);
	}

}
