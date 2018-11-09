/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.dpi;

import java.util.List;

/**
 * Service for DPI Statistics Service Manager.
 */
public interface DpiStatisticsManagerService {
    /**
     * Get the latest DpiStatistics in the Store list.
     *
     * @return the DpiStatistics object class or null if not exist
     */
    DpiStatistics getDpiStatisticsLatest();

    /**
     * Get the latest DpiStatistics in the Store list.
     *
     * @param topnProtocols detected topn protocols, default = 100
     * @param topnFlows detected topn known and unknown flows , default = 100
     *
     * @return the DpiStatistics object class or null if not exist
     */
    DpiStatistics getDpiStatisticsLatest(int topnProtocols, int topnFlows);

    /**
     * Gets the last N(Max = 100) DpiStatistics in the Store list.
     *
     * @param lastN maximum number to fetch
     * @return the List of DpiStatistics object class
     */
    List<DpiStatistics> getDpiStatistics(int lastN);

    /**
     * Gets the last N(Max = 100) DpiStatistics in the Store list.
     *
     * @param lastN latest N entries
     * @param topnProtocols detected topn protocols, default = 100
     * @param topnFlows detected topn known and unknown flows , default = 100
     * @return the List of DpiStatistics object class
     */
    List<DpiStatistics> getDpiStatistics(int lastN, int topnProtocols, int topnFlows);

    /**
     * Get the specified receivedTime DpiStatistics in the Store list.
     *
     * @param receivedTime receivedTime string with format "yyyy-MM-dd HH:mm:ss"
     * @return the DpiStatistics object class or null if not exist
     */
    DpiStatistics getDpiStatistics(String receivedTime);

    /**
     * Get the specified receivedTime DpiStatistics in the Store list.
     *
     * @param receivedTime receivedTime string with format "yyyy-MM-dd HH:mm:ss"
     * @param topnProtocols detected topn protocols, default = 100
     * @param topnFlows detected topn known and unknown flows , default = 100
     * @return the DpiStatistics object class or null if not exist
     */
    DpiStatistics getDpiStatistics(String receivedTime, int topnProtocols, int topnFlows);

    /**
     * Adds DpiStatistics at the end of the Store list.
     *
     * @param ds statistics to add
     * @return the added DpiStatistics object class
     */
    DpiStatistics addDpiStatistics(DpiStatistics ds);

}
