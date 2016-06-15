/*Copyright 2016.year Open Networking Laboratory

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package org.onosproject.yangutils.datamodel;

/**
 * Abstraction of error message and application info processing.
 */
public interface YangAppErrorInfo {

    /**
     * Returns the application's error message for data error.
     *
     * @return application's error message for data error.
     */
    String getGetErrorMessage();

    /**
     * Sets the application's error message for data error.
     *
     * @param errorMessage application's error message for data error.
     */
    void setErrorMessage(String errorMessage);

    /**
     * Returns the application's error tag for data error.
     *
     * @return application's error tag for data error.
     */
    String getGetErrorAppTag();

    /**
     * Sets the application's error tag for data error.
     *
     * @param errorMessage application's error tag for data error.
     */
    void setErrorAppTag(String errorMessage);
}
