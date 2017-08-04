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
package org.onosproject.vpls.store;

import com.google.common.collect.Maps;
import org.onosproject.store.StoreDelegate;
import org.onosproject.vpls.api.VplsData;
import org.onosproject.vpls.api.VplsStore;

import java.util.Collection;
import java.util.Map;

/**
 * Test adapter for VPLS store.
 */
public class VplsStoreAdapter implements VplsStore {
    protected Map<String, VplsData> vplsDataMap;
    protected StoreDelegate<VplsStoreEvent> delegate;

    public VplsStoreAdapter() {
        vplsDataMap = Maps.newHashMap();
    }

    @Override
    public void setDelegate(StoreDelegate<VplsStoreEvent> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void unsetDelegate(StoreDelegate<VplsStoreEvent> delegate) {
        this.delegate = null;
    }

    @Override
    public boolean hasDelegate() {
        return this.delegate != null;
    }

    @Override
    public void addVpls(VplsData vplsData) {
        vplsDataMap.put(vplsData.name(), vplsData);
    }

    @Override
    public void removeVpls(VplsData vplsData) {
        vplsDataMap.remove(vplsData.name());
    }

    @Override
    public void updateVpls(VplsData vplsData) {
        vplsDataMap.put(vplsData.name(), vplsData);
    }

    @Override
    public VplsData getVpls(String vplsName) {
        return vplsDataMap.get(vplsName);
    }

    @Override
    public Collection<VplsData> getAllVpls() {
        return vplsDataMap.values();
    }
}
