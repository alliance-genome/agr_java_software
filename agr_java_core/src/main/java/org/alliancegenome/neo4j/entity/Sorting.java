package org.alliancegenome.neo4j.entity;

import java.util.Comparator;
import java.util.List;

import org.alliancegenome.cache.repository.helper.SortingField;

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

    // the last number in a string gets padded with zeros
    static String getSmartKey(String symbol) {
        String[] parts = symbol.split("(?=\\d+$)", 2);
        if(parts.length == 1)
            return symbol.toLowerCase();
        int num = Integer.parseInt(parts[1]);
        // make an 8 digit number padding with a number or zeros as needed
        final String s = parts[0].toLowerCase() + String.format("%08d", num);
        return s;
    }

    Comparator<T> getComparator(SortingField field, Boolean ascending);

}
