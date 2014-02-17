package com.mikehelmick.proc.validators;

import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class MutualExclusionValidator extends Validator {
  
  public static class MutualExclusionValidatorBuilder {
    private int acquiresPerProcess = 1;
    private int numProcesses = 0;
    private int numResources = 0;
    
    MutualExclusionValidatorBuilder() {
    }
    
    public MutualExclusionValidatorBuilder setAcquiresPerProcess(int numAcquires) {
      Preconditions.checkArgument(numAcquires >= 1);
      acquiresPerProcess = numAcquires;
      return this;
    }
    
    public MutualExclusionValidatorBuilder setNumProcesses(int numProcs) {
      Preconditions.checkArgument(numProcs >= 2);
      numProcesses = numProcs;
      return this;
    }
    
    public MutualExclusionValidatorBuilder setNumResources(int numResources) {
      Preconditions.checkArgument(numResources >= 1);
      this.numResources = numResources;
      return this;
    }

    public MutualExclusionValidator build() {
      return new MutualExclusionValidator(acquiresPerProcess, numProcesses, numResources);
    }
  }

  private final int acquiresPerProcess;
  private final int numProcesses;
  private final int numResources;

  private MutualExclusionValidator(int acquiresPerProcess, int numProcesses, int numResources) {
    this.acquiresPerProcess = acquiresPerProcess;
    this.numProcesses = numProcesses;
    this.numResources = numResources;
  }

  @Override
  public boolean validate() throws InvalidSimulationOutputException {
    final StringBuffer errors = new StringBuffer();
    
    // For each resource
    for (Integer resource = 1; resource <= numResources; resource = resource + 1) {
      // Count the number of acquires per process and that the next element is a release.
      final List<ResourceEvent> events = Lists.newArrayList(getResourceEvents().get(resource));
      final Map<Long, Integer> counts = Maps.newHashMap();
      for (long p = 1; p <= numProcesses; p++) {
        counts.put(p, 0);
      }
      
      for (int i = 0; i < events.size() - 1; i = i + 2) {
        ResourceEvent acquire = events.get(i);
        ResourceEvent release = events.get(i + 1);
        
        boolean ok = true;
        if (!acquire.isDeclaration()) {
          errors.append("Expected event #" + i + " to be an acquire, but it was not.\n");
          ok = false;
        }
        if (release.isDeclaration()) {
          errors.append("Expected event #" + (i + 1) + " to be a release, but it was not.\n");
          ok = false;
        }
        if (acquire.getPid().equals(release.getPid())) {
          errors.append("Events #" + i + " and #" + (i + 1) + " do not have the same PID, but should. "
              + "(" + acquire.getPid() + ", " + release.getPid() + ").\n");
          ok = false;
        }
        if (ok) {
          counts.put(acquire.getPid(), counts.get(acquire.getPid()) + 1);
        }
      }
      // Check the counts.
      for (Map.Entry<Long, Integer> count : counts.entrySet()) {
        if (count.getValue() != acquiresPerProcess) {
          errors.append("Process id: " + count.getKey() + " acquired resource #" + resource + " : "
              + count.getValue() + " times, but expected " + acquiresPerProcess + "\n");
        }
      }
    }
    
    final String errorStr = errors.toString();
    if (!"".equals(errorStr)) {
      throw new InvalidSimulationOutputException(errorStr);
    }
    return true;
  }
}
