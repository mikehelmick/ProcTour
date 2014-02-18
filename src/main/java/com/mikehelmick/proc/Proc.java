package com.mikehelmick.proc;

/**
 * Extend this class to write your processes in the distributed system. Your process
 * will receive periodic heartbeats (calls to the tick method). {@link Message} objects
 * that are destined for your process will be sent via the {@link receiveMessage} method.
 * </p>
 * <strong>Your process class must either be reentrant.</strong> The tick and receiveMessage
 * methods may be called at the same time.
 * </p>
 * Both tick and receiveMessage should execute quickly (well under a second). If you need to do long running work,
 * consider starting a companion thread for each process, but ideally this will not be necessary. 
 * </p>
 * To send messages, use with the {@link sendAll} or {@link sendOne} methods.
 */
public abstract class Proc {

	protected final Long processId;
	protected boolean inShutdownMode = false;

	public Proc() {
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
	
	protected final Long getProcessId() {
	  return processId;
	}
	
	protected final long getProcessCount() {
	  return ProcessManager.getInstance().getProcessCount();
	}

	/**
	 * Send a message, based on the passed in {@link Message.MessageBuilder} object
	 * to ALL other processes in the system (multicast). 
	 */
	protected final void sendAll(Message.MessageBuilder builder) {
	  ProcessManager.getInstance().send(builder, this);
	}

	/**
	 * Send a message to a single receiver (unicast).
	 */
	protected final void sendOne(Message.MessageBuilder builder, Long receiver) {
	  ProcessManager.getInstance().sendOne(builder, receiver, this);
	}

	/**
	 * For resource mutual exclusion. Make a declaration that this process
	 * now owns this resource. Resources are numbered from 1 to the maximum number
	 * configured, available via {@link ProcessManager.getSharedResouceCount}.
	 * </p>
	 * If the declaration is incorrect, an exception will be thrown and the simulation will halt.
	 */
	protected final void declareResourceOwnership(Clock time, int resourceNum) {
	  ProcessManager.getInstance().declareOwnership(time, resourceNum, this);
	}

	/**
	 * Release the shared resource back to the resource pool. If you make a release
	 * but don't currently own the resource, an exception will be thrown.
	 */
	protected final void releaseResourceOwnership(Clock time, int resourceNum) {
	  ProcessManager.getInstance().releaseOwnership(time, resourceNum, this);
	}
	
	/**
	 * Declare that some consensus position (represented by the String {@link data}) is true.
   * Useful for testing paxos implementations, amoung other things.
	 */
	protected final void declareConsensus(Clock time, String data) {
	  ProcessManager.getInstance().declareConsensus(time, data, this);
	}
	
	@Override
	public final boolean equals(Object other) {
	  if (!(other instanceof Proc)) {
	    return false;
	  }
	  return processId.equals(((Proc)other).processId);
	}
}
