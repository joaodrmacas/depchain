package pt.tecnico.ulisboa.utils.types;

public interface RequiresEquals {
    public boolean equals(Object obj);
    public int getSenderId();
    public int hashCode();
}
