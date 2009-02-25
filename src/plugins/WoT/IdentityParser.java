/**
 * This code is part of WoT, a plugin for Freenet. It is distributed 
 * under the GNU General Public License, version 2 (or at your option
 * any later version). See http://www.gnu.org/ for details of the GPL.
 */

package plugins.WoT;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import plugins.WoT.exceptions.DuplicateIdentityException;
import plugins.WoT.exceptions.InvalidParameterException;
import plugins.WoT.exceptions.UnknownIdentityException;

import com.db4o.ObjectContainer;

import freenet.client.HighLevelSimpleClient;
import freenet.keys.FreenetURI;
import freenet.support.Logger;

/**
 * Parses an identity.xml file and updates Identity's data. 
 * 
 * @author Julien Cornuwel (batosai@freenetproject.org)
 *
 */
public class IdentityParser {

	ObjectContainer db;
	HighLevelSimpleClient client;
	IdentityFetcher fetcher;
	SAXParser saxParser;
	Identity identity;
	
	/**
	 * Creates an IdentityParser and make it ready to parse the file.
	 * 
	 * @param db A reference to the database 
	 * @param client A reference to an {@link HighLevelSimpleClient}
	 * @param fetcher A reference to the {@link IdentityFetcher} object, in order to request new editions or newly discovered identities
	 * @throws ParserConfigurationException if the parser encounters a problem
	 * @throws SAXException if the parser encounters a problem
	 */
	public IdentityParser(ObjectContainer db, HighLevelSimpleClient client, IdentityFetcher fetcher) throws ParserConfigurationException, SAXException {

		this.db = db;
		this.client = client;
		this.fetcher = fetcher;

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		saxParser = factory.newSAXParser();
	}
	
	/**
	 * Parses an identity.xml file, passed from {@link IdentityFetcher} as an {@link InputStream}
	 * 
	 * @param is the InputStream to parse
	 * @param uri the {@link FreenetURI} we just fetched, used to find the corresponding Identity
	 * @throws InvalidParameterException should never happen
	 * @throws SAXException if the parser encounters a problem
	 * @throws IOException don't know if this can even happen
	 * @throws UnknownIdentityException if the Identity hasn't been created before fetching it (should never happen)
	 * @throws DuplicateIdentityException if there is more than one Identity is the database with this requestURI (should never happen)
	 */
	public void parse (InputStream is, FreenetURI uri) throws InvalidParameterException, SAXException, IOException, UnknownIdentityException, DuplicateIdentityException {

		identity = Identity.getByURI(db,uri);
		if(!(identity instanceof OwnIdentity)) identity.updated();
		identity.setEdition(uri.getSuggestedEdition());
		
		saxParser.parse(is, new IdentityHandler() );
		db.store(identity);
		db.commit();
		
		Logger.debug(this, "Successfuly parsed identity '" + identity.getNickName() + "'");
	}
	
	/* FIXME: Use the general XML-parsing class XMLTreeGenerator from freetalk.MessageXML */
	/* FIXME: Replace list of existing contexts, not just add new ones */
	/**
	 * Subclass that actually handles the parsing. Methods are called 
	 * by SAXParser for each XML element.
	 * 
	 * @author Julien Cornuwel batosai@freenetproject.org
	 *
	 */
	public class IdentityHandler extends DefaultHandler {
		
		/**
		 * Default constructor
		 */
		public IdentityHandler() {
		}
		
		/* FIXME: How is the root elemenet of the document handled?! The document format is <WoT> ... </Wot>.

		/**
		 * Called by SAXParser for each XML element.
		 */
		@Override
		public void startElement(String nameSpaceURI, String localName, String rawName, Attributes attrs) throws SAXException {
			
			String elt_name;
			if (rawName == null) elt_name = localName;
			else elt_name = rawName;

			try {
				if (elt_name.equals("Name")) {
					identity.setNickName(attrs.getValue("value"));
				}
				else if (elt_name.equals("PublishesTrustlist")) {
					identity.setPublishTrustList(attrs.getValue("value").equals("true"));
				}
				else if (elt_name.equals("Property")) {
					identity.setProp(attrs.getValue("key"), attrs.getValue("value"), db);
				}
				else if(elt_name.equals("Context")) {
					identity.addContext(attrs.getValue("value"), db);
				}
				else if (elt_name.equals("Trust")) {
	
					Identity trustee;
					byte value = Byte.parseByte(attrs.getValue("value"));
					String comment = attrs.getValue("Comment");
					
					try{
						trustee = Identity.getByURI(db, attrs.getValue("Identity"));
						identity.setTrust(db, trustee, value, comment);
					}
					catch (UnknownIdentityException e) {
						
						// Create trustee only if the truster has a positive score.
						// This is to avoid Identity spam when announcements will be here.
						if(identity.getBestScore(db) > 0) {
							trustee = new Identity(new FreenetURI(attrs.getValue("Identity")), null, false);
							db.store(trustee);
							identity.setTrust(db, trustee, value, comment);
							fetcher.fetch(trustee); 
						}
					}						
				} else
					Logger.error(this, "Unknown element in identity " + identity.getId() + ": " + elt_name);
				
			} catch (Exception e1) {
				Logger.error(this, "Parsing error", e1);
			}
		}
	}
}