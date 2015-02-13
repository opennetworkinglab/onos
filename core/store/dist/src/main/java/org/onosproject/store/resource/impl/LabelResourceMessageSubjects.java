package org.onosproject.store.resource.impl;

import org.onosproject.store.cluster.messaging.MessageSubject;

public final class LabelResourceMessageSubjects {

    private LabelResourceMessageSubjects() {
    }

    public static final MessageSubject LABEL_POOL_CREATED = new MessageSubject(
                                                                               "label-resource-pool-created");
    public static final MessageSubject LABEL_POOL_DESTROYED = new MessageSubject(
                                                                                 "label-resource-pool-destroyed");
    public static final MessageSubject LABEL_POOL_APPLE = new MessageSubject(
                                                                             "label-resource-pool-apply");
    public static final MessageSubject LABEL_POOL_RELEASE = new MessageSubject(
                                                                               "label-resource-pool-release");
}
