package com.mikehelmick.proc;

import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.log4j.Logger;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import static com.mikehelmick.proc.Message.MessageBuilder;

/**
 * Message delivery object for a single process.
 */
public class Mailman {
  private static Logger logger = Logger.getLogger(Mailman.class);

  private final Proc process;
  private boolean busy;
  
  // Map of sender to messages from this sender.
  Map<Long, LinkedBlockingDeque<Envelope>> mailboxes = Maps.newConcurrentMap();
  
  public Mailman(final Proc process, final Long maxProcess) {
    this.process = process;
    for (long pid = 1L; pid <= maxProcess; pid++) {
      if (pid != process.getProcessId().longValue()) {
        LinkedBlockingDeque<Envelope> queue = Queues.newLinkedBlockingDeque();
        mailboxes.put(pid, queue);
      }
    }
  }

  public Proc getProcess() {
    return process;
  }
  
  public boolean isBusy() {
    return busy;
  }
  
  public void markBusy() {
    busy = true;
  }

  public synchronized void addMessage(MessageBuilder builder) {
    final Message message = builder.setReceiver(process.getProcessId()).build();
    logger.debug("Mailman for : " + process.getProcessId() + " received message: " + message);
    LinkedBlockingDeque<Envelope> destQueue = mailboxes.get(message.getSender());
    if (destQueue.isEmpty()) {
      destQueue.offer(new Envelope(message, null));
    } else {
      destQueue.offer(new Envelope(message, destQueue.getLast()));
    }
  }
  
  private synchronized RandomWindow buildRandomWindow() {
    // Select a random sender, deliver message
    int messageCount = 0;
    RandomWindow rw = new RandomWindow();
    long systemTime = SystemTime.getTime();
    for (Map.Entry<Long, LinkedBlockingDeque<Envelope>> entry : mailboxes.entrySet()) {
      messageCount += entry.getValue().size();
      if (!entry.getValue().isEmpty()) {
        long deliverAfter = entry.getValue().peek().getDeliverAfter();
        if (deliverAfter < systemTime) {
          rw.addInterval(entry.getKey(), systemTime - deliverAfter);
        }
      }
    }
    logger.debug(messageCount + " pending messages for pid: " + getProcess().getProcessId());
    return rw;
  }
  
  public boolean hasMessagesToSend() {
    return !buildRandomWindow().isEmpty();
  }

  public boolean deliverOneMessage() {
    final Message message;
    int messageCount = 0;

    RandomWindow rw = buildRandomWindow();
    if (rw.isEmpty()) {
      message = null; 
    } else {
      Long selectedSender = rw.nextRandom();
      message = mailboxes.get(selectedSender).poll().getMessage();
    }
    
    try {
      // Actually run this message through.
      if (message == null) {
        logger.debug(messageCount + " messages for pid: " + process.getProcessId()
            + " but all are being delayed.");
        return false;
      }
      
      logger.debug("Delivering message: " + message.toString());
      process.receiveMessage(message);
      logger.debug("Finished with message delivery: " + message.toString());    
    } catch (Exception ex) {
      logger.fatal("receiveMessage caused an exception", ex);
      System.exit(1);
    } finally {
      busy = false;
    }
    return true;
  }
}
