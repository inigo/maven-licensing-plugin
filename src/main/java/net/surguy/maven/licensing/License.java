package net.surguy.maven.licensing;

/**
 * Represents a software license.
 *
 * @author Inigo Surguy
 * @created 15/01/2011 14:17
 */
public class License {

    private final String name;
    private final String url;

    public License(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() { return name; }
    public String getUrl() { return url; }

    @Override
    public String toString() {
        return "License{name='" + name + '\'' + ", url='" + url + '\'' + '}';
    }
}
