package pt.tecnico.ulisboa.utils.types;

public class ArrayWithCounter<T> {
    private final T[] array;
    private int counter;
    private int size;

    @SuppressWarnings("unchecked")
    public ArrayWithCounter(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.size = size;
        this.array = (T[]) new Object[size];
        this.counter = 0;
    }

    public T[] getIfFullAndReset() {
        if (counter != size) {
            return null;
        }
        counter = 0;
        return array;
    }

    public void put(T element, int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
        }
        if (counter >= size) {
            throw new IllegalStateException("Array is already full");
        }
        array[index] = element;
        counter++;
    }
}