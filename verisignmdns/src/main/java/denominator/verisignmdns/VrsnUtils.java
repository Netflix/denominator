/**
 * 
 */
package denominator.verisignmdns;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import denominator.Credentials;

/**
 * @author smahurpawar
 *
 */
public class VrsnUtils {

	 /**
	  * Converts Provider<Credentials> object into Map<String,String> ie credential name, credential value.
	  * @param aCredentials Provider<Credentials>
	  * @return Map<String, String>
	  */
	 public static Map<String, String> getMapOfCredentials(Provider<Credentials> aCredentials) {
		 Map<String, String> result = new HashMap<String, String>();
		 if (aCredentials != null) {
			 Credentials currentCreds = aCredentials.get();
			 if (currentCreds instanceof List && ((List) currentCreds).size() >= 2) {
				 List tempCredList = List.class.cast(currentCreds);
				 result.put(VrsnConstants.CREDENITAL_USERNAME_KEY, (String)tempCredList.get(0));
				 result.put(VrsnConstants.CREDENTIAL_PASSWORD_KEY, (String)tempCredList.get(1));
			 } else if (currentCreds instanceof Map) {
				 // it must be map
				result = Map.class.cast(currentCreds);
			 }
		 }
		 return result;
	 }
	
    
}
