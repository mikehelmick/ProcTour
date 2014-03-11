package com.mikehelmick.proc;

import java.io.Serializable;

/**
 * Framework internal envelope for messages. The system time
 * is used in weighting the random message delays.</p>
 * Older messages are more likely to get delivered than newer messages.
 */
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
