package pt.tecnico.ulisboa.consensus;

import pt.tecnico.ulisboa.Node;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.RequiresEquals;

public class EpochChange<T extends RequiresEquals> {
    @SuppressWarnings("unused")
    private Node<T> member;
    private int epochNumber;

    public EpochChange(Node<T> member, int epochNumber) {
        this.member = member;
        this.epochNumber = epochNumber;
        Logger.LOG("Creating epoch change");
    }

    public int start() {
        return epochNumber + 1;
    }
}
