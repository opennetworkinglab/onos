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
public class HL4Endpoint extends VpdcHostEndpoint {

	@JsonCreator
	public HL4Endpoint(@JsonProperty("device") String device,  @JsonProperty("port") long port,
			@JsonProperty("mac") MacAddress mac, @JsonProperty("ip_client_list") ArrayList<IpPrefix> ips_client,@JsonProperty("ip_service_list") ArrayList<IpPrefix> ips_service,
			@JsonProperty("vlan") short vlan, @JsonProperty("external_access_clients") boolean externalAccessFlag){

		super(device, null, port, mac, ips_client,ips_service, vlan, externalAccessFlag);
	}
}
