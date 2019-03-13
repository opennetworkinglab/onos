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
 *
 * Application developed by Elisa Rojas Sanchez
 */
package org.ctpd.closfwd;

import java.util.UUID;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import java.util.ArrayList;
import java.util.List;


import static java.util.Optional.ofNullable;



//@JsonTypeName("vpdc")
public class VpdcEndpoint extends Endpoint {
	public IpPrefix ipClient;
	public IpPrefix ipService;
	public DeviceId node;
	public PortNumber port;

	@JsonCreator
	public VpdcEndpoint(@JsonProperty("device") String device,
			@JsonProperty("port") long port, @JsonProperty("mac") MacAddress mac,
			@JsonProperty("ip_client") IpPrefix ipClient,@JsonProperty("ip_service") IpPrefix ipService) {

		super(mac,null);

		if (ipClient == null) {
			ipClient = IpPrefix.valueOf(IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 0), 0);
		} else {
			this.ipClient = ipClient;
		}

		if (ipService == null) {
			ipService = IpPrefix.valueOf(IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 0), 0);
		} else {
			this.ipService = ipService;
		}

		this.node = DeviceId.deviceId(device);
		this.port = PortNumber.portNumber(port);

		id = UUID.nameUUIDFromBytes(this.toString().getBytes());
	}

	/* Serialization functions */

	@JsonProperty("ip_client")
	public String getIpClientSer() {
		return this.ipClient.toString();
	}

	@JsonProperty("ip_service")
	public String getIpServiceSer() {
		return this.ipService.toString();
	}

	@JsonProperty("device")
	public String getNodeSer() {
		return this.node.toString();
	}

	@JsonProperty("port")
	public String getPortSer() {
		return this.port.toString();
	}

	/* End serialization functions */

	@Override
	public DeviceId getNode() {
		return node;
	}

	// @Override
	// public IpPrefix getIpPrefix() {
	// 	return ip;
	// }

	@JsonIgnore
	public IpPrefix getIpClient() {
		return this.ipClient;
	}

	@JsonIgnore
	public IpPrefix getIpService() {
		return this.ipService;
	}

	@Override
	public PortNumber getPort() {
		return port;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), this.ipClient.hashCode(), this.ipService.hashCode());
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof VpdcEndpoint))
			return false;
			VpdcEndpoint otherVpdc = (VpdcEndpoint) other;
		if (super.equals(otherVpdc) && otherVpdc.ipClient.equals(this.ipClient) && otherVpdc.ipService.equals(this.ipService)) {

			return true;
		}
		return false;
	}

}
