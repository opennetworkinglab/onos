package net.onrc.onos.of.ctl.debugcounter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.google.common.collect.Sets;



/**
 * This class implements a central store for all counters used for debugging the
 * system. For counters based on traffic-type, see ICounterStoreService.
 *
 */
//CHECKSTYLE:OFF
public class DebugCounter implements IDebugCounterService {
    protected static final Logger log = LoggerFactory.getLogger(DebugCounter.class);

    /**
     * registered counters need a counter id.
     */
    protected AtomicInteger counterIdCounter = new AtomicInteger();

    /**
     * The counter value.
     */
    protected static class MutableLong {
        long value = 0;
        public void increment() { value += 1; }
        public void increment(long incr) { value += incr; }
        public long get() { return value; }
        public void set(long val) { value = val; }
      }

    /**
     * protected class to store counter information.
     */
    public static class CounterInfo {
        String moduleCounterHierarchy;
        String counterDesc;
        CounterType ctype;
        String moduleName;
        String counterHierarchy;
        int counterId;
        boolean enabled;
        String[] metaData;

        public CounterInfo(int counterId, boolean enabled,
                           String moduleName, String counterHierarchy,
                           String desc, CounterType ctype, String... metaData) {
            this.moduleCounterHierarchy = moduleName + "/" + counterHierarchy;
            this.moduleName = moduleName;
            this.counterHierarchy = counterHierarchy;
            this.counterDesc = desc;
            this.ctype = ctype;
            this.counterId = counterId;
            this.enabled = enabled;
            this.metaData = metaData;
        }

        public String getModuleCounterHierarchy() { return moduleCounterHierarchy; }
        public String getCounterDesc() { return counterDesc; }
        public CounterType getCtype() { return ctype; }
        public String getModuleName() { return moduleName; }
        public String getCounterHierarchy() { return counterHierarchy; }
        public int getCounterId() { return counterId; }
        public boolean isEnabled() { return enabled; }
        public String[] getMetaData() { return this.metaData.clone(); }
    }

    //******************
    //   Global stores
    //******************

    /**
     * Counter info for a debug counter.
     */
    public static class DebugCounterInfo {
        CounterInfo cinfo;
        AtomicLong cvalue;

        public DebugCounterInfo(CounterInfo cinfo) {
            this.cinfo = cinfo;
            this.cvalue = new AtomicLong();
        }
        public CounterInfo getCounterInfo() {
            return cinfo;
        }
        public Long getCounterValue() {
            return cvalue.get();
        }
    }

    /**
     * Global debug-counter storage across all threads. These are
     * updated from the local per thread counters by the flush counters method.
     */
    private static final DebugCounterInfo[] ALLCOUNTERS =
                            new DebugCounterInfo[MAX_COUNTERS];


    /**
     * per module counters, indexed by the module name and storing three levels
     * of Counter information in the form of CounterIndexStore.
     */
    protected ConcurrentHashMap<String, ConcurrentHashMap<String, CounterIndexStore>>
        moduleCounters = new ConcurrentHashMap<String,
                                                ConcurrentHashMap<String,
                                                                   CounterIndexStore>>();

    protected static class CounterIndexStore {
        int index;
        Map<String, CounterIndexStore> nextLevel;

        public CounterIndexStore(int index, Map<String, CounterIndexStore> cis) {
            this.index = index;
            this.nextLevel = cis;
        }
    }

    /**
     * fast global cache for counter ids that are currently active.
     */
    protected Set<Integer> currentCounters = Collections.newSetFromMap(
                                         new ConcurrentHashMap<Integer, Boolean>());

    //******************
    // Thread local stores
    //******************

    /**
     * Thread local storage of counter info.
     */
    protected static class LocalCounterInfo {
        boolean enabled;
        MutableLong cvalue;

        public LocalCounterInfo(boolean enabled) {
            this.enabled = enabled;
            this.cvalue = new MutableLong();
        }
    }

    /**
     * Thread local debug counters used for maintaining counters local to a thread.
     */
    protected final ThreadLocal<LocalCounterInfo[]> threadlocalCounters =
            new ThreadLocal<LocalCounterInfo[]>() {
        @Override
        protected LocalCounterInfo[] initialValue() {
            return new LocalCounterInfo[MAX_COUNTERS];
        }
    };

    /**
     * Thread local cache for counter ids that are currently active.
     */
    protected final ThreadLocal<Set<Integer>> threadlocalCurrentCounters =
            new ThreadLocal<Set<Integer>>() {
        @Override
        protected Set<Integer> initialValue() {
            return new HashSet<Integer>();
        }
    };

