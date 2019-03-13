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

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;
import java.util.UUID;

import java.lang.reflect.Field;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = VpdcEndpoint.class, name = "vpdc"),
				@Type(value = VpdcHostEndpoint.class, name = "vpdchost"),
				@Type(value = ClientServiceBypassEndpoint.class, name = "client-service-bypass"),
				@Type(value = ServiceEndpoint.class, name = "service"),
				@Type(value = StorageEndpoint.class, name = "storage"),
				@Type(value = ExternalServiceEndpoint.class, name = "external-service"),
				@Type(value = OltEndpoint.class, name = "olt"),
				@Type(value = OltControlEndpoint.class, name = "olt-control"),
				@Type(value = VoltEndpoint.class, name = "volt")})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Endpoint {

	public MacAddress mac;
	public UUID id;


	public Endpoint(MacAddress mac, UUID id) {
		this.id = id;
		if (mac != null) {
			this.mac = mac;
		} else {
			this.mac = MacAddress.valueOf("00:00:00:00:00:00");
		}
		// UUID to be generated in each subclass

	}

	@JsonIgnore
	public VlanId getVlan() {
		return VlanId.vlanId(VlanId.UNTAGGED);
	}

	@JsonIgnore
	public UUID getUUID() {
		return id;
	}

	@JsonIgnore
	public VlanId getInnerVlan() {
		return VlanId.vlanId(VlanId.UNTAGGED);
	}

	@JsonIgnore
	public DeviceId getNode() {
		return DeviceId.NONE;
	}

	@JsonIgnore
	public Boolean getExplicitVlan() {
		return false;
	}

	@JsonIgnore
	public PortNumber getPort() {
		return PortNumber.portNumber(0);
	}

	@JsonIgnore
	public IpPrefix getIpPrefix() {
		return IpPrefix.valueOf("0.0.0.0/32");
	}

	@JsonIgnore
	public boolean keepExternalVlan() {
		return false;
	}

	@JsonIgnore
	public MacAddress getMac() {
		return this.mac;
	}

	@JsonIgnore
	public MacAddress getSrcMacMask() {
		return null;
	}

	@JsonIgnore
	public MacAddress getSrcMac() {
		return null;
	}

	// @JsonIgnore
	public boolean getExternalAccessFlag() {
		return false;
	}

	/* End get methods */

	/* Serialization functions */
	@JsonProperty("mac")
	public String getMacSer() {
		return this.mac.toString();
	}
	/* End serialization functions */

	@Override
	public String toString() {

		StringBuilder result = new StringBuilder();
		String newLine = System.getProperty("line.separator");

		result.append( this.getClass().getName() );
		result.append( " Object {" );
		result.append(newLine);

		//determine fields declared in this class only (no fields of superclass)
		Field[] fields = this.getClass().getDeclaredFields();

		//print field names paired with their values
		for ( Field field : fields  ) {
			result.append("  ");
			try {
			result.append( field.getName() );
			result.append(": ");
			//requires access to private field:
			result.append( field.get(this) );
			} catch ( IllegalAccessException ex ) {
			System.out.println(ex);
			}
			result.append(newLine);
		}
		// We append mac because is an attribute of super class endpoint. Mac property is not a field of getDeclaredFields() of extended classes
		result.append("Mac: "+getMac());
		result.append("}");
		return result.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.mac.hashCode());
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Endpoint))
			return false;
			Endpoint otherDevice = (Endpoint) other;
		if (otherDevice.mac.equals(this.mac)) {
			return true;
		}
		return false;
	}
}
