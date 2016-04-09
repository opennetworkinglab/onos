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

/**
 * Set of facilities to allow the platform to be extended with
 * device specific behaviours and to allow modeling device (and other entity)
 * behaviours while hiding details of specific driver implementations.
 * While primarily intended for devices, this subsystem can be used to abstract
 * behaviours of other entities as well.
 * <p>
 * {@link org.onosproject.net.driver.Driver} is a representation of a
 * specific family of entities (devices, links, etc.) which supports set of
 * {@link org.onosproject.net.driver.Behaviour behaviour classes}. Default
 * implementation is provided by the platform and allows DriverProviders to
 * add different behaviour implementations via DriverService.
 * </p>
 * <p>
 * {@link org.onosproject.net.driver.DriverData} is a container for data
 * learned about an entity. It is associated with a specific
 * {@link org.onosproject.net.driver.Driver}
 * and provides set of {@link org.onosproject.net.driver.Behaviour behaviours}
 * for talking about an entity. A default
 * implementation provided by platform and has mutable key/value store for use by
 * implementations of {@link org.onosproject.net.driver.Behaviour behaviours}.
 * </p>
 * <p>
 * {@link org.onosproject.net.driver.DriverHandler} is an entity used as a
 * context to interact with a device. It has a peer
 * {@link org.onosproject.net.driver.DriverData} instance, which is used to
 * store information learned about a device. It also
 * provides set of {@link org.onosproject.net.driver.Behaviour behaviours}
 * for talking to a device.
 * </p>
 * <p>
 * {@link org.onosproject.net.driver.DriverService} can be used to query the
 * inventory of device drivers and their behaviours, while the
 * {@link org.onosproject.net.driver.DriverAdminService} allows adding/removing
 * drivers and managing behaviour implementations.
 * {@link org.onosproject.net.driver.DriverProvider} is an entity capable
 * of add/removing drivers and supplying and managing behaviour
 * implementations. A default implementation is provided by the framework along
 * with a {@link org.onosproject.net.driver.XmlDriverLoader loader utility} to
 * create a driver provider from an XML file structured as follows:
 * <pre>
 *     &lt;drivers&gt;
 *         &lt;driver name=“...” [manufacturer="..." hwVersion="..." swVersion="..."]&gt;
 *             &lt;behaviour api="..." impl="..."/&gt;
 *             ...
 *             [&lt;property name=“key”&gt;value&lt;/key&gt;]
 *             ...
 *         &lt;/driver&gt;
 *         ...
 *     &lt;/drivers&gt;
 * </pre>
 *
 */
package org.onosproject.net.driver;