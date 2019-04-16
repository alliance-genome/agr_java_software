package org.alliancegenome.core.service;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PaginationResult<T> {

    private List<T> result = new ArrayList<>();
    private int totalNumber;

}
