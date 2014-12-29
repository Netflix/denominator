package denominator.verisignmdns;

import static denominator.CredentialsConfiguration.credentials; 
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

public class TestZoneRRDetails {

	public static void main(String[] args) {
		DNSApiManager manager = Denominator.create("vrsndns",
				credentials("userName", "password"));

		for (Zone zone : manager.api().zones()) {
			  for (ResourceRecordSet<?> rrs : manager.api().recordSetsInZone(zone.idOrName())) {
			    System.out.println(rrs.toString());
			  }
			}
	}

}
