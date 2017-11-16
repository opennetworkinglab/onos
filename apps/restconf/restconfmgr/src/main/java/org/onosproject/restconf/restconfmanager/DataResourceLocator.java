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

package org.onosproject.restconf.restconfmanager;

import org.onosproject.net.DeviceId;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

import static org.onosproject.d.config.DeviceResourceIds.toResourceId;
import static org.onosproject.d.config.ResourceIds.concat;
import static org.onosproject.d.config.ResourceIds.removeRootNode;
import static org.onosproject.restconf.utils.RestconfUtils.convertUriToRid;

/**
 * Representation of the data resource identifiers used by the RESTCONF manager.
 * <p>
 * For a data resource under the device hierarchy, the restconf manager needs
 * to maintain 2 separate resource IDs, one used by the
 * the Dynamic Config, and the other by the Yang
 * Runtime. (i.e., The resource IDs used by the dyn-config contain the
 * "/devices/device" prefix, whereas the ones used by Yang Runtime do not.)
 * This class provides the interface for the RESTCONF manager to use these
 * 2 resource IDs.
 */
public final class DataResourceLocator {

    private static final Logger log = LoggerFactory.getLogger(DataResourceLocator.class);

    private static final String DATA_ROOT_DIR = "/onos/restconf/data";
    private static final String DEVICE_REGEX = "/devices/device=[^/]+";
    private static final String DEVICE_URI_PREFIX = DATA_ROOT_DIR + "/devices/device=";

    /**
     * The resource ID used by Yang Runtime to refer to
     * a data node.
     */
    private ResourceId yrtResourceId;

    /**
     * The resource ID used by the Dynamic Config to refer
     * to a data node.
     */
    private ResourceId dcsResourceId;

    /**
     * URI used by RESTCONF to refer to a data node.
     */
    private URI uriForRestconf;

    /**
     * URI used by Yang Runtime to refer to a data node.
     */
    private URI uriForYangRuntime;

    // Suppresses default constructor, ensuring non-instantiability.
    private DataResourceLocator() {
    }

    private DataResourceLocator(ResourceId yrtResourceId,
                                ResourceId dcsResourceId,
                                URI uriForRestconf,
                                URI uriForYangRuntime) {
        this.yrtResourceId = yrtResourceId;
        this.dcsResourceId = dcsResourceId;
        this.uriForRestconf = uriForRestconf;
        this.uriForYangRuntime = uriForYangRuntime;
    }

    public ResourceId ridForDynConfig() {
        return dcsResourceId;
    }

    public ResourceId ridForYangRuntime() {
        return yrtResourceId;
    }

    public URI uriForRestconf() {
        return uriForRestconf;
    }

    public URI uriForYangRuntime() {
        return uriForYangRuntime;
    }

    /**
     * Creates a DataResourceLocator object based on a given URI.
     *
     * @param uri given URI
     * @return instantiated DataResourceLocator object
     */
    public static DataResourceLocator newInstance(URI uri) {
        URI uriForYangRuntime = uriForYangRuntime(uri);
        ResourceId yrtResourceId = ridForYangRuntime(uriForYangRuntime);
        /*
         * If the given URI starts with "devices/device" prefix, then form the
         * resource ID used by dyn-config by adding the prefix to the resource ID
         * used by YANG runtime. Otherwise the two resource IDs are the same.
         */
        ResourceId dcsResourceId = isDeviceResource(uri) ?
                addDevicePrefix(yrtResourceId, getDeviceId(uri)) : yrtResourceId;

        return new DataResourceLocator(yrtResourceId, dcsResourceId,
                                       uri, uriForYangRuntime);
    }

    private static URI uriForYangRuntime(URI uriForRestconf) {
        return isDeviceResource(uriForRestconf) ?
                removeDeviceProxyPrefix(uriForRestconf) : uriForRestconf;
    }

    private static ResourceId ridForYangRuntime(URI uriForYangRuntime) {
        ResourceId yrtResourceId = convertUriToRid(uriForYangRuntime);
        if (yrtResourceId == null) {
            yrtResourceId = ResourceId.builder().addBranchPointSchema("/", null).build();
        }

        return yrtResourceId;
    }

    private static URI removeDeviceProxyPrefix(URI uri) {
        if (uri == null) {
            return null;
        }
        UriBuilder builder = UriBuilder.fromUri(uri);
        String newPath = rmDeviceStr(uri.getRawPath());
        builder.replacePath(newPath);

        return builder.build();
    }

    private static String rmDeviceStr(String uriStr) {
        if (uriStr == null) {
            return null;

        }
        return uriStr.replaceFirst(DEVICE_REGEX, "");
    }

    private static DeviceId getDeviceId(URI uri) {
        return DeviceId.deviceId(deviceIdStr(uri.getRawPath()));
    }

    private static String deviceIdStr(String rawPath) {
        String[] segments = rawPath.split("/");
        try {
            for (String s : segments) {
                if (s.startsWith("device=")) {
                    return URLDecoder.decode(s.substring("device=".length()), "utf-8");
                }
            }
        } catch (UnsupportedEncodingException e) {
            log.error("deviceIdStr: caught UnsupportedEncodingException");
            log.debug("deviceIdStr: ", e);
        }
        return null;
    }

    private static ResourceId addDevicePrefix(ResourceId rid, DeviceId did) {
        return concat(toResourceId(did), removeRootNode(rid));
    }

    private static boolean isDeviceResource(URI uri) {
        if (uri == null) {
            return false;
        }
        return uri.getRawPath().startsWith(DEVICE_URI_PREFIX);
    }
}