    //*******************************
    //   IDebugCounter
    //*******************************

    protected class CounterImpl implements IDebugCounter {
        private final int counterId;

        public CounterImpl(int counterId) {
            this.counterId = counterId;
        }

        @Override
        public void updateCounterWithFlush() {
            if (!validCounterId()) {
                return;
            }
            updateCounter(counterId, 1, true);
        }

        @Override
        public void updateCounterNoFlush() {
            if (!validCounterId()) {
                return;
            }
            updateCounter(counterId, 1, false);
        }

        @Override
        public void updateCounterWithFlush(int incr) {
            if (!validCounterId()) {
                return;
            }
            updateCounter(counterId, incr, true);
        }

        @Override
        public void updateCounterNoFlush(int incr) {
            if (!validCounterId()) {
                return;
            }
            updateCounter(counterId, incr, false);
        }

        @Override
        public long getCounterValue() {
            if (!validCounterId()) {
                return -1;
            }
            return ALLCOUNTERS[counterId].cvalue.get();
        }

        /**
         * Checks if this is a valid counter.
         * @return true if the counter id is valid
         */
        private boolean validCounterId() {
            if (counterId < 0 || counterId >= MAX_COUNTERS) {
                log.error("Invalid counterId invoked");
                return false;
            }
            return true;
        }

    }

   //*******************************
   //   IDebugCounterService
   //*******************************

   @Override
   public IDebugCounter registerCounter(String moduleName, String counterHierarchy,
                           String counterDescription, CounterType counterType,
                           String... metaData)
               throws CounterException {
       // check if counter already exists
       if (!moduleCounters.containsKey(moduleName)) {
           moduleCounters.putIfAbsent(moduleName,
                new ConcurrentHashMap<String, CounterIndexStore>());
       }
       RetCtrInfo rci = getCounterId(moduleName, counterHierarchy);
       if (rci.allLevelsFound) {
           // counter exists
           log.info("Counter exists for {}/{} -- resetting counters", moduleName,
                    counterHierarchy);
           resetCounterHierarchy(moduleName, counterHierarchy);
           return new CounterImpl(rci.ctrIds[rci.foundUptoLevel - 1]);
       }
       // check for validity of counter
       if (rci.levels.length > MAX_HIERARCHY) {
           String err = "Registry of counterHierarchy " + counterHierarchy +
                   " exceeds max hierachy " + MAX_HIERARCHY + ".. aborting";
           throw new MaxHierarchyRegistered(err);
       }
       if (rci.foundUptoLevel < rci.levels.length - 1) {
           StringBuilder sb = new StringBuilder();
           for (int i = 0; i <= rci.foundUptoLevel; i++) {
               sb.append(rci.levels[i]);
           }
           String needToRegister = sb.toString();
           String err = "Attempting to register hierarchical counterHierarchy " +
                   counterHierarchy + " but parts of hierarchy missing. " +
                   "Please register " +  needToRegister + " first";
           throw new MissingHierarchicalLevel(err);
       }

       // get a new counter id
       int counterId = counterIdCounter.getAndIncrement();
       if (counterId >= MAX_COUNTERS) {
           throw new MaxCountersRegistered("max counters reached");
       }
       // create storage for counter
       boolean enabled = (counterType == CounterType.ALWAYS_COUNT) ? true : false;
       CounterInfo ci = new CounterInfo(counterId, enabled, moduleName,
                                        counterHierarchy, counterDescription,
                                        counterType, metaData);
       ALLCOUNTERS[counterId] = new DebugCounterInfo(ci);

       // account for the new counter in the module counter hierarchy
       addToModuleCounterHierarchy(moduleName, counterId, rci);

       // finally add to active counters
       if (enabled) {
           currentCounters.add(counterId);
       }
       return new CounterImpl(counterId);
   }

