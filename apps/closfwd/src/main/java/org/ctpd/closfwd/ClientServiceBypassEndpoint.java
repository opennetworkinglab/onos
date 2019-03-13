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

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


//@JsonTypeName("client")
public class ClientServiceBypassEndpoint extends Endpoint {

	public DeviceId node;
	public PortNumber port;
	@JsonIgnore
	public VlanId vlanUpper;
	@JsonIgnore
	public VlanId vlanLower;
	public IpPrefix ip;
	public ArrayList<UUID> serviceUUIDs;

	@JsonCreator
	public ClientServiceBypassEndpoint(@JsonProperty("device") String device,
			@JsonProperty("mac") MacAddress mac, @JsonProperty("port") long port,
			@JsonProperty("svlan") short vlanUpper, @JsonProperty("cvlan") short vlanLower,
			@JsonProperty("ip") IpPrefix ip, @JsonProperty("service_uuids") ArrayList<UUID> serviceUUIDs) {

		super(mac,null);
		this.port = PortNumber.portNumber(port);

		if (device == null)
			device = "";
		this.node = DeviceId.deviceId(device);

		if (vlanUpper == 0)
			vlanUpper = VlanId.UNTAGGED;
		this.vlanUpper = VlanId.vlanId(vlanUpper);

		if (vlanLower == 0)
			vlanLower = VlanId.UNTAGGED;
		this.vlanLower = VlanId.vlanId(vlanLower);

		if (ip == null) {
			this.ip = IpPrefix.valueOf(IpAddress.makeMaskPrefix(IpAddress.Version.INET6, 0), 0);
		} else {
			this.ip = ip;
		}

		this.serviceUUIDs = serviceUUIDs;

		id = UUID.nameUUIDFromBytes(this.toString().getBytes());


	}

	/* Get methods */
	@Override
	@JsonIgnore
	public VlanId getVlan() {
		return this.vlanUpper;
	}

	@Override
	@JsonIgnore
	public VlanId getInnerVlan() {
		return this.vlanLower;
	}

	@Override
	public DeviceId getNode() {
		return node;
	}

	@Override
	public PortNumber getPort() {
		return port;
	}

	@Override
    public IpPrefix getIpPrefix() {
        return ip;
	}

	@JsonIgnore
	public ArrayList<UUID> getServiceUUIDs() {
		return serviceUUIDs;
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

	@JsonProperty("svlan")
	public String getVlanUpperSer() {
		return this.vlanUpper.toString();
	}

	@JsonProperty("cvlan")
	public String getVlanLowerSer() {
		return this.vlanLower.toString();
	}

    @JsonProperty("ip")
    public String getIpPrefixSer() {
        return this.ip.toString();
	}

	@JsonProperty("service_uuids")
	public List<String> getServiceUUIDsSer() {
		List<String> stringList = new ArrayList<>(this.serviceUUIDs.size());
		for (UUID uuid : this.serviceUUIDs) {
			stringList.add(uuid.toString());
		}
		return stringList;
	}

	/* End serialization functions */

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), this.vlanUpper.hashCode(), this.vlanLower.hashCode(), this.ip.hashCode());
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ClientServiceBypassEndpoint))
			return false;
			ClientServiceBypassEndpoint otherClient = (ClientServiceBypassEndpoint) other;
		if (super.equals(otherClient) && otherClient.vlanUpper.equals(this.vlanUpper)
				&& otherClient.node.equals(this.node) && otherClient.port.equals(this.port)
				&& otherClient.vlanLower.equals(this.vlanLower) && otherClient.ip.equals(this.ip)) {

			return true;
		}
		return false;
	}

}
