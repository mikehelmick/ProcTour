package com.mikehelmick.proc;

import java.io.Serializable;

/**
 * Clock is a marker interface. You have to decide how to represent clocks in your system.
 * Do so by implementing this interface.
 */
public interface Clock extends Serializable, Comparable<Clock> {

  public abstract String toString();

}
