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

/**
 * Testing adapter for stored flow entry class.
 */
public class StoredFlowEntryAdapter extends FlowEntryAdapter implements StoredFlowEntry {
    @Override
    public void setLastSeen() {

    }

    @Override
    public void setState(FlowEntryState newState) {

    }

    @Override
    public void setLife(long lifeSecs) {

    }

    @Override
    public void setLife(long life, TimeUnit timeUnit) {

    }

    @Override
    public void setLiveType(FlowLiveType liveType) {

    }

    @Override
    public void setPackets(long packets) {

    }

    @Override
    public void setBytes(long bytes) {

    }
}
