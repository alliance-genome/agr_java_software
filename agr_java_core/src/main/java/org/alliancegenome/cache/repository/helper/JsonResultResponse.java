package org.alliancegenome.cache.repository.helper;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JsonResultResponse<T> {

    public static final String DISTINCT_FIELD_VALUES = "distinctFieldValues";
    @JsonView({View.Default.class})
    private List<T> results = new ArrayList<T>();
    @JsonView({View.Default.class})
    private int total;
    @JsonView({View.Default.class})
    private int returnedRecords;
    @JsonView({View.Default.class})
    private String errorMessage = "";
    @JsonView({View.Default.class})
    private String note = "";
    @JsonView({View.Default.class})
    private String title = "";
    @JsonView({View.Default.class})
    private String requestDuration;
    @JsonView({View.Default.class})
    private Request request;
    @JsonView({View.Default.class})
    private String apiVersion;
    @JsonView({View.Default.class})
    private String requestDate;
    @JsonView({View.Default.class})
    private Map<String, Object> supplementalData;

    public JsonResultResponse() {
        requestDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    public static <T> JsonResultResponse getEmptyInstance() {
        JsonResultResponse<T> response = new JsonResultResponse<>();
        return response;
    }

    public void calculateRequestDuration(LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = new Duration(startTime, endTime);
        requestDuration = duration.toString();
    }

    public void setResults(List<T> results) {
        this.results = results;
        if (results != null) {
            returnedRecords = results.size();
        } else {
            this.results = new ArrayList<T>();
        }
    }

    public void setHttpServletRequest(HttpServletRequest request) {
        if (request == null)
            return;
        this.request = new Request();
        this.request.setUri(URLDecoder.decode(request.getRequestURI()));
        this.request.setParameterMap(request.getParameterMap());
    }

    public void addSupplementalData(String attribute, Object object) {
        if (supplementalData == null)
            supplementalData = new LinkedHashMap<>();
        supplementalData.put(attribute, object);

    }

    public void addAnnotationSummarySupplementalData(Object object) {
        if (supplementalData == null)
            supplementalData = new LinkedHashMap<>();
        supplementalData.put("annotationSummary", object);

    }

    public void addDistinctFieldValueSupplementalData(Map object) {
        if (supplementalData == null)
            supplementalData = new LinkedHashMap<>();
        supplementalData.put(DISTINCT_FIELD_VALUES, object);
    }

    public Map<String, List<String>> getDistinctFieldValues() {
        return (Map<String, List<String>>) supplementalData.get(DISTINCT_FIELD_VALUES);
    }

    public void calculateRequestDuration(long startTime) {
        // in seconds
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        requestDuration = Long.toString(duration) + "s";


    }
}
