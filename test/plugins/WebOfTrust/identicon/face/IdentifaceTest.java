package plugins.WebOfTrust.identicon.face;

import static java.awt.BorderLayout.CENTER;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import static plugins.WebOfTrust.identicon.face.Face.createFace;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Simple, non-automated {@link Identiface} tester that shows random {@link
 * Face}s when resizing or clicking the frame.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class IdentifaceTest {

	public static void main(String... arguments) {
		final JPanel facePanel = new JPanel() {
			private final Random random = new Random();

			@Override
			protected void paintComponent(Graphics graphics) {
				byte[] randomData = new byte[128];
				random.nextBytes(randomData);
				Identiface identiface = new Identiface(createFace(randomData));
				((Graphics2D) graphics).drawRenderedImage(identiface.render(getWidth(), getHeight()), null);
			}
		};
		facePanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				facePanel.repaint();
			}
		});
		JFrame frame = new JFrame();
		frame.getRootPane().setLayout(new BorderLayout());
		frame.getRootPane().add(facePanel, CENTER);
		frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		frame.setSize(512, 512);
		frame.setVisible(true);
	}

}
