package com.mikehelmick.proc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.MoreExecutors;

public final class ProcessManager {
  private static Logger logger = Logger.getLogger(ProcessManager.class);
  
  public static enum State {
    STARTUP,
    RUNNING,
    SHUTDOWN
  }
  
  public static final int MAX_RESOURCE_COUNT = 100;
  public static final int THREADS = 4;

  private static ProcessManager INSTANCE = new ProcessManager();

  private Map<Long, Process> procMap = Maps.newConcurrentMap();
  private Map<Long, LinkedBlockingQueue<Message>> mailboxes = Maps.newConcurrentMap();
  private Map<Integer, Process> resources = Maps.newConcurrentMap();
  private long maxPid = 0;
  private State state = State.STARTUP;
  private Integer resourceCount = 1;
  
  // The order of messages and ticks.
  private BlockingQueue<Long> executionQueue = Queues.newLinkedBlockingQueue();
  private ScheduledFuture<?> heartbeatHandle;
  private ScheduledExecutorService executor;
  private HeartBeat heartBeat;
  private MessageRouter messageRouter;

  private ProcessManager() {
    // Nothing left to do.
    logger.info("initialized");
  }

  @VisibleForTesting
  static void reset() {
    if (!INSTANCE.state.equals(State.SHUTDOWN)) {
      throw new IllegalStateException("Simulation must be shutdown before reset.");
    }
    INSTANCE = null;
    INSTANCE = new ProcessManager();
  }
 
  public static ProcessManager getInstance() {
    return INSTANCE;
  }
  
  private class HeartBeat implements Runnable {
    private final Logger hbLogger = Logger.getLogger(HeartBeat.class);

    private final List<Long> procIds;
    private long heartbeats = 0;
    
    HeartBeat(long maxProc) {
      hbLogger.info("HeartBeat created");
      procIds = Lists.newArrayList();
      for (long pid = 1; pid <= maxProc; pid++) {
        procIds.add(pid);
      }
    }
    
    long getHeartbeats() {
      return heartbeats;
    }
    
    @Override
    public void run() {
      heartbeats++;
      hbLogger.info("Sending heartbeats");
      Collections.shuffle(procIds);
      for (Long pid : procIds) {
        final Process process = procMap.get(pid);
        executor.submit(
            new Runnable() {
              @Override
              public void run() {
                process.tick();
              }                  
            });
      }
    }
  }
  
  /**
   * Reads the execution queue and the messages outstanding.
   * Random delays are inserted.
   */
  private class MessageRouter implements Runnable {
    private Logger mrLogger = Logger.getLogger(MessageRouter.class);
    private boolean keepGoing = true;
    private final Random rand;
    private final Map<Long, Long> pidToEarliestDelivery = Maps.newConcurrentMap();
    
    private long messages = 0;
    
    MessageRouter(Long maxPid) {
      for (long pid = 1; pid <= maxPid; pid++) {
        pidToEarliestDelivery.put(pid, SystemTime.getTime());
      }
      rand = new Random(SystemTime.getTime());
    }

    void shutdown() {
      keepGoing = false;
    }
    
    long getMessages() {
      return messages;
    }
    
    @Override
    public void run() {
      mrLogger.info("Starting message router");
      while (keepGoing) {
        try {
          final Long pid = executionQueue.poll(5, TimeUnit.SECONDS);
          if (pid == null) {
            mrLogger.info("No messages to route.");
            continue;
          }
          final Process process = procMap.get(pid);
          
          // See if there is a message for this process
          final Message message = mailboxes.get(pid).poll();
          if (message != null) {
            mrLogger.debug("scheduling message pid: " + pid + " message: " + message);
            messages++;
            // insert a random delay random delay
            Long curTime = SystemTime.getTime();
            long delay = 0;
            if (rand.nextBoolean()) {
              // Delay up to 5 seconds
              delay = rand.nextLong() % 5000;
              // Ensure delay is past other deliveries
              if (curTime + delay < pidToEarliestDelivery.get(pid)) {
                delay = (pidToEarliestDelivery.get(pid) - curTime) + delay / 2;
              }
            }
            pidToEarliestDelivery.put(pid, curTime + delay);
            executor.schedule(
                // TODO(@mikehelmick) - add some synchronization here
                new Runnable() {
                  @Override
                  public void run() {
                    mrLogger.debug("delivering message pid: " + pid + " message: " + message);
                    process.receiveMessage(message);
                  }
                },
                delay, TimeUnit.MILLISECONDS);
          } else {
            mrLogger.warn("Inconsistency - nothing in the mailbox for pid: " + pid + ", but told to send message");
          }
          
        } catch (InterruptedException e) {
          Thread.interrupted();
        }
      }
    }
  }
  
