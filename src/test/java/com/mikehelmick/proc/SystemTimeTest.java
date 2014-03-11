package com.mikehelmick.proc;

import static org.junit.Assert.*;

import org.junit.Test;

public class SystemTimeTest {

  /**
   * Unless this test runs infinitely, we can't be sure that the code is correct.
   * We will do 10,000,000 iterations and call it a day.
   */
  @Test
  public void testUniqueness() {
    long lastTime = 0;
    for (int i = 0; i < 10000000; i++) {
      long curTime = SystemTime.getTime();
      assertTrue(curTime > lastTime);
      lastTime = curTime;
    }
  }

}
