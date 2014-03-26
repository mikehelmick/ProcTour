package com.mikehelmick.proc;

import java.io.Serializable;
import java.util.Random;

import org.apache.log4j.Logger;

/**
 * Framework internal envelope for messages. The system time
 * is used in weighting the random message delays.</p>
 * Older messages are more likely to get delivered than newer messages.
 */
class Envelope implements Serializable {
  private static Logger logger = Logger.getLogger(Envelope.class);
  private static final Random rand = new Random(SystemTime.getTime());

  private static final long serialVersionUID = 2472152490678148057L;
  private final Message message;
  private final long systemTime;
  private final long deliverAfter;
  
  Envelope(Message message, Envelope precedingMessage) {
    this.message = message;
    this.systemTime = SystemTime.getTime();
    deliverAfter = calculateDeliverAfterTime(precedingMessage);
    logger.debug("Scheduled message for delivery after: " + deliverAfter
        + " curTime: " + systemTime);
  }
  
  private long calculateDeliverAfterTime(Envelope precedingMessage) {
    Long curTime = SystemTime.getTime();
    long earliestDeliveryTime = curTime;
    if (precedingMessage != null) {
      earliestDeliveryTime = precedingMessage.deliverAfter + 1;
    }
    long delay = 0;
    if (rand.nextBoolean()) {
      // Delay up to 5 seconds
      delay = Math.abs(rand.nextLong() % 2500);
      // Ensure delay is past other deliveries
      while (curTime + delay < earliestDeliveryTime) {
        delay += (earliestDeliveryTime - curTime) + delay / 2;
      }
    }
    return curTime + delay;
  }

  public Message getMessage() {
    return message;
  }

  public long getSystemTime() {
    return systemTime;
  }

  public long getDeliverAfter() {
    return deliverAfter;
  }
}
