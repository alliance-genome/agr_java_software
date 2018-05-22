package org.alliancegenome.es.index.data;

import java.util.Date;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetReleasesResponce extends APIResponce {
    private HashMap<String, Date> releases;
}
