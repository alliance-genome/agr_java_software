package org.alliancegenome.cache.repository.helper;

import static java.util.Comparator.naturalOrder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.entity.node.MITerm;
import org.alliancegenome.neo4j.entity.node.Phenotype;
import org.apache.commons.collections4.CollectionUtils;

public class InteractionAnnotationSorting implements Sorting<InteractionGeneJoin> {

	private List<Comparator<InteractionGeneJoin>> defaultList;
	private List<Comparator<InteractionGeneJoin>> moleculeList;
	private List<Comparator<InteractionGeneJoin>> detectionList;
	private List<Comparator<InteractionGeneJoin>> interactorMoleculeTypeList;
	private List<Comparator<InteractionGeneJoin>> interactorSpeciesList;
	private List<Comparator<InteractionGeneJoin>> referenceList;
	//for genetic interaction
	private List<Comparator<InteractionGeneJoin>> roleList;
	private List<Comparator<InteractionGeneJoin>> interactorRoleList;
	private List<Comparator<InteractionGeneJoin>> interactorAGeneticPerturbationList;
	private List<Comparator<InteractionGeneJoin>> interactorBGeneticPerturbationList;
	private List<Comparator<InteractionGeneJoin>> phenotypeList;

	private static Comparator<InteractionGeneJoin> interactorGeneSymbolOrder =
			Comparator.comparing(annotation -> Sorting.getSmartKey(annotation.getGeneB().getSymbol()));

	private static Comparator<InteractionGeneJoin> moleculeOrder =
			Comparator.comparing(annotation -> annotation.getInteractorAType().getLabel().toLowerCase());

	private static Comparator<InteractionGeneJoin> interactorMoleculeOrder =
			Comparator.comparing(annotation -> annotation.getInteractorBType().getLabel().toLowerCase());

	private static Comparator<InteractionGeneJoin> interactorSpeciesOrder =
			Comparator.comparing(annotation -> annotation.getGeneB().getSpecies().getName().toLowerCase());

	private static Comparator<InteractionGeneJoin> referenceOrder =
			Comparator.comparing(annotation -> annotation.getPublication().getPubId().toLowerCase());

	private static Comparator<InteractionGeneJoin> detectionOrder =
			Comparator.comparing(annotation -> {
				List<MITerm> terms = annotation.getDetectionsMethods();
				terms.sort(Comparator.comparing(miTerm -> miTerm.getDisplayName().toLowerCase()));
				return terms.get(0).getDisplayName().toLowerCase();
			});
	//here for genetic interaction
	private static Comparator<InteractionGeneJoin> interactorAGeneticPerturbationOrder =
			Comparator.comparing(annotation -> {
			 if (annotation.getAlleleA() ==null)	
				 return null;
			 else 
				 return annotation.getAlleleA().getSymbol();
			});
	private static Comparator<InteractionGeneJoin> interactorBGeneticPerturbationOrder =
			Comparator.comparing(annotation -> {
				if (annotation.getAlleleB()==null)
					return null;
				else 
				   return annotation.getAlleleB().getSymbol();
				});
	private static Comparator<InteractionGeneJoin> roleOrder =
			Comparator.comparing(annotation -> annotation.getInteractorARole().getDisplayName());
	private static Comparator<InteractionGeneJoin> interactorRoleOrder =
			Comparator.comparing(annotation -> annotation.getInteractorBRole().getDisplayName());
	
	private static Comparator<InteractionGeneJoin> phenotypeOrder =
		   // Comparator.comparing(annotation -> {
		   //	  List<Phenotype> phenotypes = annotation.getPhenotypes();
		   //	  phenotypes.sort(Comparator.comparing(phenotype -> phenotype.getPhenotypeStatement().toLowerCase()));
		   //	  return phenotypes.get(0).getPhenotypeStatement().toLowerCase();
		   // });
	Comparator.comparing(annotation -> {
		if (CollectionUtils.isEmpty(annotation.getPhenotypes()))
			return null;
		String phenotypeJoin = annotation.getPhenotypes().stream().sorted(Comparator.comparing(Phenotype::getPhenotypeStatement)).map(Phenotype::getPhenotypeStatement).collect(Collectors.joining(""));
		return phenotypeJoin.toLowerCase();
	}, Comparator.nullsLast(naturalOrder()));
	
