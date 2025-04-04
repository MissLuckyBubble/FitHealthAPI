package fit.health.fithealthapi.model;

import lombok.Getter;

import java.util.Map;

@Getter
public class QueryParams<T> {
    private Map<String, T> filters;
    private int start;
    private int end;
    private String sortField;
    private String sortOrder;

    public QueryParams(Map<String, T> filters, int start, int end, String sortField, String sortOrder) {
        this.filters = filters;
        this.start = start;
        this.end = end;
        this.sortField = sortField;
        this.sortOrder = sortOrder;
    }

}
