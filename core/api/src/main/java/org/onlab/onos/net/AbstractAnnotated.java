package org.onlab.onos.net;

import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Base abstraction of an annotated entity.
 */
public class AbstractAnnotated implements Annotated {

    private static final Map<String, String> EMPTY = new HashMap<>();

    private final Map<String, String> annotations;

    // For serialization
    protected AbstractAnnotated() {
        this.annotations = EMPTY;
    }

    /**
     * Creates a new entity, annotated with the specified annotations.
     *
     * @param annotations optional key/value annotations map
     */
    protected AbstractAnnotated(Map<String, String>[] annotations) {
        checkArgument(annotations.length <= 1, "Only one set of annotations is expected");
        this.annotations = annotations.length == 1 ? annotations[0] : EMPTY;
    }

    @Override
    public Set<String> annotationKeys() {
        return ImmutableSet.copyOf(annotations.keySet());
    }

    @Override
    public String annotation(String key) {
        return annotations.get(key);
    }

}
