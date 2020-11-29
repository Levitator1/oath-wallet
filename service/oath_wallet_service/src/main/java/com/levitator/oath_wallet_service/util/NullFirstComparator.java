package com.levitator.oath_wallet_service.util;
import java.util.Comparator;

//Compare two things in the natural ordering, taking into account the possibility of nulls,
//and ordering them first. Probably redundant with respect to the Java API, but I don't feel
//like digging through it.

//Thread-safety equivalent to lhs.compareTo(rhs)
public class NullFirstComparator<T extends Comparable<T>> implements Comparator<T>{

    static <U extends Comparable<U>> int static_compare(U lhs, U rhs){
        return lhs == null ? (rhs == null ? 0 : -1) :
            rhs == null ? 1 : lhs.compareTo(rhs);
    }
    
    @Override
    public int compare(T lhs, T rhs) {
        return static_compare(lhs, rhs);
    }  
}
