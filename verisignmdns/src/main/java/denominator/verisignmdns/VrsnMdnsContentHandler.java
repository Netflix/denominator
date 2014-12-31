/**
 * 
 */
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

/**
 * @author smahurpawar
 *
 */
public class VrsnMdnsContentHandler {
	
	//MSDN response elements for RR List, these are last elements of qname  (i.e. qname ends with ":xxxx")
	public static final String ELEMENT_DOMAIN_NAME = ":domainName";
	public static final String ELEMENT_RESOURCE_RECORD = ":resourceRecord";
	public static final String ELEMENT_RESOURCE_ID = ":resourceRecordId"; 
	public static final String ELEMENT_OWNER = ":owner";
	public static final String ELEMENT_TYPE = ":type";
	public static final String ELEMENT_TTL = ":ttl";
	public static final String ELEMENT_RDATA = ":rData";
	
	
    static class ZoneListHandler extends DefaultHandler implements ContentHandlerWithResult<List<Zone>> {
        @Inject
        ZoneListHandler() {
        }

        private final List<Zone> zones = new ArrayList<Zone>();

        private boolean domainElementFound = false;  // flag for getting the value...

        
        @Override
        public List<Zone> result() {
            return zones;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {

        	if (qName != null && qName.endsWith(ELEMENT_DOMAIN_NAME)) {
            	domainElementFound = true;
            }
        }
        
        @Override
        public void characters (char ch[], int start, int length)
                throws SAXException {
        	if (domainElementFound) {
        		zones.add(Zone.create(new String(ch, start, length )));
        	}
       }
        
        @Override
        public void endElement (String uri, String localName, String qName)
                throws SAXException {
        	if (qName != null && qName.endsWith(ELEMENT_DOMAIN_NAME)) {
            	domainElementFound = false;
            }
       }
    }
	
    
    static class RecordListHandler extends DefaultHandler implements ContentHandlerWithResult<List<Record>> {
        private final List<Record> rrs = new ArrayList<Record>();
       @Inject
        RecordListHandler() {
        }

        private Record rr = new Record();
        private boolean processingRR = false;  //flag indicating currently inside resource record element..
        private String currentElementName = VrsnConstants.STRING_EMPTY;
        private String rDataString = VrsnConstants.STRING_EMPTY;

        @Override
        public List<Record> result() {
            return rrs;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {

        	if (qName.endsWith(ELEMENT_RESOURCE_RECORD)) {
                rr = new Record();
                processingRR = true;
                currentElementName = ELEMENT_RESOURCE_RECORD;
            }
            if (processingRR && qName.endsWith(ELEMENT_RESOURCE_ID)) {
            	currentElementName = ELEMENT_RESOURCE_ID;
            }
            if (processingRR && qName.endsWith(ELEMENT_OWNER)) {
            	currentElementName = ELEMENT_OWNER;
            }
            if (processingRR && qName.endsWith(ELEMENT_TYPE)) {
            	currentElementName = ELEMENT_TYPE;
            }
            if (processingRR && qName.endsWith(ELEMENT_TTL)) {
            	currentElementName = ELEMENT_TTL;
            }
            if (processingRR && qName.endsWith(ELEMENT_RDATA)) {
            	currentElementName = ELEMENT_RDATA;
            }
        }

        @Override
        public void endElement(String uri, String name, String qName) {

        	if (qName.endsWith(ELEMENT_RESOURCE_RECORD)) {
                rrs.add(rr);
                processingRR = false;
            }
        	
        	if (qName.endsWith(ELEMENT_RDATA)) {
        		 String[] tempArray = rDataString.split(",");
        		 rr.rdata = getArrayList(tempArray);
    			 rDataString = VrsnConstants.STRING_EMPTY;
        	}
            currentElementName = VrsnConstants.STRING_EMPTY;
        }
       
       /**
        *  This method is to ensure all space characters are accounted for 
    	*    while processing rData
    	*     
        */
       @Override
       public void ignorableWhitespace(char[] ch, int start, int length)
                  throws SAXException {
        	if (currentElementName.equals(ELEMENT_RDATA)) {
        		String tempStr = new String(ch, start, length);
        		rDataString +=  tempStr;
        	}
        }
        
       
        @Override
        public void characters (char ch[], int start, int length)
                throws SAXException {
        	if (processingRR && length > 0) {
        		if (currentElementName.equals(ELEMENT_RESOURCE_ID)) {
        			rr.id = new String(ch, start, length);
        		}
        		if (currentElementName.equals(ELEMENT_OWNER)) {
        			rr.name = new String(ch, start, length);
        		}
        		if (currentElementName.equals(ELEMENT_TYPE)) {
        			rr.type = new String(ch, start, length);
        		}
        		
        		if (currentElementName.equals(ELEMENT_TTL)) {
        			try {
        				String temp = new String(ch, start, length);
        				rr.ttl = Integer.parseInt(temp);
        			} catch(Exception ex) {
        				//DO NOTHING..?
        			}
        		}
        		if (currentElementName.equals(ELEMENT_RDATA)) {
        			 String tempStr = new String(ch, start, length);
        			 rDataString +=  tempStr;
        		}
        		
        	}
       }
    }
    
    private static ArrayList<String> getArrayList(String[] anInput) {
    	ArrayList<String> result  = new ArrayList<String>();
    	if (anInput != null) {
    		for (int i = 0; i < anInput.length; i++) {
    			result.add(anInput[i]);
    		}
    	}
        return result;
    }
    
    
}
