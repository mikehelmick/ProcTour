package com.mikehelmick.proc.example;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mikehelmick.proc.Clock;
import com.mikehelmick.proc.Message;
import com.mikehelmick.proc.Proc;
import com.mikehelmick.proc.ProcessManager;
import com.mikehelmick.proc.validators.MutualExclusionValidator;

public class Example {
  private static Logger logger = Logger.getLogger(Example.class);

  public static void main(String[] args) {
    BasicConfigurator.configure();
    Logger.getRootLogger().setLevel(Level.INFO);
    logger.info("Example simulation");
    
    final int numProcs = 10;
    ProcessManager.getInstance().setSharedResourceCount(1);
    ProcessManager.getInstance().addValidator(
        MutualExclusionValidator.builder()
            .setAcquiresPerProcess(1)
            .setNumProcesses(numProcs)
            .setNumResources(1)
            .build());
    // Add the processes
    final CountDownLatch cdl = new CountDownLatch(numProcs);
    final List<ExampleProcess> procs = Lists.newArrayList();
    for (int i = 0; i < numProcs; i++) {
      procs.add(new ExampleProcess(cdl));
    }
    
    logger.info("Requesting simulation start.");
    ProcessManager.getInstance().start();
    
    while (cdl.getCount() > 0) {
      logger.info("Waiting for all processes to get resource.");
      try {
        cdl.await(15, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.interrupted();
      }
    }
    
    logger.info("All processes have gotten the resource once, requesting shutdown.");
    ProcessManager.getInstance().shutdown();
  }
  
  /** This isn't a distributed clock. It relies on local synchronization
   * via shared memory, something not truly available in a distribute dsystem.
   */
  public static class PoorClock implements Clock {
    private static final long serialVersionUID = -2004996062300612886L;

    static AtomicLong clock = new AtomicLong(1);
    
    private final Long time;
    
    PoorClock() {
      time = clock.incrementAndGet();
    }
    
    @Override
    public String toString() {
      return "pc:" + time;
    }
    
    @Override
    public int compareTo(Clock o) {
      return time.compareTo(((PoorClock)o).time);
    }
  }

  public static class ExampleProcess extends Proc {
    public static final String REQUEST = "I CAN HAZ RESOURCE?";
    public static final String REPLY = "YOU CAN HAZ:";   
    public static final String ADVANCE = "ADVANCE:";
    
    private PoorClock mySendTime = null;
    private AtomicBoolean haveResource = new AtomicBoolean(false);
    private Set<Long> receivAfterCount = Sets.newConcurrentHashSet();
    private final CountDownLatch latch;
    
    public ExampleProcess(CountDownLatch latch) {
      super();
      this.latch = latch;
    }

    @Override
    public synchronized void tick() {
      if (processId == 1 && mySendTime == null) {
        logger.info("process: " + processId + " requesting resource");
        mySendTime = new PoorClock();
        sendAll(Message.builder()
            .setClock(mySendTime)
            .setMessage(REQUEST));
      } else if (haveResource.get()) {
        // release the resource
        logger.info("process: " + processId + " releasing resource");
        if (haveResource.compareAndSet(true, false)) {
          releaseResourceOwnership(new PoorClock(), 1); 
          latch.countDown();
          // advance
          if (processId != ProcessManager.getInstance().getProcessCount()) {
            sendAll(Message.builder()
                .setClock(new PoorClock())
                .setMessage(ADVANCE + (processId + 1)));
          }
        }
      }
    }

    @Override
    public void receiveMessage(Message message) {
      if (REQUEST.equals(message.getMessage())) {
        // Of course, you can haz resource.
        // No logic here.
        logger.info("pid: " + processId + " received request from " + message.getSender());
        sendAll(Message.builder()
            .setClock(new PoorClock())
            .setMessage(REPLY + message.getSender()));
      } else if (message.getMessage().startsWith(REPLY)) {
        // See if this is a reply for us
        Long grantedTo = new Long(message.getMessage().split(":")[1]);
        if (processId.equals(grantedTo)) {
          // Hey this is us. let's see if we have enough ACKs.
          logger.info("pid: " + processId + " received reply from " + message.getSender());
          if (mySendTime.compareTo(message.getClock()) < 0) {
            receivAfterCount.add(message.getSender());
            if (receivAfterCount.size() == getProcessCount() - 1) {
              declareResourceOwnership(new PoorClock(), 1);
              haveResource.set(true);
            }
          }
        }
      } else if (message.getMessage().startsWith(ADVANCE)) {
        Long advanceTo = new Long(message.getMessage().split(":")[1]);
        if (processId.equals(advanceTo)) {
          logger.info("pid: " + processId + " received advance notice from " + message.getSender());
          mySendTime = new PoorClock();
          sendAll(Message.builder()
              .setClock(mySendTime)
              .setMessage(REQUEST));
        }
      }
    }
    
  }
}
