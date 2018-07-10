package synds;

import java.util.ArrayList;

public interface SynQ<T> {

    void enqueue(T t);

    ArrayList<? extends T> dequeue();

    void shutdown();
}
