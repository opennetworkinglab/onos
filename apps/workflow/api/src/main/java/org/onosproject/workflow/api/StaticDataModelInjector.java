/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.workflow.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.HashMap;

public class StaticDataModelInjector {

    private static final Logger log = LoggerFactory.getLogger(StaticDataModelInjector.class);

    /**
     * Injects data model to work-let.
     *
     * @param worklet            work-let to be injected
     * @param workletDescription worklet description
     * @throws WorkflowException workflow exception
     */
    public void inject(Worklet worklet, WorkletDescription workletDescription) throws WorkflowException {

        handle(worklet, workletDescription, this::injectModel);
    }

    private void handle(Worklet worklet, WorkletDescription workletDescription, DataModelFieldBehavior func)
            throws WorkflowException {
        Class cl = worklet.getClass();
        List<Field> fields = getInheritedFields(cl);
        if (Objects.isNull(fields)) {
            log.error("Invalid fields on {}", cl);
            return;
        }

        for (Field field : fields) {
            Annotation[] annotations = field.getAnnotations();
            if (Objects.isNull(annotations)) {
                continue;
            }
            for (Annotation annotation : annotations) {
                if (!(annotation instanceof StaticDataModel)) {
                    continue;
                }
                StaticDataModel model = (StaticDataModel) annotation;
                func.apply(worklet, workletDescription, field, model);
            }
        }
    }

    private static List<Field> getInheritedFields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();

