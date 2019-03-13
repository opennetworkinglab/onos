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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import java.util.UUID;


//@JsonTypeName("client")
public class OltControlEndpoint extends Endpoint {

	public static final short SERVICE = 1;
	public static final short CONTROL = 2;
	public Boolean explicitVlan;
	public DeviceId node;
	public PortNumber port;
	public VlanId vlan;
	public UUID voltUUID;
	// public IpPrefix ip;

	@JsonCreator
	public OltControlEndpoint(@JsonProperty("explicit_vlan") Boolean explicitVlan,
			@JsonProperty("device") String device,
			@JsonProperty("mac") MacAddress mac,
			@JsonProperty("port") long port,
			@JsonProperty("vlan") short vlan,
			@JsonProperty("volt_id") UUID voltUUID){

		super(mac,null);
		this.explicitVlan = explicitVlan;
		this.port = PortNumber.portNumber(port);
		this.voltUUID = voltUUID;


		if (device == null)
			device = "";
		this.node = DeviceId.deviceId(device);

		if (vlan == 0)
			vlan = VlanId.UNTAGGED;

		this.vlan = VlanId.vlanId(vlan);

		id = UUID.nameUUIDFromBytes(this.toString().getBytes());
	}

	/* Get methods */
	@Override
	public VlanId getVlan() {
		return this.vlan;
	}

	@Override
	public DeviceId getNode() {
		return node;
	}

	@Override
	public PortNumber getPort() {
		return port;
	}

	@JsonIgnore
	@Override
	public Boolean getExplicitVlan() {
		return explicitVlan;
	}

	@JsonIgnore
	public UUID getVoltUUID() {
		return this.voltUUID;
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

	@JsonProperty("volt_id")
	public String getVoltUUIDSer() {
		return this.voltUUID.toString();
	}

	@JsonProperty("explicit_vlan")
    public boolean getExplicitVlanSer() {
	    return this.explicitVlan;
	}

	/* End serialization functions */

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), this.vlan.hashCode());
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof OltEndpoint))
			return false;
			OltEndpoint otherClient = (OltEndpoint) other;
		if (super.equals(otherClient) && otherClient.vlan.equals(this.vlan)
				&& otherClient.node.equals(this.node) && otherClient.port.equals(this.port)) {
			return true;
		}
		return false;
	}

}
