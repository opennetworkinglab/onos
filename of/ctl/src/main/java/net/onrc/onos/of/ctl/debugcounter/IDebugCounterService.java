package net.onrc.onos.of.ctl.debugcounter;



import java.util.List;

import net.onrc.onos.of.ctl.debugcounter.DebugCounter.DebugCounterInfo;

public interface IDebugCounterService {

    /**
     * Different counter types. Counters that are meant to be counted-on-demand
     * need to be separately enabled/disabled.
     */
    public enum CounterType {
        ALWAYS_COUNT,
        COUNT_ON_DEMAND
    }

    /**
     * Debug Counter Qualifiers.
     */
    public static final String CTR_MDATA_WARN = "warn";
    public static final String CTR_MDATA_ERROR = "error";
    public static final String CTR_MDATA_DROP = "drop";

    /**
     *  A limit on the maximum number of counters that can be created.
     */
    public static final int MAX_COUNTERS = 5000;

    /**
     * Exception thrown when MAX_COUNTERS have been registered.
     */
    public class MaxCountersRegistered extends CounterException {
        private static final long serialVersionUID = 3173747663719376745L;
        String errormsg;
        public MaxCountersRegistered(String errormsg) {
            this.errormsg = errormsg;
        }
        @Override
        public String getMessage() {
            return this.errormsg;
        }
    }
    /**
     * Exception thrown when MAX_HIERARCHY has been reached.
     */
    public class MaxHierarchyRegistered extends CounterException {
        private static final long serialVersionUID = 967431358683523871L;
        private String errormsg;
        public MaxHierarchyRegistered(String errormsg) {
            this.errormsg = errormsg;
        }
        @Override
        public String getMessage() {
            return this.errormsg;
        }
    }
    /**
     * Exception thrown when attempting to register a hierarchical counter
     * where higher levels of the hierarchy have not been pre-registered.
     */
    public class MissingHierarchicalLevel extends CounterException {
        private static final long serialVersionUID = 517315311533995739L;
        private String errormsg;
        public MissingHierarchicalLevel(String errormsg) {
            this.errormsg = errormsg;
        }
        @Override
        public String getMessage() {
            return this.errormsg;
        }
    }

    public class CounterException extends Exception {
        private static final long serialVersionUID = 2219781500857866035L;
    }

    /**
     *  maximum levels of hierarchy.
     *  Example of moduleName/counterHierarchy:
     *           switch/00:00:00:00:01:02:03:04/pktin/drops where
     *           moduleName ==> "switch"  and
     *           counterHierarchy of 3 ==> "00:00:00:00:01:02:03:04/pktin/drops"
     */
    public static final int MAX_HIERARCHY = 3;

    /**
     * All modules that wish to have the DebugCounterService count for them, must
     * register their counters by making this call (typically from that module's
     * 'startUp' method). The counter can then be updated, displayed, reset etc.
     * using the registered moduleName and counterHierarchy.
     *
     * @param moduleName           the name of the module which is registering the
     *                             counter eg. linkdiscovery or controller or switch
     * @param counterHierarchy     the hierarchical counter name specifying all
     *                             the hierarchical levels that come above it.
     *                             For example: to register a drop counter for
     *                             packet-ins from a switch, the counterHierarchy
     *                             can be "00:00:00:00:01:02:03:04/pktin/drops"
     *                             It is necessary that counters in hierarchical levels
     *                             above have already been pre-registered - in this
     *                             example: "00:00:00:00:01:02:03:04/pktin" and
     *                             "00:00:00:00:01:02:03:04"
     * @param counterDescription   a descriptive string that gives more information
     *                             of what the counter is measuring. For example,
     *                             "Measures the number of incoming packets seen by
     *                             this module".
     * @param counterType          One of CounterType. On-demand counter types
     *                             need to be explicitly enabled/disabled using other
     *                             methods in this API -- i.e. registering them is
     *                             not enough to start counting.
     * @param metaData             variable arguments that qualify a counter
     *                             eg. warn, error etc.
     * @return                     IDebugCounter with update methods that can be
     *                             used to update a counter.
     * @throws MaxCountersRegistered
     * @throws MaxHierarchyRegistered
     * @throws MissingHierarchicalLevel
     */
    public IDebugCounter registerCounter(String moduleName, String counterHierarchy,
                             String counterDescription, CounterType counterType,
                             String... metaData)
                throws CounterException;

    /**
     * Flush all thread-local counter values (from the current thread)
     * to the global counter store. This method is not intended for use by any
     * module. It's typical usage is from floodlight core and it is meant
     * to flush those counters that are updated in the packet-processing pipeline,
     * typically with the 'updateCounterNoFlush" methods in IDebugCounter.
     */
    public void flushCounters();

