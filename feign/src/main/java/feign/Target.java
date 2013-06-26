package feign;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * <h4>relationship to JAXRS 2.0</h4>
 * <p/>
 * Similar to {@code javax.ws.rs.client.WebTarget}, as it produces requests.
 * However, {@link RequestTemplate} is a closer match to {@code WebTarget}.
 * 
 * @param <I>
 *            type of the interface this target applies to.
 */
public interface Target<T> extends Function<RequestTemplate, Request> {
    /**
     * The type of the interface this target applies to. ex. {@code Route53}.
     */
    Class<T> type();

    /**
     * configuration key associated with this target. For example,
     * {@code route53}.
     */
    String name();

    /**
     * The base HTTP URL of the target. For example, {@code https://api/v2}.
     */
    String url();

    /**
     * Targets a template to this target, adding the {@link #url() base url} and
     * any authentication headers.
     * <p>
     * 
     * For example:
     * 
     * <pre>
     * public Request apply(RequestTemplate input) {
     *     input.insert(0, url());
     *     input.replaceHeader(&quot;X-Auth&quot;, currentToken);
     *     return input.asRequest();
     * }
     * </pre>
     * 
     * <h4>relationship to JAXRS 2.0</h4>
     * <p/>
     * This call is similar to {@code javax.ws.rs.client.WebTarget.request()},
     * except that we expect transient, but necessary decoration to be applied
     * on invocation.
     */
    @Override
    public Request apply(RequestTemplate input);

    public static class HardCodedTarget<T> implements Target<T> {
        private final Class<T> type;
        private final String name;
        private final String url;

        public HardCodedTarget(Class<T> type, String url) {
            this(type, url, url);
        }

        public HardCodedTarget(Class<T> type, String name, String url) {
            this.type = checkNotNull(type, "type");
            this.name = checkNotNull(Strings.emptyToNull(name), "name");
            this.url = checkNotNull(Strings.emptyToNull(url), "url");
        }

        @Override
        public Class<T> type() {
            return type;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String url() {
            return url;
        }

        /**
         * no authentication or other special activity. just insert the url.
         */
        @Override
        public Request apply(RequestTemplate input) {
            if (input.url().indexOf("http") != 0)
                input.insert(0, url());
            return input.request();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(type, name, url);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (HardCodedTarget.class != obj.getClass())
                return false;
            HardCodedTarget<?> that = HardCodedTarget.class.cast(obj);
            return equal(this.type, that.type) && equal(this.name, that.name) && equal(this.url, that.url);
        }
    }
}
