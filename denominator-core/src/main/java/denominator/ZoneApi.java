package denominator;

import com.google.common.collect.FluentIterable;

public interface ZoneApi {
    /**
     * a listing of all zone names, including trailing dot. ex.
     * {@code netflix.com.}
     */
    FluentIterable<String> list();
}
