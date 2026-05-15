package org.alliancegenome.cache.repository.helper;

import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.view.PublicView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;

@Data
@JsonView({ PublicView.Default.class, CurationView.ModelDocument.class, CurationView.SequenceSummaryDocument.class })
public class JsonResultResponse<T> {

	public static final String DISTINCT_FIELD_VALUES = "distinctFieldValues";

	private List<T> results = new ArrayList<T>();
	private long total;
	private long returnedRecords;
	private String errorMessage = "";
	private String note = "";
	private String title = "";
	private String requestDuration;
	private Request request;
	private String apiVersion;
	private String requestDate;
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

	public void setHttpServletRequest(HttpRequest request) {
		if (request == null) {
			return;
		}
		this.request = new Request();
		try {
			this.request.setUri(URLDecoder.decode(request.uri().toString(), "UTF-8"));
		} catch (Exception e) {
			// Do nothing
		}
		// this.request.setParameterMap(request.);
	}

	public void addSupplementalData(String attribute, Object object) {
		if (supplementalData == null) {
			supplementalData = new LinkedHashMap<>();
		}
		supplementalData.put(attribute, object);

	}

	public void addAnnotationSummarySupplementalData(Object object) {
		if (supplementalData == null) {
			supplementalData = new LinkedHashMap<>();
		}
		supplementalData.put("annotationSummary", object);

	}

	public void addDistinctFieldValueSupplementalData(Map object) {
		if (supplementalData == null) {
			supplementalData = new LinkedHashMap<>();
		}
		supplementalData.put(DISTINCT_FIELD_VALUES, object);
	}

	public Map<String, List<String>> retrieveDistinctFieldValues() {
		if (supplementalData == null) {
			return null;
		}
		return (Map<String, List<String>>) supplementalData.get(DISTINCT_FIELD_VALUES);
	}

	public void calculateRequestDuration(long startTime) {
		// in seconds
		long duration = (System.currentTimeMillis() - startTime) / 1000;
		requestDuration = Long.toString(duration) + "s";

	}
}
