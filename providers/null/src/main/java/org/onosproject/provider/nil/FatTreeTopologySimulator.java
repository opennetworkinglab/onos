/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.provider.nil;
import java.util.Arrays;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Fat-tree topology simulator.
 */
public class FatTreeTopologySimulator extends TopologySimulator {

    private static final int DEFAULT_NUMBER_OF_PORTS_PER_SWITCH = 4;
    protected int kPorts;
    protected int numberOfPods;
    protected int numberOfAggLayerSwitches;
    protected int numberOfCoreLayerSwitches;
    protected int numberOfEdgeLayerSwitches;

    @Override
    protected void processTopoShape(String shape) {
        super.processTopoShape(shape);

        // If switch ports are not provided use a default value
        int k = (topoShape.length == 1) ?
                DEFAULT_NUMBER_OF_PORTS_PER_SWITCH :
                Integer.parseInt(topoShape[1]);

        // A fat-tree is parametrized by the total number of ports per switch.
        // check top of page 4 of http://web.eecs.umich.edu/~mosharaf/Readings/Fat-Tree.pdf
        kPorts = k;
        numberOfPods = k;
        numberOfCoreLayerSwitches = (k / 2) * (k / 2);
        numberOfAggLayerSwitches = k * k / 2;
        numberOfEdgeLayerSwitches = k * k / 2;

        // need to also change hostCount variable of TopologySimulator
        hostCount = kPorts / 2;
    }


    @Override
    public void setUpTopology() {

        checkArgument(kPorts > 1, "Fat Tree switches must " +
                       "have at **least** 2 ports each!");

        // this is the total number of **Switches**
        // in a fat-tree topology
        deviceCount = numberOfAggLayerSwitches +
                      numberOfCoreLayerSwitches +
                      numberOfEdgeLayerSwitches;

        log.info("Booting a {} with {}-ports, {} switches in total {} pods",
                  topoShape[0], kPorts, deviceCount,  numberOfPods);


        prepareForDeviceEvents(deviceCount);
        createDevices();
        waitForDeviceEvents();

        createLinks();
        createHosts();
    }

    @Override
    protected void createLinks() {

        // For each switch keep a count of used ports
        int[] portList = new int[deviceCount];
        Arrays.fill(portList, 0);

        // we assume that deviceIds stores all the fat tree switches in a flat list
        int end = numberOfPods / 2;
        // from [ 0, (k/2)^2 - 1] we store (k/2)^2 core switces
        int startOfCore = 0;
        // from [ (k/2)^2, (k/2)^2 + (k^2)/2 - 1] we store (k^2)/2 aggregation switches
        int startOfAgg = (numberOfPods / 2) * (numberOfPods / 2);
        // from [ (k/2)^2 + (k^2)/2, (k/2)^2 + (k^2)/2 + (k^2)/2 -1] we store (k^2)/2 edge switches
        int startOfEdge = startOfAgg + (numberOfPods * numberOfPods) / 2;

        log.debug("startOfCore = {}, startOfAgg = {}, startOfEdge = {}",
                  startOfCore, startOfAgg, startOfEdge);

        // Create links between core and aggregation switches
        for (int x = 0; x < numberOfAggLayerSwitches; x += end) {

            // each agg.switch will handle a group of k/2 core consecutive switches
            for (int i = 0; i < end; i += 1) {
                for (int j = 0; j < end; j += 1) {
                    int coreSwitch = i * end + j;
                    int aggSwitch = startOfAgg + x + i;

                    createLink(coreSwitch,
                               aggSwitch,
                               portList[coreSwitch]++,
                               portList[aggSwitch]++);
                }
            }
        }

        // Create links between aggregation and edge switches
        for (int x = 0; x < numberOfAggLayerSwitches; x += end) {
            for (int i = 0; i <  end; i += 1) {
                for (int j = 0; j < end; j += 1) {
                    int aggSwitch = startOfAgg + x + i;
                    int edgeSwitch = startOfEdge + x + j;

                    createLink(aggSwitch,
                               edgeSwitch,
                               portList[aggSwitch]++,
                               portList[edgeSwitch]++);
                }
            }
        }
    }

    @Override
    protected void createHosts() {

        int firstEdgeSwitch = (numberOfPods / 2) * (numberOfPods / 2) +
                              (numberOfPods * numberOfPods) / 2;

        // hosts connect **only** to edge switches, each edge switch has k/2 ports free for hosts
        for (int edgeSwitch = firstEdgeSwitch; edgeSwitch < deviceCount; edgeSwitch++) {

           createHosts(deviceIds.get(edgeSwitch), kPorts / 2);
        }
    }
}