	public InteractionAnnotationSorting() {
		super();

		defaultList = new ArrayList<>(4);
		defaultList.add(interactorGeneSymbolOrder);
		defaultList.add(moleculeOrder);
		defaultList.add(interactorMoleculeOrder);
		defaultList.add(interactorSpeciesOrder);

		moleculeList = new ArrayList<>(4);
		moleculeList.add(moleculeOrder);
		moleculeList.add(interactorMoleculeOrder);
		moleculeList.add(interactorGeneSymbolOrder);
		moleculeList.add(interactorSpeciesOrder);

		interactorMoleculeTypeList = new ArrayList<>(4);
		interactorMoleculeTypeList.add(interactorMoleculeOrder);
		interactorMoleculeTypeList.add(moleculeOrder);
		interactorMoleculeTypeList.add(interactorGeneSymbolOrder);
		interactorMoleculeTypeList.add(interactorSpeciesOrder);

		detectionList = new ArrayList<>(4);
		detectionList.add(detectionOrder);
		detectionList.add(interactorGeneSymbolOrder);
		detectionList.add(moleculeOrder);
		detectionList.add(interactorMoleculeOrder);

		interactorSpeciesList = new ArrayList<>(4);
		interactorSpeciesList.add(interactorSpeciesOrder);
		interactorSpeciesList.add(interactorGeneSymbolOrder);
		interactorSpeciesList.add(moleculeOrder);
		interactorSpeciesList.add(interactorMoleculeOrder);

		referenceList = new ArrayList<>(4);
		referenceList.add(referenceOrder);
		referenceList.add(interactorGeneSymbolOrder);
		referenceList.add(moleculeOrder);
		referenceList.add(interactorMoleculeOrder);
		
		//for genetic interaction		 
		roleList = new ArrayList<>(4);
		roleList.add(roleOrder);
		roleList.add(interactorGeneSymbolOrder);
		roleList.add(moleculeOrder);
		roleList.add(interactorMoleculeOrder);
		
		interactorRoleList = new ArrayList<>(4);
		interactorRoleList.add(interactorRoleOrder);
		interactorRoleList.add(interactorGeneSymbolOrder);
		interactorRoleList.add(moleculeOrder);
		interactorRoleList.add(interactorMoleculeOrder);
		
		interactorAGeneticPerturbationList = new ArrayList<>(4);
		interactorAGeneticPerturbationList.add(interactorAGeneticPerturbationOrder);
		interactorAGeneticPerturbationList.add(interactorGeneSymbolOrder);
		interactorAGeneticPerturbationList.add(moleculeOrder);
		interactorAGeneticPerturbationList.add(interactorMoleculeOrder);
		
		interactorBGeneticPerturbationList = new ArrayList<>(4);
		interactorBGeneticPerturbationList.add(interactorBGeneticPerturbationOrder);
		interactorBGeneticPerturbationList.add(interactorGeneSymbolOrder);
		interactorBGeneticPerturbationList.add(moleculeOrder);
		interactorBGeneticPerturbationList.add(interactorMoleculeOrder);
		
		phenotypeList = new ArrayList<>(4);
		phenotypeList.add(phenotypeOrder);
		phenotypeList.add(interactorGeneSymbolOrder);
		phenotypeList.add(moleculeOrder);
		phenotypeList.add(interactorMoleculeOrder);
	}

	public Comparator<InteractionGeneJoin> getComparator(SortingField field, Boolean ascending) {
		if (field == null)
			return getJoinedComparator(defaultList);

		switch (field) {
			case INTERACTOR_GENE_SYMBOL:
				return getJoinedComparator(defaultList);
			case MOLECULE_TYPE:
				return getJoinedComparator(moleculeList);
			case INTERACTOR_MOLECULE_TYPE:
				return getJoinedComparator(interactorMoleculeTypeList);
			case INTERACTOR_SPECIES:
				return getJoinedComparator(interactorSpeciesList);
			case INTERACTOR_DETECTION_METHOD:
				return getJoinedComparator(detectionList);
			case REFERENCE:
				return getJoinedComparator(referenceList);
			case ROLE:
				return getJoinedComparator(roleList);
			case INTERACTOR_ROLE:
				return getJoinedComparator(interactorRoleList);
			case INTERACTOR_A_GENETIC_PERTURBATION:
				return getJoinedComparator(interactorAGeneticPerturbationList);
			case INTERACTOR_B_GENETIC_PERTURBATION:
				return getJoinedComparator(interactorBGeneticPerturbationList);
			case PHENOTYPE:
				return getJoinedComparator(phenotypeList);
			default:
				return getJoinedComparator(defaultList);
		}
	}


}
