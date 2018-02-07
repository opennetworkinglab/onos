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
package org.onosproject.ui;

import org.onosproject.ui.lion.LionBundle;

import java.util.List;

/**
 * Adapter for testing against UI extension service.
 */
public class UiExtensionServiceAdapter implements UiExtensionService {
    @Override
    public void register(UiExtension extension) {
    }

    @Override
    public void unregister(UiExtension extension) {
    }

    @Override
    public List<UiExtension> getExtensions() {
        return null;
    }

    @Override
    public UiExtension getViewExtension(String viewId) {
        return null;
    }

    @Override
    public LionBundle getNavLionBundle() {
        return null;
    }

    @Override
    public void refreshModel() {
    }
}
