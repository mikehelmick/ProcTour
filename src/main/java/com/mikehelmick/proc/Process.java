package com.mikehelmick.proc;

public abstract class Process {

	private final Long processId;
	protected boolean inShutdownMode = false;

	public Process() {
		processId = ProcessManager.getInstance().register(this);
	}
	
	/** 
	 * Perform any computations that should be done that are not related to
	 * receiving a message.
	 * These computations should be very quick, ideally measured in milliseconds.
	 */
	public abstract void tick();
	
	/**
	 * You are receiving a message. Process this message as appropriate for the
	 * algorithm that is being implemented. It is normal that receiving a message
	 * could cause messages to be sent or resources to be claimed.
	 */
	public abstract void receiveMessage(Message message);
	
	final void shutdown() {
	  inShutdownMode = true;
	}
	
	final Long getProcessId() {
	  return processId;
	}

	final void sendAll(Message.MessageBuilder builder) {
	  ProcessManager.getInstance().send(builder, this);
	}
	
	final void sendOne(Message.MessageBuilder builder, Long receiver) {
	  ProcessManager.getInstance().sendOne(builder, receiver, this);
	}

	final void declareResourceOwnership(Clock time, int resourceNum) {
	  ProcessManager.getInstance().declareOwnership(time, resourceNum, this);
	}
	
	final void releaseResourceOwnership(Clock time, int resourceNum) {
	  ProcessManager.getInstance().releaseOwnership(time, resourceNum, this);
	}
	
	final void declareConcensus(Clock time, String data) {
	  ProcessManager.getInstance().declareConcensus(time, data, this);
	}
	
	@Override
	public final boolean equals(Object other) {
	  if (!(other instanceof Process)) {
	    return false;
	  }
	  return processId.equals(((Process)other).processId);
	}
}
