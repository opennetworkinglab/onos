package org.onlab.onos.of.controller.impl.debugcounter;

import java.util.Collections;
import java.util.List;

import org.onlab.onos.of.controller.impl.debugcounter.DebugCounter.DebugCounterInfo;

//CHECKSTYLE:OFF
public class NullDebugCounter implements IDebugCounterService {

    @Override
    public void flushCounters() {

    }

    @Override
    public void resetAllCounters() {

    }

    @Override
    public void resetAllModuleCounters(String moduleName) {

    }


    @Override
    public void resetCounterHierarchy(String moduleName, String counterHierarchy) {

    }

    @Override
    public void enableCtrOnDemand(String moduleName, String counterHierarchy) {

    }

    @Override
    public void disableCtrOnDemand(String moduleName, String counterHierarchy) {

    }

    @Override
    public List<DebugCounterInfo> getCounterHierarchy(String moduleName,
                                                      String counterHierarchy) {
        return Collections.emptyList();
    }

    @Override
    public List<DebugCounterInfo> getAllCounterValues() {
        return Collections.emptyList();
    }

    @Override
    public List<DebugCounterInfo> getModuleCounterValues(String moduleName) {
        return Collections.emptyList();
    }

    @Override
    public boolean containsModuleCounterHierarchy(String moduleName,
                                             String counterHierarchy) {
        return false;
    }

    @Override
    public boolean containsModuleName(String moduleName) {
        return false;
    }

    @Override
    public
            IDebugCounter
            registerCounter(String moduleName, String counterHierarchy,
                            String counterDescription,
                            CounterType counterType, String... metaData)
                                 throws MaxCountersRegistered {
        return new NullCounterImpl();
    }

    @Override
    public List<String> getModuleList() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getModuleCounterList(String moduleName) {
        return Collections.emptyList();
    }

    public static class NullCounterImpl implements IDebugCounter {

        @Override
        public void updateCounterWithFlush() {

        }

        @Override
        public void updateCounterNoFlush() {

        }

        @Override
        public void updateCounterWithFlush(int incr) {
        }

        @Override
        public void updateCounterNoFlush(int incr) {

        }

        @Override
        public long getCounterValue() {
            return -1;
        }

    }

}
