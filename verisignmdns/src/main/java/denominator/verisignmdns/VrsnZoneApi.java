/**
 * 
 */
package denominator.verisignmdns;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import denominator.Credentials;
import denominator.model.Zone;

/**
 * @author smahurpawar
 *
 */
public final class VrsnZoneApi implements denominator.ZoneApi {
	 private final VrsnMdns api;
	 private final String username;
	 private final String password;


    
	 @Inject VrsnZoneApi(VrsnMdns anApi, Provider<Credentials> aCredentials) {
    	Map <String, String> mapCreds = VrsnUtils.getMapOfCredentials(aCredentials);
    	username = mapCreds.get(VrsnConstants.CREDENITAL_USERNAME_KEY);
    	password = mapCreds.get(VrsnConstants.CREDENTIAL_PASSWORD_KEY);
    	api = anApi;
    }

	/* (non-Javadoc)
	 * @see denominator.ZoneApi#iterator()
	 */
	@Override
	public Iterator<Zone> iterator() {
		return api.getZonesForUser(username, password).iterator();
	}
	
}
