package org.alliancegenome.core.translators;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.neo4j.ogm.model.Result;

public abstract class ResultTSVTranslator {

    private PrintWriter writer;
    
    public ResultTSVTranslator(PrintWriter writer) {
        this.writer = writer;
        writer.print(getHeaderLine());
    }
    
    public void translateResult(Result result) {
        for (Map<String, Object> map : result) {
            writer.print(getLine(map));
        }
    }
    
    public void translateMap(Map<String, Object> map) {
        writer.print(getLine(map));
    }

    private String getHeaderLine() {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        for(String s: getHeaders()) {
            headerJoiner.add(s);
        }
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());
        return builder.toString();
    }
    
    private String getLine(Map<String, Object> map) {
        StringBuilder builder = new StringBuilder();
        StringJoiner joiner = new StringJoiner("\t");
        for(String s: mapToRow(map)) {
            joiner.add(s);
        }
        builder.append(joiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());
        return builder.toString();
    }

    protected abstract List<String> getHeaders();
    protected abstract List<String> mapToRow(Map<String, Object> map);

}
