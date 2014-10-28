package org.onlab.onos.core;

import java.util.Objects;

import static java.lang.Integer.parseInt;

/**
 * Representation of the product version.
 */
public final class Version {

    public static final String FORMAT = "%d.%d.%d.%s";

    private final int major;
    private final int minor;
    private final int patch;
    private final String build;

    private final String format;

    // Creates a new version descriptor
    private Version(int major, int minor, int patch, String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.build = build;
        this.format = String.format(FORMAT, major, minor, patch, build);
    }


    /**
     * Creates a new version from the specified constituent numbers.
     *
     * @param major major version number
     * @param minor minod version number
     * @param patch version patch number
     * @param build build string
     * @return version descriptor
     */
    public static Version version(int major, int minor, int patch, String build) {
        return new Version(major, minor, patch, build);
    }

    /**
     * Creates a new version by parsing the specified string.
     *
     * @param string version string
     * @return version descriptor
     */
    public static Version version(String string) {
        String[] fields = string.split("[.-]");
        return new Version(parseInt(fields[0]), parseInt(fields[1]),
                           parseInt(fields[2]), fields[3]);
    }

    /**
     * Returns the major version number.
     *
     * @return major version number
     */
    public int major() {
        return major;
    }

    /**
     * Returns the minor version number.
     *
     * @return minor version number
     */
    public int minor() {
        return minor;
    }

    /**
     * Returns the version patch number.
     *
     * @return patch number
     */
    public int patch() {
        return patch;
    }

    /**
     * Returns the version build string.
     *
     * @return build string
     */
    public String build() {
        return build;
    }

    @Override
    public String toString() {
        return format;
    }

    @Override
    public int hashCode() {
        return Objects.hash(format);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Version) {
            final Version other = (Version) obj;
            return Objects.equals(this.format, other.format);
        }
        return false;
    }
}
