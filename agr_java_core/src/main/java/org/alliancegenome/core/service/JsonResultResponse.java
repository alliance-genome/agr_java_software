package org.alliancegenome.core.service;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Setter
@Getter
public class JsonResultResponse<T> {

    @JsonView({View.Default.class})
    private List<T> results = new ArrayList<T>();
    @JsonView({View.Default.class})
    private int total;
    @JsonView({View.Default.class})
    private int returnedRecords;
    @JsonView({View.Default.class})
    private String errorMessage;
    @JsonView({View.Default.class})
    private String note;
    @JsonView({View.Default.class})
    private String requestDuration;
    @JsonView({View.Default.class})
    private Request request;
    @JsonView({View.Default.class})
    private String apiVersion;
    @JsonView({View.Default.class})
    private String requestDate;

    public JsonResultResponse() {
        requestDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    public void calculateRequestDuration(LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = new Duration(startTime, endTime);
        requestDuration = duration.toString();
    }

    public void setResults(List<T> results) {
        this.results = results;
        if (results != null)
            returnedRecords = results.size();
    }

    public void setHttpServletRequest(HttpServletRequest request) {
        if (request == null)
            return;
        this.request = new Request();
        this.request.setUri(URLDecoder.decode(request.getRequestURI()));
        this.request.setParameterMap(request.getParameterMap());
    }

}
