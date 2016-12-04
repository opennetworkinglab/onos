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
import org.onosproject.tetopology.management.api.TeTopologyEventSubject;

/**
 * Representation of a TE link event.
 */
public class TeLinkEventSubject implements TeTopologyEventSubject {
    private final TeLinkTpGlobalKey key;
    private final TeLink teLink;

    /**
     * Creates an instance of TE link event.
     *
     * @param key    the TE link key
     * @param teLink the TE link
     */
    public TeLinkEventSubject(TeLinkTpGlobalKey key, TeLink teLink) {
        this.key = key;
        this.teLink = teLink;
    }

    /**
     * Returns the TE link global key.
     *
     * @return the key
     */
    public TeLinkTpGlobalKey key() {
        return key;
    }

    /**
     * Returns the TE link.
     *
     * @return the TE link
     */
    public TeLink teLink() {
        return teLink;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key, teLink);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeLinkEventSubject) {
            TeLinkEventSubject that = (TeLinkEventSubject) object;
            return Objects.equal(key, that.key) &&
                    Objects.equal(teLink, that.teLink);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("key", key)
                .add("teLink", teLink)
                .toString();
    }
}
