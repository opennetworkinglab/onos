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
package org.onosproject.workflow.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Class for injecting json data model on the work-let execution context.
 */
public class JsonDataModelInjector {

    private static final Logger log = LoggerFactory.getLogger(JsonDataModelInjector.class);

    /**
     * Injects data model to work-let.
     *
     * @param worklet work-let to be injected
     * @param context workflow context
     * @throws WorkflowException workflow exception
     */
    public void inject(Worklet worklet, WorkflowContext context) throws WorkflowException {

        handle(worklet, context, this::injectModel);
    }

    /**
     * Inhales data model from work-let.
     *
     * @param worklet work-let to be inhaled
     * @param context workflow context
     * @throws WorkflowException workflow exception
     */
    public void inhale(Worklet worklet, WorkflowContext context) throws WorkflowException {

        handle(worklet, context, this::inhaleModel);
    }

    private void handle(Worklet worklet, WorkflowContext context, DataModelFieldBehavior func)
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
                if (!(annotation instanceof JsonDataModel)) {
                    continue;
                }
                JsonDataModel model = (JsonDataModel) annotation;
                func.apply(worklet, context, field, model);
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
        void apply(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
                throws WorkflowException;
    }

    private static Map<Class, DataModelFieldBehavior> injectTypeMap = new HashMap<>();

    static {
        injectTypeMap.put(String.class, JsonDataModelInjector::injectText);
        injectTypeMap.put(Integer.class, JsonDataModelInjector::injectInteger);
        injectTypeMap.put(Boolean.class, JsonDataModelInjector::injectBoolean);
        injectTypeMap.put(JsonNode.class, JsonDataModelInjector::injectJsonNode);
        injectTypeMap.put(ArrayNode.class, JsonDataModelInjector::injectArrayNode);
        injectTypeMap.put(ObjectNode.class, JsonDataModelInjector::injectObjectNode);
    }

