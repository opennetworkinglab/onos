/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.upgrade.impl;

import org.onosproject.core.Version;
import org.onosproject.upgrade.Upgrade;
import org.onosproject.upgrade.UpgradeEventListener;
import org.onosproject.upgrade.UpgradeService;

/**
 * Upgrade service adapter.
 */
public class UpgradeServiceAdapter implements UpgradeService {
    @Override
    public boolean isUpgrading() {
        return false;
    }

    @Override
    public Upgrade getState() {
        return null;
    }

    @Override
    public Version getVersion() {
        return null;
    }

    @Override
    public boolean isLocalActive() {
        return false;
    }

    @Override
    public boolean isLocalUpgraded() {
        return false;
    }

    @Override
    public void addListener(UpgradeEventListener listener) {

    }

    @Override
    public void removeListener(UpgradeEventListener listener) {

    }
}
