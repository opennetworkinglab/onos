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

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import java.lang.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.UUID;

//@JsonTypeName("service")
public class VoltEndpoint extends Endpoint {

	public DeviceId node;
	public PortNumber port;
	public IpPrefix ip;
	public VlanId vlan;
	public int reference;

	@JsonCreator
	public VoltEndpoint(@JsonProperty("device") String device,
			@JsonProperty("mac") MacAddress mac, @JsonProperty("port") long port, @JsonProperty("ip") IpPrefix ip,
			@JsonProperty("vlan") short vlan) {
		super(mac,null);
		this.port = PortNumber.portNumber(port);

		this.reference=0;

		if (device == null) {
			this.node = null;
		} else {
			this.node = DeviceId.deviceId(device);
		}

		if (ip == null) {
			this.ip = IpPrefix.valueOf(IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 0), 0);
		} else {
			this.ip = ip;
		}

		if (vlan == 0) {
			this.vlan = VlanId.vlanId(VlanId.UNTAGGED);
		} else {
			this.vlan = VlanId.vlanId(vlan);
		}

		id = UUID.nameUUIDFromBytes(this.toString().getBytes());
	}

	/* Get methods */

	@Override
	public VlanId getVlan() {
		return this.vlan;
	}

	@Override
	public DeviceId getNode() {
		return this.node;
	}

	@Override
	public PortNumber getPort() {
		return this.port;
	}
	
	public int getReference(){
		return this.reference;
	}
	public void addReference(){
		this.reference=this.reference+1;
	}
	public void drcReference(){
		this.reference=this.reference-1;
	}

	// @Override
	// public IpAddress getIpAddress(){
	// 	if (this.ip.equals(IpPrefix.valueOf(IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 0), 0))) {
	// 		return null;
	// 	}
	// 	return this.ip.address();
	// }

	@Override
	public IpPrefix getIpPrefix() {
		if (this.ip.equals(IpPrefix.valueOf(IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 0), 0))) {
			return null;
		}
		return this.ip;
	}

	/* End get methods */

	/* Serialization functions */

	@JsonProperty("device")
	public String getNodeSer() {
		return this.node.toString();
	}

	@JsonProperty("port")
	public String getPortSer() {
		return this.port.toString();
	}

	@JsonProperty("ip")
	public String getIpSer() {
		return this.ip.toString();
	}

	@JsonProperty("vlan")
	public String getVlanSer() {
		return this.vlan.toString();
	}

	/* End serialization functions */

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), this.node.hashCode(), this.port.hashCode(), this.ip.hashCode(),
				this.vlan.hashCode());
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ServiceEndpoint))
			return false;
			ServiceEndpoint otherService = (ServiceEndpoint) other;
		if (super.equals(otherService) && otherService.ip.equals(this.ip)
				&& otherService.node.equals(this.node) && otherService.port.equals(this.port)
				&& otherService.vlan.equals(this.vlan)) {

			return true;
		}
		return false;
	}
}
