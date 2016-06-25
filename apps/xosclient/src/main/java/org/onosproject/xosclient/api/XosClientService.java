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
package org.onosproject.xosclient.api;

/**
 * Provides interactions with XOS.
 */
public interface XosClientService {

    /**
     * Returns XOS API access information of the client.
     *
     * @return xos access
     */
    XosAccess access();

    /**
     * Returns XOS client with access.
     *
     * @param xosAccess xos access information
     * @return xos client; null if access fails authentication
     */
    XosClientService getClient(XosAccess xosAccess);

    /**
     * Returns CORD VTN service and service dependency API.
     *
     * @return cord vtn service api
     */
    VtnServiceApi vtnService();

    /**
     * Returns CORD VTN port API.
     *
     * @return cord vtn port api
     */
    VtnPortApi vtnPort();

    /*
     * adds more XOS service APIs below.
     */
}
