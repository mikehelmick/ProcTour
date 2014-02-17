package com.mikehelmick.proc;

import java.io.Serializable;

class Envelope implements Serializable {

  private static final long serialVersionUID = 2472152490678148057L;
  private final Message message;
  private final long systemTime;
  
  Envelope(Message message) {
    this.message = message;
    this.systemTime = SystemTime.getTime();
  }

  public Message getMessage() {
    return message;
  }

  public long getSystemTime() {
    return systemTime;
  }
}
