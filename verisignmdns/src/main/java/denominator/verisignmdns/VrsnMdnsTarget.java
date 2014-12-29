/**
 * 
 */
package denominator.verisignmdns;

import javax.inject.Inject;

import denominator.Credentials;
import denominator.Provider;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

/**
 * @author smahurpawar
 *
 */
public class VrsnMdnsTarget implements Target<VrsnMdns> {

	private final Provider provider;
    private final javax.inject.Provider<Credentials> credentials;

    @Inject VrsnMdnsTarget(Provider provider, javax.inject.Provider<Credentials> credentials) {
    	this.provider = provider;
        this.credentials = credentials;
    }
	/* (non-Javadoc)
	 * @see feign.Target#type()
	 */
	@Override
	public Class<VrsnMdns> type() {
		return VrsnMdns.class;
	}

	/* (non-Javadoc)
	 * @see feign.Target#name()
	 */
	@Override
	public String name() {
		return provider.name();
	}

	/* (non-Javadoc)
	 * @see feign.Target#url()
	 */
	@Override
	public String url() {
		return provider.url();
	}

	/* (non-Javadoc)
	 * @see feign.Target#apply(feign.RequestTemplate)
	 */
	@Override
	public Request apply(RequestTemplate anInput) {
		anInput.insert(0, url());
		anInput.header("Content-Type", "application/soap+xml");
		return anInput.request();
	}

}
