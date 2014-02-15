package com.mikehelmick.proc;

public class InvalidProcessId extends RuntimeException {

  private static final long serialVersionUID = -2815114956954445101L;

    public InvalidProcessId(String message) {
      super(message);
    }
}
