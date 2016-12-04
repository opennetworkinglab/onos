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
package org.onosproject.tetopology.management.api.link;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Representation of an ODU link resource.
 */
public class OduResource {
    private final short odu0s;
    private final short odu1s;
    private final short odu2s;
    private final short odu2es;
    private final short odu3s;
    private final short odu4s;
    private final short oduFlexes;

    /**
     * Creates an instance of an ODU link resource.
     *
     * @param odu0s     number of available ODU0 containers
     * @param odu1s     number of available ODU1 containers
     * @param odu2s     number of available ODU2 containers
     * @param odu2es    number of available ODU2e containers
     * @param odu3s     number of available ODU3 containers
     * @param odu4s     number of available ODU4 containers
     * @param oduFlexes available ODUflex bandwidth in terms of ODU0 containers
     */
    public OduResource(short odu0s, short odu1s, short odu2s,
                       short odu2es, short odu3s, short odu4s,
                       short oduFlexes) {
        this.odu0s = odu0s;
        this.odu1s = odu1s;
        this.odu2s = odu2s;
        this.odu2es = odu2es;
        this.odu3s = odu3s;
        this.odu4s = odu4s;
        this.oduFlexes = oduFlexes;
    }

    /**
     * Returns the number of available ODU0s.
     *
     * @return the odu0s
     */
    public short odu0s() {
        return odu0s;
    }

    /**
     * Returns the number of available ODU1s.
     *
     * @return the odu1s
     */
    public short odu1s() {
        return odu1s;
    }

    /**
     * Returns the number of available ODU2s.
     *
     * @return the odu2s
     */
    public short odu2s() {
        return odu2s;
    }

    /**
     * Returns the number of available ODU2es.
     *
     * @return the odu2es
     */
    public short odu2es() {
        return odu2es;
    }

    /**
     * Returns the number of available ODU3s.
     *
     * @return the odu3s
     */
    public short odu3s() {
        return odu3s;
    }

    /**
     * Returns the number of available ODU4s.
     *
     * @return the odu4s
     */
    public short odu4s() {
        return odu4s;
    }

    /**
     * Returns available ODUflex bandwidth in terms of ODU0 containers.
     *
     * @return the oduFlexes
     */
    public short oduFlexes() {
        return oduFlexes;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(odu0s, odu1s, odu2s, odu2es, odu3s,
                                odu4s, oduFlexes);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof OduResource) {
            OduResource that = (OduResource) object;
            return (this.odu0s == that.odu0s) &&
                    (this.odu1s == that.odu1s) &&
                    (this.odu2s == that.odu2s) &&
                    (this.odu2es == that.odu2es) &&
                    (this.odu3s == that.odu3s) &&
                    (this.odu4s == that.odu4s) &&
                    (this.oduFlexes == that.oduFlexes);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("odu0s", odu0s)
                .add("odu1s", odu1s)
                .add("odu2s", odu2s)
                .add("odu2es", odu2es)
                .add("odu3s", odu3s)
                .add("odu4s", odu4s)
                .add("oduFlexes", oduFlexes)
                .toString();
    }


}
