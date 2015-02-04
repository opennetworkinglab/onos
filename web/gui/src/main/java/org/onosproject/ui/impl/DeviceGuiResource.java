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
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.rest.BaseResource;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * UI REST resource for interacting with the inventory of infrastructure devices.
 */
@Path("device")
public class DeviceGuiResource extends BaseResource {

    private static final String DEVICES = "devices";

    private static final ObjectMapper MAPPER = new ObjectMapper();


    // return the list of devices in appropriate sorted order
    @GET
    @Produces("application/json")
    public Response getDevices(
            @DefaultValue("id") @QueryParam("sortCol") String colId,
            @DefaultValue("asc") @QueryParam("sortDir") String dir
    ) {
        DeviceService service = get(DeviceService.class);
        TableRow[] rows = generateTableRows(service);
        RowComparator rc = new RowComparator(colId, RowComparator.direction(dir));
        Arrays.sort(rows, rc);
        ArrayNode devices = generateArrayNode(rows);
        ObjectNode rootNode = MAPPER.createObjectNode();
        rootNode.set(DEVICES, devices);

        return Response.ok(rootNode.toString()).build();
    }

    private ArrayNode generateArrayNode(TableRow[] rows) {
        ArrayNode devices = MAPPER.createArrayNode();
        for (TableRow r : rows) {
            devices.add(r.toJsonNode());
        }
        return devices;
    }

    private TableRow[] generateTableRows(DeviceService service) {
        List<TableRow> list = new ArrayList<>();
        for (Device dev : service.getDevices()) {
            list.add(new DeviceTableRow(service, dev));
        }
        return list.toArray(new TableRow[list.size()]);
    }
}
