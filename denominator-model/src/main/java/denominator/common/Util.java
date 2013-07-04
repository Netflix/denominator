package denominator.common;

import static denominator.common.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Utilities, typically adapted from guava, so as to avoid dependency conflicts.
 */
public class Util {
    private Util() { // no instances
    }

    /** returns the {@code reader} as a string without closing it. */
    public static String slurp(Reader reader) throws IOException {
        StringBuilder to = new StringBuilder();
        CharBuffer buf = CharBuffer.allocate(BUF_SIZE);
        while (reader.read(buf) != -1) {
            buf.flip();
            to.append(buf);
            buf.clear();
        }
        return to.toString();
    }

    private static final int BUF_SIZE = 0x800; // 2K chars (4K bytes)

    public static String join(char delim, Object... parts) {
        if (parts == null || parts.length == 0)
            return "";
        StringBuilder to = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            to.append(parts[i]);
            if (i + 1 < parts.length) {
                to.append(delim);
            }
        }
        return to.toString();
    }

    /** empty fields will result in null elements in the result. */
    public static List<String> split(char delim, String toSplit) {
        checkNotNull(toSplit, "toSplit");
        LinkedList<String> out = new LinkedList<String>();
        StringBuilder currentString = new StringBuilder();
        for (char c : toSplit.toCharArray()) {
            if (c == delim) {
                out.add(emptyToNull(currentString.toString()));
                currentString.setLength(0);
            } else {
                currentString.append(c);
            }
        }
        out.add(emptyToNull(currentString.toString()));
        return out;
    }

    private static String emptyToNull(String field) {
        return "".equals(field) ? null : field;
    }

    public static <T> T nextOrNull(Iterator<T> it) {
        return it.hasNext() ? it.next() : null;
    }

    public static <T> PeekingIterator<T> peekingIterator(final Iterator<T> iterator) {
        checkNotNull(iterator, "iterator");
        return new PeekingIterator<T>() {
            protected T computeNext() {
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                return endOfData();
            }
        };
    }

    public static <T> Iterator<T> concat(final Iterator<T> first, final Iterator<T> second) {
        checkNotNull(first, "first");
        checkNotNull(second, "second");
        return new PeekingIterator<T>() {
            Iterator<? extends T> current = first;

            protected T computeNext() {
                if (!current.hasNext() && current != second) {
                    current = second;
                }
                while (current.hasNext()) {
                    T element = current.next();
                    if (element != null) {
                        return element;
                    }
                }
                return endOfData();
            }
        };
    }

    public static <T> Iterator<T> concat(final Iterable<? extends Iterable<? extends T>> i) {
        final Iterator<? extends Iterable<? extends T>> inputs = checkNotNull(i, "inputs").iterator();
        return new PeekingIterator<T>() {
            Iterator<? extends T> current = Collections.<T> emptyList().iterator();

            protected T computeNext() {
                while (!current.hasNext() && inputs.hasNext()) {
                    current = inputs.next().iterator();
                }
                while (current.hasNext()) {
                    T element = current.next();
                    if (element != null) {
                        return element;
                    }
                }
                return endOfData();
            }
        };
    }

    public static <T> Iterator<T> filter(final Iterator<T> unfiltered, final Filter<? super T> filter) {
        checkNotNull(unfiltered, "unfiltered");
        checkNotNull(filter, "filter");
        return new PeekingIterator<T>() {
            protected T computeNext() {
                while (unfiltered.hasNext()) {
                    T element = unfiltered.next();
                    if (filter.apply(element)) {
                        return element;
                    }
                }
                return endOfData();
            }
        };
    }

    public static <T> Filter<T> and(final Filter<T> first, final Filter<? super T> second) {
        checkNotNull(first, "first");
        checkNotNull(second, "second");
        return new Filter<T>() {
            public boolean apply(T in) {
                if (!first.apply(in)) {
                    return false;
                }
                return second.apply(in);
            }
        };
    }
}
