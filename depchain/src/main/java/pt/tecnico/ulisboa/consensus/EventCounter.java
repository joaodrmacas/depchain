package pt.tecnico.ulisboa.consensus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import pt.tecnico.ulisboa.utils.RequiresEquals;

public class EventCounter<T extends RequiresEquals> {
    private Map<T, Integer> counter = new HashMap<>();
    private HashSet<Integer> alreadyCounted = new HashSet<>();

    public void inc(T value, int id) {
        if (alreadyCounted.contains(id)) {
            return;
        }

        alreadyCounted.add(id);
        counter.put(value, counter.getOrDefault(value, 0) + 1);
    }

    public void inc(int id) {
        inc(null, id);
    }

    public boolean exceeded(T value, int max_counts) {
        return counter.get(value) > max_counts;
    }
    
}
