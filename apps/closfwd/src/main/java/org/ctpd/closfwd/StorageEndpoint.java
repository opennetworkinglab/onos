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
public class StorageEndpoint extends Endpoint {

	public DeviceId node;
	public PortNumber port;
	public IpPrefix ip;
	public VlanId vlan;
	public MacAddress srcMacMask;
	public MacAddress srcMac;
	public boolean externalAccessFlag;
	public boolean bypass;


	@JsonCreator
	public StorageEndpoint(@JsonProperty("device") String device,
			@JsonProperty("mac") MacAddress mac, @JsonProperty("port") long port, @JsonProperty("ip") IpPrefix ip,
			@JsonProperty("vlan") short vlan, @JsonProperty("src_mac") MacAddress srcMac, @JsonProperty("src_mac_mask") MacAddress srcMacMask,
			@JsonProperty("external_access") boolean externalAccessFlag,
			@JsonProperty("bypass_enabled") boolean bypass){

		super(mac,null);
		this.port = PortNumber.portNumber(port);

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
		this.bypass = bypass;

		this.srcMacMask = srcMacMask;
		this.srcMac = srcMac;

		this.externalAccessFlag = externalAccessFlag;

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

	@Override
	public IpPrefix getIpPrefix() {
		return this.ip;
	}

	public MacAddress getSrcMacMask(){
		return this.srcMacMask;
	}

	public MacAddress getSrcMac(){
		return this.srcMac;
	}
	
	@JsonIgnore
	public boolean getExternalAccessFlag() {
		return this.externalAccessFlag;
	}

	@JsonIgnore
	public boolean getBypassEnabled() {
		return this.bypass;
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

	@JsonProperty("src_mac")
	public String getSrcMacSer() {
		return this.srcMac.toString();
	}

	@JsonProperty("src_mac_mask")
	public String getSrcMacMaskSer() {
		return this.srcMacMask.toString();
	}
	@JsonProperty("external_access")
    public String getExternalAccessFlagSer() {
	    return String.valueOf(this.externalAccessFlag);
	}
	

	

	@JsonProperty("bypass_enabled")
    public String getBypassEnabledSer() {
	    return String.valueOf(this.bypass);
	}
	/* End serialization functions */

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), this.node.hashCode(), this.port.hashCode(), this.ip.hashCode(),
				this.vlan.hashCode());
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof StorageEndpoint))
			return false;
			StorageEndpoint otherService = (StorageEndpoint) other;
		if (super.equals(otherService) && otherService.ip.equals(this.ip)
				&& otherService.node.equals(this.node) && otherService.port.equals(this.port)) {
			return true;
		}
		return false;
	}
}
