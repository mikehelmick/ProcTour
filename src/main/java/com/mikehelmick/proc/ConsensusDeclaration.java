package com.mikehelmick.proc;

import java.util.Map;

import com.google.common.collect.Maps;

public final class ConsensusDeclaration {

  private final String data;
  private final Map<Long, Clock> voters = Maps.newConcurrentMap();
  
  private final long numProcs;
  private final long numRequired;
  
  public ConsensusDeclaration(String data, long numProcs) {
    this(data, numProcs, numProcs);
  }

  public ConsensusDeclaration(String data, long numProcs, long numRequired) {
    this.data = data;
    this.numProcs = numProcs;
    this.numRequired = numRequired;
  }
  
  public void addConsensus(Long pid, Clock time) {
    voters.put(pid, time);
  }
  
  /**
   * @return true if at least numRequired processes have checked in.
   */
  public boolean isSufficient() {
    return voters.size() >= numRequired;
  }
 
  public boolean isComplete() {
    return voters.size() >= numProcs;
  }
  
  public String getData() {
    return data;
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof ConsensusDeclaration) {
      ConsensusDeclaration that = (ConsensusDeclaration) other;
      return this.data.equals(that.data);
    }
    return false; 
  } 
}
