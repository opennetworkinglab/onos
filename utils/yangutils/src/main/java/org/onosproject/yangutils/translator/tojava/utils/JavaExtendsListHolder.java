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

package org.onosproject.yangutils.translator.tojava.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.JavaImportData;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_SERVICE_AND_MANAGER;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.getTempJavaFragement;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_CLOSE_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_OPEN_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_LISTENER_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EXTEND;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_REG;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;

/**
 * Represent the extends list for generated java classes. It holds the class details which needs
 * to be extended by the generated java code.
 */
public class JavaExtendsListHolder {

    /**
     * Creates an instance of JavaExtendsListHolder.
     */
    public JavaExtendsListHolder() {
        setExtendedClassStore(new HashMap<>());
        setExtendsList(new ArrayList<>());
    }

    private Map<JavaQualifiedTypeInfo, Boolean> extendedClassStore;
    private List<JavaQualifiedTypeInfo> extendsList;

    /**
     * Returns extends list.
     *
     * @return extends list
     */
    public Map<JavaQualifiedTypeInfo, Boolean> getExtendedClassStore() {
        return extendedClassStore;
    }

    /**
     * Sets extends list.
     *
     * @param extendsList list of classes need to be extended
     */
    private void setExtendedClassStore(Map<JavaQualifiedTypeInfo, Boolean> extendedClass) {
        this.extendedClassStore = extendedClass;
    }

    /**
     * Adds to the extends list.
     *
     * @param info java file info
     * @param node YANG node
     */
    public void addToExtendsList(JavaQualifiedTypeInfo info, YangNode node) {
        JavaFileInfo fileInfo = ((JavaFileInfoContainer) node).getJavaFileInfo();

        if (!fileInfo.getPackage().equals(info.getPkgInfo())) {
            JavaImportData importData = getTempJavaFragement(node).getJavaImportData();
            importData.addImportInfo(info);
            /*true means import should be added*/
            getExtendedClassStore().put(info, true);
        }
        getExtendedClassStore().put(info, false);
        addToExtendsList(info);
    }

    /**
     * Returns extends string for class.
     *
     * @param genFileType generated file type
     * @param className class name
     * @param isNotificationPresent if notification node is present
     * @return extends string
     */
    public String getExtendsString(int genFileType, String className, boolean isNotificationPresent) {
        String extend = EXTEND + SPACE;
        if (genFileType == GENERATE_SERVICE_AND_MANAGER && isNotificationPresent) {
            extend = extend + LISTENER_REG + DIAMOND_OPEN_BRACKET + className + EVENT_STRING + COMMA + SPACE
                    + className + EVENT_LISTENER_STRING + DIAMOND_CLOSE_BRACKET + NEW_LINE;
        } else {
            for (JavaQualifiedTypeInfo info : getExtendsList()) {
                if (info.getClassInfo().equals(className)) {
                    if (!getExtendedClassStore().get(info)) {
                        return extend + info.getClassInfo();
                    } else {
                        return extend + info.getPkgInfo() + PERIOD + info.getClassInfo();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns extends list.
     *
     * @return the extendsList
     */
    public List<JavaQualifiedTypeInfo> getExtendsList() {
        return extendsList;
    }

    /**
     * Sets extends info list.
     *
     * @param classInfoList the extends List to set
     */
    private void setExtendsList(List<JavaQualifiedTypeInfo> classInfoList) {
        this.extendsList = classInfoList;
    }

    /**
     * Adds extends info to list.
     *
     * @param classInfo class info
     */
    private void addToExtendsList(JavaQualifiedTypeInfo classInfo) {
        getExtendsList().add(classInfo);
    }

}
