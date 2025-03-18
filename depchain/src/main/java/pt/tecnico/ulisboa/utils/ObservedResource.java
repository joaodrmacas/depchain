package pt.tecnico.ulisboa.utils;

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

    public boolean waitForChange(int timeout) {
        lock.lock();
        try {
            while (!hasChanged) {
                if (timeout == -1) {
                    condition.await();
                } else {
                boolean hasTimeouted =
                    !condition.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (hasTimeouted) return false;
                }
            }
            hasChanged = false;
        }  catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return true;
    }

    public void notifyChange() {
        lock.lock();
        try {
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