   private void updateCounter(int counterId, int incr, boolean flushNow) {
       if (counterId < 0 || counterId >= MAX_COUNTERS) {
        return;
    }

       LocalCounterInfo[] thiscounters =  this.threadlocalCounters.get();
       if (thiscounters[counterId] == null) {
           // seeing this counter for the first time in this thread - create local
           // store by consulting global store
           DebugCounterInfo dc = ALLCOUNTERS[counterId];
           if (dc != null) {
               thiscounters[counterId] = new LocalCounterInfo(dc.cinfo.enabled);
               if (dc.cinfo.enabled) {
                   Set<Integer> thisset = this.threadlocalCurrentCounters.get();
                   thisset.add(counterId);
               }
           } else {
               log.error("updateCounter seen locally for counter {} but no global"
                          + "storage exists for it yet .. not updating", counterId);
               return;
           }
       }

       // update local store if enabled locally for updating
       LocalCounterInfo lc = thiscounters[counterId];
       if (lc.enabled) {
           lc.cvalue.increment(incr);
           if (flushNow) {
               DebugCounterInfo dc = ALLCOUNTERS[counterId];
               if (dc.cinfo.enabled) {
                   // globally enabled - flush now
                   dc.cvalue.addAndGet(lc.cvalue.get());
                   lc.cvalue.set(0);
               } else {
                   // global counter is disabled - don't flush, disable locally
                   lc.enabled = false;
                   Set<Integer> thisset = this.threadlocalCurrentCounters.get();
                   thisset.remove(counterId);
               }
           }
       }
   }

   @Override
   public void flushCounters() {
       LocalCounterInfo[] thiscounters =  this.threadlocalCounters.get();
       Set<Integer> thisset = this.threadlocalCurrentCounters.get();
       ArrayList<Integer> temp = new ArrayList<Integer>();

       for (int counterId : thisset) {
           LocalCounterInfo lc = thiscounters[counterId];
           if (lc.cvalue.get() > 0) {
               DebugCounterInfo dc = ALLCOUNTERS[counterId];
               if (dc.cinfo.enabled) {
                   // globally enabled - flush now
                   dc.cvalue.addAndGet(lc.cvalue.get());
                   lc.cvalue.set(0);
               } else {
                   // global counter is disabled - don't flush, disable locally
                   lc.enabled = false;
                   temp.add(counterId);
               }
           }
       }
       for (int cId : temp) {
           thisset.remove(cId);
       }

       // At this point it is possible that the thread-local set does not
       // include a counter that has been enabled and is present in the global set.
       // We need to sync thread-local currently enabled set of counterIds with
       // the global set.
       Sets.SetView<Integer> sv = Sets.difference(currentCounters, thisset);
       for (int counterId : sv) {
           if (thiscounters[counterId] != null) {
               thiscounters[counterId].enabled = true;
               thisset.add(counterId);
           }
       }
   }

   @Override
   public void resetCounterHierarchy(String moduleName, String counterHierarchy) {
       RetCtrInfo rci = getCounterId(moduleName, counterHierarchy);
       if (!rci.allLevelsFound) {
           String missing = rci.levels[rci.foundUptoLevel];
           log.error("Cannot reset counter hierarchy - missing counter {}", missing);
           return;
       }
       // reset at this level
       ALLCOUNTERS[rci.ctrIds[rci.foundUptoLevel - 1]].cvalue.set(0);
       // reset all levels below
       ArrayList<Integer> resetIds = getHierarchyBelow(moduleName, rci);
       for (int index : resetIds) {
           ALLCOUNTERS[index].cvalue.set(0);
       }
   }

   @Override
   public void resetAllCounters() {
       RetCtrInfo rci = new RetCtrInfo();
       rci.levels = "".split("/");
       for (String moduleName : moduleCounters.keySet()) {
           ArrayList<Integer> resetIds = getHierarchyBelow(moduleName, rci);
           for (int index : resetIds) {
               ALLCOUNTERS[index].cvalue.set(0);
           }
       }
   }

   @Override
   public void resetAllModuleCounters(String moduleName) {
       Map<String, CounterIndexStore> target = moduleCounters.get(moduleName);
       RetCtrInfo rci = new RetCtrInfo();
       rci.levels = "".split("/");

       if (target != null) {
           ArrayList<Integer> resetIds = getHierarchyBelow(moduleName, rci);
           for (int index : resetIds) {
               ALLCOUNTERS[index].cvalue.set(0);
           }
       } else {
           if (log.isDebugEnabled()) {
            log.debug("No module found with name {}", moduleName);
        }
       }
   }

   @Override
   public void enableCtrOnDemand(String moduleName, String counterHierarchy) {
       RetCtrInfo rci = getCounterId(moduleName, counterHierarchy);
       if (!rci.allLevelsFound) {
           String missing = rci.levels[rci.foundUptoLevel];
           log.error("Cannot enable counter - counter not found {}", missing);
           return;
       }
       // enable specific counter
       DebugCounterInfo dc = ALLCOUNTERS[rci.ctrIds[rci.foundUptoLevel - 1]];
       dc.cinfo.enabled = true;
       currentCounters.add(dc.cinfo.counterId);
   }

