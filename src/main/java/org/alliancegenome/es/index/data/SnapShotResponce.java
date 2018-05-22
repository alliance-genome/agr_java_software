package org.alliancegenome.es.index.data;

import org.alliancegenome.es.index.data.doclet.SnapShotDoclet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SnapShotResponce extends APIResponce {

    private SnapShotDoclet snapShot;
}
