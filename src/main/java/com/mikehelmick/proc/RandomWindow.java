package com.mikehelmick.proc;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

/**
 * Framework internal class. Used for weighted random in generating message delays.
 */
public class RandomWindow {
  private static Random rand = new Random(System.currentTimeMillis());
  
  private final List<Pairing> pairings = Lists.newArrayList();
  private long maxVal = 0;;
  
  public RandomWindow() {
    
  }

  public void addInterval(long val, long interval) {
    pairings.add(new Pairing(val, maxVal + interval));
    maxVal += interval;
  }

  public long nextRandom() {
    long randVal = rand.nextLong() % maxVal;
    for (Pairing pairing : pairings) {
      if (randVal < pairing.interval) {
        return pairing.val;
      }
    }
    return pairings.get(pairings.size() - 1).val;
  }
  
  private static class Pairing {
    private final long val;
    private final long interval;
    
    Pairing(long val, long interval) {
      this.val = val;
      this.interval = interval;
    }
  }

}