    /**
     * Injects data model on the filed of work-let.
     *
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   data model for the field
     * @throws WorkflowException workflow exception
     */
    private void injectModel(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {

        DataModelFieldBehavior behavior = injectTypeMap.get(field.getType());
        if (Objects.isNull(behavior)) {
            throw new WorkflowException("Not supported type(" + field.getType() + ")");
        }
        behavior.apply(worklet, context, field, model);
    }

    /**
     * Injects text data model on the filed of work-let.
     *
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   text data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectText(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {

        String text = ((JsonDataModelTree) context.data()).textAt(model.path());
        if (Objects.isNull(text)) {
            if (model.optional()) {
                return;
            }
            throw new WorkflowException("Invalid text data model on (" + model.path() + ")");
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
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   integer data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectInteger(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {
        Integer number = ((JsonDataModelTree) context.data()).intAt(model.path());
        if (Objects.isNull(number)) {
            if (model.optional()) {
                return;
            }
            throw new WorkflowException("Invalid number data model on (" + model.path() + ")");
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
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   boolean data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectBoolean(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {

        Boolean bool = ((JsonDataModelTree) context.data()).booleanAt(model.path());
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
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   json node data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectJsonNode(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {

        JsonNode jsonNode = ((JsonDataModelTree) context.data()).nodeAt(model.path());
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
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   json array node data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectArrayNode(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {

        ArrayNode arrayNode = ((JsonDataModelTree) context.data()).arrayAt(model.path());
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
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   json object node data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void injectObjectNode(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {

        ObjectNode objNode = ((JsonDataModelTree) context.data()).objectAt(model.path());
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

    private static Map<Class, DataModelFieldBehavior> inhaleTypeMap = new HashMap<>();

    static {
        inhaleTypeMap.put(String.class, JsonDataModelInjector::inhaleText);
        inhaleTypeMap.put(Integer.class, JsonDataModelInjector::inhaleInteger);
        inhaleTypeMap.put(Boolean.class, JsonDataModelInjector::inhaleBoolean);
        inhaleTypeMap.put(JsonNode.class, JsonDataModelInjector::inhaleJsonNode);
        inhaleTypeMap.put(ArrayNode.class, JsonDataModelInjector::inhaleArrayNode);
        inhaleTypeMap.put(ObjectNode.class, JsonDataModelInjector::inhaleObjectNode);
    }

    /**
     * Inhales data model on the filed of work-let.
     *
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   data model for the field
     * @throws WorkflowException workflow exception
     */
    private void inhaleModel(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {

        DataModelFieldBehavior behavior = inhaleTypeMap.get(field.getType());
        if (Objects.isNull(behavior)) {
            throw new WorkflowException("Not supported type(" + field.getType() + ")");
        }
        behavior.apply(worklet, context, field, model);
    }

    /**
     * Inhales text data model on the filed of work-let.
     *
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   text data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void inhaleText(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {

        if (!(Objects.equals(field.getType(), String.class))) {
            throw new WorkflowException("Target field (" + field + ") is not String");
        }

        String text;
        try {
            field.setAccessible(true);
            text = (String) field.get(worklet);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }

        if (Objects.isNull(text)) {
            return;
        }

        JsonDataModelTree tree = (JsonDataModelTree) context.data();
        JsonNode jsonNode = tree.nodeAt(model.path());

        if (Objects.isNull(jsonNode) || jsonNode instanceof MissingNode) {
            tree.setAt(model.path(), text);
        } else if (!(jsonNode instanceof TextNode)) {
            throw new WorkflowException("Invalid text data model on (" + model.path() + ")");
        } else {
            tree.remove(model.path());
            tree.setAt(model.path(), text);
        }
    }

    /**
     * Inhales integer data model on the filed of work-let.
     *
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   integer data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void inhaleInteger(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {
        if (!(Objects.equals(field.getType(), Integer.class))) {
            throw new WorkflowException("Target field (" + field + ") is not Integer");
        }

        Integer number;
        try {
            field.setAccessible(true);
            number = (Integer) field.get(worklet);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }

        if (Objects.isNull(number)) {
            return;
        }

        JsonDataModelTree tree = (JsonDataModelTree) context.data();
        JsonNode jsonNode = tree.nodeAt(model.path());

        if (Objects.isNull(jsonNode) || jsonNode instanceof MissingNode) {
            tree.setAt(model.path(), number);
        } else if (!(jsonNode instanceof IntNode)) {
            throw new WorkflowException("Invalid integer data model on (" + model.path() + ")");
        } else {
            tree.remove(model.path());
            tree.setAt(model.path(), number);
        }
    }

    /**
     * Inhales boolean data model on the filed of work-let.
     *
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   boolean data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void inhaleBoolean(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {

        if (!(Objects.equals(field.getType(), Boolean.class))) {
            throw new WorkflowException("Target field (" + field + ") is not Boolean");
        }

        Boolean bool;
        try {
            field.setAccessible(true);
            bool = (Boolean) field.get(worklet);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }

        if (Objects.isNull(bool)) {
            return;
        }

        JsonDataModelTree tree = (JsonDataModelTree) context.data();
        JsonNode jsonNode = tree.nodeAt(model.path());

        if (Objects.isNull(jsonNode) || jsonNode instanceof MissingNode) {
            tree.setAt(model.path(), bool);
        } else if (!(jsonNode instanceof BooleanNode)) {
            throw new WorkflowException("Invalid boolean data model on (" + model.path() + ")");
        } else {
            tree.remove(model.path());
            tree.setAt(model.path(), bool);
        }
    }

    /**
     * Inhales json node data model on the filed of work-let.
     *
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   json node data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void inhaleJsonNode(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {

        if (!(Objects.equals(field.getType(), JsonNode.class))) {
            throw new WorkflowException("Target field (" + field + ") is not JsonNode");
        }

        JsonNode tgtJsonNode;
        try {
            field.setAccessible(true);
            tgtJsonNode = (JsonNode) field.get(worklet);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }

        if (Objects.isNull(tgtJsonNode)) {
            return;
        }

        JsonDataModelTree tree = (JsonDataModelTree) context.data();
        JsonNode jsonNode = tree.nodeAt(model.path());

        if (Objects.isNull(jsonNode) || jsonNode instanceof MissingNode) {
            tree.attach(model.path(), new JsonDataModelTree(tgtJsonNode));
        } else if (!(jsonNode instanceof JsonNode)) {
            throw new WorkflowException("Invalid json node data model on (" + model.path() + ")");
        } else {
            // do nothing
        }
    }

    /**
     * Inhales json array node data model on the filed of work-let.
     *
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   json array node data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void inhaleArrayNode(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {
        if (!(Objects.equals(field.getType(), ArrayNode.class))) {
            throw new WorkflowException("Target field (" + field + ") is not ArrayNode");
        }

        ArrayNode tgtArrayNode;
        try {
            field.setAccessible(true);
            tgtArrayNode = (ArrayNode) field.get(worklet);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }

        if (Objects.isNull(tgtArrayNode)) {
            return;
        }

        JsonDataModelTree tree = (JsonDataModelTree) context.data();
        JsonNode jsonNode = tree.nodeAt(model.path());

        if (Objects.isNull(jsonNode) || jsonNode instanceof MissingNode) {
            tree.attach(model.path(), new JsonDataModelTree(tgtArrayNode));
        } else if (!(jsonNode instanceof ArrayNode)) {
            throw new WorkflowException("Invalid array node data model on (" + model.path() + ")");
        } else {
            // do nothing
        }
    }

    /**
     * Inhales json object node data model on the filed of work-let.
     *
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param model   json object node data model for the field
     * @throws WorkflowException workflow exception
     */
    private static void inhaleObjectNode(Worklet worklet, WorkflowContext context, Field field, JsonDataModel model)
            throws WorkflowException {
        if (!(Objects.equals(field.getType(), ObjectNode.class))) {
            throw new WorkflowException("Target field (" + field + ") is not ObjectNode");
        }

        ObjectNode tgtObjNode;
        try {
            field.setAccessible(true);
            tgtObjNode = (ObjectNode) field.get(worklet);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }

        if (Objects.isNull(tgtObjNode)) {
            return;
        }

        JsonDataModelTree tree = (JsonDataModelTree) context.data();
        JsonNode jsonNode = tree.nodeAt(model.path());

        if (Objects.isNull(jsonNode) || jsonNode instanceof MissingNode) {
            tree.attach(model.path(), new JsonDataModelTree(tgtObjNode));
        } else if (!(jsonNode instanceof ObjectNode)) {
            throw new WorkflowException("Invalid object node data model on (" + model.path() + ")");
        } else {
            // do nothing
        }
    }
}
