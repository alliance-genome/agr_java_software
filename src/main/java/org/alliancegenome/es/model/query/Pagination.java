package org.alliancegenome.es.model.query;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
public class Pagination {

    int page;
    int limit;
    SortBy sortBy;
    Boolean asc;
    Map<FieldFilter, String> fieldFilterValueMap = new HashMap<>(10);
    List<String> errorList = new ArrayList<>();

    public Pagination(int page, int limit, String sortBy, String asc) {
        this.page = page;
        this.limit = limit;
        init(sortBy, asc);
    }

    private void init(String sortBy, String asc) {
        if (page < 1)
            errorList.add("Invalid 'page' value. Needs to be greater or equal than 1");
        if (limit < 1)
            errorList.add("Invalid 'limit' value. Needs to be greater or equal than 1");
        this.sortBy = SortBy.getSortBy(sortBy);
        if (this.sortBy == null) {
            String message = "Invalid 'sortBy' value. Needs to have the following values: [";
            message = message + SortBy.getAllValues() + "]";
            errorList.add(message);
        }

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


    public boolean hasErrors() {
        return !errorList.isEmpty();
    }

    public List<String> getErrors() {
        return errorList;
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
