/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.flow;

import java.util.concurrent.TimeUnit;

import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

/**
 * Adapter for the flow entry interface.
 */
public class FlowEntryAdapter implements FlowEntry {
    @Override
    public FlowEntryState state() {
        return FlowEntryState.ADDED;
    }

    @Override
    public long life() {
        return life(SECONDS);
    }

    @Override
    public FlowLiveType liveType() {
        return null;
    }

    @Override
    public long life(TimeUnit timeUnit) {
        return SECONDS.convert(0, timeUnit);
    }

    @Override
    public long packets() {
        return 0;
    }

    @Override
    public long bytes() {
        return 0;
    }

    @Override
    public long lastSeen() {
        return 0;
    }

    @Override
    public int errType() {
        return 0;
    }

    @Override
    public int errCode() {
        return 0;
    }

    @Override
    public FlowId id() {
        return FlowId.valueOf(0);
    }

    @Override
    public GroupId groupId() {
        return new GroupId(3);
    }

    @Override
    public short appId() {
        return 1;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public DeviceId deviceId() {
        return null;
    }

    @Override
    public TrafficSelector selector() {
        return null;
    }

    @Override
    public TrafficTreatment treatment() {
        return null;
    }

    @Override
    public int timeout() {
        return 0;
    }

    @Override
    public int hardTimeout() {
        return 0;
    }

    @Override
    public FlowRule.FlowRemoveReason reason() {
        return FlowRule.FlowRemoveReason.NO_REASON;
    }

    @Override
    public boolean isPermanent() {
        return false;
    }

    @Override
    public int tableId() {
        return 0;
    }

    @Override
    public boolean exactMatch(FlowRule rule) {
        return false;
    }

    @Override
    public FlowRuleExtPayLoad payLoad() {
        return null;
    }

}
