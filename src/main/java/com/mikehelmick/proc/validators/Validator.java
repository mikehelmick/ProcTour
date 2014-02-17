package com.mikehelmick.proc.validators;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mikehelmick.proc.Clock;
import com.mikehelmick.proc.ConsensusDeclaration;

public abstract class Validator {
  
  protected static long numProcesses;
  // Consensus name to ConsensusDeclaration
  protected static final Map<String, ConsensusDeclaration> consensusDeclarations = Maps.newConcurrentMap();
  // Resource id, to a sorted Set of time (Sorted, by the Clock ordering)
  protected static final Map<Integer, Set<ResourceEvent>> resourceEvents = Maps.newConcurrentMap();
  
  public static void start(long numProcs) {
    numProcesses = numProcs;
  }
  
  protected Map<String, ConsensusDeclaration> getConsensusDeclarations() {
    return consensusDeclarations;
  }
  
  protected Map<Integer, Set<ResourceEvent>> getResourceEvents() {
    return resourceEvents;
  }
  
  /**
   * Validates the simulation.
   * 
   * @return true if the simulation is valid. 
   * @throws InvalidSimulationOutputException with the details of why the simulation isn't valid
   */
  public abstract boolean validate() throws InvalidSimulationOutputException;
  
  /**
   * A consensus declaration has been created or updated.
   */
  public static void concensusDeclaration(ConsensusDeclaration cd) {
    consensusDeclarations.put(cd.getData(), cd);
  }
  
  private static Set<ResourceEvent> getResourceEventSet(Integer resourceNumber) {
    Set<ResourceEvent> reSet;
    synchronized (resourceEvents) {
      reSet = resourceEvents.get(resourceNumber);
      if (reSet == null) {
        reSet = Sets.newTreeSet();
        reSet = Collections.synchronizedSet(reSet);
        resourceEvents.put(resourceNumber, reSet);
      }
    }
    return reSet;
  }
  
  public static void resourceDeclaration(Integer resourceNumber, Long pid, Clock time) {
    getResourceEventSet(resourceNumber).add(new ResourceEvent(time, pid, true));
  }
  
  public static void resourceReleased(Integer resourceNumber, Long pid, Clock time) {
    getResourceEventSet(resourceNumber).add(new ResourceEvent(time, pid, false));
  }
  
  public static class ResourceEvent implements Comparable<ResourceEvent> {
    private final Clock clock;
    private final Long pid;
    private final boolean declaration;
    
    public ResourceEvent(Clock clock, Long pid, boolean declaration) {
      this.clock = clock;
      this.pid = pid;
      this.declaration = declaration;
    }

    public Clock getClock() {
      return clock;
    }

    public Long getPid() {
      return pid;
    }

    public boolean isDeclaration() {
      return declaration;
    }
 
    public boolean isRelease() {
      return !declaration;
    }

    @Override 
    public boolean equals(Object o) {
      if (!(o instanceof ResourceEvent)) {
        return false;
      }
      ResourceEvent that = (ResourceEvent) o;
      return clock.equals(that.clock);
    }

    @Override
    public int compareTo(ResourceEvent o) {
      return this.clock.compareTo(o.clock);
    }
  }
}
