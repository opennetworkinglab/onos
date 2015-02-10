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
package org.onosproject.net.intent;

import com.google.common.collect.ImmutableList;
import org.onosproject.store.Timestamp;

import java.util.List;
import java.util.Objects;

/**
 * A wrapper class that contains an intents, its state, and other metadata for
 * internal use.
 */
public class IntentData { //FIXME need to make this "immutable"
                          // manager should be able to mutate a local copy while processing
    private Intent intent;

    private IntentState state;
    private Timestamp version;

    private List<Intent> installables;

    public IntentData(Intent intent, IntentState state, Timestamp version) {
        this.intent = intent;
        this.state = state;
        this.version = version;
    }

    // kryo constructor
    protected IntentData() {
    }

    public Intent intent() {
        return intent;
    }

    public IntentState state() {
        return state;
    }

    public Key key() {
        return intent.key();
    }

    public Timestamp version() {
        return version;
    }

    public void setState(IntentState newState) {
        this.state = newState;
    }

    /**
     * Sets the version for this intent data.
     * <p>
     * The store should call this method only once when the IntentData is
     * first passed into the pending map. Ideally, an IntentData is timestamped
     * on the same thread that the called used to submit the intents.
     * </p>
     *
     * @param version the version/timestamp for this intent data
     */
    public void setVersion(Timestamp version) {
        this.version = version;
    }

    public void setInstallables(List<Intent> installables) {
        this.installables = ImmutableList.copyOf(installables);
    }

    public List<Intent> installables() {
        return installables;
    }

    @Override
    public int hashCode() {
        return Objects.hash(intent, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final IntentData other = (IntentData) obj;
        return Objects.equals(this.intent, other.intent)
                && Objects.equals(this.version, other.version);
    }
}
