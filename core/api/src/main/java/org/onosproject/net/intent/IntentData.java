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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.Timestamp;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper class that contains an intents, its state, and other metadata for
 * internal use.
 */
public class IntentData { //FIXME need to make this "immutable"
                          // manager should be able to mutate a local copy while processing
    private final Intent intent;

    private IntentState state;
    private Timestamp version;
    private NodeId origin;

    private List<Intent> installables;

    /**
     * Creates a new intent data object.
     *
     * @param intent intent this metadata references
     * @param state intent state
     * @param version version of the intent for this key
     */
    public IntentData(Intent intent, IntentState state, Timestamp version) {
        this.intent = intent;
        this.state = state;
        this.version = version;
    }

    /**
     * Copy constructor.
     *
     * @param intentData intent data to copy
     */
    public IntentData(IntentData intentData) {
        checkNotNull(intentData);

        intent = intentData.intent;
        state = intentData.state;
        version = intentData.version;
        origin = intentData.origin;
        installables = intentData.installables;
    }

    // kryo constructor
    protected IntentData() {
        intent = null;
    }

    /**
     * Returns the intent this metadata references.
     *
     * @return intent
     */
    public Intent intent() {
        return intent;
    }

    /**
     * Returns the state of the intent.
     *
     * @return intent state
     */
    public IntentState state() {
        return state;
    }

    /**
     * Returns the intent key.
     *
     * @return intent key
     */
    public Key key() {
        return intent.key();
    }

    /**
     * Returns the version of the intent for this key.
     *
     * @return intent version
     */
    public Timestamp version() {
        return version;
    }

    /**
     * Sets the origin, which is the node that created the intent.
     *
     * @param origin origin instance
     */
    public void setOrigin(NodeId origin) {
        this.origin = origin;
    }

    /**
     * Returns the origin node that created this intent.
     *
     * @return origin node ID
     */
    public NodeId origin() {
        return origin;
    }

    /**
     * Updates the state of the intent to the given new state.
     *
     * @param newState new state of the intent
     */
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

    /**
     * Sets the intent installables to the given list of intents.
     *
     * @param installables list of installables for this intent
     */
    public void setInstallables(List<Intent> installables) {
        this.installables = ImmutableList.copyOf(installables);
    }

    /**
     * Returns the installables associated with this intent.
     *
     * @return list of installable intents
     */
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("key", key())
                .add("state", state())
                .add("version", version())
                .add("intent", intent())
                .add("origin", origin())
                .add("installables", installables())
                .toString();
    }

}
