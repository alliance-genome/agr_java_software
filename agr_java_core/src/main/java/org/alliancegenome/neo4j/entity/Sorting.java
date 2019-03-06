package org.alliancegenome.neo4j.entity;

import java.util.Comparator;
import java.util.List;

public interface Sorting<T> {

    default Comparator<T> getJoinedComparator(List<Comparator<T>> comparatorList) {
        Comparator<T> joinedComparator = null;
        for (Comparator<T> comparator : comparatorList) {
            if (joinedComparator == null)
                joinedComparator = comparator;
            else
                joinedComparator = joinedComparator.thenComparing(comparator);
        }
        return joinedComparator;
    }

}
