package com.mikehelmick.proc;

import java.io.Serializable;

import com.google.common.annotations.VisibleForTesting;

/**
 * Message that is sent from one process to another (or all other) process(es).
 * This framework does NOT interpret the payload (a string) in any way.
 * </p>
 * Create messages using the {@link MessageBuilder}. When you send a message
 * the sender and receiver are filled in by the message routing code to ensure correctness / 
 * to avoid spoofing. A future version may allow for ovrriding of this behavior to allow
 * for greater fault simulation.
 */
public final class Message implements Serializable {

  private static final long serialVersionUID = -7079069657584794688L;
 
  private final Clock clock;
  private final Long sender;
  private final Long receiver;
  private final String message;
  
  Message(Clock clock, Long sender, Long receiver, String message) {
    this.clock = clock;
    this.sender = sender;
    this.receiver = receiver;
    this.message = message;
  }

  public Clock getClock() {
    return clock;
  }

  public String getMessage() {
    return message;
  }

  public Long getSender() {
    return sender;
  }

  public Long getReceiver() {
    return receiver;
  }
  
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append(sender);
    b.append("->");
    b.append(receiver);
    b.append(" @ ");
    b.append(clock);
    b.append(" :: ");
    b.append(message);
    return b.toString();
  }

  public static MessageBuilder builder() {
    return new MessageBuilder();
  }
  
  public static final class MessageBuilder {
    private Clock clock;
    private Long sender;
    private Long receiver;
    private String message;    

    public MessageBuilder() {
    }
    
    public static MessageBuilder create() {
      return new MessageBuilder();
    }
    
    public MessageBuilder setClock(Clock clock) {
      this.clock = clock;
      return this;
    }
    
    public MessageBuilder setMessage(String message) {
      this.message = message;
      return this;
    }
    
    @VisibleForTesting
    public MessageBuilder setSender(Long sender) {
      this.sender = sender;
      return this;
    }

    @VisibleForTesting
    public MessageBuilder setReceiver(Long receiver) {
      this.receiver = receiver;
      return this;
    }
    
    Message build() {
      return new Message(clock, sender, receiver, message);
    }
  }
}
