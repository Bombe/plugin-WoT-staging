package plugins.WebOfTrust.identicon.face;

import static java.awt.Color.black;
import static java.lang.Math.toRadians;
import static org.hamcrest.MatcherAssert.assertThat;

import java.awt.geom.Point2D;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

/**
 * Unit test for {@link Oval}.
 *
 * @author <a href="mailto:d.roden@xplosion.de">David Roden</a>
 */
public class OvalTest {

	private static final double EPSILON = 0.000001;

	@Test
	public void canGetPointOfOvalAt0Degrees() {
		Oval oval = new Oval(0, 0, 20, 20, 0, 0, black, black);
		assertThat(oval.getPoint(toRadians(0)), PointMatcher.isCloseTo(new Point2D.Double(10, 0), EPSILON));
	}

	@Test
	public void canGetPointOfOvalAt90Degrees() {
		Oval oval = new Oval(0, 0, 20, 20, 0, 0, black, black);
		assertThat(oval.getPoint(toRadians(90)), PointMatcher.isCloseTo(new Point2D.Double(0, -10), EPSILON));
	}

	@Test
	public void canGetPointOfOvalAt180Degrees() {
		Oval oval = new Oval(0, 0, 20, 20, 0, 0, black, black);
		assertThat(oval.getPoint(toRadians(180)), PointMatcher.isCloseTo(new Point2D.Double(-10, 0), EPSILON));
	}

	@Test
	public void canGetPointOfOvalAt270Degrees() {
		Oval oval = new Oval(0, 0, 20, 20, 0, 0, black, black);
		assertThat(oval.getPoint(toRadians(270)), PointMatcher.isCloseTo(new Point2D.Double(0, 10), EPSILON));
	}

	@Test
	public void canGetPointOfRotatedOvalAt0Degrees() {
		Oval oval = new Oval(0, 0, 20, 20, toRadians(45), 0, black, black);
		assertThat(oval.getPoint(toRadians(0)), PointMatcher.isCloseTo(new Point2D.Double(7.0710678118, 7.0710678118), EPSILON));
	}

	@Test
	public void canGetPointOfRotatedOvalAt90Degrees() {
		Oval oval = new Oval(0, 0, 20, 20, toRadians(45), 0, black, black);
		assertThat(oval.getPoint(toRadians(90)), PointMatcher.isCloseTo(new Point2D.Double(7.0710678118, -7.0710678118), EPSILON));
	}

	private static class PointMatcher extends TypeSafeMatcher<Point2D.Double> {

		private final Point2D.Double matchingPoint;
		private final double epsilon;

		private PointMatcher(Point2D.Double matchingPoint, double epsilon) {
			this.matchingPoint = matchingPoint;
			this.epsilon = epsilon;
		}

		@Override
		protected boolean matchesSafely(Point2D.Double point) {
			return (Math.abs(point.getX() - matchingPoint.getX()) <= epsilon) && (Math.abs(point.getY() - matchingPoint.getY()) <= epsilon);
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("is within ").appendValue(epsilon).appendText(" to ").appendValue(matchingPoint);
		}

		public static PointMatcher isCloseTo(Point2D.Double point, double epsilon) {
			return new PointMatcher(point, epsilon);
		}

	}

}
