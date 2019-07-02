/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.segmentrouting.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.mcast.McastFilteringObjStoreKey;

import java.util.List;
import java.util.Map;

/**
 * Command to show the list of mcast filtering obj.
 */
@Service
@Command(scope = "onos", name = "sr-filt-mcast",
        description = "Lists all mcast filtering objs")
public class McastFilterListCommand extends AbstractShellCommand {

    private static final String FORMAT_HEADER = "device=%s";
    private static final String FILTER_HEADER = "\t%s,%s,%s";

    @Override
    protected void doExecute() {
        // Get SR service
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        // Get the filt objs
        Map<DeviceId, List<McastFilteringObjStoreKey>> filteringObjKeys = srService.getMcastFilters();
        filteringObjKeys.forEach(this::printMcastFilter);
    }

    private void printMcastFilter(DeviceId deviceId, List<McastFilteringObjStoreKey> filteringObjs) {
        print(FORMAT_HEADER, deviceId);
        filteringObjs.forEach(filteringObj -> print(FILTER_HEADER, filteringObj.ingressCP(),
                                                    filteringObj.isIpv4() ? "IPv4" : "IPv6",
                                                    filteringObj.vlanId()));
    }
}
