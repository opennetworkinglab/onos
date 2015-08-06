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
package org.onosproject.segmentrouting;

import org.onosproject.net.Link;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkCostFunctions {

    private static Logger log = LoggerFactory.getLogger(LinkCostFunctions.class);
    private HashMap<Link, LinkStatsService.LinkStats> prevLinkStatsMapper =
                                    new HashMap<Link, LinkStatsService.LinkStats>();

    /* Returns folw rates for each link in unit of bytes/sec */
    public HashMap<Link, Double> flowRates(
                    HashMap<Link, LinkStatsService.LinkStats> linkStatsMapper) {

        HashMap<Link, Double> flowRatesMapper = new HashMap<Link, Double>();

        for (Link link : linkStatsMapper.keySet()) {
            LinkStatsService.LinkStats linkStats = linkStatsMapper
                                                   .get(link);
            double rate = 0.0;
            if (prevLinkStatsMapper.containsKey(link)) {
                LinkStatsService.LinkStats prevLinkStats = prevLinkStatsMapper
                                                           .get(link);
                if (linkStats.durationInNanosec() - prevLinkStats.durationInNanosec() != 0) {
                    rate = (linkStats.bytesTransferred() - prevLinkStats.bytesTransferred()) /
                                (1e-9 * (linkStats.durationInNanosec() -
                                         prevLinkStats.durationInNanosec()));
                }
            } else {
                if (linkStats.durationInNanosec() != 0) {
                    rate = linkStats.bytesTransferred() /
                                (linkStats.durationInNanosec() * 1e-9);
                }
            }
            flowRatesMapper.put(link, rate);
        }
        prevLinkStatsMapper.clear();
        prevLinkStatsMapper.putAll(linkStatsMapper);

        return flowRatesMapper;
    }
}
