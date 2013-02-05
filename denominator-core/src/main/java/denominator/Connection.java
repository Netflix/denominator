package denominator;

import java.io.Closeable;
import java.io.IOException;

import javax.inject.Inject;

public class Connection implements Closeable {
    @Inject
    Edge edge;
    @Inject
    Closeable closer;

    public Edge open() {
        return edge;
    }

    /**
     * closes resources associated with the connections, such as thread pools or
     * open files.
     */
    @Override
    public void close() throws IOException {
        closer.close();
    }
}