   @Override
   public void disableCtrOnDemand(String moduleName, String counterHierarchy) {
       RetCtrInfo rci = getCounterId(moduleName, counterHierarchy);
       if (!rci.allLevelsFound) {
           String missing = rci.levels[rci.foundUptoLevel];
           log.error("Cannot disable counter - counter not found {}", missing);
           return;
       }
       // disable specific counter
       DebugCounterInfo dc = ALLCOUNTERS[rci.ctrIds[rci.foundUptoLevel - 1]];
       if (dc.cinfo.ctype == CounterType.COUNT_ON_DEMAND) {
           dc.cinfo.enabled = false;
           dc.cvalue.set(0);
           currentCounters.remove(dc.cinfo.counterId);
       }
   }

   @Override
   public List<DebugCounterInfo> getCounterHierarchy(String moduleName,
                                                     String counterHierarchy) {
       RetCtrInfo rci = getCounterId(moduleName, counterHierarchy);
       if (!rci.allLevelsFound) {
           String missing = rci.levels[rci.foundUptoLevel];
           log.error("Cannot fetch counter - counter not found {}", missing);
           return Collections.emptyList();
       }
       ArrayList<DebugCounterInfo> dcilist = new ArrayList<DebugCounterInfo>();
       // get counter and all below it
       DebugCounterInfo dc = ALLCOUNTERS[rci.ctrIds[rci.foundUptoLevel - 1]];
       dcilist.add(dc);
       ArrayList<Integer> belowIds = getHierarchyBelow(moduleName, rci);
       for (int index : belowIds) {
           dcilist.add(ALLCOUNTERS[index]);
       }
       return dcilist;
   }

   @Override
   public List<DebugCounterInfo> getAllCounterValues() {
       List<DebugCounterInfo> dcilist = new ArrayList<DebugCounterInfo>();
       RetCtrInfo rci = new RetCtrInfo();
       rci.levels = "".split("/");

       for (String moduleName : moduleCounters.keySet()) {
           ArrayList<Integer> resetIds = getHierarchyBelow(moduleName, rci);
           for (int index : resetIds) {
               dcilist.add(ALLCOUNTERS[index]);
           }
       }
       return dcilist;
   }

   @Override
   public List<DebugCounterInfo> getModuleCounterValues(String moduleName) {
       List<DebugCounterInfo> dcilist = new ArrayList<DebugCounterInfo>();
       RetCtrInfo rci = new RetCtrInfo();
       rci.levels = "".split("/");

       if (moduleCounters.containsKey(moduleName)) {
           ArrayList<Integer> resetIds = getHierarchyBelow(moduleName, rci);
           for (int index : resetIds) {
               dcilist.add(ALLCOUNTERS[index]);
           }
       }
       return dcilist;
   }

   @Override
   public boolean containsModuleCounterHierarchy(String moduleName,
                                                 String counterHierarchy) {
       if (!moduleCounters.containsKey(moduleName)) {
        return false;
    }
       RetCtrInfo rci = getCounterId(moduleName, counterHierarchy);
       return rci.allLevelsFound;
   }

   @Override
   public boolean containsModuleName(String moduleName) {
       return  (moduleCounters.containsKey(moduleName)) ? true : false;
   }

   @Override
   public List<String> getModuleList() {
       List<String> retval = new ArrayList<String>();
       retval.addAll(moduleCounters.keySet());
       return retval;
   }

   @Override
   public List<String> getModuleCounterList(String moduleName) {
       if (!moduleCounters.containsKey(moduleName)) {
        return Collections.emptyList();
    }

       List<String> retval = new ArrayList<String>();
       RetCtrInfo rci = new RetCtrInfo();
       rci.levels = "".split("/");

       ArrayList<Integer> cids = getHierarchyBelow(moduleName, rci);
       for (int index : cids) {
           retval.add(ALLCOUNTERS[index].cinfo.counterHierarchy);
       }
       return retval;
   }

   //*******************************
   //   Internal Methods
   //*******************************

   protected class RetCtrInfo {
       boolean allLevelsFound; // counter indices found all the way down the hierarchy
       boolean hierarchical; // true if counterHierarchy is hierarchical
       int foundUptoLevel;
       int[]  ctrIds;
       String[] levels;

