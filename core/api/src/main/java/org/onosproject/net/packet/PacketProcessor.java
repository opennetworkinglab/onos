/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.packet;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Abstraction of an inbound packet processor.
 */
public interface PacketProcessor {

    int ADVISOR_MAX = Integer.MAX_VALUE / 3;
    int DIRECTOR_MAX = (Integer.MAX_VALUE / 3) * 2;
    int OBSERVER_MAX = Integer.MAX_VALUE;

    /**
     * Returns a priority in the ADVISOR range, where processors can take early action and
     * influence the packet context. However, they cannot handle the packet (i.e. call send() or block()).
     * The valid range is from 1 to ADVISOR_MAX.
     * Processors in this range get to see the packet first.
     *
     * @param priority priority within ADVISOR range
     * @return overall priority
     */
    static int advisor(int priority) {
        int overallPriority = priority + 1;
        checkArgument(overallPriority > 0 && overallPriority <= ADVISOR_MAX,
                      "Priority not within ADVISOR range");
        return overallPriority;
    }

    /**
     * Returns a priority in the DIRECTOR range, where processors can handle the packet.
     * The valid range is from ADVISOR_MAX+1 to DIRECTOR_MAX.
     * Processors in this range get to see the packet second, after ADVISORS.
     *
     * @param priority priority within the DIRECTOR range
     * @return overall priority
     */
    static int director(int priority) {
        int overallPriority = ADVISOR_MAX + priority + 1;
        checkArgument(overallPriority > ADVISOR_MAX && overallPriority <= DIRECTOR_MAX,
                      "Priority not within DIRECTOR range");
        return overallPriority;
    }

    /**
     * Returns a priority in the OBSERVER range, where processors cannot take any action,
     * but can observe what action has been taken until then.
     * The valid range is from DIRECTOR_MAX+1 to OBSERVER_MAX.
     * Processors in this range get to see the packet last, after ADVISORS and DIRECTORS.
     *
     * @param priority priority within the OBSERVER range
     * @return overall priority
     */
    static int observer(int priority) {
        int overallPriority = DIRECTOR_MAX + priority + 1;
        checkArgument(overallPriority > DIRECTOR_MAX,
                      "Priority not within OBSERVER range");
        return overallPriority;
    }

    /**
     * Processes the inbound packet as specified in the given context.
     *
     * @param context packet processing context
     */
    void process(PacketContext context);

}
