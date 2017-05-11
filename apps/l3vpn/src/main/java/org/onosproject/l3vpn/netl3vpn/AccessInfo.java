/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.l3vpn.netl3vpn;

import com.google.common.base.Objects;

/**
 * Representation of site network access information.
 */
public class AccessInfo {

    /**
     * Site id from sites list.
     */
    private String siteId;

    /**
     * Site network access id from site network access list.
     */
    private String accessId;

    /**
     * Constructs access info with site id and access id.
     *
     * @param s site id
     * @param a access id
     */
    public AccessInfo(String s, String a) {
        siteId = s;
        accessId = a;
    }

    /**
     * Returns the site id.
     *
     * @return site id
     */
    public String siteId() {
        return siteId;
    }

    /**
     * Returns the access id.
     *
     * @return access id
     */
    public String accessId() {
        return accessId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(siteId, accessId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof AccessInfo) {
            AccessInfo that = (AccessInfo) object;
            return Objects.equal(siteId, that.siteId) &&
                    Objects.equal(accessId, that.accessId);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Access id : " + accessId + "\nSite id : " + siteId;
    }
}
