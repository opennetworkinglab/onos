package org.onlab.onos.net;

public final class AnnotationsUtil {

    public static boolean isEqual(Annotations lhs, Annotations rhs) {
        if (lhs == rhs) {
            return true;
        }
        if (lhs == null || rhs == null) {
            return false;
        }

        if (!lhs.keys().equals(rhs.keys())) {
            return false;
        }

        for (String key : lhs.keys()) {
            if (!lhs.value(key).equals(rhs.value(key))) {
                return false;
            }
        }
        return true;
    }

    // not to be instantiated
    private AnnotationsUtil() {}
}
