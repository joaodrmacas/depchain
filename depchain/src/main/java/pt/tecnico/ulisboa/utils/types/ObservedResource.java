package pt.tecnico.ulisboa.utils.types;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ObservedResource<T> {
    private T resource;
    private boolean hasChanged = false;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public ObservedResource(T resource) {
        this.resource = resource;
    }

    public boolean waitForChange(int timeout) throws InterruptedException {
        Logger.DEBUG("Begining waiting for change");
        lock.lock();
        try {
            while (!hasChanged) {
                if (timeout == -1) {
                    condition.await();
                } else {
                boolean hasTimedout =
                    !condition.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
                
                    if(Logger.IS_DEBUGGING()) {
                        Logger.DEBUG("Timeouted: " + hasTimedout);
                        System.err.println(resource);
                    }
                    
                    if (hasTimedout) return false;
                }
                if (Logger.IS_DEBUGGING()) {
                    if (!hasChanged) {
                        Logger.DEBUG("Woked up but not changed nor timed out");
                    } else Logger.DEBUG("Woked up and changed");

                    new Exception().printStackTrace();
                }
            }
            hasChanged = false;
            Logger.DEBUG("Flag changed to false");
        }  catch (InterruptedException e) {
            Logger.LOG("Interrupted while waiting for change: " + e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
        return true;
    }

    public void notifyChange() {
        lock.lock();
        try {
            Logger.DEBUG("Notifying change: flag changed to true");
            hasChanged = true;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public T getResource() {
        return resource;
    }
}
