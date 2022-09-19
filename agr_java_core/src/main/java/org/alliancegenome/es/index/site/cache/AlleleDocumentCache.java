package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.Allele;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlleleDocumentCache extends IndexerCache {

	protected Map<String, Set<String>> constructExpressedComponents = new HashMap<>();
	protected Map<String, Set<String>> constructKnockdownComponents = new HashMap<>();
	protected Map<String, Set<String>> constructRegulatoryRegions = new HashMap<>();


	@Override
	protected <D extends SearchableItemDocument> void addExtraCachedFields(D document) {
		String id = document.getPrimaryKey();
		
		document.setConstructExpressedComponent(constructExpressedComponents.get(id));
		document.setConstructKnockdownComponent(constructKnockdownComponents.get(id));
		document.setConstructRegulatoryRegion(constructRegulatoryRegions.get(id));

		document.setAlleles(alleles.get(id));
		Allele a = alleleMap.get(id);
		if(a!=null) {
			if(diseases != null && diseases.get(id)!=null )
				a.setDisease(diseases.get(id).size() > 0);
			if(phenotypeStatements != null && phenotypeStatements.get(id)!=null)
				a.setPhenotype(phenotypeStatements.get(id).size() > 0);

			if(document instanceof AlleleVariantSequence)
				((AlleleVariantSequence) document).setAllele(a);
		}
	}


}
