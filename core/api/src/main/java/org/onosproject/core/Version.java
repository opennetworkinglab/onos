/*
 * Copyright 2014-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.core;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Integer.parseInt;

/**
 * Representation of the product version.
 */
public final class Version implements Comparable<Version> {

    public static final String FORMAT_MINIMAL = "%d.%d";
    public static final String FORMAT_SHORT = "%d.%d.%s";
    public static final String FORMAT_LONG = "%d.%d.%s.%s";

    private static final String NEGATIVE = "Version segment cannot be negative";
    public static final String TOO_SHORT = "Version must have at least major and minor numbers";

    private final int major;
    private final int minor;
    private final String patch;
    private final String build;

    private final String format;

    // Creates a new version descriptor
    private Version(int major, int minor, String patch, String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.build = build;
        this.format =
                isNullOrEmpty(patch) ?
                        String.format(FORMAT_MINIMAL, major, minor) :
                        (isNullOrEmpty(build) ?
                                String.format(FORMAT_SHORT, major, minor, patch) :
                                String.format(FORMAT_LONG, major, minor, patch, build));
    }


    /**
     * Creates a new version from the specified constituent numbers.
     *
     * @param major major version number
     * @param minor minor version number
     * @param patch version patch segment
     * @param build optional build string
     * @return version descriptor
     */
    public static Version version(int major, int minor, String patch, String build) {
        checkArgument(major >= 0, NEGATIVE);
        checkArgument(minor >= 0, NEGATIVE);
        return new Version(major, minor, patch, build);
    }

    /**
     * Creates a new version by parsing the specified string.
     *
     * @param string version string
     * @return version descriptor
     */
    public static Version version(String string) {
        checkArgument(string != null, TOO_SHORT);
        String[] fields = string.split("[.-]", 4);
        checkArgument(fields.length >= 2, TOO_SHORT);
        return new Version(parseInt(fields[0]), parseInt(fields[1]),
                           fields.length >= 3 ? fields[2] : null,
                           fields.length >= 4 ? fields[3] : null);
    }

    /**
     * Returns an version from integer.
     * <p>
     * The version integer must be in the following format (big endian):
     * <ul>
     *     <li>8-bit unsigned major version</li>
     *     <li>8-bit unsigned minor version</li>
     *     <li>16-bit unsigned patch version</li>
     * </ul>
     *
     * @param version the version integer
     * @return the version instance
     */
    public static Version fromInt(int version) {
        int major = (version >> 24) & 0xff;
        int minor = (version >> 16) & 0xff;
        int patch = (version >> 8) & 0xff;
        int build = version & 0xff;
        return new Version(major, minor, String.valueOf(patch), String.valueOf(build));
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
     * Returns the version patch segment.
     *
     * @return patch number
     */
    public String patch() {
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

    /**
     * Returns an integer representation of the version.
     * <p>
     * The version integer can be used to compare two versions to one another.
     * The integer representation of the version number is in the following format (big endian):
     * <ul>
     *     <li>8-bit unsigned major version</li>
     *     <li>8-bit unsigned minor version</li>
     *     <li>16-bit unsigned patch version</li>
     * </ul>
     * If the {@link #patch()} is not a number, it will default to {@code 0}.
     *
     * @return an integer representation of the version
     */
    public int toInt() {
        byte major = (byte) this.major;
        byte minor = (byte) this.minor;

        byte patch;
        if (this.patch != null) {
            try {
                patch = (byte) Integer.parseInt(this.patch.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                patch = 0;
            }
        } else {
            patch = 0;
        }

        byte build;
        if (this.build != null) {
            try {
                build = (byte) Integer.parseInt(this.build.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                build = 0;
            }
        } else {
            build = 0;
        }

        return major << 24 | (minor & 0xff) << 16 | (patch & 0xff) << 8 | (build & 0xff);
    }

    @Override
    public int compareTo(Version other) {
        return Integer.compare(toInt(), other.toInt());
    }

    @Override
    public String toString() {
        return format;
    }

    @Override
    public int hashCode() {
        return format.hashCode();
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
