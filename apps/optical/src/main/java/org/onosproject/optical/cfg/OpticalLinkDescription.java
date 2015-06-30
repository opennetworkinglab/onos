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
import org.onlab.util.HexString;

/**
 * Public class corresponding to JSON described data model.
 *
 * @deprecated in Cardinal Release
 */
@Deprecated
public class OpticalLinkDescription {
    protected String type;
    protected Boolean allowed;
    protected long dpid1;
    protected long dpid2;
    protected String nodeDpid1;
    protected String nodeDpid2;
    protected Map<String, JsonNode> params;
    protected Map<String, String> publishAttributes;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(Boolean allowed) {
        this.allowed = allowed;
    }

    public String getNodeDpid1() {
        return nodeDpid1;
    }

    public void setNodeDpid1(String nodeDpid1) {
        this.nodeDpid1 = nodeDpid1;
        this.dpid1 = HexString.toLong(nodeDpid1);
    }

    public String getNodeDpid2() {
        return nodeDpid2;
    }

    public void setNodeDpid2(String nodeDpid2) {
        this.nodeDpid2 = nodeDpid2;
        this.dpid2 = HexString.toLong(nodeDpid2);
    }

    public long getDpid1() {
        return dpid1;
    }

    public void setDpid1(long dpid1) {
        this.dpid1 = dpid1;
        this.nodeDpid1 = HexString.toHexString(dpid1);
    }

    public long getDpid2() {
        return dpid2;
    }

    public void setDpid2(long dpid2) {
        this.dpid2 = dpid2;
        this.nodeDpid2 = HexString.toHexString(dpid2);
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

