/**
 * 
 */
package denominator.verisignmdns;


import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.txt;
import static denominator.model.ResourceRecordSets.mx;
import static denominator.model.ResourceRecordSets.naptr;
import static denominator.model.ResourceRecordSets.ptr;
import static denominator.model.ResourceRecordSets.srv;
import static denominator.model.ResourceRecordSets.ns;
import static denominator.model.ResourceRecordSets.ds;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import denominator.model.ResourceRecordSet;
import denominator.verisignmdns.VrsnMdns.Record;

/**
 * @author smahurpawar
 *
 */
public class VrsnContentConversionHelper {
	
	/**
	 * Convernts Resource Record returned by MDNS Api to Denominator ResourceRecordSet
	 * @param aMDNSRecord
	 * @return
	 */
	public static ResourceRecordSet<?> convertMDNSRecordToDenominator(Record aMDNSRecord) {
  	  ResourceRecordSet<?> result = null;
 
		if (aMDNSRecord != null && aMDNSRecord.type != null) {
			if (VrsnConstants.RR_TYPE_A.equals(aMDNSRecord.type)) {
				result = a(aMDNSRecord.name, aMDNSRecord.ttl, aMDNSRecord.rdata);

			}
			if (VrsnConstants.RR_TYPE_AAAA.equals(aMDNSRecord.type)) {
				result = aaaa(aMDNSRecord.name, aMDNSRecord.ttl, aMDNSRecord.rdata);
			}
			
			if (VrsnConstants.RR_TYPE_NS.equals(aMDNSRecord.type)) {
				result = ns(aMDNSRecord.name, aMDNSRecord.ttl, aMDNSRecord.rdata);
			}
			
			if (VrsnConstants.RR_TYPE_CNAME.equals(aMDNSRecord.type)) {
				result = cname(aMDNSRecord.name, aMDNSRecord.ttl, aMDNSRecord.rdata);
			}
			if (VrsnConstants.RR_TYPE_TXT.equals(aMDNSRecord.type)) {
				result = txt(aMDNSRecord.name, aMDNSRecord.ttl, aMDNSRecord.rdata);
			}
			if (VrsnConstants.RR_TYPE_MX.equals(aMDNSRecord.type)) {
				result = mx(aMDNSRecord.name, aMDNSRecord.ttl,aMDNSRecord.rdata);
				
			}
			if (VrsnConstants.RR_TYPE_PTR.equals(aMDNSRecord.type)) {
				result = ptr(aMDNSRecord.name, aMDNSRecord.ttl, aMDNSRecord.rdata);

			}
			if (VrsnConstants.RR_TYPE_NAPTR.equals(aMDNSRecord.type)) {
				
				List<String> tempRdata = aMDNSRecord.rdata;
				result = naptr(aMDNSRecord.name, aMDNSRecord.ttl,	tempRdata);
			}
			if (VrsnConstants.RR_TYPE_SRV.equals(aMDNSRecord.type)) {
				result = srv(aMDNSRecord.name, aMDNSRecord.ttl,
						aMDNSRecord.rdata);
			}
			
			if (VrsnConstants.RR_TYPE_DS.equals(aMDNSRecord.type)) {
				result = ds(aMDNSRecord.name, aMDNSRecord.ttl,
						aMDNSRecord.rdata);
			}
		}
  	  
  	  return result;
	}

    public static SortedSet<ResourceRecordSet<?>> getSortedSetForDenominator(List<Record> aMDNSRecordList) {
  	  Comparator<ResourceRecordSet<?>> toStringComparator = new Comparator<ResourceRecordSet<?>>() {
            @Override
            public int compare(ResourceRecordSet<?> arg0, ResourceRecordSet<?> arg1) {
                return arg0.toString().compareTo(arg1.toString());
            }
        };
        SortedSet<ResourceRecordSet<?>> result = new ConcurrentSkipListSet<ResourceRecordSet<?>>(toStringComparator);
        if (aMDNSRecordList != null) {
      	  for (Record rr : aMDNSRecordList) {
      		  result.add(convertMDNSRecordToDenominator(rr));
      	  }
        }

        return result;
    }
    
}