       public RetCtrInfo() {
           ctrIds = new int[MAX_HIERARCHY];
           for (int i = 0; i < MAX_HIERARCHY; i++) {
               ctrIds[i] = -1;
           }
       }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getOuterType().hashCode();
        result = prime * result + (allLevelsFound ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(ctrIds);
        result = prime * result + foundUptoLevel;
        result = prime * result + (hierarchical ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(levels);
        return result;
    }

    @Override
    public boolean equals(Object oth) {
        if (!(oth instanceof RetCtrInfo)) {
         return false;
     }
        RetCtrInfo other = (RetCtrInfo) oth;
        if (other.allLevelsFound != this.allLevelsFound) {
         return false;
     }
        if (other.hierarchical != this.hierarchical) {
         return false;
     }
        if (other.foundUptoLevel != this.foundUptoLevel) {
         return false;
     }
        if (!Arrays.equals(other.ctrIds, this.ctrIds)) {
         return false;
     }
        if (!Arrays.equals(other.levels, this.levels)) {
         return false;
     }
        return true;
    }

    private DebugCounter getOuterType() {
        return DebugCounter.this;
    }



   }

   protected RetCtrInfo getCounterId(String moduleName, String counterHierarchy) {
       RetCtrInfo rci = new RetCtrInfo();
       Map<String, CounterIndexStore> templevel = moduleCounters.get(moduleName);
       rci.levels = counterHierarchy.split("/");
       if (rci.levels.length > 1) {
        rci.hierarchical = true;
    }
       if (templevel == null) {
           log.error("moduleName {} does not exist in debugCounters", moduleName);
           return rci;
       }

       /*
       if (rci.levels.length > MAX_HIERARCHY) {
           // chop off all array elems greater that MAX_HIERARCHY
           String[] temp = new String[MAX_HIERARCHY];
           System.arraycopy(rci.levels, 0, temp, 0, MAX_HIERARCHY);
           rci.levels = temp;
       }
       */
       for (int i = 0; i < rci.levels.length; i++) {
           if (templevel != null) {
               CounterIndexStore cis = templevel.get(rci.levels[i]);
               if (cis == null) {
                   // could not find counterHierarchy part at this level
                   break;
               } else {
                   rci.ctrIds[i] = cis.index;
                   templevel = cis.nextLevel;
                   rci.foundUptoLevel++;
                   if (i == rci.levels.length - 1) {
                       rci.allLevelsFound = true;
                   }
               }
           } else {
               // there are no more levels, which means that some part of the
               // counterHierarchy has no corresponding map
               break;
           }
       }
       return rci;
   }

   protected void addToModuleCounterHierarchy(String moduleName, int counterId,
                                            RetCtrInfo rci) {
       Map<String, CounterIndexStore> target = moduleCounters.get(moduleName);
       if (target == null) {
        return;
    }
       CounterIndexStore cis = null;

       for (int i = 0; i < rci.foundUptoLevel; i++) {
           cis = target.get(rci.levels[i]);
           target = cis.nextLevel;
       }
       if (cis != null) {
           if (cis.nextLevel == null) {
            cis.nextLevel = new ConcurrentHashMap<String, CounterIndexStore>();
        }
           cis.nextLevel.put(rci.levels[rci.foundUptoLevel],
                             new CounterIndexStore(counterId, null));
       } else {
           target.put(rci.levels[rci.foundUptoLevel],
                      new CounterIndexStore(counterId, null));
       }
   }

   // given a partial hierarchical counter, return the rest of the hierarchy
   protected ArrayList<Integer> getHierarchyBelow(String moduleName, RetCtrInfo rci) {
       Map<String, CounterIndexStore> target = moduleCounters.get(moduleName);
       CounterIndexStore cis = null;
       ArrayList<Integer> retval = new ArrayList<Integer>();
       if (target == null) {
        return retval;
    }

       // get to the level given
       for (int i = 0; i < rci.foundUptoLevel; i++) {
           cis = target.get(rci.levels[i]);
           target = cis.nextLevel;
       }

       if (target == null || rci.foundUptoLevel == MAX_HIERARCHY) {
           // no more levels
           return retval;
       } else {
           // recursively get all ids
           getIdsAtLevel(target, retval, rci.foundUptoLevel + 1);
       }

       return retval;
   }

   protected void getIdsAtLevel(Map<String, CounterIndexStore> hcy,
                                ArrayList<Integer> retval, int level) {
       if (level > MAX_HIERARCHY) {
        return;
    }
       if (hcy == null || retval == null) {
        return;
    }

       // Can return the counter names as well but for now ids are enough.
       for (CounterIndexStore cistemp : hcy.values()) {
           retval.add(cistemp.index); // value at this level
           if (cistemp.nextLevel != null) {
               getIdsAtLevel(cistemp.nextLevel, retval, level + 1);
           }
       }
   }

}
