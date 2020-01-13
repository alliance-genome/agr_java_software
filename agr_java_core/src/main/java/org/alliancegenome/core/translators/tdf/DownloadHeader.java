package org.alliancegenome.core.translators.tdf;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.core.config.ConfigHelper;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;

@Setter
@Getter
public class DownloadHeader<T> {

    private String name;
    private Function<T, String> function;

    public DownloadHeader(String name, Function<T, String> function) {
        this.name = name;
        this.function = function;
    }

    public static <T> String getDownloadOutput(List<T> list, List<DownloadHeader> headers) {
        StringBuilder builder = new StringBuilder();
        StringJoiner joiner = new StringJoiner("\t");
        headers.forEach(header -> joiner.add(header.getName()));
        builder.append(joiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        list.forEach(row -> {
            StringJoiner dataJoiner = new StringJoiner("\t");
            headers.forEach(header -> {
                Object value = header.getFunction().apply(row);
                String valueStr;
                if (value != null)
                    valueStr = value.toString();
                else
                    valueStr = "";
                dataJoiner.add(valueStr);

            });
            builder.append(dataJoiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();
    }

}
