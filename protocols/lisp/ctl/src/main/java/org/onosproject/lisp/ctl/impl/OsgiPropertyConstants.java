/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.lisp.ctl.impl;

/**
 * Constants for default values of configurable properties.
 */
public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {}

    public static final String LISP_AUTH_KEY = "lispAuthKey";
    public static final String LISP_AUTH_KEY_DEFAULT = "onos";

    public static final String LISP_AUTH_KEY_ID = "lispAuthKeyId";
    public static final int LISP_AUTH_KEY_ID_DEFAULT = 1;

    public static final String ENABLE_SMR = "enableSmr";
    public static final boolean ENABLE_SMR_DEFAULT = false;

}
