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

package org.onosproject.net.intent;

import org.onosproject.core.ApplicationId;

import java.util.List;

/**
 *  Test adapter for intent service.
 */
public class IntentServiceAdapter implements IntentService {
    @Override
    public void submit(Intent intent) {

    }

    @Override
    public void withdraw(Intent intent) {

    }

    @Override
    public void purge(Intent intent) {

    }

    @Override
    public Iterable<Intent> getIntents() {
        return null;
    }

    @Override
    public Iterable<Intent> getIntentsByAppId(ApplicationId id) {
        return null;
    }

    @Override
    public void addPending(IntentData intentData) {

    }

    @Override
    public Iterable<IntentData> getIntentData() {
        return null;
    }

    @Override
    public long getIntentCount() {
        return 0;
    }

    @Override
    public Intent getIntent(Key intentKey) {
        return null;
    }

    @Override
    public IntentState getIntentState(Key intentKey) {
        return IntentState.INSTALLED;
    }

    @Override
    public List<Intent> getInstallableIntents(Key intentKey) {
        return null;
    }

    @Override
    public boolean isLocal(Key intentKey) {
        return false;
    }

    @Override
    public Iterable<Intent> getPending() {
        return null;
    }

    @Override
    public void addListener(IntentListener listener) {

    }

    @Override
    public void removeListener(IntentListener listener) {

    }
}
