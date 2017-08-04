/*
 *  Copyright 2016-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * Server-side modeling of Topology View UI entities.
 * <p>
 * The classes in this package are thin wrappers around core model objects
 * (where possible). Together, they provide a model of what the user can
 * "see" in the UI, and how the user can interact with that (visual) model.
 * <p>
 * Of note: the {@link org.onosproject.ui.model.topo.UiLink} is a
 * representation of a "bi-directional" link that is backed by two
 * "uni-directional" core model
 * {@link org.onosproject.net.Link} objects.
 */
package org.onosproject.ui.model.topo;