package org.alliancegenome.es.model.query;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.api.service.ColumnFieldMapping;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.alliancegenome.es.model.query.FieldFilter.FILTER_PREFIX;

@Setter
@Getter
public class Pagination {

    public static final String SORTING_DELIMITER = ",";
    private Integer page = 1;
    private Integer limit = 20;
    private String sortBy;
    private FieldFilter sortByField;
    private Boolean asc;
    private BaseFilter fieldFilterValueMap = new BaseFilter();
    private List<String> errorList = new ArrayList<>();
    private List<String> invalidFilterList = new ArrayList<>();
    private ColumnFieldMapping mapping;

    private boolean isCount = false;

    public Pagination(Integer page, Integer limit, String sortBy, String asc, ColumnFieldMapping mapping) {
        this(page, limit, sortBy, asc);
        this.mapping = mapping;
    }

    public Pagination(Integer page, Integer limit, String sortBy, String asc) {
        if (page != null)
            this.page = page;
        if (limit != null)
            this.limit = limit;
        this.sortBy = sortBy;
        sortByField = FieldFilter.getFieldFilterByName(sortBy);
        if (this.page < 1)
            errorList.add("'page' request parameter invalid: Found [" + page + "]. It has to be an integer number greater than 0");
        if (this.limit < 0)
            errorList.add("'limit' request parameter invalid: Found [" + limit + "].  It has to be an integer number greater than 0");
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
        // if mapping exists
        checkIfMappingExists(fieldFilter);
        fieldFilterValueMap.put(fieldFilter, value);
    }

    private boolean checkIfMappingExists(FieldFilter fieldFilter) {
        boolean valid = true;
        if (mapping != null) {
            if (!mapping.getColumnFieldFilters().contains(fieldFilter)) {
                String e = "The filter name '" + fieldFilter.getName() + "' is not a valid parameter name. ";
                e += "Allowed values are [" + mapping.getAllowedFieldFilterNames() + "]";
                errorList.add(e);
                valid = false;
            }
        }
        return valid;
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

    public String getAscending() {
        return asc ? "ASC" : "DESC";
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
        if (StringUtils.isEmpty(sortBy))
            return null;
        String[] sortingTokens = sortBy.split(SORTING_DELIMITER);
        return Arrays.stream(sortingTokens)
                .map(FieldFilter::getFieldFilterByName)
                .collect(Collectors.toList());
    }

    public boolean hasInvalidElements() {
        return invalidFilterList == null || !invalidFilterList.isEmpty();

    }

    public void setLimitToAll() {
        limit = Integer.MAX_VALUE;
    }

    public void validateFilterValues(MultivaluedMap<String, String> queryParameters) {
        if (mapping == null)
            return;
        queryParameters.keySet().stream()
                .filter(parameter -> parameter.startsWith(FILTER_PREFIX))
                .forEach(parameter -> {
                    if (!mapping.getColumnFieldFilters().contains(parameter)) {
                        String e = "The filter name '" + parameter + "' is not a valid parameter name. ";
                        e += "Allowed values are [" + mapping.getAllowedFieldFilterNames() + "]";
                        errorList.add(e);
                    }
                });
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
            Arrays.asList(values()).forEach(sorting ->
                    values.add(sorting.name()));
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
