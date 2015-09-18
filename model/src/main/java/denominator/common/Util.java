package denominator.common;

import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CERTData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NAPTRData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SOAData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.SSHFPData;
import denominator.model.rdata.TXTData;

import static denominator.common.Preconditions.checkNotNull;

/**
 * Utilities, inspired by or adapted from guava.
 */
public class Util {

  private static final int BUF_SIZE = 0x800; // 2K chars (4K bytes)

  private Util() { // no instances
  }

  public static boolean equal(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  /**
   * returns the {@code reader} as a string without closing it.
   */
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

  public static String join(char delim, Object... parts) {
    if (parts == null || parts.length == 0) {
      return "";
    }
    StringBuilder to = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      to.append(parts[i]);
      if (i + 1 < parts.length) {
        to.append(delim);
      }
    }
    return to.toString();
  }

  /**
   * empty fields will result in null elements in the result.
   */
  public static List<String> split(char delim, String toSplit) {
    checkNotNull(toSplit, "toSplit");
    if (toSplit.indexOf(delim) == -1) {
      return Arrays.asList(toSplit); // sortable in JRE 7 and 8
    }
    List<String> out = new LinkedList<String>();
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

  public static <T> Iterator<T> singletonIterator(final T nullableValue) {
    if (nullableValue == null) {
      return (Iterator<T>) EMPTY_ITERATOR;
    }
    return new Iterator<T>() {
      boolean done;

      @Override
      public boolean hasNext() {
        return !done;
      }

      @Override
      public T next() {
        if (done) {
          throw new NoSuchElementException();
        }
        done = true;
        return nullableValue;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("remove");
      }
    };
  }

  private static final Iterator<Object> EMPTY_ITERATOR = new Iterator<Object>() {
    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public Object next() {
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  };

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
      Iterator<? extends T> current = Collections.<T>emptyList().iterator();

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

  public static <T> Iterator<T> filter(final Iterator<T> unfiltered,
                                       final Filter<? super T> filter) {
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

  public static String flatten(Map<String, Object> input) {
    Collection<Object> orderedRdataValues = input.values();
    if (orderedRdataValues.size() == 1) {
      Object rdata = orderedRdataValues.iterator().next();
      return rdata instanceof InetAddress ? InetAddress.class.cast(rdata).getHostAddress()
                                          : rdata.toString();
    }
    return join(' ', orderedRdataValues.toArray());
  }

  public static Map<String, Object> toMap(String type, String rdata) {
    return "TXT".equals(type) ? TXTData.create(rdata) : toMap(type, Util.split(' ', rdata));
  }

  public static Map<String, Object> toMap(String type, List<String> parts) {
    if ("A".equals(type)) {
      return AData.create(parts.get(0));
    } else if ("AAAA".equals(type)) {
      return AAAAData.create(parts.get(0));
    } else if ("CNAME".equals(type)) {
      return CNAMEData.create(parts.get(0));
    } else if ("MX".equals(type)) {
      return MXData.create(Integer.valueOf(parts.get(0)), parts.get(1));
    } else if ("NS".equals(type)) {
      return NSData.create(parts.get(0));
    } else if ("PTR".equals(type)) {
      return PTRData.create(parts.get(0));
    } else if ("SOA".equals(type)) {
      return SOAData.builder().mname(parts.get(0)).rname(parts.get(1))
          .serial(Integer.valueOf(parts.get(2)))
          .refresh(Integer.valueOf(parts.get(3))).retry(Integer.valueOf(parts.get(4)))
          .expire(Integer.valueOf(parts.get(5))).minimum(Integer.valueOf(parts.get(6))).build();
    } else if ("SPF".equals(type)) {
      return SPFData.create(parts.get(0));
    } else if ("SRV".equals(type)) {
      return SRVData.builder().priority(Integer.valueOf(parts.get(0)))
          .weight(Integer.valueOf(parts.get(1)))
          .port(Integer.valueOf(parts.get(2))).target(parts.get(3)).build();
    } else if ("TXT".equals(type)) {
      return TXTData.create(parts.get(0));
    } else if ("CERT".equals(type)) {
      return CERTData.builder().format(Integer.valueOf(parts.get(0)))
          .tag(Integer.valueOf(parts.get(1)))
          .algorithm(Integer.valueOf(parts.get(2)))
          .certificate(parts.get(3))
          .build();
    } else if ("NAPTR".equals(type)) {
      return NAPTRData.builder().order(Integer.valueOf(parts.get(0)))
          .preference(Integer.valueOf(parts.get(1)))
          .flags(parts.get(2))
          .services(parts.get(3))
          .regexp(parts.get(4))
          .replacement(parts.get(5))
          .build();
    } else if ("SSHFP".equals(type)) {
      return SSHFPData.builder().algorithm(Integer.valueOf(parts.get(0)))
          .fptype(Integer.valueOf(parts.get(1)))
          .fingerprint(parts.get(2))
          .build();
    } else {
      throw new IllegalArgumentException("unsupported type: " + type);
    }
  }
}