        Class<?> cl = type;
        while (cl != null && cl != Object.class) {
            for (Field field : cl.getDeclaredFields()) {
                if (!field.isSynthetic()) {
                    fields.add(field);
                }
            }
            cl = cl.getSuperclass();
        }
        return fields;
    }

    /**
     * Functional interface for json data model annotated field behavior.
     */
    @FunctionalInterface
    public interface DataModelFieldBehavior {
        void apply(Worklet worklet, WorkletDescription workletDescription, Field field, StaticDataModel model)
                throws WorkflowException;
    }

    private static Map<Class, DataModelFieldBehavior> injectTypeMap = new HashMap<>();

    static {
        injectTypeMap.put(String.class, StaticDataModelInjector::injectText);
        injectTypeMap.put(Integer.class, StaticDataModelInjector::injectInteger);
        injectTypeMap.put(Boolean.class, StaticDataModelInjector::injectBoolean);
        injectTypeMap.put(JsonNode.class, StaticDataModelInjector::injectJsonNode);
        injectTypeMap.put(ArrayNode.class, StaticDataModelInjector::injectArrayNode);
        injectTypeMap.put(ObjectNode.class, StaticDataModelInjector::injectObjectNode);
    }

    /**
     * Injects data model on the filed of work-let.
     *
     * @param worklet            work-let
     * @param workletDescription worklet description
     * @param field              the field of work-let
     * @param model              data model for the field
     * @throws WorkflowException workflow exception
     */
    private void injectModel(Worklet worklet, WorkletDescription workletDescription, Field field, StaticDataModel model)
            throws WorkflowException {

        DataModelFieldBehavior behavior = injectTypeMap.get(field.getType());
        if (Objects.isNull(behavior)) {
            throw new WorkflowException("Not supported type(" + field.getType() + ")");
        }
        behavior.apply(worklet, workletDescription, field, model);
    }

    /**
     * Injects text data model on the filed of work-let.
     *
     * @param worklet            work-let
     * @param workletDescription worklet description
     * @param field              the field of work-let
     * @param model              text data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectText(Worklet worklet, WorkletDescription workletDescription, Field field,
                                   StaticDataModel model) throws WorkflowException {

        String text = ((JsonDataModelTree) workletDescription.data()).textAt(model.path());
        if (Objects.isNull(text)) {
            if (model.optional()) {
                return;
            }
            throw new WorkflowException("Invalid array node data model on (" + model.path() + ")");
        }


        if (!(Objects.equals(field.getType(), String.class))) {
            throw new WorkflowException("Target field (" + field + ") is not String");
        }

        try {
            field.setAccessible(true);
            field.set(worklet, text);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }
    }

    /**
     * Injects integer data model on the filed of work-let.
     *
     * @param worklet            work-let
     * @param workletDescription worklet description
     * @param field              the field of work-let
     * @param model              integer data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectInteger(Worklet worklet, WorkletDescription workletDescription, Field field,
                                      StaticDataModel model) throws WorkflowException {

        Integer number = ((JsonDataModelTree) workletDescription.data()).intAt(model.path());
        if (Objects.isNull(number)) {
            if (model.optional()) {
                return;
            }
            throw new WorkflowException("Invalid array node data model on (" + model.path() + ")");
        }
        if (!(Objects.equals(field.getType(), Integer.class))) {
            throw new WorkflowException("Target field (" + field + ") is not Integer");
        }

        try {
            field.setAccessible(true);
            field.set(worklet, number);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }
    }


    /**
     * Injects boolean data model on the filed of work-let.
     *
     * @param worklet            work-let
     * @param workletDescription worklet description
     * @param field              the field of work-let
     * @param model              boolean data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectBoolean(Worklet worklet, WorkletDescription workletDescription, Field field,
                                      StaticDataModel model) throws WorkflowException {

        Boolean bool = ((JsonDataModelTree) workletDescription.data()).booleanAt(model.path());
        if (Objects.isNull(bool)) {
            if (model.optional()) {
                return;
            }
            throw new WorkflowException("Invalid boolean data model on (" + model.path() + ")");
        }

        if (!(Objects.equals(field.getType(), Boolean.class))) {
            throw new WorkflowException("Target field (" + field + ") is not Boolean");
        }

        try {
            field.setAccessible(true);
            field.set(worklet, bool);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }
    }

    /**
     * Injects json node data model on the filed of work-let.
     *
     * @param worklet            work-let
     * @param workletDescription worklet description
     * @param field              the field of work-let
     * @param model              json node data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectJsonNode(Worklet worklet, WorkletDescription workletDescription, Field field,
                                       StaticDataModel model) throws WorkflowException {

        JsonNode jsonNode = ((JsonDataModelTree) workletDescription.data()).nodeAt(model.path());
        if (Objects.isNull(jsonNode)) {
            if (model.optional()) {
                return;
            }
            throw new WorkflowException("Invalid json node data model on (" + model.path() + ")");
        }

        if (!(Objects.equals(field.getType(), JsonNode.class))) {
            throw new WorkflowException("Target field (" + field + ") is not JsonNode");
        }

        try {
            field.setAccessible(true);
            field.set(worklet, jsonNode);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }
    }

    /**
     * Injects json array node data model on the filed of work-let.
     *
     * @param worklet            work-let
     * @param workletDescription worklet description
     * @param field              the field of work-let
     * @param model              json array node data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectArrayNode(Worklet worklet, WorkletDescription workletDescription, Field field,
                                        StaticDataModel model) throws WorkflowException {

        ArrayNode arrayNode = ((JsonDataModelTree) workletDescription.data()).arrayAt(model.path());
        if (Objects.isNull(arrayNode)) {
            if (model.optional()) {
                return;
            }
            throw new WorkflowException("Invalid array node data model on (" + model.path() + ")");
        }

        if (!(Objects.equals(field.getType(), ArrayNode.class))) {
            throw new WorkflowException("Target field (" + field + ") is not ArrayNode");
        }

        try {
            field.setAccessible(true);
            field.set(worklet, arrayNode);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }
    }

    /**
     * Injects json object node data model on the filed of work-let.
     *
     * @param worklet            work-let
     * @param workletDescription worklet description
     * @param field              the field of work-let
     * @param model              json object node data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectObjectNode(Worklet worklet, WorkletDescription workletDescription, Field field,
                                         StaticDataModel model) throws WorkflowException {

        ObjectNode objNode = ((JsonDataModelTree) workletDescription.data()).objectAt(model.path());
        if (Objects.isNull(objNode)) {
            if (model.optional()) {
                return;
            }
            throw new WorkflowException("Invalid object node data model on (" + model.path() + ")");
        }

        if (!(Objects.equals(field.getType(), ObjectNode.class))) {
            throw new WorkflowException("Target field (" + field + ") is not ObjectNode");
        }

        try {
            field.setAccessible(true);
            field.set(worklet, objNode);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }
    }
}
