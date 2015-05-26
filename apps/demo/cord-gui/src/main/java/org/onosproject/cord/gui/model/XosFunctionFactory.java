package org.onosproject.cord.gui.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

import static org.onosproject.cord.gui.model.XosFunctionDescriptor.URL_FILTER;

/**
 * Utility factory for operating on XOS functions.
 */
public class XosFunctionFactory extends JsonFactory {

    private static final String PARAMS = "params";
    private static final String LEVEL = "level";
    private static final String LEVELS = "levels";


    // no instantiation
    private XosFunctionFactory() {}

    /**
     * Produces the JSON representation of the given XOS function descriptor.
     *
     * @param xfd function descriptor
     * @return JSON encoding
     */
    public static ObjectNode toObjectNode(XosFunctionDescriptor xfd) {
        ObjectNode root = objectNode()
                .put(ID, xfd.id())
                .put(NAME, xfd.displayName())
                .put(DESC, xfd.description());
        root.set(PARAMS, paramsForXfd(xfd));
        return root;
    }

    private static ObjectNode paramsForXfd(XosFunctionDescriptor xfd) {
        ParamsFactory psf = PARAM_MAP.get(xfd);
        if (psf == null) {
            psf = DEF_PARAMS_FACTORY;
        }
        return psf.params();
    }


    // ==== handling different parameter structures...
    private static final Map<XosFunctionDescriptor, ParamsFactory>
        PARAM_MAP = new HashMap<XosFunctionDescriptor, ParamsFactory>();

    private static final ParamsFactory DEF_PARAMS_FACTORY = new ParamsFactory();
    static {
        PARAM_MAP.put(URL_FILTER, new UrlFilterParamsFactory());
    }

    /**
     * Creates an object node representation of the profile for the
     * specified user.
     *
     * @param user the user
     * @return object node profile
     */
    public static ObjectNode profileForUser(SubscriberUser user) {
        ObjectNode root = objectNode();
        for (XosFunctionDescriptor xfd: XosFunctionDescriptor.values()) {
            XosFunction.Memento mem = user.getMemento(xfd);
            if (mem != null) {
                root.set(xfd.id(), mem.toObjectNode());
            }
        }
        return root;
    }


    // ===================================================================
    // === factories for creating parameter structures, both default
    //     and from a memento...

    // private parameter structure creator
    static class ParamsFactory {
        ObjectNode params() {
            return objectNode();
        }
    }

    static class UrlFilterParamsFactory extends ParamsFactory {
        @Override
        ObjectNode params() {
            ObjectNode result = objectNode();
            result.put(LEVEL, UrlFilterFunction.DEFAULT_LEVEL.name());
            ArrayNode levels = arrayNode();
            for (UrlFilterFunction.Level lvl: UrlFilterFunction.Level.values()) {
                levels.add(lvl.name());
            }
            result.set(LEVELS, levels);
            return result;
        }
    }
}
