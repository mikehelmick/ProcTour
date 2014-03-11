package com.mikehelmick.proc.validators;

public class InvalidSimulationOutputException extends Exception {

  private static final long serialVersionUID = -7208134544195576433L;

  public InvalidSimulationOutputException(String message) {
    super(message);
  }  
}
