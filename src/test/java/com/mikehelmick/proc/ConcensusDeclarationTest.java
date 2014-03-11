package com.mikehelmick.proc;

import org.junit.Test;
import static org.junit.Assert.*;

import com.mikehelmick.proc.validators.FakeClock;

public class ConcensusDeclarationTest {

  @Test
  public void testConcensus() {
    ConsensusDeclaration cd = new ConsensusDeclaration("mark1", 3);
    cd.addConsensus(1L, new FakeClock());
    assertFalse(cd.isSufficient() || cd.isComplete());
    cd.addConsensus(2L, new FakeClock());
    assertFalse(cd.isSufficient() || cd.isComplete());
    cd.addConsensus(3L, new FakeClock());
    assertTrue(cd.isSufficient() && cd.isComplete());
  }
}
