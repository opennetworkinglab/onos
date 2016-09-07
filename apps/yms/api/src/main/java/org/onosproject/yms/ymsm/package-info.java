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
 * Provides interfaces to YANG application management system manager. YMSM is
 * manager of the YANG Core.
 *
 * In NBI, it acts as a broker in between the protocol and application,
 * here there is a separate protocol implementation, which does the conversion
 * of protocol representation to abstract data tree. The protocol
 * implementation takes care of the protocol specific actions for
 * e.g. RESTCONF handling the entity-tag / timestamp related operations.
 *
 * In SBI, driver or provider uses YANG codec handler as a utility to translate
 * the request information in java(YANG utils generated) to protocol specific
 * format and vice versa.
 */
package org.onosproject.yms.ymsm;