  public int getSharedResouceCount() {
    return resources.size();
  }
  
  public synchronized void setSharedResourceCount(int resourceCount) {
    if (resourceCount < 1 || resourceCount > MAX_RESOURCE_COUNT) {
      throw new IllegalArgumentException("Resource count must be between 1 and " + MAX_RESOURCE_COUNT + ", inclusive.");
    }
    resources.clear();
    // No need to put anything in the resource map. If unheld, we will leave that slot empty (null);
  }

  public synchronized void scheduleShutdown(long delay, TimeUnit unit) {
    if (!(state.equals(State.RUNNING))) {
      logger.error("Attempt to schedule a future shutdown when simulation isn't running");
      throw new IllegalStateException("Cannot schedule shutdown unless simulation is running");
    }
    
    // Simply schedule the simultion to reuest an orderly shutdown at some point in the future.
    executor.schedule(
        new Runnable() {
          @Override
          public void run() {
            shutdown();
          }
        }, delay, unit);
  }
  
  public synchronized void start() {
    logger.info("Startup requested");
    if (!(state.equals(State.STARTUP))) {
      throw new IllegalStateException("Cannot start simulation, not in startup state.");
    }
    if (procMap.isEmpty()) {
      throw new IllegalStateException("No processes have been created. Unable to start simulation");
    }
    
    // Create the execution pool
    logger.info("Creating thread pool");
    executor = Executors.newScheduledThreadPool(THREADS);
    // Create the heartbeat, this will get things started. Hopefully.
    heartBeat = new HeartBeat(maxPid);
    heartbeatHandle = executor.scheduleWithFixedDelay(
        heartBeat, 1, 5, TimeUnit.SECONDS);
    logger.info("Thread pool initialized, simulator running");
    
    logger.info("Message router starting up.");
    messageRouter = new MessageRouter(maxPid);
    Thread mrThread = new Thread(messageRouter);
    mrThread.start();
    logger.info("Message router is running.");
    
    state = State.RUNNING;
  }
  
  public synchronized void shutdown() {
    if (!(state.equals(State.RUNNING))) {
      throw new IllegalStateException("Simulation is not running, can not shutdown.");
    }
    
    logger.info("Shutdown requested");
    logger.info("Stopping message router - all pending messages will be lost");
    messageRouter.shutdown();
    
    logger.info("Stopping executor service");
    state = State.SHUTDOWN;
    heartbeatHandle.cancel(false);
    executor.shutdown();
    logger.info("Execution service shutdown entered.");
    
    long maxWaitTime = TimeUnit.MINUTES.toMillis(1) + System.currentTimeMillis();
    while (System.currentTimeMillis() < maxWaitTime && !executor.isTerminated()) {
      logger.info(" ... awaiting shutdown ...");
      try {
        // Ensure we don't have a negative sleep amount.
        long sleepTime = Math.max(1, (maxWaitTime - System.currentTimeMillis()) / 4);
        Thread.sleep(sleepTime);
      } catch (InterruptedException iex) {
        Thread.interrupted();
      }
    }

    if (!executor.isTerminated()) {
      logger.error("Shutdown did not occur within timeout. Halting system.");
      executor.shutdownNow();
    }
    
    logger.info("All tasks terminated.");
    logger.info("Heartbeat periods: " + heartBeat.getHeartbeats());
    logger.info("Messages delivered: " + messageRouter.getMessages());
  }
 
