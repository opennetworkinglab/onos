/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.drivers.corsa;

import com.google.common.annotations.Beta;
import org.onosproject.driver.query.FullVlanAvailable;

/**
 * Driver which always responds that all VLAN IDs are available for the Device.
 *
 * FIXME
 * To avoid CorsaFullVlanAvailable.
 *
 * OSGi: help bundle plugin discover runtime package dependency.
 * <pre>
 * <code>
 *  Remember to add the tag: SuppressWarnings("unused")
 *  private FullVlanAvailable fullVlans;
 * </code>
 * </pre>
 */


@Beta
public class CorsaFullVlanAvailable extends FullVlanAvailable {

}
