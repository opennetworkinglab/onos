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
    public void replace(IntentId oldIntentId, Intent newIntent) {

    }

    @Override
    public void execute(IntentOperations operations) {

    }

    @Override
    public Iterable<Intent> getIntents() {
        return null;
    }

    @Override
    public long getIntentCount() {
        return 0;
    }

    @Override
    public Intent getIntent(IntentId id) {
        return null;
    }

    @Override
    public IntentState getIntentState(IntentId id) {
        return null;
    }

    @Override
    public List<Intent> getInstallableIntents(IntentId intentId) {
        return null;
    }

    @Override
    public void addListener(IntentListener listener) {

    }

    @Override
    public void removeListener(IntentListener listener) {

    }
}
