package org.onlab.onos.net;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Base implementation of an annotated model description.
 */
public class AbstractDescription implements Annotated {

    private static final SparseAnnotations EMPTY = DefaultAnnotations.builder().build();

    private final SparseAnnotations annotations;

    // For serialization
    protected AbstractDescription() {
        this.annotations = null;
    }

    /**
     * Creates a new entity, annotated with the specified annotations.
     *
     * @param annotations optional key/value annotations map
     */
    protected AbstractDescription(SparseAnnotations... annotations) {
        checkArgument(annotations.length <= 1, "Only one set of annotations is expected");
        this.annotations = annotations.length == 1 ? annotations[0] : EMPTY;
    }

    @Override
    public SparseAnnotations annotations() {
        return annotations;
    }

}
