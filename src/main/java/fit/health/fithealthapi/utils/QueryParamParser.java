package fit.health.fithealthapi.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fit.health.fithealthapi.model.QueryParams;

import java.util.Collections;
import java.util.Map;

public class QueryParamParser {
    public static <T> QueryParams<T> parse(String filter, String range, String sort, Class<T> filterValueType) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, T> filters = filter != null
                ? objectMapper.readValue(filter, objectMapper.getTypeFactory()
                .constructMapType(Map.class, String.class, filterValueType))
                : Collections.emptyMap();

        int start = 0, end = 10;
        if (range != null) {
            int[] rangeArray = objectMapper.readValue(range, int[].class);
            start = rangeArray[0];
            end = rangeArray[1] + 1;
        }

        String sortField = "id";
        String sortOrder = "ASC";
        if (sort != null) {
            String[] sortArray = objectMapper.readValue(sort, String[].class);
            sortField = sortArray[0];
            sortOrder = sortArray[1];
        }

        return new QueryParams<>(filters, start, end, sortField, sortOrder);
    }
}
