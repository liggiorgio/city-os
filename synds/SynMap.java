package synds;

import java.util.ArrayList;

public interface SynMap<K, V> {

    V get(K k);

    void put(K k, V v);

    void replace(K k, V v);

    void remove(K k);

    ArrayList<? extends V> values();

}
