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
     * Sets the XOS API access information to the client service.
     *
     * @return true if it is set and authenticated, otherwise false
     */
    boolean setAccess(XosAccess xosAccess);

    /**
     * Returns CORD VTN service API.
     *
     * @return cord vtn service api
     */
    VtnServiceApi vtnServiceApi();

    /*
     * adds more XOS service APIs below.
     */
}
