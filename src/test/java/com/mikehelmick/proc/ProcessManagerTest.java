package com.mikehelmick.proc;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.mikehelmick.proc.Message.MessageBuilder;

public class ProcessManagerTest {
  private static Logger logger = Logger.getLogger(ProcessManagerTest.class);
  
  @BeforeClass
  public static void setupLogging() {
    BasicConfigurator.configure();
    Logger.getRootLogger().setLevel(Level.DEBUG);
  }
  
  @Test
  public void testHeartbeat() {
    final CountDownLatch cdl = new CountDownLatch(2);
    
    Heartbeat hb = new Heartbeat(cdl);
    ProcessManager.getInstance().start();
    
    while (true) {
      try {
        if (cdl.await(10, TimeUnit.SECONDS)) {
          break;
        } 
      } catch (InterruptedException iex) {
        Thread.interrupted();
      }
    }
    
    ProcessManager.getInstance().shutdown();
    ProcessManager.reset();
  }

  private static class Heartbeat extends Proc {
    final CountDownLatch latch;

    Heartbeat(CountDownLatch latch) {
      this.latch = latch;
    }
    
    @Override
    public void tick() {
      latch.countDown();
    }

    @Override
    public void receiveMessage(Message message) {
      
    }
  }
  
  @Test
  public void testPingPong() throws InterruptedException {
    PingPong p1 = new PingPong(3);
    PingPong p2 = new PingPong(3);
    PingPong p3 = new PingPong(3);
    
    ProcessManager.getInstance().start();
    
    assertTrue(p1.await());
    assertTrue(p2.await());
    assertTrue(p3.await());
    
    ProcessManager.getInstance().shutdown();
    ProcessManager.reset();
  }

  private static class PingPong extends Proc {
    
    private final long totalProcs;

    private Set<Long> pongReceived = Sets.newConcurrentHashSet();
    private final CountDownLatch cdl;
    
    private boolean sentPings = false;
    
    PingPong(int totalProcs) {
      this.totalProcs = totalProcs;
      cdl = new CountDownLatch(totalProcs - 1);
    }
    
    boolean await() throws InterruptedException {
      return cdl.await(30, TimeUnit.SECONDS);
    }

    @Override
    public void tick() {
      if (!sentPings) {
        logger.info("Tick, sending pings");
        sendAll(MessageBuilder.create().setMessage("ping"));
        sentPings = true;
      }
      logger.info("tick, latch: " + cdl.getCount());
    }

    @Override
    public void receiveMessage(Message message) {
      if (message.getMessage().equals("pong")) {
        logger.info(getProcessId() + " received pong from " + message.getSender());
        if (pongReceived.add(message.getSender())) {
          cdl.countDown();
        }
      } else if (message.getMessage().equals("ping")) {
        sendOne(MessageBuilder.create().setMessage("pong"), message.getSender());
      }
    }
    
  }
  
}
