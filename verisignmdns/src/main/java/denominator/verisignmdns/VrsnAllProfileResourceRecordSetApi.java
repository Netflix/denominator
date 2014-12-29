package denominator.verisignmdns;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.model.ResourceRecordSets.notNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Provider;



import denominator.AllProfileResourceRecordSetApi;
import denominator.Credentials;
import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.verisignmdns.VrsnMdns.Record;

public class VrsnAllProfileResourceRecordSetApi implements denominator.AllProfileResourceRecordSetApi {
    private final String username;
    private final String password;
    private final String domainName;
    private final VrsnMdns api;
	

    VrsnAllProfileResourceRecordSetApi(String aUsername, String aPassword,  String aDomainName,  VrsnMdns anApi) {
        username = aUsername;
        password = aPassword;
        this.domainName = aDomainName;
        this.api = anApi;
    }

    /**
     * sorted to help tests from breaking
     */
    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {

        List<Record> recordList = api.getResourceRecordsList(username, password, domainName);
       
    	return VrsnContentConversionHelper.getSortedSetForDenominator(recordList).iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    	checkNotNull(name, "name was null");
    	throw new VrsnMdnsException("Method Not Implemented" , -1);
    }

    protected void put(Filter<ResourceRecordSet<?>> valid, ResourceRecordSet<?> rrset) {
 
    	checkNotNull(rrset, "rrset was null");
        checkArgument(rrset.qualifier() != null, "no qualifier on: %s", rrset);
        checkArgument(valid.apply(rrset), "%s failed on: %s", valid, rrset);
        
        
        //@TODO IMPLEMENT -- in future development phase  /////////
        throw new VrsnMdnsException("Method Not Implemented" , -1);
        
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
    	//@ Keeping MOCK code for now...
    	//@TODO IMPLEMENT -- in future development phase  /////////
    	put(notNull(), rrset);
    	throw new VrsnMdnsException("Method Not Implemented" , -1);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
 

    	checkNotNull(type, "type was null");
    	checkNotNull(name, "name was null");

    	List<Record> recordList = api.getResourceRecordsListForTypeAndName(username, password, domainName, type, name);
    	Iterator<ResourceRecordSet<?>> result =  VrsnContentConversionHelper.getSortedSetForDenominator(recordList).iterator();
    	return result;
    }

    /**
     * NOTE- for MDNS get only required ResourceRecordId ie. qualifier.
     *  Parameters name and type are ignored.
     */
    @Override
    public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type, String qualifier) {
    	ResourceRecordSet<?> result = null;
    	List<Record> recordList =  api.getResourceRecordByQualifier(username, password, qualifier);
    	SortedSet<ResourceRecordSet<?>> rrSet =  VrsnContentConversionHelper.getSortedSetForDenominator(recordList);
    	if (!rrSet.isEmpty()) {
    		result = rrSet.first();
    	}
    	return result;
    }

    @Override
    public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
    	//@ Keeping MOCK code for now...
    	
   	
    	//@TODO IMPLEMENT -- in future development phase /////////
    	throw new VrsnMdnsException("Method Not Implemented" , -1);
    	
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
    	throw new VrsnMdnsException("Method Not Implemented" , -1);
    }

    static class Factory implements denominator.AllProfileResourceRecordSetApi.Factory {
        private  Map<Zone, SortedSet<ResourceRecordSet<?>>> records;
        private  String domainName;
        private  VrsnMdns api;
        private static String username; 
        private static String password; 
          
        // unbound wildcards are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Provider<Credentials> credentials, denominator.Provider provider,  VrsnMdns api) {
            
      	
        	this.records = Map.class.cast(records);
        	Map <String, String> mapCreds = VrsnUtils.getMapOfCredentials(credentials);
        	username = mapCreds.get(VrsnConstants.CREDENITAL_USERNAME_KEY);
        	password = mapCreds.get(VrsnConstants.CREDENTIAL_PASSWORD_KEY);
        	String url = provider.url();
        	this.api = api;
        }

        @Override
        public AllProfileResourceRecordSetApi create(String idOrName) {
            Zone zone = Zone.create(idOrName);
            
            return new VrsnAllProfileResourceRecordSetApi(username, password, idOrName,  api);
        }
    }
}
