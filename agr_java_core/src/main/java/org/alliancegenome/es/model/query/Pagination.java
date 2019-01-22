package org.alliancegenome.es.model.query;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Setter
@Getter
public class Pagination {

    public static final String SORTING_DELIMITER = ",";
    private Integer page;
    private Integer limit;
    private String sortBy;
    private FieldFilter sortByField;
    private Boolean asc;
    private BaseFilter fieldFilterValueMap = new BaseFilter();
    private List<String> errorList = new ArrayList<>();

    private boolean isCount = false;

    public Pagination(Integer page, Integer limit, String sortBy, String asc) {
        this.page = page;
        this.limit = limit;
        this.sortBy = sortBy;
        sortByField = FieldFilter.getFieldFilterByName(sortBy);
        init(asc);
    }

    public Pagination() {
        isCount = true;
    }

    public boolean isCountPagination() {
        return isCount;
    }

    private void init(String asc) {
        if (asc == null) {
            this.asc = true;
        } else {
            if (!AscendingValues.isValidValue(asc)) {
                String message = "Invalid 'asc' value. Needs to have the following values: [";
                message = message + AscendingValues.getAllValues() + "]";
                errorList.add(message);
            }
            this.asc = AscendingValues.getValue(asc);
        }
    }

    public void addFieldFilter(FieldFilter fieldFilter, String value) {
        fieldFilterValueMap.put(fieldFilter, value);
    }

    public void makeSingleFieldFilter(FieldFilter fieldFilter, String value) {
        fieldFilterValueMap.clear();
        fieldFilterValueMap.put(fieldFilter, value);
    }

    public void removeFieldFilter(FieldFilter fieldFilter) {
        fieldFilterValueMap.remove(fieldFilter);
    }

    public boolean hasErrors() {
        return !errorList.isEmpty();
    }

    public List<String> getErrors() {
        return errorList;
    }

    public boolean sortByDefault() {
        if (StringUtils.isEmpty(sortBy))
            return true;
        if (sortBy.equalsIgnoreCase("default"))
            return true;
        return false;
    }

    public int getStart() {
        if (page == null || limit == null)
            return 0;
        return (page - 1) * limit;
    }

    public int getEnd() {
        return page * limit;
    }

    public List<FieldFilter> getSortByList() {
        if(sortBy == null)
            return null;
        String[] sortingTokens = sortBy.split(SORTING_DELIMITER);
        return Arrays.stream(sortingTokens)
                .map(FieldFilter::getFieldFilterByName)
                .collect(Collectors.toList());
    }

    enum AscendingValues {
        TRUE(true), FALSE(false), YES(true), NO(false), UP(true), DOWN(false);

        private Boolean val;

        AscendingValues(Boolean val) {
            this.val = val;
        }

        public static boolean isValidValue(String name) {
            for (AscendingValues val : values()) {
                if (val.name().equalsIgnoreCase(name))
                    return true;
            }
            return false;
        }

        public static String getAllValues() {
            StringJoiner values = new StringJoiner(",");
            for (AscendingValues sorting : values())
                values.add(sorting.name());
            return values.toString();
        }

        public static Boolean getValue(String asc) {
            for (AscendingValues val : values()) {
                if (val.name().equalsIgnoreCase(asc))
                    return val.val;
            }
            return null;
        }
    }

    public int getIndexOfFirstElement() {
        return (page - 1) * limit;
    }

    public static Pagination getDownloadPagination() {
        return new Pagination(1, Integer.MAX_VALUE, null, null);
    }
}
