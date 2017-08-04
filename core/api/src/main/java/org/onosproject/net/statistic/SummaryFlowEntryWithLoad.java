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

package org.onosproject.net.statistic;

import org.onosproject.net.ConnectPoint;

/**
 * Summary Load classified by flow live type.
 */
public class SummaryFlowEntryWithLoad {
    private ConnectPoint cp;
    private Load totalLoad;
    private Load immediateLoad;
    private Load shortLoad;
    private Load midLoad;
    private Load longLoad;
    private Load unknownLoad;

    /**
     * Creates a new summary flow entry having load for the given connect point and total load.
     *
     * @param cp        connect point
     * @param totalLoad total load
     */
    public SummaryFlowEntryWithLoad(ConnectPoint cp, Load totalLoad) {
        this.cp = cp;
        this.totalLoad = totalLoad;
        this.immediateLoad = new DefaultLoad();
        this.shortLoad = new DefaultLoad();
        this.midLoad = new DefaultLoad();
        this.longLoad = new DefaultLoad();
        this.unknownLoad = new DefaultLoad();
    }

    /**
     * Creates a new summary flow entry having load for the given connect point
     * and total, immediate, short, mid, and long load.
     *
     * @param cp        connect point
     * @param totalLoad total load
     * @param immediateLoad immediate load
     * @param shortLoad short load
     * @param midLoad mid load
     * @param longLoad long load
     */
    public SummaryFlowEntryWithLoad(ConnectPoint cp,
                                    Load totalLoad, Load immediateLoad, Load shortLoad, Load midLoad, Load longLoad) {
        this.cp = cp;
        this.totalLoad = totalLoad;
        this.immediateLoad = immediateLoad;
        this.shortLoad = shortLoad;
        this.midLoad = midLoad;
        this.longLoad = longLoad;
        this.unknownLoad = new DefaultLoad();
    }

    /**
     * Creates a new summary flow entry having load for the given connect point
     * and total, immediate, short, mid, long, and unknown load.
     *
     * @param cp        connect point
     * @param totalLoad total load
     * @param immediateLoad immediate load
     * @param shortLoad short load
     * @param midLoad mid load
     * @param longLoad long load
     * @param unknownLoad long load
     */
    public SummaryFlowEntryWithLoad(ConnectPoint cp,
                                    Load totalLoad, Load immediateLoad,
                                    Load shortLoad, Load midLoad, Load longLoad, Load unknownLoad) {
        this.cp = cp;
        this.totalLoad = totalLoad;
        this.immediateLoad = immediateLoad;
        this.shortLoad = shortLoad;
        this.midLoad = midLoad;
        this.longLoad = longLoad;
        this.unknownLoad = unknownLoad;
    }

    /**
     * Returns connect point.
     *
     * @return connect point
     */
    public ConnectPoint connectPoint() {
        return cp;
    }

    /**
     * Returns total load of connect point.
     *
     * @return total load
     */
    public Load totalLoad() {
        return totalLoad;
    }

    /**
     * Returns immediate load of connect point.
     *
     * @return immediate load
     */
    public Load immediateLoad() {
        return immediateLoad;
    }

    /**
     * Returns short load of connect point.
     *
     * @return short load
     */
    public Load shortLoad() {
        return shortLoad;
    }

    /**
     * Returns mid load of connect point.
     *
     * @return mid load
     */
    public Load midLoad() {
        return midLoad;
    }

    /**
     * Returns long load of connect point.
     *
     * @return long load
     */
    public Load longLoad() {
        return longLoad;
    }

    /**
     * Returns unknown load of connect point.
     *
     * @return unknown load
     */
    public Load unknownLoad() {
        return unknownLoad;
    }
}
