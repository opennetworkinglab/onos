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

/**
 * In SBI, the provider or driver uses YANG management system as a CODEC
 * utility. These providers/drivers use the YANG codec utility to register
 * the device schema. YANG utils is used to generate the java files
 * corresponding to the device schema. Provider or driver use these classes
 * to seamlessly manage the device as java objects. While sending the request
 * to device, drivers use the utility to translate the objects to protocol
 * specific data representation and then send to the device.
 * Protocol or driver use the same instance of the codec utility across multiple
 * translation request.
 * Protocol or driver should not use the same instance of utility concurrently.
 */
package org.onosproject.yms.ych;
