package pt.tecnico.ulisboa.consensus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import pt.tecnico.ulisboa.utils.types.Consensable;
import pt.tecnico.ulisboa.utils.types.Logger;

public class EventCounter<T extends Consensable> {
    private Map<T, Integer> counter = new HashMap<>();
    private HashSet<Integer> alreadyCounted = new HashSet<>();
    private boolean stopped = false;

    public synchronized void inc(T value, int id) {
        if (alreadyCounted.contains(id)) {
            // already counted this id
            Logger.LOG("Already counted " + id);
            return;
        }

        alreadyCounted.add(id);
        counter.put(value, counter.getOrDefault(value, 0) + 1);
    }

    public synchronized boolean counted(int id) {
        return alreadyCounted.contains(id);
    }

    public synchronized void inc(int id) {
        inc(null, id);
    }

    public synchronized boolean exceeded(T value, int max_counts) {
        if (stopped) return false;
        
        Integer count = counter.get(value);
        if (count == null) {
            return false;
        }

        return count > max_counts;
    }

    public synchronized boolean exceeded(int max_counts) {
        return exceeded(null, max_counts);
    }

    public synchronized T getExceeded(int max_counts) {
        if (stopped) return null;

        for (Map.Entry<T, Integer> entry : counter.entrySet()) {
            if (entry.getValue() > max_counts) {
                return entry.getKey();
            }
        }
        return null;
    }

    public synchronized int getCount(T value) {
        return counter.getOrDefault(value, 0);
    }



    public synchronized void stop() {
        stopped = true;
    }

    @Override
    public String toString() {
        return counter.toString() + " | " + alreadyCounted.toString();
    }

    // for debugging purposes
    public synchronized void printStatus() {
        System.err.println("Counter: " + counter);

        // compare every key pair for equality
        for (T key1 : counter.keySet()) {
            for (T key2 : counter.keySet()) {
                if (key1.equals(key2)) {
                    continue;
                }

                System.err.println("\nComparing " + key1 + " with " + key2 + ": " + key1.equals(key2));
                System.err.println("Hashcode " + key1 + ": " + key1.hashCode());
                System.err.println("Hashcode " + key2 + ": " + key2.hashCode());
                System.err.println();
            }
        }

        System.err.println("\nAlready counted: " + alreadyCounted);
    }
}
