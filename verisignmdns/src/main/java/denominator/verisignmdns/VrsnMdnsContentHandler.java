package denominator.verisignmdns;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import denominator.model.Zone;
import denominator.verisignmdns.VrsnMdns.Record;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

class VrsnMdnsContentHandler {

	static class ZoneListHandler extends DefaultHandler implements
			ContentHandlerWithResult<List<Zone>> {
		@Inject
		ZoneListHandler() {
		}

		private final List<Zone> zones = new ArrayList<Zone>();
		private boolean domainElementFound = false; // flag for getting the
													// value...

		@Override
		public List<Zone> result() {
			return zones;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attrs) {

			if (qName != null && qName.endsWith(":domainName")) {
				domainElementFound = true;
			}
		}

		@Override
		public void characters(char ch[], int start, int length)
				throws SAXException {
			if (domainElementFound) {
				zones.add(Zone.create(new String(ch, start, length)));
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if (qName != null && qName.endsWith(":domainName")) {
				domainElementFound = false;
			}
		}
	}

	static class RecordListHandler extends DefaultHandler implements
			ContentHandlerWithResult<List<Record>> {
		private final List<Record> rrs = new ArrayList<Record>();

		@Inject
		RecordListHandler() {
		}

		private Record rr = new Record();
		private boolean processingRR = false; // flag indicating currently
												// inside resource record
												// element..
		private String currentElementName = "";
		private String rDataString = "";

		@Override
		public List<Record> result() {
			return rrs;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attrs) {

			if (qName.endsWith(":resourceRecord")) {
				rr = new Record();
				processingRR = true;
				currentElementName = ":resourceRecord";
			}
			if (processingRR && qName.endsWith(":resourceRecordId")) {
				currentElementName = ":resourceRecordId";
			}
			if (processingRR && qName.endsWith(":owner")) {
				currentElementName = ":owner";
			}
			if (processingRR && qName.endsWith(":type")) {
				currentElementName = ":type";
			}
			if (processingRR && qName.endsWith(":ttl")) {
				currentElementName = ":ttl";
			}
			if (processingRR && qName.endsWith(":rData")) {
				currentElementName = ":rData";
			}
		}

		@Override
		public void endElement(String uri, String name, String qName) {

			if (qName.endsWith(":resourceRecord")) {
				rrs.add(rr);
				processingRR = false;
			}

			if (qName.endsWith(":rData")) {
				String[] tempArray = rDataString.split(",");
				rr.rdata = getArrayList(tempArray);
				rDataString = "";
			}
			currentElementName = "";
		}

		/**
		 * This method is to ensure all space characters are accounted for while
		 * processing rData
		 * 
		 */
		@Override
		public void ignorableWhitespace(char[] ch, int start, int length)
				throws SAXException {
			if (currentElementName.equals(":rData")) {
				String tempStr = new String(ch, start, length);
				rDataString += tempStr;
			}
		}

		@Override
		public void characters(char ch[], int start, int length)
				throws SAXException {
			if (processingRR && length > 0) {
				if (currentElementName.equals(":resourceRecordId")) {
					rr.id = new String(ch, start, length);
				}
				if (currentElementName.equals(":owner")) {
					rr.name = new String(ch, start, length);
				}
				if (currentElementName.equals(":type")) {
					rr.type = new String(ch, start, length);
				}

				if (currentElementName.equals(":ttl")) {
					try {
						String temp = new String(ch, start, length);
						rr.ttl = Integer.parseInt(temp);
					} catch (Exception ex) {
						// DO NOTHING..?
					}
				}
				if (currentElementName.equals(":rData")) {
					String tempStr = new String(ch, start, length);
					rDataString += tempStr;
				}

			}
		}
	}

	private static ArrayList<String> getArrayList(String[] input) {
		ArrayList<String> result = new ArrayList<String>();
		if (input != null) {
			for (int i = 0; i < input.length; i++) {
				result.add(input[i]);
			}
		}
		return result;
	}
}
