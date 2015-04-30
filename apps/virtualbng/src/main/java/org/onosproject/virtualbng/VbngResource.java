/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.virtualbng;

import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.onlab.packet.IpAddress;
import org.onlab.rest.BaseResource;
import org.slf4j.Logger;

/**
 * This class provides REST services to virtual BNG.
 */
@Path("privateip")
public class VbngResource extends BaseResource {

    private final Logger log = getLogger(getClass());

    @POST
    @Path("{privateip}")
    public String privateIpNotification(@PathParam("privateip")
            String privateIp) {
        if (privateIp == null) {
            log.info("Private IP address is null");
            return "0";
        }
        log.info("Received a private IP address : {}", privateIp);
        IpAddress privateIpAddress = IpAddress.valueOf(privateIp);

        VbngService vbngService = get(VbngService.class);

        IpAddress publicIpAddress = null;
        synchronized (this) {
            // Create a virtual BNG
            publicIpAddress = vbngService.createVbng(privateIpAddress);
        }

        if (publicIpAddress != null) {
            return publicIpAddress.toString();
        } else {
            return "0";
        }
    }
}