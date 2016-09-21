/*
 * Copyright 2015-present Open Networking Laboratory
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

import java.util.List;

/**
 * Service for registering user interface extensions.
 */
public interface UiExtensionService {

    /**
     * Registers the specified user interface extension.
     *
     * @param extension UI extension to register
     */
    void register(UiExtension extension);

    /**
     * Unregisters the specified user interface extension.
     *
     * @param extension UI extension to unregister
     */
    void unregister(UiExtension extension);

    /**
     * Returns the list of registered user interface extensions.
     *
     * @return list of extensions
     */
    List<UiExtension> getExtensions();

    /**
     * Returns the user interface extension that contributed the specified view.
     *
     * @param viewId view identifier
     * @return contributing user interface extension
     */
    UiExtension getViewExtension(String viewId);
}
