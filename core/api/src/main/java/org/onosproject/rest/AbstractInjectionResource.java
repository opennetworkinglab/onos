/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.rest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Resource for serving semi-static resources.
 */
public class AbstractInjectionResource extends AbstractWebResource {

    /**
     * Returns the index into the supplied string where the end of the
     * specified pattern is located.
     *
     * @param string      string to split
     * @param start       index where to start looking for pattern
     * @param stopPattern optional pattern where to stop
     * @return index where the split should occur
     */
    protected int split(String string, int start, String stopPattern) {
        int i = stopPattern != null ? string.indexOf(stopPattern, start) : string.length();
        checkArgument(i >= 0, "Unable to locate pattern %s", stopPattern);
        return i + (stopPattern != null ? stopPattern.length() : 0);
    }

    /**
     * Produces an input stream from the bytes of the specified sub-string.
     *
     * @param string source string
     * @param start  index where to start stream
     * @param end    index where to end stream
     * @return input stream
     */
    protected InputStream stream(String string, int start, int end) {
        return new ByteArrayInputStream(string.substring(start, end).getBytes());
    }

    /**
     * Auxiliary enumeration to sequence input streams.
     */
    protected class StreamEnumeration implements Enumeration<InputStream> {
        private final Iterator<InputStream> iterator;

        public StreamEnumeration(List<InputStream> streams) {
            this.iterator = streams.iterator();
        }

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public InputStream nextElement() {
            return iterator.next();
        }
    }
}
