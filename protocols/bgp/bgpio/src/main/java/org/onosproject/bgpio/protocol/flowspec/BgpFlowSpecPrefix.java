/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.bgpio.protocol.flowspec;

import java.util.Objects;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.ByteBuffer;

import com.google.common.base.MoreObjects;

/**
 * Provides BGP flow specification rule index.
 */
public class BgpFlowSpecPrefix implements Comparable<Object> {

    private static final Logger log = LoggerFactory.getLogger(BgpFlowSpecPrefix.class);

    private final IpPrefix destinationPrefix;
    private final IpPrefix sourcePrefix;

    /**
     * Constructor to initialize parameters.
     *
     * @param destinationPrefix destination prefix
     * @param sourcePrefix source prefix
     */
    public BgpFlowSpecPrefix(IpPrefix destinationPrefix, IpPrefix sourcePrefix) {
        if (destinationPrefix == null) {
            destinationPrefix = IpPrefix.valueOf(0, 0);
        }

        if (sourcePrefix == null) {
            sourcePrefix = IpPrefix.valueOf(0, 0);
        }

        this.destinationPrefix = destinationPrefix;
        this.sourcePrefix = sourcePrefix;
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinationPrefix, sourcePrefix);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpFlowSpecPrefix) {
             BgpFlowSpecPrefix other = (BgpFlowSpecPrefix) obj;

             if ((this.destinationPrefix != null) && (this.sourcePrefix != null)
                 && (this.destinationPrefix.equals(other.destinationPrefix))) {
                 return this.sourcePrefix.equals(other.sourcePrefix);
             } else if (this.destinationPrefix != null) {
                 return this.destinationPrefix.equals(other.destinationPrefix);
             } else if (this.sourcePrefix != null) {
                 return this.sourcePrefix.equals(other.sourcePrefix);
             }
             return false;
        }
        return false;
    }

    /**
     * Returns destination prefix.
     *
     * @return destination prefix
     */
    public IpPrefix destinationPrefix() {
        return this.destinationPrefix;
    }

    /**
     * Returns source prefix.
     *
     * @return source prefix
     */
    public IpPrefix sourcePrefix() {
        return this.sourcePrefix;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("destinationPrefix", destinationPrefix)
                .add("sourcePrefix", destinationPrefix)
                .toString();
    }

    /**
     * Compares this and o object.
     *
     * @param o object to be compared with this object
     * @return which object is greater
     */
    public int compareTo(Object o) {
        if (this.equals(o)) {
            return 0;
        }

        if (o instanceof BgpFlowSpecPrefix) {
            BgpFlowSpecPrefix that = (BgpFlowSpecPrefix) o;
            if (this.destinationPrefix() != null) {
                if (this.destinationPrefix().prefixLength() == that.destinationPrefix().prefixLength()) {
                    ByteBuffer value1 = ByteBuffer.wrap(this.destinationPrefix().address().toOctets());
                    ByteBuffer value2 = ByteBuffer.wrap(that.destinationPrefix().address().toOctets());
                    int cmpVal = value1.compareTo(value2);
                    if (cmpVal != 0) {
                        return cmpVal;
                    }
                } else {
                    if (this.destinationPrefix().prefixLength() > that.destinationPrefix().prefixLength()) {
                        return 1;
                    } else if (this.destinationPrefix().prefixLength() < that.destinationPrefix().prefixLength()) {
                        return -1;
                    }
                }
            }
            if (this.sourcePrefix() != null) {
                if (this.sourcePrefix().prefixLength() == that.sourcePrefix().prefixLength()) {
                    ByteBuffer value1 = ByteBuffer.wrap(this.sourcePrefix().address().toOctets());
                    ByteBuffer value2 = ByteBuffer.wrap(that.sourcePrefix().address().toOctets());
                    return value1.compareTo(value2);
                }

                if (this.sourcePrefix().prefixLength() > that.sourcePrefix().prefixLength()) {
                    return 1;
                } else if (this.sourcePrefix().prefixLength() < that.sourcePrefix().prefixLength()) {
                    return -1;
                }
            }
            return 0;
        }
        return 1;
    }
}
