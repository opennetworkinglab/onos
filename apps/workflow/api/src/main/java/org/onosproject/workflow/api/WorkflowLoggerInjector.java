/*
 * Copyright 2022-present Open Networking Foundation
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class for injecting workflow logger on the work-let execution context.
 */
public class WorkflowLoggerInjector {

    /**
     * Injects logger to work-let.
     *
     * @param worklet work-let to be injected
     * @param context workflow context
     * @throws WorkflowException workflow exception
     */
    public void inject(Worklet worklet, WorkflowContext context) throws WorkflowException {

        handle(worklet, context, this::injectLogger);
    }


    private void handle(Worklet worklet, WorkflowContext context, WorkflowLoggerInjector.ReferenceFieldBehavior func)
            throws WorkflowException {
        Class<?> cl = worklet.getClass();
        List<Field> fields = getInheritedFields(cl);

        for (Field field : fields) {
            Annotation[] annotations = field.getAnnotations();
            if (Objects.isNull(annotations)) {
                continue;
            }
            for (Annotation annotation : annotations) {
                if (!(annotation instanceof WorkflowLogger)) {
                    continue;
                }

                if (Modifier.isStatic(field.getModifiers())) {
                    throw new WorkflowException("Static field(" + field + " ) cannot use @WorkflowLogger in " + cl);
                }

                WorkflowLogger reference = (WorkflowLogger) annotation;
                func.apply(worklet, context, field, reference);
            }
        }
    }

    private static List<Field> getInheritedFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();

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
     * Functional interface for workflow logger annotated field behavior.
     */
    @FunctionalInterface
    public interface ReferenceFieldBehavior {
        void apply(Worklet worklet, WorkflowContext context, Field field, WorkflowLogger reference)
                throws WorkflowException;
    }

    /**
     * Injects logger on the filed of work-let.
     *
     * @param worklet work-let
     * @param context workflow context
     * @param field   the field of work-let
     * @param reference   logger reference for the field
     * @throws WorkflowException workflow exception
     */
    private void injectLogger(Worklet worklet, WorkflowContext context, Field field, WorkflowLogger reference)
            throws WorkflowException {

        Object obj = new WorkflowLoggerFactory(context.name(), worklet.getClass().getSimpleName());

        try {
            field.setAccessible(true);
            field.set(worklet, obj);
        } catch (IllegalAccessException e) {
            throw new WorkflowException(e);
        }
    }

}
