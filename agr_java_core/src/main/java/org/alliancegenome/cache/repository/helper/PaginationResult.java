package org.alliancegenome.cache.repository.helper;

import java.util.*;

import lombok.*;

@Getter
@Setter
public class PaginationResult<T> {

    private List<T> result = new ArrayList<>();
    private int totalNumber;
    private Map<String, List<String>> distinctFieldValueMap;

}
