package org.alliancegenome.neo4j.entity;

import java.util.Comparator;
import java.util.List;

public interface Sorting<T> {

    default Comparator<T> getJoinedComparator(List<Comparator<T>> comparatorList) {
        if (comparatorList.isEmpty())
            return null;
        Comparator<T> joinedComparator = comparatorList.get(0);
        comparatorList.remove(0);
        for (Comparator<T> comparator : comparatorList) {
            joinedComparator = joinedComparator.thenComparing(comparator);
        }
        return joinedComparator;
    }

}
