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

package org.onosproject.store.intent.impl;

import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.Key;

import java.util.List;

/**
 * Adapter for IntentStore.
 */
public class IntentStoreAdapter implements IntentStore {
    @Override
    public long getIntentCount() {
        return 0;
    }

    @Override
    public Iterable<Intent> getIntents() {
        return null;
    }

    @Override
    public Iterable<IntentData> getIntentData(boolean localOnly, long olderThan) {
        return null;
    }

    @Override
    public IntentState getIntentState(Key intentKey) {
        return null;
    }

    @Override
    public List<Intent> getInstallableIntents(Key intentKey) {
        return null;
    }

    @Override
    public void write(IntentData newData) {

    }

    @Override
    public void batchWrite(Iterable<IntentData> updates) {

    }

    @Override
    public Intent getIntent(Key key) {
        return null;
    }

    @Override
    public IntentData getIntentData(Key key) {
        return null;
    }

    @Override
    public void addPending(IntentData intent) {

    }

    @Override
    public boolean isMaster(Key intentKey) {
        return false;
    }

    @Override
    public Iterable<Intent> getPending() {
        return null;
    }

    @Override
    public Iterable<IntentData> getPendingData() {
        return null;
    }

    @Override
    public IntentData getPendingData(Key intentKey) {
        return null;
    }

    @Override
    public Iterable<IntentData> getPendingData(boolean localOnly, long olderThan) {
        return null;
    }

    @Override
    public void setDelegate(IntentStoreDelegate delegate) {

    }

    @Override
    public void unsetDelegate(IntentStoreDelegate delegate) {

    }

    @Override
    public boolean hasDelegate() {
        return false;
    }
}
