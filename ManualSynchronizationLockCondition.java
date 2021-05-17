package manualsynchronizationlockcondition;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class ManualSynchronizationLockCondition {

   
    public static void main(String[] args) throws InterruptedException {
        
        
        // Return new executorService instance with threadpool.
        ExecutorService es = Executors.newCachedThreadPool();
        
        // Create the circular shared buffer to store ints;
        Buffer sharedLocation = new SynchronizedCircularBuffer();
        
        es.execute(new Producer(sharedLocation));
        es.execute(new Consumer(sharedLocation));
        
        es.shutdown();
        
        es.awaitTermination(1, TimeUnit.MINUTES);
        
        
    }
    
}
