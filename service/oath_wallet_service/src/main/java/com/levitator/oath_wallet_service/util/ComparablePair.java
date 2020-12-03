package com.levitator.oath_wallet_service.util;

public class ComparablePair<T extends Comparable<T>, U extends Comparable<U>> 
        extends Pair<T,U> 
        implements Comparable< ComparablePair<T, U>> {
    
    public ComparablePair(){
        super(null, null);
    }
    
    public ComparablePair(T a, U b) {
        super(a, b);
    }

    @Override
    public int compareTo(ComparablePair<T, U> rhs) {
        var result = NullFirstComparator.static_compare(first, rhs.first);
        return result != 0 ? result : 
                NullFirstComparator.static_compare(second, rhs.second);
    }    
}
