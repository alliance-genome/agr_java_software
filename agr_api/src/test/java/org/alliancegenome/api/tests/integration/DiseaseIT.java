package org.alliancegenome.api.tests.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.api.controller.DiseaseController;
import org.alliancegenome.api.entity.AGMDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.entity.GeneDiseaseAnnotationDocument;
import org.alliancegenome.api.service.DiseaseESService;
import org.alliancegenome.api.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.api.service.DiseaseService;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Synonym;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DiseaseIT {

	private ObjectMapper mapper = new ObjectMapper();
	private DiseaseService diseaseService = new DiseaseService();
	private DiseaseESService diseaseESService = new DiseaseESService();

	private DiseaseController diseaseController = new DiseaseController();
	DiseaseService service = new DiseaseService();

	@Before
	public void before() {
//		  Configurator.setRootLevel(Level.INFO);
		ConfigHelper.init();

		mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.registerModule(new OrthologyModule());
	}

	@Test
	public void checkSingleDiseaseTerm() {
		DiseaseService service = new DiseaseService();
		DOTerm term = service.getById("DOID:3594");
		assertNotNull(term);
		assertThat(term.getName(), equalTo("choriocarcinoma"));
		assertThat(term.getSynonyms().stream().map(Synonym::getPrimaryKey).collect(Collectors.toList()), containsInAnyOrder("Chorioepithelioma"));
		assertThat(term.getChildren().size(), greaterThanOrEqualTo(8));
		assertThat(term.getParents().size(), equalTo(1));
		assertThat(term.getDefLinks().size(), equalTo(1));
	}

	@Test
	// Test Sox9 from MGI for disease via experiment records
	public void checkDiseaseRibbonHeader() {
		DiseaseRibbonSummary summary = diseaseESService.getDiseaseRibbonSummary(List.of("MGI:98297"), false, false);
		assertNotNull(summary);
	}

	@Test
	public void diseaseModelDownload() {

		// Diamond-Blackfan anemia
		String diseaseID = "DOID:1838";

		JsonResultResponse<AGMDiseaseAnnotationDocument> response = diseaseController.getDiseaseAnnotationsForModel(diseaseID, 15, 1, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		assertResponse(response, 15, 17);

		DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
		/*
		 * String output = translator.getAllRowsForModel(response.getResults());
		 * assertEquals(output,
		 * "Model ID\tModel Symbol\tSpecies ID\tSpecies Name\tDisease ID\tDisease Name\tEvidence Code\tEvidence Code Name\tSource\tReference\n"
		 * +
		 * "MGI:6324209\tAtp7a<Mo-blo>/? [background:] involves: C57BL/6J\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:6685755\n"
		 * +
		 * "MGI:6324210\tAtp7a<Mo-blo>/Atp7a<+> [background:] involves: C57BL/6J\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:6685755\n"
		 * +
		 * "MGI:3793780\tAtp7a<Mo-br>/? [background:] involves: C57BL\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:4858102\n"
		 * +
		 * "MGI:5696621\tAtp7a<Mo-dp>/? [background:] involves: 101/H * C3H/HeH\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:25456742\n"
		 * +
		 * "MGI:5696613\tAtp7a<Mo-dp>/Atp7a<+> [background:] involves: 101/H * C3H/HeH\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:25456742\n"
		 * +
		 * "MGI:6324231\tAtp7a<Mo-ml>/? [background:] involves: C3Hf/He\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:1819648\n"
		 * +
		 * "MGI:6324231\tAtp7a<Mo-ml>/? [background:] involves: C3Hf/He\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:1819648\n"
		 * +
		 * "MGI:4940051\tAtp7a<Mo-ms>/? [background:] Not Specified\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:20831904\n"
		 * +
		 * "MGI:3618244\tAtp7a<Mo-Tohm>/Atp7a<+> [background:] B6.Cg-Atp7a<Mo-Tohm>\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:16338116\n"
		 * +
		 * "MGI:3793729\tAtp7a<Mo-vbr>/? [background:] Not Specified\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:10098864\n"
		 * +
		 * "MGI:2175712\tAtp7a<Mo>/Atp7a<+> [background:] Not Specified\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:13103353\n"
		 * +
		 * "MGI:2657020\tLox<tm1Ikh>/Lox<tm1Ikh> [background:] involves: 129X1/SvJ * C57BL/6J\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:12473682\n"
		 * +
		 * "ZFIN:ZDB-FISH-180905-22\tatp7a<gw71/gw71>\tNCBITaxon:7955\tDanio rerio\tDOID:1838\tMenkes disease\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:29507920\n"
		 * +
		 * "ZFIN:ZDB-FISH-150901-6650\tatp7a<j246/j246>\tNCBITaxon:7955\tDanio rerio\tDOID:1838\tMenkes disease\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:18316734\n"
		 * +
		 * "ZFIN:ZDB-FISH-150901-17526\tatp7a<vu69/vu69>\tNCBITaxon:7955\tDanio rerio\tDOID:1838\tMenkes disease\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:16890543\n"
		 * +
		 * "ZFIN:ZDB-FISH-150901-27568\tatp7a<vu69/vu69>\tNCBITaxon:7955\tDanio rerio\tDOID:1838\tMenkes disease\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:18316734\n"
		 * );
		 */

		diseaseID = "DOID:1324";

		response = diseaseController.getDiseaseAnnotationsForModel(diseaseID, 100, 1, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		/*
		 * int rowSize =
		 * translator.getDiseaseModelDownloadRows(response.getResults()).size();
		 * assertNotNull(response); assertThat(rowSize,
		 * greaterThan(response.getTotal()));
		 */
	}

	@Test
	// ToDo Fix up this test. It's been broken as the endpoint was migrated over to
	// ES
	public void diseaseGeneDownload() {

		// Diamond-Blackfan anemia
		String diseaseID = "DOID:1838";

		JsonResultResponse<GeneDiseaseAnnotationDocument> response = diseaseController.getDiseaseAnnotationsByGene(diseaseID, 7, 1, null, null, null, null, "Alliance", null, null, null, null, null, null, null);

		assertResponse(response, 7, 20);

		DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
		/*
		 * String output = translator.getAllRowsForGenes(response.getResults());
		 * assertEquals(output,
		 * "Species ID\tSpecies Name\tGene ID\tGene Symbol\tGenetic Entity ID\tGenetic Entity Name\tGenetic Entity Type\tAssociation\tDisease ID\tDisease Name\tEvidence Code\tEvidence Code Name\tBased On ID\tBased On Name\tSource\tReference\n"
		 * +
		 * "NCBITaxon:9606\tHomo sapiens\tHGNC:869\tATP7A\tHGNC:869\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tFB:FBgn0030343\tATP7\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:9606\tHomo sapiens\tHGNC:869\tATP7A\tHGNC:869\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tMGI:99400\tAtp7a\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:9606\tHomo sapiens\tHGNC:869\tATP7A\tHGNC:869\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tSGD:S000002678\tCCC2\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:9606\tHomo sapiens\tHGNC:869\tATP7A\tHGNC:869\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tWB:WBGene00000834\tcua-1\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:9606\tHomo sapiens\tHGNC:870\tATP7B\tHGNC:870\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tFB:FBgn0030343\tATP7\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:9606\tHomo sapiens\tHGNC:870\tATP7B\tHGNC:870\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tSGD:S000002678\tCCC2\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:9606\tHomo sapiens\tHGNC:870\tATP7B\tHGNC:870\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tWB:WBGene00000834\tcua-1\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:9606\tHomo sapiens\tHGNC:6664\tLOX\tHGNC:6664\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tMGI:96817\tLox\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:9606\tHomo sapiens\tHGNC:11017\tSLC31A2\tHGNC:11017\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tSGD:S000006328\tCTR1\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:10116\tRattus norvegicus\tRGD:2179\tAtp7a\tRGD:2179\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tFB:FBgn0030343\tATP7\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:10116\tRattus norvegicus\tRGD:2179\tAtp7a\tRGD:2179\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tHGNC:869\tATP7A\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:10116\tRattus norvegicus\tRGD:2179\tAtp7a\tRGD:2179\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tMGI:99400\tAtp7a\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:10116\tRattus norvegicus\tRGD:2179\tAtp7a\tRGD:2179\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tSGD:S000002678\tCCC2\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:10116\tRattus norvegicus\tRGD:2179\tAtp7a\tRGD:2179\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tWB:WBGene00000834\tcua-1\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:10116\tRattus norvegicus\tRGD:2180\tAtp7b\tRGD:2180\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tFB:FBgn0030343\tATP7\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:10116\tRattus norvegicus\tRGD:2180\tAtp7b\tRGD:2180\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tSGD:S000002678\tCCC2\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:10116\tRattus norvegicus\tRGD:2180\tAtp7b\tRGD:2180\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tWB:WBGene00000834\tcua-1\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:10116\tRattus norvegicus\tRGD:3015\tLox\tRGD:3015\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tHGNC:6664\tLOX\tAlliance\tMGI:6194238\n"
		 * +
		 * "NCBITaxon:10116\tRattus norvegicus\tRGD:3015\tLox\tRGD:3015\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tMGI:96817\tLox\tAlliance\tMGI:6194238\n"
		 * );
		 */

		diseaseID = "DOID:1324";

		response = diseaseController.getDiseaseAnnotationsByGene(diseaseID, 2000, 1, null, null, null, null, null, null, null, null, null, null, null, null);
		int rowSize = 0;
		// int rowSize =
		// translator.getDownloadRowsFromGenes(response.getResults()).size();
		assertNotNull(response);
		assertThat(rowSize, greaterThan(response.getTotal()));
	}

	@Test
	public void diseaseAlleleDownload() {

		// Diamond-Blackfan anemia
//		String diseaseID = "DOID:1339";
//
//		Pagination pagination = new Pagination(1, 10, null, null);
//
//		JsonResultResponse<DiseaseAnnotation> response = diseaseController.getDiseaseAnnotationsByAllele(diseaseID,
//				10,
//				1,
//				"DiseaseAlleleDefault",
//				null,
//				null,
//				null,
//				null,
//				null,
//				null,
//				null,
//				null,
//				null
//		);
//
//		assertResponse(response, 6, 6);
//
//		DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
//		String output = translator.getAllRowsForAllele(response.getResults());
//		assertEquals(output, "Allele ID\tAllele Symbol\tGenetic Entity ID\tGenetic Entity Name\tGenetic Entity Type\tSpecies ID\tSpecies Name\tAssociation\tDisease ID\tDisease Name\tEvidence Code\tEvidence Code Name\tSource\tReference\n" +
//				"MGI:3776022\tFlvcr1<tm1.1Jlab>\tMGI:3807528\tFlvcr1<tm1.1Jlab>/Flvcr1<tm1.1Jlab> [background:] involves: 129S4/SvJae * C57BL/6 * DBA/2\tgenotype\tNCBITaxon:10090\tMus musculus\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:18258918\n" +
//				"MGI:3776021\tFlvcr1<tm1Jlab>\tMGI:3807529\tFlvcr1<tm1Jlab>/Flvcr1<tm1Jlab> Tg(Mx1-cre)1Cgn/? [background:] involves: 129S4/SvJae * C57BL/6 * CBA\tgenotype\tNCBITaxon:10090\tMus musculus\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:18258918\n" +
//				"MGI:3803603\tRpsa<tm1Ells>\tMGI:3804635\tRpsa<tm1Ells>/Rpsa<+> [background:] involves: 129S6/SvEvTac * C57BL/6\tgenotype\tNCBITaxon:10090\tMus musculus\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tMGI:3804630\n" +
//				"MGI:4839313\tTg(CAG-RPS19*R62W)#Dmb\tMGI:4839332\tTg(CAG-RPS19*R62W)#Dmb/? Tg(Prnp-GFP/cre)1Blw/? [background:] involves: 129S6/SvEvTac * FVB/N\tgenotype\tNCBITaxon:10090\tMus musculus\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:20606162\n" +
//				"ZFIN:ZDB-ALT-041001-12\thi3820bTg\tZFIN:ZDB-FISH-150901-16866\trpl11<hi3820bTg/hi3820bTg>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:24812435\n" +
//				"ZFIN:ZDB-ALT-041001-12\thi3820bTg\tZFIN:ZDB-FISH-150901-16866\trpl11<hi3820bTg/hi3820bTg>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:25058426\n" +
//				"ZFIN:ZDB-ALT-041001-12\thi3820bTg\tZFIN:ZDB-FISH-150901-8506\trpl11<hi3820bTg/hi3820bTg>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:26109203\n" +
//				"ZFIN:ZDB-ALT-041001-12\thi3820bTg\tZFIN:ZDB-FISH-150901-8506\trpl11<hi3820bTg/hi3820bTg>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:29225165\n" +
//				"ZFIN:ZDB-ALT-041001-12\thi3820bTg\tZFIN:ZDB-FISH-150901-8506\trpl11<hi3820bTg/hi3820bTg>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:29581525\n" +
//				"ZFIN:ZDB-ALT-151012-9\tzf556\tZFIN:ZDB-FISH-151013-1\trps19<zf556/zf556>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:25058426\n" +
//				"ZFIN:ZDB-ALT-151012-9\tzf556\tZFIN:ZDB-FISH-151013-1\trps19<zf556/zf556>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:26109203\n");
//
//		// Menkes Disease
//		diseaseID = "DOID:1838";
//		response = diseaseController.getDiseaseAnnotationsByAllele(diseaseID,
//				10,
//				1,
//				null,
//				null,
//				null,
//				null,
//				null,
//				null,
//				null,
//				null,
//				null,
//				null
//		);
//
//		assertResponse(response, 10, 16);
//
//		translator = new DiseaseAnnotationToTdfTranslator();
//		output = translator.getAllRowsForAllele(response.getResults());
	}

	private void assertResponse(JsonResultResponse response, int resultSize, int totalSize) {
		assertNotNull(response);
		assertThat("Number of returned records", response.getResults().size(), equalTo(resultSize));
		assertThat("Number of total records", response.getTotal(), equalTo(totalSize));
	}

	private void assertLimitResponse(JsonResultResponse response, int resultSize, int totalSize) {
		assertNotNull(response);
		assertThat("Number of returned records", response.getResults().size(), greaterThanOrEqualTo(resultSize));
		assertThat("Number of total records", response.getTotal(), greaterThanOrEqualTo(totalSize));
	}

}
