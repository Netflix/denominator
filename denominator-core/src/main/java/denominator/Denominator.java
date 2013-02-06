package denominator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.uniqueIndex;

import java.util.Map;
import java.util.ServiceLoader;

import com.google.common.base.Function;

import dagger.ObjectGraph;
import denominator.stub.StubBackend;

public final class Denominator {

    /**
     * returns the currently configured backends
     */
    public static Iterable<Backend> listBackends() {
        return ServiceLoader.load(Backend.class);
    }

    /**
     * creates a connection to the backend, such as {@link StubBackend}
     * 
     * @see #listBackends
     */
    public static Connection connectToBackend(Backend in) {
        return ObjectGraph.create(in).get(Connection.class);
    }

    /**
     * creates a connection to the backend, based on key look up. Ex.
     * {@code stub}
     * 
     * @see Backend#getName()
     * @throws IllegalArgumentException
     *             if the backendName is not configured
     */
    public static Connection connectToBackend(String backendName) throws IllegalArgumentException {
        checkNotNull(backendName, "backendName");
        Map<String, Backend> allBackendsByName = uniqueIndex(listBackends(), new Function<Backend, String>() {
            public String apply(Backend input) {
                return input.getName();
            }
        });
        checkArgument(allBackendsByName.containsKey(backendName), "backend %s not in set of configured backends: %s",
                backendName, allBackendsByName.keySet());
        return ObjectGraph.create(allBackendsByName.get(backendName)).get(Connection.class);
    }
}
