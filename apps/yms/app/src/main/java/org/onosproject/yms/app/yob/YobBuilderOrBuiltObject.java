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

package org.onosproject.yms.app.yob;

import org.onosproject.yms.app.yob.exception.YobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.yms.app.yob.YobConstants.E_BUILDER_IS_NOT_ALREADY_SET;
import static org.onosproject.yms.app.yob.YobConstants.E_BUILDER_IS_NOT_SET;
import static org.onosproject.yms.app.yob.YobConstants.E_BUILT_OBJ_IS_NOT_SET;
import static org.onosproject.yms.app.yob.YobConstants.E_FAIL_TO_CREATE_OBJ;
import static org.onosproject.yms.app.yob.YobConstants.E_FAIL_TO_LOAD_CLASS;
import static org.onosproject.yms.app.yob.YobConstants.E_OBJ_BUILDING_WITHOUT_BUILDER;
import static org.onosproject.yms.app.yob.YobConstants.E_OBJ_IS_ALREADY_BUILT_NOT_BUILD;
import static org.onosproject.yms.app.yob.YobConstants.E_OBJ_IS_ALREADY_BUILT_NOT_FETCH;
import static org.onosproject.yms.app.yob.YobConstants.E_OBJ_IS_ALREADY_BUILT_NOT_SET;
import static org.onosproject.yms.app.yob.YobConstants.E_OBJ_IS_NOT_SET_NOT_FETCH;
import static org.onosproject.yms.app.yob.YobConstants.E_REFLECTION_FAIL_TO_CREATE_OBJ;
import static org.onosproject.yms.app.yob.YobConstants.L_FAIL_TO_CREATE_OBJ;
import static org.onosproject.yms.app.yob.YobConstants.L_FAIL_TO_LOAD_CLASS;
import static org.onosproject.yms.app.yob.YobConstants.L_REFLECTION_FAIL_TO_CREATE_OBJ;

/**
 * Represents the container of YANG object being built or the builder.
 */
class YobBuilderOrBuiltObject {
    private static final Logger log =
            LoggerFactory.getLogger(YobWorkBench.class);

    /**
     * Is the contained object a built object.
     */
    private boolean isBuilt;

    /**
     * Builder or built object.
     */
    private Object builderOrBuiltObject;

    /**
     * Default / op param builder class.
     */
    Class<?> yangBuilderClass;

    /**
     * Default Class.
     */
    Class<?> yangDefaultClass;

    /**
     * Create Node Object holder.
     *
     * @param qualifiedClassName       name of the class
     * @param registeredAppClassLoader class loader to be used
     * @throws YobException if failed to create the node object
     */
    YobBuilderOrBuiltObject(String qualifiedClassName,
                            ClassLoader registeredAppClassLoader) {
        try {
            yangDefaultClass =
                    registeredAppClassLoader.loadClass(qualifiedClassName);
            yangBuilderClass = yangDefaultClass.getDeclaredClasses()[0];
            setBuilderObject(yangBuilderClass.newInstance());
        } catch (ClassNotFoundException e) {
            log.error(L_FAIL_TO_LOAD_CLASS, qualifiedClassName);
            throw new YobException(E_FAIL_TO_LOAD_CLASS + qualifiedClassName);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error(L_FAIL_TO_CREATE_OBJ, qualifiedClassName);
            throw new YobException(E_FAIL_TO_CREATE_OBJ + qualifiedClassName);
        } catch (NullPointerException e) {
            log.error(L_REFLECTION_FAIL_TO_CREATE_OBJ, qualifiedClassName);
            throw new YobException(E_REFLECTION_FAIL_TO_CREATE_OBJ +
                                           qualifiedClassName);
        }
    }

    /**
     * Returns the builder object if it is set.
     *
     * @return builder object
     * @throws YobException if builder is not available
     */
    Object getBuilderObject() {
        if (isBuilt) {
            throw new YobException(E_OBJ_IS_ALREADY_BUILT_NOT_FETCH);
        }

        if (builderOrBuiltObject == null) {
            throw new YobException(E_BUILDER_IS_NOT_SET);
        }

        return builderOrBuiltObject;
    }

    /**
     * Check if the builder object is being initialized for the first time and
     * set it.
     *
     * @param builderObject new builder object
     * @throws YobException if built object is not available
     */
    private void setBuilderObject(Object builderObject) {
        if (isBuilt) {
            throw new YobException(E_OBJ_IS_ALREADY_BUILT_NOT_SET);
        }

        if (builderOrBuiltObject != null) {
            throw new YobException(E_BUILDER_IS_NOT_ALREADY_SET);
        }

        builderOrBuiltObject = builderObject;
    }

    /**
     * Returns the built object.
     *
     * @return built object
     * @throws YobException if built object is not available or if it is not
     *                      built
     */
    Object getBuiltObject() {
        if (!isBuilt) {
            throw new YobException(E_OBJ_IS_NOT_SET_NOT_FETCH);
        }

        if (builderOrBuiltObject == null) {
            throw new YobException(E_BUILT_OBJ_IS_NOT_SET);
        }

        return builderOrBuiltObject;
    }

    /**
     * Check if the built object is being initialized for the 1st time and
     * set it.
     *
     * @param builtObject new built object
     * @throws YobException if builder object is not available or if it is
     *                      already built
     */
    void setBuiltObject(Object builtObject) {
        if (isBuilt) {
            throw new YobException(E_OBJ_IS_ALREADY_BUILT_NOT_BUILD);
        }

        if (builderOrBuiltObject == null) {
            throw new YobException(E_OBJ_BUILDING_WITHOUT_BUILDER);
        }

        isBuilt = true;
        builderOrBuiltObject = builtObject;
    }
}
