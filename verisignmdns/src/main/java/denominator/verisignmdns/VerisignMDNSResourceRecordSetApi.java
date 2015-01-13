package denominator.verisignmdns;

import static denominator.common.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Provider;

import denominator.Credentials;
import denominator.ResourceRecordSetApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.verisignmdns.VrsnMdns.Record;

public final class VerisignMDNSResourceRecordSetApi implements
		ResourceRecordSetApi {
	private final String domainName;
	private final VrsnMdns api;
	private final Provider<Credentials> credentialsProvider;

	VerisignMDNSResourceRecordSetApi(Provider<Credentials> credentialsProvider,
			String domainName, VrsnMdns api) {
		this.domainName = domainName;
		this.api = api;
		this.credentialsProvider = credentialsProvider;
	}


	@Override
	public Iterator<ResourceRecordSet<?>> iterator() {

		List<Record> recordList = api.getResourceRecordsList(domainName);
		return VrsnContentConversionHelper.getSortedSetForDenominator(
				recordList).iterator();
	}

	public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name,
			String type) {
		checkNotNull(type, "type was null");
		checkNotNull(name, "name was null");

		List<Record> recordList = api.getResourceRecordsListForTypeAndName(
				domainName, type, name);
		Iterator<ResourceRecordSet<?>> result = VrsnContentConversionHelper
				.getSortedSetForDenominator(recordList).iterator();
		return result;
	}

	@Override
	public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
		try {
			throw new VrsnMdnsException("Method Not Implemented", -1);

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	@Override
	public ResourceRecordSet<?> getByNameAndType(String name, String type) {
		checkNotNull(type, "type was null");
		checkNotNull(name, "name was null");

		ResourceRecordSet<?> result = null;
		List<Record> recordList = api.getResourceRecordsListForTypeAndName(
				domainName, type, name);
		SortedSet<ResourceRecordSet<?>> tempSet = VrsnContentConversionHelper
				.getSortedSetForDenominator(recordList);
		if (tempSet != null && tempSet.size() > 0) {
			result = tempSet.first();
		}
		return result;
	}

	@Override
	public void put(ResourceRecordSet<?> rrset) {

		checkNotNull(rrset, "Resource Record was null");
		checkNotNull(rrset.name(), "Resource Record Name was null");
		checkNotNull(rrset.type(), "Resource Record Type was null");

		// At this point disable delete
		// as we might delete multiple records for name and type.
		// Need to decide if that is correct
		// ResourceRecordSet<?> rrsMatch = getByNameAndType(rrset.name(),
		// rrset.type());
		// if (rrsMatch != null) {
		// deleteByNameAndType(rrset.name(), rrset.type());
		// }
		int ttlInt = 86000;
		Integer ttlRRSet = rrset.ttl();
		if (ttlRRSet != null) {
			ttlInt = ttlRRSet.intValue();
		}
		String rData = getRDataStringFromRRSet(rrset);
		api.createResourceRecord(domainName, rrset.type(), rrset.name(), ""
				+ ttlInt, rData);

	}

	@Override
	public void deleteByNameAndType(String name, String type) {
		List<Record> recordList = api.getResourceRecordsListForTypeAndName(
				domainName, type, name);
		if (recordList != null && !recordList.isEmpty()) {
			// delete all records in recordList
			for (Record record : recordList) {
				api.deleteRecourceRecord(domainName, record.id);
			}

		} else {
			throw new RuntimeException(
					"deleteByNameAndType() failled to delete record for domain :"
							+ domainName + " type :" + type
							+ " No Record Found");
		}
	}

	public static final class Factory implements
			denominator.ResourceRecordSetApi.Factory {
		private Map<Zone, SortedSet<ResourceRecordSet<?>>> records;
		private String domainName;
		private VrsnMdns api;
		private Provider<Credentials> credentialsProvider;

		// unbound wildcards are not currently injectable in dagger
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Inject
		Factory(Provider<Credentials> credentialsProvider,
				denominator.Provider provider, VrsnMdns api) {
			this.records = Map.class.cast(records);
			String url = provider.url();
			this.api = api;
			this.credentialsProvider = credentialsProvider;
		}

		@Override
		public ResourceRecordSetApi create(String idOrName) {
			Zone zone = Zone.create(idOrName);
			return new VerisignMDNSResourceRecordSetApi(credentialsProvider,
					idOrName, api);
		}
	}

	private String getRDataStringFromRRSet(ResourceRecordSet rRSet) {
		StringBuilder sb = new StringBuilder();
		if (rRSet != null && rRSet.records() != null) {
			if (rRSet.type().equals("NAPTR")) {
				sb.append(VrsnMdnsRequestHelper.getNAPTRData(rRSet));
			} else {
				for (Object obj : rRSet.records()) {
					if (sb.length() > 0) {
						sb.append(",");
					}
					if (obj instanceof Map) {
						sb.append(Util.flatten((Map<String, Object>) obj));
					} else {
						sb.append(obj.toString());
					}
				}
			}
		}
		return sb.toString();
	}
}
