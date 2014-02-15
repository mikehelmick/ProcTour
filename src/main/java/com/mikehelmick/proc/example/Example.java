package com.mikehelmick.proc.example;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.mikehelmick.proc.ProcessManager;

public class Example {
  private static Logger logger = Logger.getLogger(Example.class);

  public static void main(String[] args) {
    BasicConfigurator.configure();
    Logger.getRootLogger().setLevel(Level.INFO);
    logger.info("Example simulation");
    
    
    ProcessManager.getInstance().setSharedResourceCount(10);
    

    ProcessManager.getInstance().start();
    
  }

}
