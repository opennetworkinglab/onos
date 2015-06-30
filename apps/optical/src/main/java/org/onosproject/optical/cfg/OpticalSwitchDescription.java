/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.optical.cfg;

import java.util.Map;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.onlab.util.HexString;

/**
 * Public class corresponding to JSON described data model.
 *
 * @deprecated in Cardinal Release
 */
@Deprecated
public class OpticalSwitchDescription {
    protected String name;
    protected long dpid;
    protected String nodeDpid;
    protected String type;
    protected double latitude;
    protected double longitude;
    protected boolean allowed;
    protected Map<String, JsonNode> params;
    protected Map<String, String> publishAttributes;

    public String getName() {
        return name;
    }
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public long getDpid() {
        return dpid;
    }
    @JsonProperty("dpid")
    public void setDpid(long dpid) {
        this.dpid = dpid;
        this.nodeDpid = HexString.toHexString(dpid);
    }

    public String getNodeDpid() {
        return nodeDpid;
    }

    public String getHexDpid() {
        return nodeDpid;
    }

    public void setNodeDpid(String nodeDpid) {
        this.nodeDpid = nodeDpid;
        this.dpid = HexString.toLong(nodeDpid);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public Map<String, JsonNode> getParams() {
        return params;
    }

    public void setParams(Map<String, JsonNode> params) {
        this.params = params;
    }

    public Map<String, String> getPublishAttributes() {
        return publishAttributes;
    }

    public void setPublishAttributes(Map<String, String> publishAttributes) {
        this.publishAttributes = publishAttributes;
    }

}
