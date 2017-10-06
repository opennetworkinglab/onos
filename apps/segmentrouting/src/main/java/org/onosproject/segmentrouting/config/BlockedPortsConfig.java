/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.segmentrouting.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Network config to describe ports that should be blocked until authenticated.
 */
public class BlockedPortsConfig extends Config<ApplicationId> {

    /**
     * Returns the top level keys to the config,
     * which should be device ID strings.
     *
     * @return this list of top level keys
     */
    public List<String> deviceIds() {
        List<String> devIds = new ArrayList<>();
        if (object != null) {
            Iterator<String> it = object.fieldNames();
            if (it != null) {
                it.forEachRemaining(devIds::add);
            }
        }
        return devIds;
    }

    /**
     * Returns the port range strings associated with the given device id key.
     *
     * @param deviceId the device id key
     * @return the associated port range strings
     */
    public List<String> portRanges(String deviceId) {
        List<String> portRanges = new ArrayList<>();
        if (object != null) {
            JsonNode jnode = object.get(deviceId);
            if (ArrayNode.class.isInstance(jnode)) {
                ArrayNode array = (ArrayNode) jnode;
                array.forEach(pr -> portRanges.add(pr.asText()));
            }
        }
        return portRanges;
    }

    /**
     * Returns an iterator over the port numbers defined by the port ranges
     * defined in the configuration, for the given device.
     *
     * @param deviceId the specific device
     * @return an iterator over the configured ports
     */
    public Iterator<Long> portIterator(String deviceId) {
        List<String> ranges = portRanges(deviceId);
        return new PortIterator(ranges);
    }

    /**
     * Private implementation of an iterator that aggregates several range
     * iterators into a single iterator.
     */
    class PortIterator implements Iterator<Long> {
        private final List<Range> ranges;
        private final int nRanges;
        private int currentRange = 0;
        private Iterator<Long> iterator;

        PortIterator(List<String> rangeSpecs) {
            nRanges = rangeSpecs.size();
            ranges = new ArrayList<>(nRanges);
            if (nRanges > 0) {
                for (String rs : rangeSpecs) {
                    ranges.add(new Range(rs));
                }
                iterator = ranges.get(0).iterator();
            }
        }

        @Override
        public boolean hasNext() {
            return nRanges > 0 &&
                    (currentRange < nRanges - 1 ||
                            (currentRange < nRanges && iterator.hasNext()));
        }

        @Override
        public Long next() {
            if (nRanges == 0) {
                throw new NoSuchElementException();
            }

            Long value;
            if (iterator.hasNext()) {
                value = iterator.next();
            } else {
                currentRange++;
                if (currentRange < nRanges) {
                    iterator = ranges.get(currentRange).iterator();
                    value = iterator.next();
                } else {
                    throw new NoSuchElementException();
                }
            }
            return value;
        }
    }

    /**
     * Private implementation of a "range" of long numbers, defined by a
     * string of the form {@code "<lo>-<hi>"}, for example, "17-32".
     */
    static final class Range {
        private static final Pattern RE_SINGLE = Pattern.compile("(\\d+)");
        private static final Pattern RE_RANGE = Pattern.compile("(\\d+)-(\\d+)");
        private static final String E_BAD_FORMAT = "Bad Range Format ";

        private final long lo;
        private final long hi;

        /**
         * Constructs a range from the given string definition.
         * For example:
         * <pre>
         *     Range r = new Range("17-32");
         * </pre>
         *
         * @param s the string representation of the range
         * @throws IllegalArgumentException if the range string is malformed
         */
        Range(String s) {
            String lohi = s;
            Matcher m = RE_SINGLE.matcher(s);
            if (m.matches()) {
                lohi = s + "-" + s;
            }
            m = RE_RANGE.matcher(lohi);
            if (!m.matches()) {
                throw new IllegalArgumentException(E_BAD_FORMAT + s);
            }
            try {
                lo = Long.parseLong(m.group(1));
                hi = Long.parseLong(m.group(2));

                if (hi < lo) {
                    throw new IllegalArgumentException(E_BAD_FORMAT + s);
                }
            } catch (NumberFormatException nfe) {
                // unlikely to be thrown, since the matcher will have failed first
                throw new IllegalArgumentException(E_BAD_FORMAT + s, nfe);
            }
        }


        /**
         * Returns an iterator over this range, starting from the lowest value
         * and iterating up to the highest value (inclusive).
         *
         * @return an iterator over this range
         */
        Iterator<Long> iterator() {
            return new RangeIterator();
        }

        /**
         * Private implementation of an iterator over the range.
         */
        class RangeIterator implements Iterator<Long> {
            long current;

            RangeIterator() {
                current = lo - 1;
            }

            @Override
            public boolean hasNext() {
                return current < hi;
            }

            @Override
            public Long next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return ++current;
            }
        }
    }
}
