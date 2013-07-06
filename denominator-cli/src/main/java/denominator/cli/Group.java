package denominator.cli;

public interface Group {
    String name();

    boolean needsDNSApi();

    Object module();
}
