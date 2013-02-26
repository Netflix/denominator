
package denominator.model;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;


public class RData {
    private static final Splitter SPLITTER = Splitter.on(' ').omitEmptyStrings().trimResults();
    private final String value;
    
    protected RData(String value) {
        this.value = value;
    }

    public ImmutableList<String> getValues() {
        return ImmutableList.<String>builder().addAll(splitter().split(value)).build();
    }
    
    
    public static abstract class Builder<R extends RData> {
        public abstract R build();
    }
    
    public String toString() {
        return value;
    }
    
    protected Splitter splitter() {
        return SPLITTER;
    }
}
