package org.alliancegenome.core.variant.converters;

import htsjdk.variant.variantcontext.VariantContext;
import io.github.lukehutch.fastclasspathscanner.utils.Join;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Converter {
    public List<AlleleVariantSequence> convertContextToAlleleVariantSequence(VariantContext ctx, String[] header, SpeciesType speciesType) throws Exception {
        List<AlleleVariantSequence> returnDocuments = new ArrayList<AlleleVariantSequence>();

        htsjdk.variant.variantcontext.Allele refNuc = ctx.getReference();
        Species species=new Species();
        species.setName(speciesType.getName());
        species.setCommonNames(speciesType.getDisplayName());
        species.setId(Long.valueOf(speciesType.getTaxonIDPart()));
        species.setPrimaryKey(speciesType.getTaxonID());
        for (htsjdk.variant.variantcontext.Allele a : ctx.getAlternateAlleles()) {
            if (a.compareTo(refNuc) < 0) {
                continue;
            }
            if (!alleleIsValid(ctx.getReference().getBaseString())) {
                //   System.out.println(" *** Ref Nucleotides must be A,C,G,T,N");
                continue;
            }
            if (!alleleIsValid(a.getBaseString())) {
                //     System.out.println(" *** Var Nucleotides must be A,C,G,T,N");
                continue;
            }

            SOTerm variantType = new SOTerm();
            variantType.setName(ctx.getType().name().toUpperCase());
            variantType.setPrimaryKey(ctx.getType().name());
            if ("INDEL".equals(ctx.getType().name())) {
                variantType.setName("delins");
                variantType.setPrimaryKey("delins");
            }
            int endPos = 0;

            if (ctx.isSNP()) {
                endPos = ctx.getStart() + 1;
            } // insertions
            if (ctx.isSimpleInsertion()) {
                endPos = ctx.getStart();
                //  System.out.println("INSERTION");
            } else if (ctx.isSimpleDeletion()) {
                endPos = ctx.getStart() + refNuc.getDisplayString().length();
                //  System.out.println("Deletion");
            } else {
                //   System.out.println("Unexpected var type");
            }
            for (TranscriptLevelConsequence c : getConsequences(ctx, a.getBaseString(), header)) {
                AlleleVariantSequence s = new AlleleVariantSequence();
                Variant variant = new Variant();
                variant.setVariantType(variantType);
                variant.setSpecies(species);
                variant.setStart(String.valueOf(ctx.getStart()));
                variant.setEnd(String.valueOf(endPos));
                variant.setNucleotideChange(a.getBaseString()); // variantDocument.setVarNuc(a.getBaseString());
                variant.setName(c.getHgvsVEPGeneNomenclature());
                variant.setHgvsNomenclature(c.getHgvsVEPGeneNomenclature());
                variant.setGene(c.getAssociatedGene());
                s.setVariant(variant);
                s.setConsequence(c);
                s.setAlterationType("variant");
                   /* s.setNameKey(c.getHgvsVEPGeneNomenclature());
                    s.setName(c.getHgvsVEPGeneNomenclature());
                    s.setAlterationType("variant");*/

                returnDocuments.add(s);
            }
        }
        return returnDocuments;
    }
    public List<AlleleVariantSequence> translateToNewAlleleVariantSequence(Allele a,String hgvsg, String matchedWithHtp){
        List<AlleleVariantSequence> sequences=new ArrayList<>();
        Gene g=new Gene();
        g.setPrimaryKey(a.getGene().getPrimaryKey());
        g.setSymbol(a.getGene().getSymbol());
        g.setSpecies(a.getGene().getSpecies());
        Allele al=  mapAllele(a);
        for(Variant v:a.getVariants()){
            AlleleVariantSequence seq=new AlleleVariantSequence();
            seq.setCategory("allele");
            seq.setMatchedWithHtp(matchedWithHtp);
            Variant vt=getMappedVariant(v);
            vt.setGene(g);
            vt.setHgvsNomenclature(hgvsg);

            if(v.getTranscriptLevelConsequence()!=null && v.getTranscriptLevelConsequence().size()>0) {
                for (TranscriptLevelConsequence c : v.getTranscriptLevelConsequence()) {
                    TranscriptLevelConsequence con = getConsequenceMapped(c);
                    if(c.getAssociatedGene()!=null) {
                        Gene ag = new Gene();
                        ag.setSymbol(c.getAssociatedGene().getSymbol());
                        ag.setModGlobalId(c.getAssociatedGene().getModGlobalId());
                        con.setAssociatedGene(ag);
                    }
                    seq.setConsequence(con);
                    seq.setAllele(al);
                    seq.setVariant(vt);
                    sequences.add(seq);
                }
            }else{
                seq.setAllele(al);
                seq.setVariant(vt);
                sequences.add(seq);
            }
        }
        return  sequences;
    }
    public TranscriptLevelConsequence getConsequenceMapped(TranscriptLevelConsequence c){
        TranscriptLevelConsequence con= new TranscriptLevelConsequence();
        con.setPolyphenPrediction(c.getPolyphenPrediction());
        con.setPolyphenScore(c.getPolyphenScore());
        con.setSiftPrediction(c.getSiftPrediction());
        con.setSiftScore(c.getSiftScore());
        con.setCodonChange(c.getCodonChange());
        con.setCodonReference(c.getCodonReference());
        con.setAminoAcidChange(c.getAminoAcidChange());
        con.setProteinStartPosition(c.getProteinStartPosition());
        con.setHgvsProteinNomenclature(c.getHgvsProteinNomenclature());
        con.setHgvsCodingNomenclature(c.getHgvsCodingNomenclature());
        con.setCdsStartPosition(c.getCdsStartPosition());
        con.setCdnaStartPosition(c.getCdnaStartPosition());
        con.setTranscriptLocation(c.getTranscriptLocation());
        con.setTranscriptID(c.getTranscriptID());
        con.setSequenceFeatureType(c.getSequenceFeatureType());
        con.setImpact(c.getImpact());
        con.setTranscriptLevelConsequence(c.getTranscriptLevelConsequence());
        con.setTranscriptName(c.getTranscriptName());
        return con;
    }
    public Variant getMappedVariant(Variant v){

        Variant vt=new Variant();
        vt.setStart(String.valueOf(v.getLocation().getStart()));
        vt.setEnd(String.valueOf(v.getLocation().getEnd()));
        vt.setGenomicVariantSequence(v.getGenomicVariantSequence());
        vt.setGenomicReferenceSequence(v.getGenomicReferenceSequence());
        vt.setVariantType(v.getVariantType());
        vt.setName(v.getHgvsG().get(1));
        return vt;
    }
    public Allele mapAllele(Allele a){
        Gene g=new Gene();
        g.setPrimaryKey(a.getGene().getPrimaryKey());
        g.setSymbol(a.getGene().getSymbol());
        g.setSpecies(a.getGene().getSpecies());

        Allele al=new Allele();
        al.setGene(g);
        al.setGlobalId(a.getGlobalId());
        al.setSymbolText(a.getSymbolText());
        al.setSymbol(a.getSymbol());
        return al;

    }

        public List<TranscriptLevelConsequence> getConsequences(VariantContext ctx, String varNuc, String[] header) throws Exception {
            List<TranscriptLevelConsequence> features = new ArrayList<>();
            List<String> alreadyAdded=new ArrayList<>();
            for (String s : ctx.getAttributeAsStringList("CSQ", "")) {
                if (s.length() > 0) {
                    String[] infos = s.split("\\|", -1);

                    if (header.length == infos.length) {
                        if (infos[0].equalsIgnoreCase(varNuc)) {
                            TranscriptLevelConsequence feature = getTranscriptLevelConsq(infos);
                            if(!alreadyAdded.contains(feature.getTranscriptID())) {
                                features.add(feature);
                                alreadyAdded.add(feature.getTranscriptID());
                            }
                        }
                    } else {
                        String message = "Diff: " + header.length + " " + infos.length;
                        message += "\r" + Join.join("|", header);
                        message += "\r" +String.join("|", Arrays.asList(infos));
                        throw new RuntimeException("CSQ header is not matching the line " + message);
                    }
                }
            }
            return features;
        }
    public boolean alleleIsValid(String allele) {
        for (int i = 0; i < allele.length(); i++) {
            char c = allele.charAt(i);
            if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N' || c == '-')
                continue;
            return false;
        }
        return true;
    }
    public TranscriptLevelConsequence  getTranscriptLevelConsq(String[] infos ){
        // Mod VEP
        //  Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON
        // |HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|DISTANCE|STRAND|
        //  FLAGS|SYMBOL_SOURCE|HGNC_ID|GIVEN_REF|USED_REF|BAM_EDIT|SOURCE|HGVS_OFFSET|HGVSg|
        //  PolyPhen_prediction|PolyPhen_score|SIFT_prediction|SIFT_score|Genomic_end_position|Genomic_start_position
        TranscriptLevelConsequence c=new TranscriptLevelConsequence();
        c.setTranscriptLevelConsequence( infos[1]);
        c.setImpact( infos[2]);
        Gene g=new Gene();
        g.setSymbol(infos[3]);
        g.setPrimaryKey(infos[4]);
        c.setAssociatedGene(g);
        c.setSequenceFeatureType(infos[7]);
        c.setTranscriptID(infos[6]);
        String location = "";
        if (StringUtils.isNotEmpty(infos[8]))
            location += "Exon " + infos[8];
        if (StringUtils.isNotEmpty(infos[9]))
            location += "Intron " + infos[9];
        c.setTranscriptLocation(location);
        /*  biotype = infos[7];
         */
        c.setCdnaStartPosition(infos[12]);
        c.setCdsStartPosition( infos[13]);
        c.setHgvsCodingNomenclature( infos[10]);
        c.setHgvsProteinNomenclature(infos[11]);
        c.setProteinStartPosition(infos[14]);
        c.setAminoAcidChange(infos[15]);
        c.setCodonChange(infos[16]);
        c.setPolyphenPrediction(infos[30]);
        c.setPolyphenPrediction(infos[30]);
        c.setSiftPrediction(infos[29]);
        c.setSiftScore(infos[32]);
        c.setHgvsVEPGeneNomenclature(infos[28]);

        //  c.setCodonReference(infos[26]); // need to verify

        //  variant.setGenomicVariantSequence(transcriptFeature.getAllele());
        //  genomicEnd = infos[33];
        //   genomicStart = infos[34];

      /*  existingVariation = infos[17];
        distance = infos[18];
        strand = infos[19];

        flags = infos[20];
        symbolSource = infos[21];
        hgncId = infos[22];
        refseqMatch = infos[23];
        source = infos[24];
        refseqOffset = infos[25];
        givenRef = infos[26];
        usedRef = infos[27];
        bamEdit = infos[28];

        hgvsOffset = infos[31];*/
        return c;
    }

}
