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
package org.onosproject.yangutils.translator.tojava.javamodel;

import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfoTranslator;
import org.onosproject.yangutils.datamodel.javadatamodel.YangToJavaNamingConflictUtil;

import static org.onosproject.yangutils.translator.tojava.javamodel.AttributesJavaDataType.getJavaDataType;
import static org.onosproject.yangutils.translator.tojava.javamodel.AttributesJavaDataType.getJavaImportClass;
import static org.onosproject.yangutils.translator.tojava.javamodel.AttributesJavaDataType.getJavaImportPackage;

/**
 * Represents java information corresponding to the YANG type.
 *
 * @param <T> generic parameter for YANG java type
 */
public class YangJavaTypeTranslator<T>
        extends YangType<T>
        implements JavaQualifiedTypeResolver {

    private JavaQualifiedTypeInfoTranslator javaQualifiedAccess;

    /**
     * Create a YANG leaf object with java qualified access details.
     */
    public YangJavaTypeTranslator() {
        super();
        setJavaQualifiedInfo(new JavaQualifiedTypeInfoTranslator());
    }

    @Override
    public void updateJavaQualifiedInfo(YangToJavaNamingConflictUtil conflictResolver) {
        JavaQualifiedTypeInfoTranslator importInfo = getJavaQualifiedInfo();

        /*
         * Type is added as an attribute in the class.
         */
        String className = getJavaImportClass(this, false, conflictResolver);
        if (className != null) {
            /*
             * Corresponding to the attribute type a class needs to be imported,
             * since it can be a derived type or a usage of wrapper classes.
             */
            importInfo.setClassInfo(className);
            String classPkg = getJavaImportPackage(this,
                    false,  conflictResolver);
            if (classPkg == null) {
                throw new TranslatorException("import package cannot be null when the class is used");
            }
            importInfo.setPkgInfo(classPkg);
        } else {
            /*
             * The attribute does not need a class to be imported, for example
             * built in java types.
             */
            String dataTypeName = getJavaDataType(this);
            if (dataTypeName == null) {
                throw new TranslatorException("not supported data type");
            }
            importInfo.setClassInfo(dataTypeName);
        }
        setJavaQualifiedInfo(importInfo);
    }

    @Override
    public JavaQualifiedTypeInfoTranslator getJavaQualifiedInfo() {
        return javaQualifiedAccess;
    }

    @Override
    public void setJavaQualifiedInfo(JavaQualifiedTypeInfoTranslator typeInfo) {
        javaQualifiedAccess = typeInfo;
    }
}
