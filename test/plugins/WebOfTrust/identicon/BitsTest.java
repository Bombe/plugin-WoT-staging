package plugins.WebOfTrust.identicon;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static plugins.WebOfTrust.identicon.Bits.decodeBits;

import org.junit.Test;

/**
 * Unit test for {@link Bits}.
 *
 * @author <a href="mailto:d.roden@xplosion.de">David Roden</a>
 */
public class BitsTest {

	@Test
	public void decodingBitsFromByteArray() {
		byte[] data = new byte[] { 0x55, (byte) 0xaa, (byte) 0x80, 0x14, 0x2a };
		assertThat(decodeBits(data, 14, 14), is(0x2804L));
	}

}
