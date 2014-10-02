package org.onlab.onos.net;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Base abstraction of an annotated entity.
 */
public abstract class AbstractAnnotated implements Annotated {

    private static final Annotations EMPTY = DefaultAnnotations.builder().build();

    private final Annotations annotations;

    // For serialization
    protected AbstractAnnotated() {
        this.annotations = null;
    }

    /**
     * Creates a new entity, annotated with the specified annotations.
     *
     * @param annotations optional key/value annotations map
     */
    protected AbstractAnnotated(Annotations... annotations) {
        checkArgument(annotations.length <= 1, "Only one set of annotations is expected");
        this.annotations = annotations.length == 1 ? annotations[0] : EMPTY;
    }

    @Override
    public Annotations annotations() {
        return annotations;
    }

}
