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

import org.onosproject.yms.app.yob.exception.YobExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.yms.app.yob.YobConstants.BUILDER_IS_NOT_SET;
import static org.onosproject.yms.app.yob.YobConstants.BUILT_OBJ_IS_NOT_SET;
import static org.onosproject.yms.app.yob.YobConstants.FAIL_TO_CREATE_OBJ;
import static org.onosproject.yms.app.yob.YobConstants.FAIL_TO_LOAD_CLASS;
import static org.onosproject.yms.app.yob.YobConstants.OBJ_BUILDING_WITHOUT_BUILDER;
import static org.onosproject.yms.app.yob.YobConstants.OBJ_IS_ALREADY_BUILT_NOT_BUILD;
import static org.onosproject.yms.app.yob.YobConstants.OBJ_IS_ALREADY_BUILT_NOT_FETCH;
import static org.onosproject.yms.app.yob.YobConstants.OBJ_IS_NOT_SET_NOT_FETCH;
import static org.onosproject.yms.app.yob.YobConstants.REFLECTION_FAIL_TO_CREATE_OBJ;

/**
 * Represents the container of YANG object being built or the builder.
 */
class YobBuilderOrBuiltObject {
    private static final Logger log = LoggerFactory.getLogger(YobWorkBench.class);

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

    YobBuilderOrBuiltObject(String qualifiedClassName,
                            ClassLoader registeredAppClassLoader) {
        try {
            yangDefaultClass =
                    registeredAppClassLoader.loadClass(qualifiedClassName);
            yangBuilderClass = yangDefaultClass.getDeclaredClasses()[0];
            builderOrBuiltObject = yangBuilderClass.newInstance();
        } catch (ClassNotFoundException e) {
            log.error(FAIL_TO_LOAD_CLASS + qualifiedClassName);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error(FAIL_TO_CREATE_OBJ + qualifiedClassName);
        } catch (NullPointerException e) {
            log.error(REFLECTION_FAIL_TO_CREATE_OBJ + qualifiedClassName);
        }
    }

    /**
     * Returns the builder object if it is set.
     *
     * @return builder object
     * @throws YobExceptions if builder object is not available
     */
    Object getBuilderObject() {
        if (isBuilt) {
            throw new YobExceptions(OBJ_IS_ALREADY_BUILT_NOT_FETCH);
        }

        if (builderOrBuiltObject == null) {
            throw new YobExceptions(BUILDER_IS_NOT_SET);
        }

        return builderOrBuiltObject;
    }

    /**
     * Returns the built object.
     *
     * @return built object
     * @throws YobExceptions if built object is not available
     */
    Object getBuiltObject() {
        if (!isBuilt) {
            throw new YobExceptions(OBJ_IS_NOT_SET_NOT_FETCH);
        }

        if (builderOrBuiltObject == null) {
            throw new YobExceptions(BUILT_OBJ_IS_NOT_SET);
        }

        return builderOrBuiltObject;
    }

    /**
     * Check if the built object is being initialized for the 1st time and
     * set it.
     *
     * @param builtObject new built object
     * @throws YobExceptions if builder or built object is not available
     */
    void setBuiltObject(Object builtObject) {
        if (isBuilt) {
            throw new YobExceptions(OBJ_IS_ALREADY_BUILT_NOT_BUILD);
        }

        if (builderOrBuiltObject == null) {
            throw new YobExceptions(OBJ_BUILDING_WITHOUT_BUILDER);
        }

        isBuilt = true;
        builderOrBuiltObject = builtObject;
    }
}
