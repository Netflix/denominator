package denominator;

import java.util.Iterator;

public interface ZoneApi {
    /**
     * a listing of all zone names, including trailing dot. ex.
     * {@code netflix.com.}. Implementations are lazy when possible.
     */
    Iterator<String> list();
}
