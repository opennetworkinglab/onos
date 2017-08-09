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
package org.onosproject.gluon.rsc;

/**
 * Gluon application related constants.
 */

public final class GluonConstants {

    protected GluonConstants() {
    }

    /**
     * String constants.
     */
    public static final String KEYS = "/keys";
    public static final String PROTON = "/proton/";
    public static final String MODE_STOP = "stop";
    public static final String MODE_START = "start";
    public static final String ACTION_SET = "set";
    public static final String ACTION_GET = "get";
    public static final String ACTION_DEL = "delete";
    public static final String GLUON_HTTP = "http://";
    public static final String KEY_TYPE = "net-l3vpn";
    public static final String GLUON_ACTION = "action";
    public static final String GLUON_KEY = "key";
    public static final String GLUON_NODE = "node";
    public static final String GLUON_NODES = "nodes";
    public static final String GLUON_VALUE = "value";
    public static final String GLUON_MOD_INDEX = "modifiedIndex";
    public static final String GLUON_CREATE_INDEX = "createdIndex";
    public static final String GLUON_DEFAULT_PORT = "2379";

    /**
     * INFO Constants.
     */
    public static final String BATCH_SERVICE_STATUS =
            "executorBatchService shutdown status: {}";
    public static final String REAL_TIME_SERVICE_STATUS =
            "executorRealTimeService shutdown status: {}";
    public static final String SERVER_RUNNING =
            "Server is already running";
    public static final String ACTIVE_SERVER =
            "Number of active servers: {}";
    public static final String NO_SUBKEYS_AVAIL =
            "No subKeys available. Nothing to smooth";
    public static final String SERVER_STOPPED =
            "Server has stopped successfully";
    public static final String NO_SERVER_AVAIL =
            "Server is unavailable";
    public static final String NO_SERVER_AVAIL_ON_PORT =
            "Server is unavailable on specified port";
    public static final String REAL_TIME_PROCESSING =
            "Started Real time etcd monitoring for {}";
    public static final String BATCH_PROCESSING =
            "Started Batch time etcd monitoring for {}";
    public static final String BATCH_QUERING =
            "Sending Batch time etcd request for {}";
    public static final String BATCH_STOPPED =
            "Stopped Batch time etcd monitoring for {}";
    public static final String REAL_TIME_RECEIVED =
            "Received RealTime etcd monitor data {}";
    public static final String BATCH_RECEIVED =
            "Received batch etcd monitor data {}";
    public static final String SUBKEYS_RECEIVED =
            "Recieved subkeys {}";
    public static final String INVALID_ACTION =
            "Invalid action has been received";
    public static final String DATA_UPDATED =
            "Gluon data updated to network config datastore";
    public static final String DATA_REMOVED =
            "Gluon data removed from network config datastore";
    public static final String SERVER_POOL =
            "Server IP is not available in server pool";
    public static final String PROTON_KEY_SUPPORT =
            "Currently only net-l3vpn type supported";
    public static final String WRONG_INPUT = "Either server is not available " +
            "or wrong input";
    public static final String WRONG_INPUT_TYPE = "Wrong format type";
    public static final String INVALID_MODE = "Invalid mode";
    public static final String WRONG_IP_FORMAT = "Wrong IP address format";
    public static final String INVALID_RANGE = "Wrong port range <1-65535>";
    public static final String PROCESSING_FAILED = "Error occurred while " +
            "processing";

    /**
     * ERROR Constants.
     */
    public static final String E_BATCH_PROCESSING =
            "Batch mode etcd monitor failed with error {}";
    public static final String E_BATCH_PROCESSING_URL =
            "Batch mode etcd monitor failed for {}";
    public static final String E_SUBKEYS_PROCESSING =
            "Error observed while fetching subkeys for {}";
    public static final String E_REAL_TIME_PROCESSING =
            "Real time etcd monitor failed with error {}";
    public static final String E_CLIENT_STOP =
            "http client unable to stop with error {}";

    /**
     * Integer Constants.
     */
    public static final int STATUS_CODE = 200;

}
