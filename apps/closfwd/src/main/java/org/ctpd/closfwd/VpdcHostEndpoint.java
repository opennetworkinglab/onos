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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import java.lang.*;


//@JsonTypeName("vpdchost")
public class VpdcHostEndpoint extends Endpoint {

	public DeviceId node;
	public PortNumber port;
	public VlanId vlan;
	public boolean externalAccessFlag;
	public ArrayList<IpPrefix> ipClientList;
	public ArrayList<IpPrefix> ipServiceList;
	public UUID oltUUID;
	public int reference;

	@JsonCreator
	public VpdcHostEndpoint(@JsonProperty("device") String device, @JsonProperty("olt_id") UUID oltUUID, @JsonProperty("port") long port,
			@JsonProperty("mac") MacAddress mac, @JsonProperty("ip_client_list") ArrayList<IpPrefix> ips_client,@JsonProperty("ip_service_list") ArrayList<IpPrefix> ips_service,
			 @JsonProperty("vlan") short vlan, @JsonProperty("external_access_clients") boolean externalAccessFlag){
		super(mac,null);
		if (device == null)
			device = "";
		this.node = DeviceId.deviceId(device);
		this.port = PortNumber.portNumber(port);
		this.reference=0;
		if(oltUUID != null){
			this.oltUUID = oltUUID;
		} else{
			UUID uuidDefautl = UUID.randomUUID();
			this.oltUUID = uuidDefautl;
		}
		this.externalAccessFlag = externalAccessFlag;

		if (vlan == 0) {
			this.vlan = VlanId.vlanId(VlanId.UNTAGGED);
		} else {
			this.vlan = VlanId.vlanId(vlan);
		}

		if (ips_client != null)
			this.ipClientList = ips_client;

		if (ips_service != null)
			this.ipServiceList = ips_service;

		id = UUID.nameUUIDFromBytes(this.toString().getBytes());

	}

	/* Get methods */
	public DeviceId getNode() {
		return node;
	}

	@Override
	public PortNumber getPort() {
		return port;
	}

	@Override
	public VlanId getVlan() {
		return this.vlan;
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

	@JsonProperty("vlan")
	public String getVlanSer() {
		return this.vlan.toString();
	}

	@JsonProperty("olt_id")
	public String getOltUUIDSer() {
		return this.oltUUID.toString();
	}

	@JsonProperty("ip_client_list")
	public List<String> getIpClientListSer() {
		List<String> stringList = new ArrayList<>(this.ipClientList.size());
		for (IpPrefix ip : this.ipClientList) {
			stringList.add(ip.toString());
		}
		return stringList;
	}

	@JsonProperty("ip_service_list")
	public List<String> getIpServiceListSer() {
		List<String> stringList = new ArrayList<>(this.ipServiceList.size());
		for (IpPrefix ip : this.ipServiceList) {
			stringList.add(ip.toString());
		}
		return stringList;
	}

	@JsonProperty("external_access_clients")
	public String getExternalAccessFlagSer() {
		return String.valueOf(this.externalAccessFlag);
	}
	/* End serialization functions */

	@JsonIgnore
	public List<IpPrefix> getIpClientList() {
		List<IpPrefix> ipClientList = new ArrayList<>();
		for (IpPrefix ip : this.ipClientList) {
			ipClientList.add(ip);
		}
		return ipClientList;
	}

	@JsonIgnore
	public List<IpPrefix> getIpServiceList() {
		List<IpPrefix> ipServiceList = new ArrayList<>();
		for (IpPrefix ip : this.ipServiceList) {
			ipServiceList.add(ip);
		}
		return ipServiceList;
	}

	@JsonIgnore
	public UUID getOltUUID() {
		return this.oltUUID;
	}

	public boolean getExternalAccessFlag() {
		return this.externalAccessFlag;
	}
	

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), this.node.hashCode(), this.port.hashCode(), this.ipsClientHashCode(),this.ipsServiceHashCode());
	}

	private int ipsClientHashCode() {
		int hashCode = 0;
		for (IpPrefix prefix : ipClientList) {
			hashCode = Objects.hashCode(hashCode, prefix.hashCode());
		}
		return hashCode;
	}

	private int ipsServiceHashCode() {
		int hashCode = 0;
		for (IpPrefix prefix : ipServiceList) {
			hashCode = Objects.hashCode(hashCode, prefix.hashCode());
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof VpdcHostEndpoint))
			return false;
			VpdcHostEndpoint otherVpdc = (VpdcHostEndpoint) other;
		if (super.equals(otherVpdc) && otherVpdc.ipsClientEquals(this.ipClientList) && otherVpdc.ipsServiceEquals(this.ipServiceList) && otherVpdc.node.equals(this.node)
				&& otherVpdc.port.equals(this.port)) {

			return true;
		}
		return false;
	}

	private boolean ipsClientEquals(ArrayList<IpPrefix> prefixes) {
		if (prefixes.size() != ipClientList.size()) {
			return false;
		}

		return ipClientList.equals(prefixes);
	}

	private boolean ipsServiceEquals(ArrayList<IpPrefix> prefixes) {
		if (prefixes.size() != ipServiceList.size()) {
			return false;
		}

		return ipServiceList.equals(prefixes);
	}

}
