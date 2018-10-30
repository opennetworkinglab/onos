/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.protocol.rest;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;

import static com.google.common.base.Strings.nullToEmpty;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation for Rest devices.
 */
public class DefaultRestSBDevice implements RestSBDevice {
    private static final String REST = "rest";
    private static final String COLON = ":";
    private final IpAddress ip;
    private final int port;
    private final String username;
    private final String password;
    private boolean isActive;
    private String protocol;
    private String url;
    private boolean isProxy;
    private AuthenticationScheme authenticationScheme;
    private String token;
    private final Optional<String> testUrl;
    private final Optional<String> manufacturer;
    private final Optional<String> hwVersion;
    private final Optional<String> swVersion;

    public DefaultRestSBDevice(IpAddress ip, int port, String name, String password,
                               String protocol, String url, boolean isActive) {
        this(ip, port, name, password, protocol, url, isActive, "", "", "", "", AuthenticationScheme.BASIC, "");
    }

    public DefaultRestSBDevice(IpAddress ip, int port, String name, String password,
                               String protocol, String url, boolean isActive, String testUrl, String manufacturer,
                               String hwVersion, String swVersion, AuthenticationScheme authenticationScheme,
                               String token) {
        Preconditions.checkNotNull(ip, "IP address cannot be null");
        Preconditions.checkArgument(port > 0, "Port address cannot be negative");
        Preconditions.checkNotNull(protocol, "protocol address cannot be null");
        this.ip = ip;
        this.port = port;
        this.username = name;
        this.password = StringUtils.isEmpty(password) ? null : password;
        this.isActive = isActive;
        this.protocol = protocol;
        this.url = StringUtils.isEmpty(url) ? null : url;
        this.authenticationScheme = authenticationScheme;
        this.token = token;
        this.manufacturer = StringUtils.isEmpty(manufacturer) ?
                Optional.empty() : Optional.ofNullable(manufacturer);
        this.hwVersion = StringUtils.isEmpty(hwVersion) ?
                Optional.empty() : Optional.ofNullable(hwVersion);
        this.swVersion = StringUtils.isEmpty(swVersion) ?
                Optional.empty() : Optional.ofNullable(swVersion);
        this.testUrl = StringUtils.isEmpty(testUrl) ?
                Optional.empty() : Optional.ofNullable(testUrl);
        this.isProxy = false;
    }

    public DefaultRestSBDevice(IpAddress ip, int port, String name, String password,
                               String protocol, String url, boolean isActive, String testUrl, String manufacturer,
                               String hwVersion, String swVersion, boolean isProxy,
                               AuthenticationScheme authenticationScheme, String token) {
        Preconditions.checkNotNull(ip, "IP address cannot be null");
        Preconditions.checkArgument(port > 0, "Port address cannot be negative");
        Preconditions.checkNotNull(protocol, "protocol address cannot be null");
        this.ip = ip;
        this.port = port;
        this.username = name;
        this.password = StringUtils.isEmpty(password) ? null : password;
        this.isActive = isActive;
        this.protocol = protocol;
        this.url = StringUtils.isEmpty(url) ? null : url;
        this.authenticationScheme = authenticationScheme;
        this.token = token;
        this.manufacturer = StringUtils.isEmpty(manufacturer) ?
                Optional.empty() : Optional.ofNullable(manufacturer);
        this.hwVersion = StringUtils.isEmpty(hwVersion) ?
                Optional.empty() : Optional.ofNullable(hwVersion);
        this.swVersion = StringUtils.isEmpty(swVersion) ?
                Optional.empty() : Optional.ofNullable(swVersion);
        this.testUrl = StringUtils.isEmpty(testUrl) ?
                Optional.empty() : Optional.ofNullable(testUrl);
        this.isProxy = isProxy;
    }

    @Override
    public IpAddress ip() {
        return ip;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public DeviceId deviceId() {
        try {
            return DeviceId.deviceId(new URI(REST, ip + COLON + port, null));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot create deviceID " +
                                                       REST + COLON + ip +
                                                       COLON + port, e);
        }
    }

    @Override
    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public String protocol() {
        return protocol;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public boolean isProxy() {
        return isProxy;
    }

    @Override
    public Optional<String> testUrl() {
        return testUrl;
    }

    @Override
    public Optional<String> manufacturer() {
        return manufacturer;
    }

    @Override
    public Optional<String> hwVersion() {
        return hwVersion;
    }

    @Override
    public Optional<String> swVersion() {
        return swVersion;
    }

    @Override
    public AuthenticationScheme authentication() {
        return authenticationScheme;
    }

    @Override
    public String token() {
        return token;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("url", url)
                .add("protocol", protocol)
                .add("username", username)
                .add("ip", ip)
                .add("port", port)
                .add("authentication", authenticationScheme.name())
                .add("token", token)
                .add("testUrl", testUrl.orElse(null))
                .add("manufacturer", manufacturer.orElse(null))
                .add("hwVersion", hwVersion.orElse(null))
                .add("swVersion", swVersion.orElse(null))
                .toString();

    }

    // FIXME revisit equality condition. Why urls are not included?
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RestSBDevice)) {
            return false;
        }
        RestSBDevice that = (RestSBDevice) obj;
        return Objects.equals(this.ip, that.ip()) &&
               this.port == that.port() &&
               nullToEmpty(this.username).equals(nullToEmpty(that.username()));

    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, nullToEmpty(username));
    }

}
