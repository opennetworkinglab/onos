/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.restconf.api;

import javax.ws.rs.core.MediaType;

/*
 * Extension of the REST MediaType with 2 new types from the RESTCONF standard RFC 8040.
 *
 * Currently (1.14) the XML Media Type is not supported by the RESTCONF server in ONOS
 */
public class MediaTypeRestconf extends MediaType {
    public static final String APPLICATION_YANG_DATA_XML = "application/yang-data+xml";
    public static final MediaType APPLICATION_YANG_DATA_XML_TYPE = new MediaType("application", "yang-data+xml");
    public static final String APPLICATION_YANG_DATA_JSON = "application/yang-data+json";
    public static final MediaType APPLICATION_YANG_DATA_JSON_TYPE = new MediaType("application", "yang-data+json");

}
