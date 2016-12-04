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
 * Representation of an external domain link.
 */
public class ExternalLink {
    private final TeLinkTpGlobalKey externalLinkKey;
    private final Long plugId;

    /**
     * Creates an instance of an external domain link.
     *
     * @param externalLinkKey external TE link key
     * @param plugId          global plug identifier
     */
    public ExternalLink(TeLinkTpGlobalKey externalLinkKey, long plugId) {
        this.externalLinkKey = externalLinkKey;
        this.plugId = plugId;
    }

    /**
     * Returns the external TE link key.
     *
     * @return the externalLinkKey
     */
    public TeLinkTpGlobalKey externalLinkKey() {
        return externalLinkKey;
    }

    /**
     * Returns the global plug identifier.
     *
     * @return value of the global plug id
     */
    public Long plugId() {
        return plugId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(externalLinkKey, plugId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof ExternalLink) {
            ExternalLink that = (ExternalLink) object;
            return Objects.equal(externalLinkKey, that.externalLinkKey) &&
                    Objects.equal(plugId, that.plugId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("externalLinkKey", externalLinkKey)
                .add("plugId", plugId)
                .toString();
    }
}