  /*
  Message receive(Long pid) throws InvalidProcessId {
    final Queue<Message> mailbox = mailboxes.get(pid);
    if (mailbox == null) {
      throw new InvalidProcessId(pid + " is in invalid process id.");
    }
    return mailbox.poll();
  }

  Message receiveBlocking(Long pid) throws InvalidProcessId {
    final LinkedBlockingQueue<Message> mailbox = mailboxes.get(pid);
    if (mailbox == null) {
      throw new InvalidProcessId(pid + " is in invalid process id.");
    }
    try {
      return mailbox.poll(1, TimeUnit.MINUTES);
    } catch (InterruptedException iex) {
      Thread.interrupted();
    }
    return null;
  }
  */

  void send(Message.MessageBuilder builder, Process proc) {
    builder.setSender(proc.getProcessId());
    for (long pid = 1; pid <= maxPid; pid++) {
      if (pid == proc.getProcessId()) {
        // don't send to self
        continue;
      }
      mailboxes.get(pid).offer(builder.setReceiver(pid).build());
      executionQueue.add(pid);
    }
  }
  
  void sendOne(Message.MessageBuilder builder, Long receiver, Process proc) {
    if (receiver.equals(proc.getProcessId())) {
      throw new IllegalArgumentException("Cannot send a message to yourself.");
    }
    mailboxes.get(receiver).offer(
        builder.setReceiver(receiver).setSender(proc.getProcessId()).build());
    executionQueue.add(receiver);
  }
  
  public synchronized void declareOwnership(Clock time, Integer resource, Process proc) {
    if (resource < 0 || resource > resourceCount) {
      throw new IllegalArgumentException("Invalid resource, " + resource + ", must be >= 1 and <= " + resourceCount);
    }
    
    logger.info("Resource declaration for resource " + resource + " by pid: " + proc.getProcessId());
    if (resources.get(resource) == null) {
       logger.info("Successful resource declaration for resource " + resource + " by pid: " + proc.getProcessId());
       resources.put(resource, proc);
    } else {
      final String error = "Invalid resource declaration: " + resource + " is currently owned by "
          + resources.get(resource) + " and cannot be claimed by " + proc.getProcessId();
      logger.fatal(error);
      throw new IllegalStateException(error);
    }
  }
  
  public synchronized void releaseOwnership(Clock time, int resource, Process proc) {
    if (resource < 0 || resource > resourceCount) {
      throw new IllegalArgumentException("Invalid resource, " + resource + ", must be >= 1 and <= " + resourceCount);
    }
    
    logger.info("Resource release requested for resource " + resource + " byd pid: " + proc.getProcessId());
    if (!resources.containsKey(resource)) {
      final String error = "Invalid resource release: " + resource + " is is not currently owned. "
          + "Release requested by " + proc.getProcessId();
      throw new IllegalStateException(error);
    } else if (!resources.get(resource).equals(proc)) {
      final String error = "Invalid resource release: " + resource + " is owned by "
          + resources.get(resource).getProcessId()
          + ", but release requested by " + proc.getProcessId();
      throw new IllegalStateException(error);
    } else {
      logger.info("Resource release of " + resource + " was successful");
      resources.remove(resources);
    }
  }
  
  public synchronized void declareConcensus(Clock time, String data, Process proc) {
    // TODO(mikehelmick@) implement
  }

  public synchronized Long register(Process proc) {
    if (!state.equals(State.STARTUP)) {
      throw new IllegalStateException("Impossible to regsiter process, no longer in startup state.");
    }
    
    final long procId = ++maxPid;
    logger.info("New process registered, pid: " + procId);
    procMap.put(procId, proc);
    // Create a mailbox for this process
    LinkedBlockingQueue<Message> mailbox = Queues.newLinkedBlockingQueue();
    mailboxes.put(procId, mailbox);
    logger.info("Mailbox created, ready for pid: " + procId);
    return procId;
  }
}
