/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.pce.cli;

import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;

import com.google.common.collect.Lists;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.util.DataRateUnit;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pce.pceservice.ExplicitPathInfo;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
import org.onosproject.pce.pceservice.LspType;
import org.onosproject.pce.pceservice.api.PceService;
import org.slf4j.Logger;

/**
 * Supports creating the pce path.
 */
@Command(scope = "onos", name = "pce-setup-path", description = "Supports creating pce path.")
public class PceSetupPathCommand extends AbstractShellCommand {
    private final Logger log = getLogger(getClass());
    public static final byte SUBTYPE_DEVICEID = 0;
    public static final byte SUBTYPE_LINK = 1;
    public static final byte SUBTYPE_INDEX = 1;
    public static final byte TYPE_INDEX = 0;

    public static final byte DEVICEID_INDEX = 2;
    public static final byte SOURCE_DEVICEID_INDEX = 2;
    public static final byte SOURCE_PORTNO_INDEX = 3;
    public static final byte DESTINATION_DEVICEID_INDEX = 4;
    public static final byte DESTINATION_PORTNO_INDEX = 5;

    @Argument(index = 0, name = "src", description = "source device.", required = true, multiValued = false)
    String src = null;

    @Argument(index = 1, name = "dst", description = "destination device.", required = true, multiValued = false)
    String dst = null;

    @Argument(index = 2, name = "type", description = "LSP type:" + " It includes "
            + "PCE tunnel with signalling in network (0), "
            + "PCE tunnel without signalling in network with segment routing (1), "
            + "PCE tunnel without signalling in network (2).",
            required = true, multiValued = false)
    int type = 0;

    @Argument(index = 3, name = "name", description = "symbolic-path-name.", required = true, multiValued = false)
    String name = null;

    @Option(name = "-c", aliases = "--cost", description = "The cost attribute IGP cost(1) or TE cost(2)",
            required = false, multiValued = false)
    int cost = 2;

    @Option(name = "-b", aliases = "--bandwidth", description = "The bandwidth attribute of path. "
            + "Data rate unit is in BPS.", required = false, multiValued = false)
    double bandwidth = 0.0;

    @Option(name = "-e", aliases = "--explicitPathObjects", description = "List of strict and loose hopes",
            required = false, multiValued = true)
    String[] explicitPathInfoStrings;

    //explicitPathInfo format : Type/SubType/Value(DeviceId or Link info)
    //If Value is Device : Type/SubType/deviceId
    //If Value is Link : Type/SubType/SourceDeviceId/SourcePortNo/DestinationDeviceId/DestinationPortNo
    List<ExplicitPathInfo> explicitPathInfo = Lists.newLinkedList();

    @Override
    protected void execute() {
        log.info("executing pce-setup-path");

        PceService service = get(PceService.class);
        TunnelService tunnelService = get(TunnelService.class);

        DeviceId srcDevice = DeviceId.deviceId(src);
        DeviceId dstDevice = DeviceId.deviceId(dst);
        List<Constraint> listConstrnt = new LinkedList<>();

        // LSP type validation
        if ((type < 0) || (type > 2)) {
           error("The LSP type value can be PCE tunnel with signalling in network (0), " +
                 "PCE tunnel without signalling in network with segment routing (1), " +
                 "PCE tunnel without signalling in network (2).");
           return;
        }
        LspType lspType = LspType.values()[type];

        //Validating tunnel name, duplicated tunnel names not allowed
        Collection<Tunnel> existingTunnels = tunnelService.queryTunnel(Tunnel.Type.MPLS);
        for (Tunnel t : existingTunnels) {
            if (t.tunnelName().toString().equals(name)) {
                error("Path creation failed, Tunnel name already exists");
                return;
            }
        }

        // Add bandwidth
        // bandwidth default data rate unit is in BPS
        if (bandwidth != 0.0) {
            listConstrnt.add(BandwidthConstraint.of(bandwidth, DataRateUnit.valueOf("BPS")));
        }

        // Add cost
        // Cost validation
        if ((cost < 1) || (cost > 2)) {
            error("The cost attribute value either IGP cost(1) or TE cost(2).");
            return;
        }
        // Here 'cost - 1' indicates the index of enum
        CostConstraint.Type costType = CostConstraint.Type.values()[cost - 1];
        listConstrnt.add(CostConstraint.of(costType));

        if (explicitPathInfoStrings != null)  {
            for (String str : explicitPathInfoStrings) {
                String[] splitted = str.split("/");
                DeviceId deviceId;
                NetworkResource res = null;
                PortNumber portNo;
                int explicitPathType = Integer.parseInt(splitted[TYPE_INDEX]);
                if ((explicitPathType < 0) || (explicitPathType > 1)) {
                    error("Explicit path validation failed");
                    return;
                }

                //subtype 0 = deviceId, 1 = link
                //subtype is required to store either as deviceId or Link
                if (splitted[DEVICEID_INDEX] != null && Integer.parseInt(splitted[SUBTYPE_INDEX]) == SUBTYPE_DEVICEID) {
                    res = DeviceId.deviceId(splitted[DEVICEID_INDEX]);
                } else if (Integer.parseInt(splitted[SUBTYPE_INDEX]) == SUBTYPE_LINK
                        && splitted[SOURCE_DEVICEID_INDEX] != null
                        && splitted[SOURCE_PORTNO_INDEX] != null
                        && splitted[DESTINATION_DEVICEID_INDEX] != null
                        && splitted[DESTINATION_PORTNO_INDEX] != null) {

                    deviceId = DeviceId.deviceId(splitted[SOURCE_DEVICEID_INDEX]);
                    portNo = PortNumber.portNumber(splitted[SOURCE_PORTNO_INDEX]);
                    ConnectPoint cpSrc = new ConnectPoint(deviceId, portNo);
                    deviceId = DeviceId.deviceId(splitted[DESTINATION_DEVICEID_INDEX]);
                    portNo = PortNumber.portNumber(splitted[DESTINATION_PORTNO_INDEX]);
                    ConnectPoint cpDst = new ConnectPoint(deviceId, portNo);
                    res = DefaultLink.builder()
                            .providerId(ProviderId.NONE)
                            .src(cpSrc)
                            .dst(cpDst)
                            .type(DIRECT)
                            .state(ACTIVE)
                            .build();
                } else {
                    error("Explicit path validation failed");
                    return;
                }
                ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.values()[explicitPathType], res);
                explicitPathInfo.add(obj);
            }
        }
        if (!service.setupPath(srcDevice, dstDevice, name, listConstrnt, lspType, explicitPathInfo)) {
            error("Path creation failed.");
        }
    }
}
