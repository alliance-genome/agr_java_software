package org.alliancegenome.api.service;

import static org.alliancegenome.api.service.Column.*;

import java.sql.Ref;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

public class InteractionColumnFieldMapping extends ColumnFieldMapping<InteractionGeneJoin> {

    private Map<Column, Function<InteractionGeneJoin, Set<String>>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<InteractionGeneJoin, Set<String>>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public InteractionColumnFieldMapping() {
        mapColumnFieldName.put(INTERACTOR_MOLECULE_TYPE, FieldFilter.INTERACTOR_MOLECULE_TYPE);
        mapColumnFieldName.put(MOLECULE_TYPE, FieldFilter.MOLECULE_TYPE);
        mapColumnFieldName.put(JOIN_TYPE, FieldFilter.JOIN_TYPE);
        mapColumnFieldName.put(INTERACTOR_SPECIES, FieldFilter.INTERACTOR_SPECIES);
        mapColumnFieldName.put(INTERACTOR_GENE_SYMBOL, FieldFilter.INTERACTOR_GENE_SYMBOL);
        mapColumnFieldName.put(INTERACTOR_SOURCE, FieldFilter.SOURCE);
        mapColumnFieldName.put(INTERACTOR_REFERENCE, FieldFilter.INTERACTOR_REFERENCE);
        mapColumnFieldName.put(INTERACTOR_DETECTION_METHOD, FieldFilter.DETECTION_METHOD);
        //genetic interaction
        mapColumnFieldName.put(ROLE, FieldFilter.ROLE);
        mapColumnFieldName.put(GENETIC_PERTURBATION, FieldFilter.GENETIC_PERTURBATION);
        mapColumnFieldName.put(INTERACTOR_ROLE, FieldFilter.INTERACTOR_ROLE);   
        mapColumnFieldName.put(INTERACTOR_GENETIC_PERTURBATION, FieldFilter.INTERACTOR_GENETIC_PERTURBATION);
        mapColumnFieldName.put(PHENOTYPES, FieldFilter.PHENOTYPES);
        mapColumnFieldName.put(INTERACTION_TYPE, FieldFilter.INTERACTION_TYPE);
        
        //those will used for DistinctFieldValueSupplementalData
        mapColumnAttribute.put(INTERACTOR_MOLECULE_TYPE, (join) -> Set.of(join.getInteractorBType().getDisplayName()));
        mapColumnAttribute.put(MOLECULE_TYPE, (join) -> Set.of(join.getInteractorAType().getDisplayName()));
        mapColumnAttribute.put(INTERACTOR_SPECIES, (join) -> Set.of(join.getGeneB().getSpecies().getName()));
        mapColumnAttribute.put(INTERACTOR_DETECTION_METHOD, (join) -> {
        	if (join.getDetectionsMethods()==null)
        		return Set.of("");
        	else {
              return join.getDetectionsMethods().stream().map(method-> method.getLabel()).collect(Collectors.toSet()   );
             }
         });
        mapColumnAttribute.put(INTERACTOR_REFERENCE, (join)->Set.of(join.getPublication().getPubId()));
        //for genetic interaction
        mapColumnAttribute.put(ROLE, (join) -> Set.of(join.getInteractorARole().getDisplayName()));
        mapColumnAttribute.put(INTERACTOR_ROLE, (join) -> Set.of(join.getInteractorBRole().getDisplayName()));
        mapColumnAttribute.put(INTERACTION_TYPE, (join) -> Set.of(join.getInteractionType().getDisplayName()));
        mapColumnAttribute.put(INERACTION_SOURCE, (join)->{
        	Set<String> hashSet = new HashSet<String>();
        	if (join.getCrossReferences()!=null) {
        		hashSet=join.getCrossReferences().stream().map(cross->cross.getPrefix() + ":" + cross.getDisplayName()).collect(Collectors.toSet());
        	}
        	if (join.getSourceDatabase()!=null)
        		hashSet.add(join.getSourceDatabase().getLabel() + " " + join.getSourceDatabase().getDisplayName());
        	if (join.getAggregationDatabase() !=null)
        		hashSet.add(join.getAggregationDatabase().getLabel() + " " + join.getAggregationDatabase().getDisplayName());
        	return hashSet;
        }
        );
        
        singleValueDistinctFieldColumns.add(INTERACTOR_MOLECULE_TYPE);
        singleValueDistinctFieldColumns.add(MOLECULE_TYPE);
        singleValueDistinctFieldColumns.add(JOIN_TYPE);
        singleValueDistinctFieldColumns.add(INTERACTOR_SPECIES);
        singleValueDistinctFieldColumns.add(INTERACTOR_DETECTION_METHOD);
        singleValueDistinctFieldColumns.add(INTERACTOR_REFERENCE);
        //genetic interaction
        singleValueDistinctFieldColumns.add(ROLE);
        singleValueDistinctFieldColumns.add(GENETIC_PERTURBATION);
        singleValueDistinctFieldColumns.add(INTERACTOR_ROLE);
        singleValueDistinctFieldColumns.add(INTERACTOR_GENETIC_PERTURBATION);
        singleValueDistinctFieldColumns.add(PHENOTYPES);
        singleValueDistinctFieldColumns.add(INTERACTION_TYPE);        
    }

}
