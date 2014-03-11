package com.mikehelmick.proc.validators;

import com.mikehelmick.proc.Clock;
import com.mikehelmick.proc.SystemTime;

/**
 * This is a fake clock that uses the provided SystemClock class.
 * This guarantees that the can be a total ordering, but this isn't what you want.
 */
public class FakeClock implements Clock {
  
  private static final long serialVersionUID = -4726993618044420577L;
  final long time;
  
  public FakeClock() {
    time = SystemTime.getTime();
  }
  
  @Override
  public String toString() {
    return Long.toString(time);
  }

  @Override
  public int compareTo(Clock other) {
    if (other instanceof FakeClock) {
      return Long.valueOf(time).compareTo(Long.valueOf(((FakeClock)other).time));
    }
    // compare the strings? what else can we do?
    return this.toString().compareTo(other.toString());
  }
}