    /**
     * Resets the value of counters in the hierarchy to zero. Note that the reset
     * applies to the level of counter hierarchy specified AND ALL LEVELS BELOW it
     * in the hierarchy.
     * For example: If a hierarchy exists like "00:00:00:00:01:02:03:04/pktin/drops"
     *              specifying a reset hierarchy: "00:00:00:00:01:02:03:04"
     *              will reset all counters for the switch dpid specified;
     *              while specifying a reset hierarchy: ""00:00:00:00:01:02:03:04/pktin"
     *              will reset the pktin counter and all levels below it (like drops)
     *              for the switch dpid specified.
     */
    void resetCounterHierarchy(String moduleName, String counterHierarchy);

    /**
     * Resets the values of all counters in the system.
     */
    public void resetAllCounters();

    /**
     * Resets the values of all counters belonging
     * to a module with the given 'moduleName'.
     */
    public void resetAllModuleCounters(String moduleName);

    /**
     * This method applies only to CounterType.COUNT_ON_DEMAND. It is used to
     * enable counting on the counter. Note that this step is necessary to start
     * counting for these counter types - merely registering the counter is not
     * enough (as is the case for CounterType.ALWAYS_COUNT). Newly
     * enabled counters start from an initial value of zero.
     *
     * Enabling a counter in a counterHierarchy enables only THAT counter. It
     * does not enable any other part of the counterHierarchy. For example, if
     * a hierarchy exists like "00:00:00:00:01:02:03:04/pktin/drops", where the
     * 'pktin' and 'drops' counters are CounterType.COUNT_ON_DEMAND, then enabling
     * the 'pktin' counter by specifying the counterHierarchy as
     * "00:00:00:00:01:02:03:04/pktin" does NOT enable the 'drops' counter.
     */
    public void enableCtrOnDemand(String moduleName, String counterHierarchy);

    /**
     * This method applies only to CounterType.COUNT_ON_DEMAND. It is used to
     * enable counting on the counter. Note that disabling a counter results in a loss
     * of the counter value. When re-enabled the counter will restart from zero.
     *
     * Disabling a counter in a counterHierarchy disables only THAT counter. It
     * does not disable any other part of the counterHierarchy. For example, if
     * a hierarchy exists like "00:00:00:00:01:02:03:04/pktin/drops", where the
     * 'pktin' and 'drops' counters are CounterType.COUNT_ON_DEMAND, then disabling
     * the 'pktin' counter by specifying the counterHierarchy as
     * "00:00:00:00:01:02:03:04/pktin" does NOT disable the 'drops' counter.
     */
    public void disableCtrOnDemand(String moduleName, String counterHierarchy);

    /**
     * Get counter value and associated information for the specified counterHierarchy.
     * Note that information on the level of counter hierarchy specified
     * AND ALL LEVELS BELOW it in the hierarchy will be returned.
     *
     * For example,
     * if a hierarchy exists like "00:00:00:00:01:02:03:04/pktin/drops", then
     * specifying a counterHierarchy of "00:00:00:00:01:02:03:04/pktin" in the
     * get call will return information on the 'pktin' as well as the 'drops'
     * counters for the switch dpid specified.
     *
     * @return A list of DebugCounterInfo or an empty list if the counter
     *         could not be found
     */
    public List<DebugCounterInfo> getCounterHierarchy(String moduleName,
                                                      String counterHierarchy);

    /**
     * Get counter values and associated information for all counters in the
     * system.
     *
     * @return the list of values/info or an empty list
     */
    public  List<DebugCounterInfo> getAllCounterValues();

    /**
     * Get counter values and associated information for all counters associated
     * with a module.
     *
     * @param moduleName
     * @return the list of values/info or an empty list
     */
    public  List<DebugCounterInfo> getModuleCounterValues(String moduleName);

    /**
     * Convenience method to figure out if the the given 'counterHierarchy' corresponds
     * to a registered counterHierarchy for 'moduleName'. Note that the counter may or
     * may not be enabled for counting, but if it is registered the method will
     * return true.
     *
     * @param param
     * @return false if moduleCounterHierarchy is not a registered counter
     */
    public boolean containsModuleCounterHierarchy(String moduleName,
                                                  String counterHierarchy);

    /**
     * Convenience method to figure out if the the given 'moduleName' corresponds
     * to a registered moduleName or not. Note that the module may or may not have
     * a counter enabled for counting, but if it is registered the method will
     * return true.
     *
     * @param param
     * @return false if moduleName is not a registered counter
     */
    public boolean containsModuleName(String moduleName);

    /**
     * Returns a list of moduleNames registered for debug counters or an empty
     * list if no counters have been registered in the system.
     */
    public List<String> getModuleList();

    /**
     * Returns a list of all counters registered for a specific moduleName
     * or a empty list.
     */
    public List<String> getModuleCounterList(String moduleName);


}
