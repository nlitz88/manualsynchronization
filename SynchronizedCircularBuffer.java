package manualsynchronizationlockcondition;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;




// In this Buffer class, our Put and Get methods will actually be implementing the synchronization. In this case, manually synchronized using lock and condition instances.
public class SynchronizedCircularBuffer implements Buffer {
    
    
    // SYNCHRONIZATION VARIABLES
    
    // Lock to control synchronization with this buffer. ReentrantLock is a basic implementation of the Lock interface.
    private final Lock accessLock = new ReentrantLock();
    
    // Conditions to control thread access to the Lock for producers and consumers. Produced by factory methods of the lock. Default fairness policy (longest waiting gets lock first).
    // By using two different conditions, we can separate what threads are waiting on what condition.
    //      I think this just might reduce the number of threads that have to transition to runnable state upon the lock being released (only appropriate threads notified?).
    private final Condition canWrite = accessLock.newCondition();
    private final Condition canRead = accessLock.newCondition();
    
    
    // BUFFER VARIABLES
    
    // Static Array with finite size as our Circular Buffer's underyling data structure.
    private final int[] buffer = {-1, -1, -1};
    
    // Variables to keep track of current buffer status.
    private int occupiedCells = 0;                                              // Count number of buffer cells used.
    private int writeIndex = 0;                                                 // Index of next element to write to.
    private int readIndex = 0;                                                  // Index of next element to read from.
    
    
    // Implicit constructor.
    
    
    // blockingPut introduces synchronization by (manually) explicitly stating when a thread should attempt to obtain the lock.
    @Override
    public void blockingPut(int value) throws InterruptedException {
        
        
        // Thread in the runnable state tries to obtain lock. If available, obtains it. If unavailable, the thread is blocked until the lock is released again.
        accessLock.lock();
        System.out.println("Producer thread attempted to obtain lock.");
        
        // Once lock obtained, determine whether or not task can be completed.
        try {
            
            // While the buffer is full, make the thread wait.
            while(occupiedCells == buffer.length) {

                // Debugging only, no io in synchronized blocks.
                System.out.println("Producer tries to write");
                displayState("Buffer Full. Producer waits");

                // Because task of this thread cannot complete currently, add it to the queue of threads waiting on this condition. These threads won't go to runnable until
                // a consumer thread reads a value and subsequently calls "canWrite.signal()" in order to make the longest waiting thread in canWrite's thread queue runnable again.
                canWrite.await();

            }
            
            // If able to complete task, continue here.

            // If not full, the producer thread can add its value.
            buffer[writeIndex] = value;

            // Then, have to update writeIndex so that it wraps back around to the beginning.
            writeIndex = (writeIndex + 1) % buffer.length;
            // This works as, until the consumer reads value out of the buffer, the buffer will remain full, and the producer can't write to its next index.
            // The producer can only continue to write to its next index after the consumer has read from its next index. This works because the read and write
            // indices will follow eachother, essentially.

            // Finally, increment the number of cells occupied.
            ++occupiedCells;
            displayState("Producer writes " + value);  // debug only.

            // Then, once this producer thread has completed this atomic operation, it will notify ALL threads currently in the waiting state that are waiting on the "canWrite"
            // condition (meaning that they are in the canWrite condition object's waiting thread queue).
            canRead.signalAll();
            
        } finally {
            
            // To prevent deadlock, unlock the lock in case an exception occurs during the check or task itself.
            accessLock.unlock();
            
        }
        
        
    }
    
    
    
    @Override
    public int blockingGet() throws InterruptedException {
        
        
        // Thread in the runnable state tries to obtain lock. If available, obtains it. If unavailable, the thread is blocked until the lock is released again.
        accessLock.lock();
        System.out.println("Consumer thread attempted to obtain lock.");
        
        // In case an exception occurs and the lock isn't released on a wait or on completion, release the lock in a finally block.
        try {
            
            // While the buffer is empty, consumer threads that attempt to write will be placed in the canWrite's waiting queue.
            while(occupiedCells == 0) {

                System.out.println("Consumer tries to read");
                displayState("Buffer Empty. Consumer waits");
                // Place this consumer thread in canWrite's thread queue.
                canRead.await();

            }

            // If can write, then proceed here.

            // Read value from buffer at current readIndex.
            int readValue = buffer[readIndex];

            // Update index.
            readIndex = (readIndex + 1) % buffer.length;

            // Decrement Occupied cells.
            --occupiedCells;
            displayState("Consumer reads " + readValue);  // debug only.

            // If a consumer was able to read the next value, then we can inform all waiting producer threads waiting on the "canWrite" condition
            // that they can attempt to obtain the lock and perform their write to the buffer.
            canWrite.signalAll();

            return readValue;
            
        } finally {

            accessLock.unlock();
            
        }
        
        
    }
    
    
    // Again, manually synchronized displayState operation.
    private void displayState(String operation) {
        
        accessLock.lock();
        
        try {
            
            System.out.printf("%s%s%d%n%s", operation, " buffer cells occupied: ", occupiedCells, "buffer cells: ");
        
            for(int value :buffer) {
                System.out.printf(" %2d  ", value);
            }

            System.out.printf("%n              ");

            for (int i = 0; i < buffer.length; i++) {
                System.out.print("---- ");
            }

            System.out.printf("%n              ");

            for (int i = 0; i < buffer.length; i++) {

                if (i == writeIndex && i == readIndex) {
                    System.out.print(" WR"); // both write and read index
                }
                else if (i == writeIndex) {
                    System.out.print(" W   "); // just write index
                }
                else if (i == readIndex) {
                    System.out.print("  R  "); // just read index
                }
                else {
                    System.out.print("     "); // neither index
                }
            }

            System.out.printf("%n%n");
            
        } 
        // Again, in case exception occurs within the displayState task, lock can still be released to avoid deadlock.
        finally {
            
            accessLock.unlock();
            
        }
        
        
        

    }
    
    
}
