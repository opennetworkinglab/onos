/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.t3.cli;

import org.apache.commons.lang.StringUtils;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.t3.api.GroupsInDevice;
import org.onosproject.t3.api.StaticPacketTrace;

import java.util.List;

/**
 * Class containing utility methods for T3 cli.
 */
final class T3CliUtils {

    private T3CliUtils() {
        //banning construction
    }

    private static final String FLOW_SHORT_FORMAT = "    %s, bytes=%s, packets=%s, "
            + "table=%s, priority=%s, selector=%s, treatment=%s";

    private static final String GROUP_FORMAT =
            "   id=0x%s, state=%s, type=%s, bytes=%s, packets=%s, appId=%s, referenceCount=%s";
    private static final String GROUP_BUCKET_FORMAT =
            "       id=0x%s, bucket=%s, bytes=%s, packets=%s, actions=%s";

    /**
     * Builds a string output for the given trace for a specific level of verbosity.
     *
     * @param trace      the trace
     * @param verbosity1 middle verbosity level
     * @param verbosity2 high verbosity level
     * @return a string representing the trace.
     */
    static String printTrace(StaticPacketTrace trace, boolean verbosity1, boolean verbosity2) {
        StringBuilder tracePrint = new StringBuilder();
        //Print based on verbosity
        if (verbosity1) {
            tracePrint = printTrace(trace, false, tracePrint);
        } else if (verbosity2) {
            tracePrint = printTrace(trace, true, tracePrint);
        } else {
            tracePrint.append("Paths");
            tracePrint.append("\n");
            List<List<ConnectPoint>> paths = trace.getCompletePaths();
            for (List<ConnectPoint> path : paths) {
                tracePrint.append(path);
                tracePrint.append("\n");
            }
        }
        tracePrint.append("Result: \n" + trace.resultMessage());
        return tracePrint.toString();
    }

    //prints the trace
    private static StringBuilder printTrace(StaticPacketTrace trace, boolean verbose, StringBuilder tracePrint) {
        List<List<ConnectPoint>> paths = trace.getCompletePaths();
        for (List<ConnectPoint> path : paths) {
            tracePrint.append("Path " + path);
            tracePrint.append("\n");
            ConnectPoint previous = null;
            if (path.size() == 1) {
                ConnectPoint connectPoint = path.get(0);
                tracePrint.append("Device " + connectPoint.deviceId());
                tracePrint.append("\n");
                tracePrint.append("Input from " + connectPoint);
                tracePrint.append("\n");
                tracePrint = printFlows(trace, verbose, connectPoint, tracePrint);
                tracePrint = printGroups(trace, verbose, connectPoint, tracePrint);
                tracePrint.append("\n");
            } else {
                for (ConnectPoint connectPoint : path) {
                    if (previous == null || !previous.deviceId().equals(connectPoint.deviceId())) {
                        tracePrint.append("Device " + connectPoint.deviceId());
                        tracePrint.append("\n");
                        tracePrint.append("    Input from " + connectPoint);
                        tracePrint.append("\n");
                        tracePrint = printFlows(trace, verbose, connectPoint, tracePrint);
                    } else {
                        tracePrint = printGroups(trace, verbose, connectPoint, tracePrint);
                        tracePrint.append("    Output through " + connectPoint);
                        tracePrint.append("\n");
                    }
                    previous = connectPoint;
                }
            }
            tracePrint.append(StringUtils.leftPad("\n", 100, '-'));
        }
        return tracePrint;
    }


    //Prints the flows for a given trace and a specified level of verbosity
    private static StringBuilder printFlows(StaticPacketTrace trace, boolean verbose, ConnectPoint connectPoint,
                                            StringBuilder tracePrint) {
        tracePrint.append("    Flows ");
        tracePrint.append(trace.getFlowsForDevice(connectPoint.deviceId()).size());
        tracePrint.append("    \n");
        trace.getFlowsForDevice(connectPoint.deviceId()).forEach(f -> {
            if (verbose) {
                tracePrint.append("    " + String.format(FLOW_SHORT_FORMAT, f.state(), f.bytes(), f.packets(),
                        f.table(), f.priority(), f.selector().criteria(),
                        printTreatment(f.treatment())));
                tracePrint.append("\n");
            } else {
                tracePrint.append(String.format("       flowId=%s, table=%s, selector=%s", f.id(), f.table(),
                        f.selector().criteria()));
                tracePrint.append("\n");
            }
        });
        return tracePrint;
    }

    //Prints the groups for a given trace and a specified level of verbosity
    private static StringBuilder printGroups(StaticPacketTrace trace, boolean verbose, ConnectPoint connectPoint,
                                             StringBuilder tracePrint) {
        List<GroupsInDevice> groupsInDevice = trace.getGroupOuputs(connectPoint.deviceId());
        if (groupsInDevice != null) {
            tracePrint.append("    Groups");
            tracePrint.append("\n");
            groupsInDevice.forEach(output -> {
                if (output.getOutput().equals(connectPoint)) {
                    output.getGroups().forEach(group -> {
                        if (verbose) {
                            tracePrint.append("    " + String.format(GROUP_FORMAT, Integer.toHexString(group.id().id()),
                                    group.state(), group.type(), group.bytes(), group.packets(),
                                    group.appId().name(), group.referenceCount()));
                            tracePrint.append("\n");
                            int i = 0;
                            for (GroupBucket bucket : group.buckets().buckets()) {
                                tracePrint.append("    " + String.format(GROUP_BUCKET_FORMAT,
                                        Integer.toHexString(group.id().id()),
                                        ++i, bucket.bytes(), bucket.packets(),
                                        bucket.treatment().allInstructions()));
                                tracePrint.append("\n");
                            }
                        } else {
                            tracePrint.append("       groupId=" + group.id());
                            tracePrint.append("\n");
                        }
                    });
                    tracePrint.append("    Outgoing Packet " + output.getFinalPacket());
                    tracePrint.append("\n");
                }
            });
        }
        return tracePrint;
    }

    private static String printTreatment(TrafficTreatment treatment) {
        final String delimiter = ", ";
        StringBuilder builder = new StringBuilder("[");
        if (!treatment.immediate().isEmpty()) {
            builder.append("immediate=" + treatment.immediate() + delimiter);
        }
        if (!treatment.deferred().isEmpty()) {
            builder.append("deferred=" + treatment.deferred() + delimiter);
        }
        if (treatment.clearedDeferred()) {
            builder.append("clearDeferred" + delimiter);
        }
        if (treatment.tableTransition() != null) {
            builder.append("transition=" + treatment.tableTransition() + delimiter);
        }
        if (treatment.metered() != null) {
            builder.append("meter=" + treatment.metered() + delimiter);
        }
        if (treatment.writeMetadata() != null) {
            builder.append("metadata=" + treatment.writeMetadata() + delimiter);
        }
        // Chop off last delimiter
        builder.replace(builder.length() - delimiter.length(), builder.length(), "");
        builder.append("]");
        return builder.toString();
    }
}
