package com.mikehelmick.proc;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a monotonically increasing clock based on the system time.
 * Framework internal, but you may use it if you like. 
 * </p>
 * Very frequent accesses to this will advance the clock rapidly.
 */
public class SystemTime {

  private static AtomicLong lastTime = new AtomicLong(0);
  
  public static long getTime() {
    long expected = lastTime.get();
    long curTime = System.currentTimeMillis();
    if (expected >= curTime) {
      return advance(expected, expected + 1);
    }
    return advance(expected, curTime);
  }
  
  private static long advance(long expected, long newTime) {
    while (!lastTime.compareAndSet(expected, newTime)) {
      expected = lastTime.get();
      newTime = expected + 1;
    }
    return newTime;
  }
}
