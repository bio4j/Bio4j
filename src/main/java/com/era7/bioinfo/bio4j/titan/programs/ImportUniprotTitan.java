/*
 * Copyright (C) 2010-2013  "Bio4j"
 *
 * This file is part of Bio4j
 *
 * Bio4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.era7.bioinfo.bio4j.titan.programs;

import com.era7.bioinfo.bio4j.CommonData;
import com.era7.bioinfo.bio4j.blueprints.model.nodes.*;
import com.era7.bioinfo.bio4j.blueprints.model.nodes.reactome.ReactomeTermNode;
import com.era7.bioinfo.bio4j.blueprints.model.nodes.refseq.GenomeElementNode;
import com.era7.bioinfo.bio4j.blueprints.model.relationships.TaxonParentRel;
import com.era7.bioinfo.bio4j.blueprints.model.relationships.protein.*;
import com.era7.bioinfo.bio4j.titan.model.util.Bio4jManager;
import com.era7.bioinfo.bio4j.titan.model.util.NodeRetriever;
import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.bio4j.UniprotDataXML;
import com.era7.lib.era7xmlapi.model.XMLElement;
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.jdom.Element;

/**
 * This class deals with the main part of Bio4j importing process.
 * ImportGeneOntology importation must have been performed prior to this step.
 * Features, comments, GeneOntology annotations and all information directly
 * related to entries are imported in this step, (except protein interactions
 * and isoform sequences).
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class ImportUniprotTitan implements Executable {

    private static final Logger logger = Logger.getLogger("ImportUniprotTitan");
    private static FileHandler fh;

    @Override
    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {

        if (args.length != 3) {
            System.out.println("This program expects the following parameters: \n"
                    + "1. Uniprot xml filename \n"
                    + "2. Bio4j DB folder \n"
                    + "3. Config XML file");
        } else {

            long initTime = System.nanoTime();

            File inFile = new File(args[0]);
            File configFile = new File(args[2]);

            String currentAccessionId = "";

            BufferedWriter enzymeIdsNotFoundBuff = null;
            BufferedWriter statsBuff = null;

            int proteinCounter = 0;
            int limitForPrintingOut = 10000;

            //------------------ init DB handlers------------------------
            Configuration conf = new BaseConfiguration();
            conf.setProperty("storage.directory", args[1]);
            conf.setProperty("storage.backend", "local");

            Bio4jManager manager = new Bio4jManager(conf);
            NodeRetriever nodeRetriever = new NodeRetriever(manager);
            BatchGraph bGraph = new BatchGraph(manager.getGraph(), BatchGraph.IdType.STRING, 1000);

            try {

                // This block configures the logger with handler and formatter
                fh = new FileHandler("ImportUniprotTitan" + args[0].split("\\.")[0] + ".log", false);

                SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);
                logger.addHandler(fh);
                logger.setLevel(Level.ALL);

                System.out.println("Reading conf file...");
                BufferedReader reader = new BufferedReader(new FileReader(configFile));
                String line;
                StringBuilder stBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    stBuilder.append(line);
                }
                reader.close();

                UniprotDataXML uniprotDataXML = new UniprotDataXML(stBuilder.toString());

                //---creating writer for enzymes not found file-----
                enzymeIdsNotFoundBuff = new BufferedWriter(new FileWriter(new File("EnzymeIdsNotFound.log")));

                //---creating writer for stats file-----
                statsBuff = new BufferedWriter(new FileWriter(new File("ImportUniprotTitanStats_" + inFile.getName().split("\\.")[0] + ".txt")));

                reader = new BufferedReader(new FileReader(inFile));
                StringBuilder entryStBuilder = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("<" + CommonData.ENTRY_TAG_NAME)) {

                        while (!line.trim().startsWith("</" + CommonData.ENTRY_TAG_NAME + ">")) {
                            entryStBuilder.append(line);
                            line = reader.readLine();
                        }

                        ProteinNode currentProteinNode = new ProteinNode(manager.createNode(ProteinNode.NODE_TYPE));

                        //linea final del organism
                        entryStBuilder.append(line);
                        //System.out.println("organismStBuilder.toString() = " + organismStBuilder.toString());
                        XMLElement entryXMLElem = new XMLElement(entryStBuilder.toString());
                        entryStBuilder.delete(0, entryStBuilder.length());

                        String modifiedDateSt = entryXMLElem.asJDomElement().getAttributeValue(CommonData.ENTRY_MODIFIED_DATE_ATTRIBUTE);

                        String accessionSt = entryXMLElem.asJDomElement().getChildText(CommonData.ENTRY_ACCESSION_TAG_NAME);
                        String nameSt = entryXMLElem.asJDomElement().getChildText(CommonData.ENTRY_NAME_TAG_NAME);
                        String fullNameSt = getProteinFullName(entryXMLElem.asJDomElement().getChild(CommonData.PROTEIN_TAG_NAME));
                        String shortNameSt = getProteinShortName(entryXMLElem.asJDomElement().getChild(CommonData.PROTEIN_TAG_NAME));

                        if (shortNameSt == null) {
                            shortNameSt = "";
                        }
                        if (fullNameSt == null) {
                            fullNameSt = "";
                        }

                        currentAccessionId = accessionSt;

                        //-----------alternative accessions-------------
                        ArrayList<String> alternativeAccessions = new ArrayList<String>();
                        List<Element> altAccessionsList = entryXMLElem.asJDomElement().getChildren(CommonData.ENTRY_ACCESSION_TAG_NAME);
                        for (int i = 1; i < altAccessionsList.size(); i++) {
                            alternativeAccessions.add(altAccessionsList.get(i).getText());
                        }
                        currentProteinNode.setAlternativeAccessions(convertToStringArray(alternativeAccessions));

                        //-----db references-------------
                        String pirIdSt = "";
                        String keggIdSt = "";
                        String ensemblIdSt = "";
                        String uniGeneIdSt = "";
                        String arrayExpressIdSt = "";

                        List<Element> dbReferenceList = entryXMLElem.asJDomElement().getChildren(CommonData.DB_REFERENCE_TAG_NAME);
                        ArrayList<String> emblCrossReferences = new ArrayList<String>();
                        ArrayList<String> refseqReferences = new ArrayList<String>();
                        ArrayList<String> enzymeDBReferences = new ArrayList<String>();
                        ArrayList<String> ensemblPlantsReferences = new ArrayList<String>();
                        HashMap<String, String> reactomeReferences = new HashMap<String, String>();

                        for (Element dbReferenceElem : dbReferenceList) {
                            String refId = dbReferenceElem.getAttributeValue("id");
                            if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("Ensembl")) {
                                ensemblIdSt = refId;
                            } else if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("PIR")) {
                                pirIdSt = refId;
                            } else if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("UniGene")) {
                                uniGeneIdSt = refId;
                            } else if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("KEGG")) {
                                keggIdSt = refId;
                            } else if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("EMBL")) {
                                emblCrossReferences.add(refId);
                            } else if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("EC")) {
                                enzymeDBReferences.add(refId);
                            } else if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("ArrayExpress")) {
                                arrayExpressIdSt = refId;
                            } else if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("RefSeq")) {
                                //refseqReferences.add(refId);
                                List<Element> children = dbReferenceElem.getChildren("property");
                                for (Element propertyElem : children) {
                                    if (propertyElem.getAttributeValue("type").equals("nucleotide sequence ID")) {
                                        refseqReferences.add(propertyElem.getAttributeValue("value"));
                                    }
                                }
                            } else if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("Reactome")) {
                                Element propertyElem = dbReferenceElem.getChild("property");
                                String pathwayName = "";
                                if (propertyElem.getAttributeValue("type").equals("pathway name")) {
                                    pathwayName = propertyElem.getAttributeValue("value");
                                }
                                reactomeReferences.put(refId, pathwayName);
                            } else if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("EnsemblPlants")) {
                                ensemblPlantsReferences.add(refId);
                            }

                        }

                        Element sequenceElem = entryXMLElem.asJDomElement().getChild(CommonData.ENTRY_SEQUENCE_TAG_NAME);
                        String sequenceSt = sequenceElem.getText();
                        int seqLength = Integer.parseInt(sequenceElem.getAttributeValue(CommonData.SEQUENCE_LENGTH_ATTRIBUTE));
                        float seqMass = Float.parseFloat(sequenceElem.getAttributeValue(CommonData.SEQUENCE_MASS_ATTRIBUTE));

                        currentProteinNode.setModifiedDate(modifiedDateSt);
                        currentProteinNode.setAccession(accessionSt);
                        currentProteinNode.setName(nameSt);
                        currentProteinNode.setFullName(fullNameSt);
                        currentProteinNode.setShortName(shortNameSt);
                        currentProteinNode.setSequence(sequenceSt);
                        currentProteinNode.setLength(seqLength);
                        currentProteinNode.setMass(seqMass);
                        currentProteinNode.setArrayExpressId(arrayExpressIdSt);
                        currentProteinNode.setPIRId(pirIdSt);
                        currentProteinNode.setKeggId(keggIdSt);
                        currentProteinNode.setEMBLreferences(convertToStringArray(emblCrossReferences));
                        currentProteinNode.setEnsemblPlantsReferences(convertToStringArray(ensemblPlantsReferences));
                        currentProteinNode.setUniGeneId(uniGeneIdSt);
                        currentProteinNode.setEnsemblId(ensemblIdSt);

                        //---------------gene-names-------------------
                        Element geneElement = entryXMLElem.asJDomElement().getChild(CommonData.GENE_TAG_NAME);
                        ArrayList<String> geneNames = new ArrayList<String>();
                        if (geneElement != null) {
                            List<Element> genesList = geneElement.getChildren(CommonData.GENE_NAME_TAG_NAME);
                            for (Element geneNameElem : genesList) {
                                geneNames.add(geneNameElem.getText());
                            }
                        }
                        currentProteinNode.setGeneNames(convertToStringArray(geneNames));
                        //-----------------------------------------


                        //--------------refseq associations----------------
                        if (uniprotDataXML.getRefseq()) {
                            for (String refseqReferenceSt : refseqReferences) {

                                GenomeElementNode genomeElementNode = nodeRetriever.getGenomeElementByVersion(refseqReferenceSt);

                                if (genomeElementNode != null) {
                                    bGraph.addEdge(null, currentProteinNode.getNode(), genomeElementNode.getNode(), ProteinGenomeElementRel.NAME);
                                } else {
                                    logger.log(Level.INFO, ("GenomeElem not found for: " + currentAccessionId + " , " + refseqReferenceSt));
                                }

                            }
                        }

                        //--------------reactome associations----------------
                        if (uniprotDataXML.getReactome()) {
                            for (String reactomeId : reactomeReferences.keySet()) {

                                ReactomeTermNode reactomeTermNode = nodeRetriever.getReactomeTermById(reactomeId);

                                if (reactomeTermNode == null) {
                                    reactomeTermNode = new ReactomeTermNode(manager.createNode(ReactomeTermNode.NODE_TYPE));
                                    reactomeTermNode.setId(reactomeId);
                                    reactomeTermNode.setPathwayName(reactomeReferences.get(reactomeId));
                                }

                                bGraph.addEdge(null, currentProteinNode.getNode(), reactomeTermNode.getNode(), ProteinReactomeRel.NAME);
                            }
                        }
                        //-------------------------------------------------------

                        //---------------enzyme db associations----------------------
                        if (uniprotDataXML.getEnzymeDb()) {
                            for (String enzymeDBRef : enzymeDBReferences) {

                                EnzymeNode enzymeNode = nodeRetriever.getEnzymeById(enzymeDBRef);

                                if (enzymeNode != null) {
                                    bGraph.addEdge(null, currentProteinNode.getNode(), enzymeNode.getNode(), ProteinEnzymaticActivityRel.NAME);
                                } else {
                                    enzymeIdsNotFoundBuff.write("Enzyme term: " + enzymeDBRef + " not found.\t" + currentAccessionId);
                                }
                            }
                        }
                        //------------------------------------------------------------


                        //-----comments import---
                        if (uniprotDataXML.getComments()) {
                            importProteinComments(entryXMLElem, inserter, indexProvider, currentProteinId, sequenceSt, uniprotDataXML);
                        }

                        //-----features import----
                        if (uniprotDataXML.getFeatures()) {
                            importProteinFeatures(entryXMLElem, inserter, indexProvider, currentProteinId);
                        }

                        //--------------------------------datasets--------------------------------------------------
                        String proteinDataSetSt = entryXMLElem.asJDomElement().getAttributeValue(CommonData.ENTRY_DATASET_ATTRIBUTE);

                        DatasetNode datasetNode = nodeRetriever.getDatasetByName(proteinDataSetSt);

                        if (datasetNode == null) {
                            datasetNode = new DatasetNode(manager.createNode(DatasetNode.NODE_TYPE));
                            datasetNode.setName(proteinDataSetSt);
                        }
                        bGraph.addEdge(null, currentProteinNode.getNode(), datasetNode.getNode(), ProteinDatasetRel.NAME);
                        //---------------------------------------------------------------------------------------------


                        if (uniprotDataXML.getCitations()) {
                            importProteinCitations(entryXMLElem,
                                    inserter,
                                    indexProvider,
                                    currentProteinId,
                                    uniprotDataXML);
                        }


                        //-------------------------------keywords------------------------------------------------------
                        if (uniprotDataXML.getKeywords()) {
                            List<Element> keywordsList = entryXMLElem.asJDomElement().getChildren(CommonData.KEYWORD_TAG_NAME);
                            for (Element keywordElem : keywordsList) {
                                String keywordId = keywordElem.getAttributeValue(CommonData.KEYWORD_ID_ATTRIBUTE);
                                String keywordName = keywordElem.getText();

                                KeywordNode keywordNode = nodeRetriever.getKeywordById(keywordId);

                                if (keywordNode == null) {

                                    keywordNode = new KeywordNode(manager.createNode(KeywordNode.NAME_PROPERTY));
                                    keywordNode.setId(keywordId);
                                    keywordNode.setName(keywordName);

                                }
                                bGraph.addEdge(null, currentProteinNode.getNode(), currentProteinNode.getNode(), ProteinKeywordRel.NAME);
                            }
                        }
                        //---------------------------------------------------------------------------------------


                        for (Element dbReferenceElem : dbReferenceList) {

                            //-------------------------------INTERPRO------------------------------------------------------  
                            if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals(CommonData.INTERPRO_DB_REFERENCE_TYPE)) {

                                if (uniprotDataXML.getInterpro()) {
                                    String interproId = dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_ID_ATTRIBUTE);

                                    InterproNode interproNode = nodeRetriever.getInterproById(interproId);

                                    if (interproNode == null) {
                                        String interproEntryNameSt = "";
                                        List<Element> properties = dbReferenceElem.getChildren(CommonData.DB_REFERENCE_PROPERTY_TAG_NAME);
                                        for (Element prop : properties) {
                                            if (prop.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals(CommonData.INTERPRO_ENTRY_NAME)) {
                                                interproEntryNameSt = prop.getAttributeValue(CommonData.DB_REFERENCE_VALUE_ATTRIBUTE);
                                                break;
                                            }
                                        }

                                        interproNode = new InterproNode(manager.createNode(InterproNode.NODE_TYPE));
                                        interproNode.setId(interproId);
                                        interproNode.setName(interproEntryNameSt);

                                    }

                                    bGraph.addEdge(null, currentProteinNode.getNode(), interproNode.getNode(), ProteinInterproRel.NAME);
                                }

                            } //-------------------------------PFAM------------------------------------------------------  
                            else if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("Pfam")) {

                                if (uniprotDataXML.getPfam()) {
                                    String pfamId = dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_ID_ATTRIBUTE);

                                    PfamNode pfamNode = nodeRetriever.getPfamById(pfamId);

                                    if (pfamNode == null) {
                                        String pfamEntryNameSt = "";
                                        List<Element> properties = dbReferenceElem.getChildren(CommonData.DB_REFERENCE_PROPERTY_TAG_NAME);
                                        for (Element prop : properties) {
                                            if (prop.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals("entry name")) {
                                                pfamEntryNameSt = prop.getAttributeValue(CommonData.DB_REFERENCE_VALUE_ATTRIBUTE);
                                                break;
                                            }
                                        }
                                        pfamNode = new PfamNode(manager.createNode(PfamNode.NODE_TYPE));
                                        pfamNode.setId(pfamId);
                                        pfamNode.setName(pfamEntryNameSt);
                                    }

                                    bGraph.addEdge(null, currentProteinNode.getNode(), pfamNode.getNode(), ProteinPfamRel.NAME);
                                }


                            } //-------------------GO -----------------------------
                            else if (dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).toUpperCase().equals(CommonData.GO_DB_REFERENCE_TYPE)) {

                                if (uniprotDataXML.getGeneOntology()) {
                                    String goId = dbReferenceElem.getAttributeValue(CommonData.DB_REFERENCE_ID_ATTRIBUTE);
                                    String evidenceSt = "";
                                    List<Element> props = dbReferenceElem.getChildren(CommonData.DB_REFERENCE_PROPERTY_TAG_NAME);
                                    for (Element element : props) {
                                        if (element.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE).equals(CommonData.EVIDENCE_TYPE_ATTRIBUTE)) {
                                            evidenceSt = element.getAttributeValue("value");
                                            if (evidenceSt == null) {
                                                evidenceSt = "";
                                            }
                                            break;
                                        }
                                    }
                                    GoTermNode goTermNode = nodeRetriever.getGoTermById(goId);
                                    ProteinGoRel proteinGoRel = new ProteinGoRel(bGraph.addEdge(null, currentProteinNode.getNode(), goTermNode.getNode(), ProteinGoRel.NAME));
                                    proteinGoRel.setEvidence(evidenceSt);
                                }

                            }

                        }
                        //---------------------------------------------------------------------------------------

                        //---------------------------------------------------------------------------------------
                        //--------------------------------organism-----------------------------------------------

                        String scName, commName, synName;
                        scName = "";
                        commName = "";
                        synName = "";

                        Element organismElem = entryXMLElem.asJDomElement().getChild(CommonData.ORGANISM_TAG_NAME);

                        List<Element> organismNames = organismElem.getChildren(CommonData.ORGANISM_NAME_TAG_NAME);
                        for (Element element : organismNames) {
                            String type = element.getAttributeValue(CommonData.ORGANISM_NAME_TYPE_ATTRIBUTE);
                            if (type.equals(CommonData.ORGANISM_SCIENTIFIC_NAME_TYPE)) {
                                scName = element.getText();
                            } else if (type.equals(CommonData.ORGANISM_COMMON_NAME_TYPE)) {
                                commName = element.getText();
                            } else if (type.equals(CommonData.ORGANISM_SYNONYM_NAME_TYPE)) {
                                synName = element.getText();
                            }
                        }

                        OrganismNode organismNode = nodeRetriever.getOrganismByScientificName(scName);

                        if (organismNode == null) {

                            organismNode = new OrganismNode(manager.createNode(OrganismNode.NODE_TYPE));

                            organismNode.setCommonName(commName);
                            organismNode.setScientificName(scName);
                            organismNode.setSynonymName(synName);

                            List<Element> organismDbRefElems = organismElem.getChildren(CommonData.DB_REFERENCE_TAG_NAME);
                            if (organismDbRefElems != null) {
                                for (Element dbRefElem : organismDbRefElems) {
                                    String t = dbRefElem.getAttributeValue("type");
                                    if (t.equals("NCBI Taxonomy")) {
                                        organismNode.setNcbiTaxonomyId(dbRefElem.getAttributeValue("id"));
                                        break;
                                    }
                                }
                            }

                            Element lineage = entryXMLElem.asJDomElement().getChild("organism").getChild("lineage");
                            List<Element> taxons = lineage.getChildren("taxon");

                            Element firstTaxonElem = taxons.get(0);

                            TaxonNode firstTaxon = nodeRetriever.getTaxonByName(firstTaxonElem.getText());

                            if (firstTaxon == null) {

                                String firstTaxonName = firstTaxonElem.getText();
                                firstTaxon = new TaxonNode(manager.createNode(TaxonNode.NODE_TYPE));
                                firstTaxon.setName(firstTaxonName);

                            }

                            TaxonNode lastTaxon = firstTaxon;

                            for (int i = 1; i < taxons.size(); i++) {

                                String taxonName = taxons.get(i).getText();
                                TaxonNode currentTaxon = nodeRetriever.getTaxonByName(taxonName);

                                if (currentTaxon == null) {

                                    currentTaxon = new TaxonNode(manager.createNode(TaxonNode.NODE_TYPE));
                                    currentTaxon.setName(taxonName);

                                    bGraph.addEdge(null, lastTaxon.getNode(), currentTaxon.getNode(), TaxonParentRel.NAME);

                                }
                                lastTaxon = currentTaxon;
                            }

                            bGraph.addEdge(null, lastTaxon.getNode(), organismNode.getNode(), TaxonParentRel.NAME);

                        }


                        //---------------------------------------------------------------------------------------
                        //---------------------------------------------------------------------------------------

                        bGraph.addEdge(null, currentProteinNode.getNode(), organismNode.getNode(), ProteinOrganismRel.NAME);

                        proteinCounter++;
                        if ((proteinCounter % limitForPrintingOut) == 0) {
                            String countProteinsSt = proteinCounter + " proteins inserted!!";
                            logger.log(Level.INFO, countProteinsSt);
                        }

                    }
                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, ("Exception retrieving protein " + currentAccessionId));
                logger.log(Level.SEVERE, e.getMessage());
                StackTraceElement[] trace = e.getStackTrace();
                for (StackTraceElement stackTraceElement : trace) {
                    logger.log(Level.SEVERE, stackTraceElement.toString());
                }
            } finally {

                try {
                    //------closing writers-------
                    enzymeIdsNotFoundBuff.close();

                    // shutdown, makes sure all changes are written to disk
                    manager.shutDown();

                    // closing logger file handler
                    fh.close();

                    //-----------------writing stats file---------------------
                    long elapsedTime = System.nanoTime() - initTime;
                    long elapsedSeconds = Math.round((elapsedTime / 1000000000.0));
                    long hours = elapsedSeconds / 3600;
                    long minutes = (elapsedSeconds % 3600) / 60;
                    long seconds = (elapsedSeconds % 3600) % 60;

                    statsBuff.write("Statistics for program ImportUniprot:\nInput file: " + inFile.getName()
                            + "\nThere were " + proteinCounter + " proteins inserted.\n"
                            + "The elapsed time was: " + hours + "h " + minutes + "m " + seconds + "s\n");

                    //---closing stats writer---
                    statsBuff.close();


                } catch (IOException ex) {
                    Logger.getLogger(ImportUniprotTitan.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

    }

    private static void importProteinFeatures(XMLElement entryXMLElem,
            BatchInserter inserter,
            BatchInserterIndexProvider indexProvider,
            long currentProteinId) {

        //-----------------create batch indexes----------------------------------
        //----------------------------------------------------------------------
        BatchInserterIndex featureTypeNameIndex = indexProvider.nodeIndex(FeatureTypeNode.FEATURE_TYPE_NAME_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex nodeTypeIndex = indexProvider.nodeIndex(Bio4jManager.NODE_TYPE_INDEX_NAME,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        //------------------------------------------------------------------------


        //--------------------------------features----------------------------------------------------
        List<Element> featuresList = entryXMLElem.asJDomElement().getChildren(CommonData.FEATURE_TAG_NAME);

        for (Element featureElem : featuresList) {

            String featureTypeSt = featureElem.getAttributeValue(CommonData.FEATURE_TYPE_ATTRIBUTE);
            //long featureTypeNodeId = indexService.getSingleNode(FeatureTypeNode.FEATURE_TYPE_NAME_INDEX, featureTypeSt);
            long featureTypeNodeId = -1;
            IndexHits<Long> featureTypeNameIndexHits = featureTypeNameIndex.get(FeatureTypeNode.FEATURE_TYPE_NAME_INDEX, featureTypeSt);
            if (featureTypeNameIndexHits.hasNext()) {
                featureTypeNodeId = featureTypeNameIndexHits.getSingle();
            }

            if (featureTypeNodeId < 0) {

                featureTypeProperties.put(FeatureTypeNode.NAME_PROPERTY, featureTypeSt);
                featureTypeNodeId = inserter.createNode(featureTypeProperties);
                //indexService.index(featureTypeNodeId, FeatureTypeNode.FEATURE_TYPE_NAME_INDEX, featureTypeSt);
                featureTypeNameIndex.add(featureTypeNodeId, MapUtil.map(FeatureTypeNode.FEATURE_TYPE_NAME_INDEX, featureTypeSt));
                //---flushing feature type name index----
                featureTypeNameIndex.flush();

                //---adding feature type node to node_type index----
                nodeTypeIndex.add(featureTypeNodeId, MapUtil.map(Bio4jManager.NODE_TYPE_INDEX_NAME, FeatureTypeNode.NODE_TYPE));

            }

            String featureDescSt = featureElem.getAttributeValue(CommonData.FEATURE_DESCRIPTION_ATTRIBUTE);
            if (featureDescSt == null) {
                featureDescSt = "";
            }
            String featureIdSt = featureElem.getAttributeValue(CommonData.FEATURE_ID_ATTRIBUTE);
            if (featureIdSt == null) {
                featureIdSt = "";
            }
            String featureStatusSt = featureElem.getAttributeValue(CommonData.STATUS_ATTRIBUTE);
            if (featureStatusSt == null) {
                featureStatusSt = "";
            }
            String featureEvidenceSt = featureElem.getAttributeValue(CommonData.EVIDENCE_ATTRIBUTE);
            if (featureEvidenceSt == null) {
                featureEvidenceSt = "";
            }

            Element locationElem = featureElem.getChild(CommonData.FEATURE_LOCATION_TAG_NAME);
            Element positionElem = locationElem.getChild(CommonData.FEATURE_POSITION_TAG_NAME);
            String beginFeatureSt;
            String endFeatureSt;
            if (positionElem != null) {
                beginFeatureSt = positionElem.getAttributeValue(CommonData.FEATURE_POSITION_POSITION_ATTRIBUTE);
                endFeatureSt = beginFeatureSt;
            } else {
                beginFeatureSt = locationElem.getChild(CommonData.FEATURE_LOCATION_BEGIN_TAG_NAME).getAttributeValue(CommonData.FEATURE_LOCATION_POSITION_ATTRIBUTE);
                endFeatureSt = locationElem.getChild(CommonData.FEATURE_LOCATION_END_TAG_NAME).getAttributeValue(CommonData.FEATURE_LOCATION_POSITION_ATTRIBUTE);
            }

            if (beginFeatureSt == null) {
                beginFeatureSt = "";
            }
            if (endFeatureSt == null) {
                endFeatureSt = "";
            }

            String originalSt = featureElem.getChildText(CommonData.FEATURE_ORIGINAL_TAG_NAME);
            String variationSt = featureElem.getChildText(CommonData.FEATURE_VARIATION_TAG_NAME);
            if (originalSt == null) {
                originalSt = "";
            }
            if (variationSt == null) {
                variationSt = "";
            }
            String featureRefSt = featureElem.getAttributeValue(CommonData.FEATURE_REF_ATTRIBUTE);
            if (featureRefSt == null) {
                featureRefSt = "";
            }

            featureProperties.put(BasicFeatureRel.DESCRIPTION_PROPERTY, featureDescSt);
            featureProperties.put(BasicFeatureRel.ID_PROPERTY, featureIdSt);
            featureProperties.put(BasicFeatureRel.EVIDENCE_PROPERTY, featureEvidenceSt);
            featureProperties.put(BasicFeatureRel.STATUS_PROPERTY, featureStatusSt);
            featureProperties.put(BasicFeatureRel.BEGIN_PROPERTY, beginFeatureSt);
            featureProperties.put(BasicFeatureRel.END_PROPERTY, endFeatureSt);
            featureProperties.put(BasicFeatureRel.ORIGINAL_PROPERTY, originalSt);
            featureProperties.put(BasicFeatureRel.VARIATION_PROPERTY, variationSt);
            featureProperties.put(BasicFeatureRel.REF_PROPERTY, featureRefSt);


            if (featureTypeSt.equals(ActiveSiteFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, activeSiteFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(BindingSiteFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, bindingSiteFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(CrossLinkFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, crossLinkFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(GlycosylationSiteFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, glycosylationSiteFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(InitiatorMethionineFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, initiatorMethionineFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(LipidMoietyBindingRegionFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, lipidMoietyBindingRegionFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(MetalIonBindingSiteFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, metalIonBindingSiteFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(ModifiedResidueFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, modifiedResidueFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(NonStandardAminoAcidFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, nonStandardAminoAcidFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(NonTerminalResidueFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, nonTerminalResidueFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(PeptideFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, peptideFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(UnsureResidueFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, unsureResidueFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(MutagenesisSiteFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, mutagenesisSiteFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(SequenceVariantFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, sequenceVariantFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(CalciumBindingRegionFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, calciumBindingRegionFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(ChainFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, chainFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(CoiledCoilRegionFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, coiledCoilRegionFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(CompositionallyBiasedRegionFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, compositionallyBiasedRegionFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(DisulfideBondFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, disulfideBondFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(DnaBindingRegionFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, dnaBindingRegionFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(DomainFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, domainFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(HelixFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, helixFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(IntramembraneRegionFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, intramembraneRegionFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(NonConsecutiveResiduesFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, nonConsecutiveResiduesFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(NucleotidePhosphateBindingRegionFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, nucleotidePhosphateBindingRegionFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(PropeptideFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, propeptideFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(RegionOfInterestFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, regionOfInterestFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(RepeatFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, repeatFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(ShortSequenceMotifFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, shortSequenceMotifFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(SignalPeptideFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, signalPeptideFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(SpliceVariantFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, spliceVariantFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(StrandFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, strandFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(TopologicalDomainFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, topologicalDomainFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(TransitPeptideFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, transitPeptideFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(TransmembraneRegionFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, transmembraneRegionFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(ZincFingerRegionFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, zincFingerRegionFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(SiteFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, siteFeatureRel, featureProperties);
            } else if (featureTypeSt.equals(TurnFeatureRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, featureTypeNodeId, turnFeatureRel, featureProperties);
            }

            inserter.createRelationship(currentProteinId, featureTypeNodeId, sequenceConflictFeatureRel, featureProperties);

        }

    }

    private static void importProteinComments(XMLElement entryXMLElem,
            BatchInserter inserter,
            BatchInserterIndexProvider indexProvider,
            long currentProteinId,
            String proteinSequence,
            UniprotDataXML uniprotDataXML) {

        //---------------indexes declaration---------------------------
        BatchInserterIndex commentTypeNameIndex = indexProvider.nodeIndex(CommentTypeNode.COMMENT_TYPE_NAME_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex subcellularLocationNameIndex = indexProvider.nodeIndex(SubcellularLocationNode.SUBCELLULAR_LOCATION_NAME_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex isoformIdIndex = indexProvider.nodeIndex(IsoformNode.ISOFORM_ID_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex nodeTypeIndex = indexProvider.nodeIndex(Bio4jManager.NODE_TYPE_INDEX_NAME,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        //-----------------------------------------------------------

        List<Element> comments = entryXMLElem.asJDomElement().getChildren(CommonData.COMMENT_TAG_NAME);

        for (Element commentElem : comments) {

            String commentTypeSt = commentElem.getAttributeValue(CommonData.COMMENT_TYPE_ATTRIBUTE);

            Element textElem = commentElem.getChild("text");
            String commentTextSt = "";
            String commentStatusSt = "";
            String commentEvidenceSt = "";
            if (textElem != null) {
                commentTextSt = textElem.getText();
                commentStatusSt = textElem.getAttributeValue("status");
                if (commentStatusSt == null) {
                    commentStatusSt = "";
                }
                commentEvidenceSt = textElem.getAttributeValue("evidence");
                if (commentEvidenceSt == null) {
                    commentEvidenceSt = "";
                }
            }

            commentProperties.put(BasicCommentRel.TEXT_PROPERTY, commentTextSt);
            commentProperties.put(BasicCommentRel.STATUS_PROPERTY, commentStatusSt);
            commentProperties.put(BasicCommentRel.EVIDENCE_PROPERTY, commentEvidenceSt);

            //-----------------COMMENT TYPE NODE RETRIEVING/CREATION---------------------- 
            //long commentTypeId = indexService.getSingleNode(CommentTypeNode.COMMENT_TYPE_NAME_INDEX, commentTypeSt);
            IndexHits<Long> commentTypeNameIndexHits = commentTypeNameIndex.get(CommentTypeNode.COMMENT_TYPE_NAME_INDEX, commentTypeSt);
            long commentTypeId = -1;
            if (commentTypeNameIndexHits.hasNext()) {
                commentTypeId = commentTypeNameIndexHits.getSingle();
            }
            if (commentTypeId < 0) {
                commentTypeProperties.put(CommentTypeNode.NAME_PROPERTY, commentTypeSt);
                commentTypeId = inserter.createNode(commentTypeProperties);
                commentTypeNameIndex.add(commentTypeId, MapUtil.map(CommentTypeNode.COMMENT_TYPE_NAME_INDEX, commentTypeSt));

                //----flushing the indexation----
                commentTypeNameIndex.flush();

                //---adding comment type node to node_type index----
                nodeTypeIndex.add(commentTypeId, MapUtil.map(Bio4jManager.NODE_TYPE_INDEX_NAME, CommentTypeNode.NODE_TYPE));
            }

            //-----toxic dose----------------
            if (commentTypeSt.equals(ToxicDoseCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, toxicDoseCommentRel, commentProperties);
            } //-----caution---------
            else if (commentTypeSt.equals(CautionCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, cautionCommentRel, commentProperties);
            } //-----cofactor---------
            else if (commentTypeSt.equals(CofactorCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, cofactorCommentRel, commentProperties);
            } //-----disease---------
            else if (commentTypeSt.equals(DiseaseCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, diseaseCommentRel, commentProperties);
            } //-----online information---------
            else if (commentTypeSt.equals(OnlineInformationCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                onlineInformationCommentProperties.put(OnlineInformationCommentRel.STATUS_PROPERTY, commentStatusSt);
                onlineInformationCommentProperties.put(OnlineInformationCommentRel.EVIDENCE_PROPERTY, commentEvidenceSt);
                onlineInformationCommentProperties.put(OnlineInformationCommentRel.TEXT_PROPERTY, commentTextSt);
                String nameSt = commentElem.getAttributeValue("name");
                if (nameSt == null) {
                    nameSt = "";
                }
                String linkSt = "";
                Element linkElem = commentElem.getChild("link");
                if (linkElem != null) {
                    String uriSt = linkElem.getAttributeValue("uri");
                    if (uriSt != null) {
                        linkSt = uriSt;
                    }
                }
                onlineInformationCommentProperties.put(OnlineInformationCommentRel.NAME_PROPERTY, nameSt);
                onlineInformationCommentProperties.put(OnlineInformationCommentRel.LINK_PROPERTY, linkSt);
                inserter.createRelationship(currentProteinId, commentTypeId, onlineInformationCommentRel, onlineInformationCommentProperties);
            } //-----tissue specificity---------
            else if (commentTypeSt.equals(TissueSpecificityCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, tissueSpecificityCommentRel, commentProperties);
            } //----------function----------------
            else if (commentTypeSt.equals(FunctionCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, functionCommentRel, commentProperties);
            } //----------biotechnology----------------
            else if (commentTypeSt.equals(BiotechnologyCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, biotechnologyCommentRel, commentProperties);
            } //----------subunit----------------
            else if (commentTypeSt.equals(SubunitCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, subunitCommentRel, commentProperties);
            } //----------polymorphism----------------
            else if (commentTypeSt.equals(PolymorphismCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, polymorphismCommentRel, commentProperties);
            } //----------domain----------------
            else if (commentTypeSt.equals(DomainCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, domainCommentRel, commentProperties);
            } //----------post transactional modification----------------
            else if (commentTypeSt.equals(PostTranslationalModificationCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, postTranslationalModificationCommentRel, commentProperties);
            } //----------catalytic activity----------------
            else if (commentTypeSt.equals(CatalyticActivityCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, catalyticActivityCommentRel, commentProperties);
            } //----------disruption phenotype----------------
            else if (commentTypeSt.equals(DisruptionPhenotypeCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, disruptionPhenotypeCommentRel, commentProperties);
            } //----------biophysicochemical properties----------------
            else if (commentTypeSt.equals(BioPhysicoChemicalPropertiesCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                biophysicochemicalCommentProperties.put(BioPhysicoChemicalPropertiesCommentRel.STATUS_PROPERTY, commentStatusSt);
                biophysicochemicalCommentProperties.put(BioPhysicoChemicalPropertiesCommentRel.EVIDENCE_PROPERTY, commentEvidenceSt);
                biophysicochemicalCommentProperties.put(BioPhysicoChemicalPropertiesCommentRel.TEXT_PROPERTY, commentTextSt);
                String phDependenceSt = commentElem.getChildText("phDependence");
                String temperatureDependenceSt = commentElem.getChildText("temperatureDependence");
                if (phDependenceSt == null) {
                    phDependenceSt = "";
                }
                if (temperatureDependenceSt == null) {
                    temperatureDependenceSt = "";
                }
                String absorptionMaxSt = "";
                String absorptionTextSt = "";
                Element absorptionElem = commentElem.getChild("absorption");
                if (absorptionElem != null) {
                    absorptionMaxSt = absorptionElem.getChildText("max");
                    absorptionTextSt = absorptionElem.getChildText("text");
                    if (absorptionMaxSt == null) {
                        absorptionMaxSt = "";
                    }
                    if (absorptionTextSt == null) {
                        absorptionTextSt = "";
                    }
                }
                String kineticsSt = "";
                Element kineticsElem = commentElem.getChild("kinetics");
                if (kineticsElem != null) {
                    kineticsSt = new XMLElement(kineticsElem).toString();
                }
                String redoxPotentialSt = "";
                String redoxPotentialEvidenceSt = "";
                Element redoxPotentialElem = commentElem.getChild("redoxPotential");
                if (redoxPotentialElem != null) {
                    redoxPotentialSt = redoxPotentialElem.getText();
                    redoxPotentialEvidenceSt = redoxPotentialElem.getAttributeValue("evidence");
                    if (redoxPotentialSt == null) {
                        redoxPotentialSt = "";
                    }
                    if (redoxPotentialEvidenceSt == null) {
                        redoxPotentialEvidenceSt = "";
                    }
                }

                biophysicochemicalCommentProperties.put(BioPhysicoChemicalPropertiesCommentRel.TEMPERATURE_DEPENDENCE_PROPERTY, temperatureDependenceSt);
                biophysicochemicalCommentProperties.put(BioPhysicoChemicalPropertiesCommentRel.PH_DEPENDENCE_PROPERTY, phDependenceSt);
                biophysicochemicalCommentProperties.put(BioPhysicoChemicalPropertiesCommentRel.KINETICS_XML_PROPERTY, kineticsSt);
                biophysicochemicalCommentProperties.put(BioPhysicoChemicalPropertiesCommentRel.ABSORPTION_MAX_PROPERTY, absorptionMaxSt);
                biophysicochemicalCommentProperties.put(BioPhysicoChemicalPropertiesCommentRel.ABSORPTION_TEXT_PROPERTY, absorptionTextSt);
                biophysicochemicalCommentProperties.put(BioPhysicoChemicalPropertiesCommentRel.REDOX_POTENTIAL_EVIDENCE_PROPERTY, redoxPotentialEvidenceSt);
                biophysicochemicalCommentProperties.put(BioPhysicoChemicalPropertiesCommentRel.REDOX_POTENTIAL_PROPERTY, redoxPotentialSt);

                inserter.createRelationship(currentProteinId, commentTypeId, bioPhysicoChemicalPropertiesCommentRel, biophysicochemicalCommentProperties);

            } //----------allergen----------------
            else if (commentTypeSt.equals(AllergenCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, allergenCommentRel, commentProperties);
            } //----------pathway----------------
            else if (commentTypeSt.equals(PathwayCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, pathwayCommentRel, commentProperties);
            } //----------induction----------------
            else if (commentTypeSt.equals(InductionCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, inductionCommentRel, commentProperties);
            } //----- subcellular location---------
            else if (commentTypeSt.equals(ProteinSubcellularLocationRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                if (uniprotDataXML.getSubcellularLocations()) {
                    List<Element> subcLocations = commentElem.getChildren(CommonData.SUBCELLULAR_LOCATION_TAG_NAME);

                    for (Element subcLocation : subcLocations) {

                        List<Element> locations = subcLocation.getChildren(CommonData.LOCATION_TAG_NAME);
                        Element firstLocation = locations.get(0);
                        //long firstLocationId = indexService.getSingleNode(SubcellularLocationNode.SUBCELLULAR_LOCATION_NAME_INDEX, firstLocation.getTextTrim());
                        long firstLocationId = -1;
                        IndexHits<Long> firstLocationIndexHits = subcellularLocationNameIndex.get(SubcellularLocationNode.SUBCELLULAR_LOCATION_NAME_INDEX, firstLocation.getTextTrim());
                        if (firstLocationIndexHits.hasNext()) {
                            firstLocationId = firstLocationIndexHits.getSingle();
                        }
                        long lastLocationId = firstLocationId;

                        if (firstLocationId < 0) {
                            subcellularLocationProperties.put(SubcellularLocationNode.NAME_PROPERTY, firstLocation.getTextTrim());
                            lastLocationId = createSubcellularLocationNode(subcellularLocationProperties, inserter, subcellularLocationNameIndex, nodeTypeIndex);
                            //---flushing subcellular location name index---
                            subcellularLocationNameIndex.flush();
                        }

                        for (int i = 1; i < locations.size(); i++) {

                            long tempLocationId;
                            IndexHits<Long> tempLocationIndexHits = subcellularLocationNameIndex.get(SubcellularLocationNode.SUBCELLULAR_LOCATION_NAME_INDEX, locations.get(i).getTextTrim());
                            if (tempLocationIndexHits.hasNext()) {
                                tempLocationId = tempLocationIndexHits.getSingle();
                            } else {
                                subcellularLocationProperties.put(SubcellularLocationNode.NAME_PROPERTY, locations.get(i).getTextTrim());
                                tempLocationId = createSubcellularLocationNode(subcellularLocationProperties, inserter, subcellularLocationNameIndex, nodeTypeIndex);
                                subcellularLocationNameIndex.flush();
                            }

                            inserter.createRelationship(tempLocationId, lastLocationId, subcellularLocationParentRel, null);
                            lastLocationId = tempLocationId;
                        }
                        Element lastLocation = locations.get(locations.size() - 1);
                        String evidenceSt = lastLocation.getAttributeValue(CommonData.EVIDENCE_ATTRIBUTE);
                        String statusSt = lastLocation.getAttributeValue(CommonData.STATUS_ATTRIBUTE);
                        String topologyStatusSt = "";
                        String topologySt = "";
                        Element topologyElem = subcLocation.getChild("topology");
                        if (topologyElem != null) {
                            topologySt = topologyElem.getText();
                            topologyStatusSt = topologyElem.getAttributeValue("status");
                        }
                        if (topologyStatusSt == null) {
                            topologyStatusSt = "";
                        }
                        if (topologySt == null) {
                            topologySt = "";
                        }
                        if (evidenceSt == null) {
                            evidenceSt = "";
                        }
                        if (statusSt == null) {
                            statusSt = "";
                        }
                        proteinSubcellularLocationProperties.put(ProteinSubcellularLocationRel.EVIDENCE_PROPERTY, evidenceSt);
                        proteinSubcellularLocationProperties.put(ProteinSubcellularLocationRel.STATUS_PROPERTY, statusSt);
                        proteinSubcellularLocationProperties.put(ProteinSubcellularLocationRel.TOPOLOGY_PROPERTY, topologySt);
                        proteinSubcellularLocationProperties.put(ProteinSubcellularLocationRel.TOPOLOGY_STATUS_PROPERTY, topologyStatusSt);
                        inserter.createRelationship(currentProteinId, lastLocationId, proteinSubcellularLocationRel, proteinSubcellularLocationProperties);

                    }
                }

            } //----- alternative products---------
            else if (commentTypeSt.equals(CommonData.COMMENT_ALTERNATIVE_PRODUCTS_TYPE)) {

                if (uniprotDataXML.getIsoforms()) {
                    List<Element> eventList = commentElem.getChildren("event");
                    List<Element> isoformList = commentElem.getChildren("isoform");

                    for (Element isoformElem : isoformList) {
                        String isoformIdSt = isoformElem.getChildText("id");
                        String isoformNoteSt = isoformElem.getChildText("note");
                        String isoformNameSt = isoformElem.getChildText("name");
                        String isoformSeqSt = "";
                        Element isoSeqElem = isoformElem.getChild("sequence");
                        if (isoSeqElem != null) {
                            String isoSeqTypeSt = isoSeqElem.getAttributeValue("type");
                            if (isoSeqTypeSt.equals("displayed")) {
                                isoformSeqSt = proteinSequence;
                            }
                        }
                        if (isoformNoteSt == null) {
                            isoformNoteSt = "";
                        }
                        if (isoformNameSt == null) {
                            isoformNameSt = "";
                        }
                        isoformProperties.put(IsoformNode.ID_PROPERTY, isoformIdSt);
                        isoformProperties.put(IsoformNode.NOTE_PROPERTY, isoformNoteSt);
                        isoformProperties.put(IsoformNode.NAME_PROPERTY, isoformNameSt);
                        isoformProperties.put(IsoformNode.SEQUENCE_PROPERTY, isoformSeqSt);
                        //--------------------------------------------------------
                        //long isoformId = indexService.getSingleNode(IsoformNode.ISOFORM_ID_INDEX, isoformIdSt);
                        long isoformId = -1;
                        IndexHits<Long> isoformIdIndexHits = isoformIdIndex.get(IsoformNode.ISOFORM_ID_INDEX, isoformIdSt);
                        if (isoformIdIndexHits.hasNext()) {
                            isoformId = isoformIdIndexHits.getSingle();
                        }
                        if (isoformId < 0) {
                            isoformId = createIsoformNode(isoformProperties, inserter, isoformIdIndex, nodeTypeIndex);
                        }

                        for (Element eventElem : eventList) {

                            String eventTypeSt = eventElem.getAttributeValue("type");
                            if (eventTypeSt.equals(AlternativeProductInitiationRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                                inserter.createRelationship(isoformId, alternativeProductInitiationId, isoformEventGeneratorRel, null);
                            } else if (eventTypeSt.equals(AlternativeProductPromoterRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                                inserter.createRelationship(isoformId, alternativeProductPromoterId, isoformEventGeneratorRel, null);
                            } else if (eventTypeSt.equals(AlternativeProductRibosomalFrameshiftingRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                                inserter.createRelationship(isoformId, alternativeProductRibosomalFrameshiftingId, isoformEventGeneratorRel, null);
                            } else if (eventTypeSt.equals(AlternativeProductSplicingRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                                inserter.createRelationship(isoformId, alternativeProductSplicingId, isoformEventGeneratorRel, null);
                            }
                        }

                        //protein isoform relationship
                        inserter.createRelationship(currentProteinId, isoformId, proteinIsoformRel, null);

                    }
                }

            } //----- sequence caution---------
            else if (commentTypeSt.equals(CommonData.COMMENT_SEQUENCE_CAUTION_TYPE)) {

                sequenceCautionProperties.put(BasicProteinSequenceCautionRel.EVIDENCE_PROPERTY, commentEvidenceSt);
                sequenceCautionProperties.put(BasicProteinSequenceCautionRel.STATUS_PROPERTY, commentStatusSt);
                sequenceCautionProperties.put(BasicProteinSequenceCautionRel.TEXT_PROPERTY, commentTextSt);

                Element conflictElem = commentElem.getChild("conflict");
                if (conflictElem != null) {

                    String conflictTypeSt = conflictElem.getAttributeValue("type");
                    String resourceSt = "";
                    String idSt = "";
                    String versionSt = "";

                    ArrayList<String> positionsList = new ArrayList<String>();

                    Element sequenceElem = conflictElem.getChild("sequence");
                    if (sequenceElem != null) {
                        resourceSt = sequenceElem.getAttributeValue("resource");
                        if (resourceSt == null) {
                            resourceSt = "";
                        }
                        idSt = sequenceElem.getAttributeValue("id");
                        if (idSt == null) {
                            idSt = "";
                        }
                        versionSt = sequenceElem.getAttributeValue("version");
                        if (versionSt == null) {
                            versionSt = "";
                        }
                    }

                    Element locationElem = commentElem.getChild("location");
                    if (locationElem != null) {
                        Element positionElem = locationElem.getChild("position");
                        if (positionElem != null) {
                            String tempPos = positionElem.getAttributeValue("position");
                            if (tempPos != null) {
                                positionsList.add(tempPos);
                            }
                        }
                    }

                    sequenceCautionProperties.put(BasicProteinSequenceCautionRel.RESOURCE_PROPERTY, resourceSt);
                    sequenceCautionProperties.put(BasicProteinSequenceCautionRel.ID_PROPERTY, idSt);
                    sequenceCautionProperties.put(BasicProteinSequenceCautionRel.VERSION_PROPERTY, versionSt);

                    if (conflictTypeSt.equals(ProteinErroneousGeneModelPredictionRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                        if (positionsList.size() > 0) {
                            for (String tempPosition : positionsList) {
                                sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, tempPosition);
                                inserter.createRelationship(currentProteinId, seqCautionErroneousGeneModelPredictionId, proteinErroneousGeneModelPredictionRel, sequenceCautionProperties);
                            }
                        } else {
                            sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, "");
                            inserter.createRelationship(currentProteinId, seqCautionErroneousGeneModelPredictionId, proteinErroneousGeneModelPredictionRel, sequenceCautionProperties);
                        }

                    } else if (conflictTypeSt.equals(ProteinErroneousInitiationRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                        if (positionsList.size() > 0) {
                            for (String tempPosition : positionsList) {
                                sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, tempPosition);
                                inserter.createRelationship(currentProteinId, seqCautionErroneousInitiationId, proteinErroneousInitiationRel, sequenceCautionProperties);
                            }
                        } else {
                            sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, "");
                            inserter.createRelationship(currentProteinId, seqCautionErroneousInitiationId, proteinErroneousInitiationRel, sequenceCautionProperties);
                        }

                    } else if (conflictTypeSt.equals(ProteinErroneousTranslationRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                        if (positionsList.size() > 0) {
                            for (String tempPosition : positionsList) {
                                sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, tempPosition);
                                inserter.createRelationship(currentProteinId, seqCautionErroneousTranslationId, proteinErroneousTranslationRel, sequenceCautionProperties);
                            }
                        } else {
                            sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, "");
                            inserter.createRelationship(currentProteinId, seqCautionErroneousTranslationId, proteinErroneousTranslationRel, sequenceCautionProperties);
                        }

                    } else if (conflictTypeSt.equals(ProteinErroneousTerminationRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                        if (positionsList.size() > 0) {
                            for (String tempPosition : positionsList) {
                                sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, tempPosition);
                                inserter.createRelationship(currentProteinId, seqCautionErroneousTerminationId, proteinErroneousTerminationRel, sequenceCautionProperties);
                            }
                        } else {
                            sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, "");
                            inserter.createRelationship(currentProteinId, seqCautionErroneousTerminationId, proteinErroneousTerminationRel, sequenceCautionProperties);
                        }

                    } else if (conflictTypeSt.equals(ProteinFrameshiftRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                        if (positionsList.size() > 0) {
                            for (String tempPosition : positionsList) {
                                sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, tempPosition);
                                inserter.createRelationship(currentProteinId, seqCautionFrameshiftId, proteinFrameshiftRel, sequenceCautionProperties);
                            }
                        } else {
                            sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, "");
                            inserter.createRelationship(currentProteinId, seqCautionFrameshiftId, proteinFrameshiftRel, sequenceCautionProperties);
                        }

                    } else if (conflictTypeSt.equals(ProteinMiscellaneousDiscrepancyRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                        if (positionsList.size() > 0) {
                            for (String tempPosition : positionsList) {
                                sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, tempPosition);
                                inserter.createRelationship(currentProteinId, seqCautionMiscellaneousDiscrepancyId, proteinMiscellaneousDiscrepancyRel, sequenceCautionProperties);
                            }
                        } else {
                            sequenceCautionProperties.put(BasicProteinSequenceCautionRel.POSITION_PROPERTY, "");
                            inserter.createRelationship(currentProteinId, seqCautionMiscellaneousDiscrepancyId, proteinMiscellaneousDiscrepancyRel, sequenceCautionProperties);
                        }

                    }
                }


            } //----------developmental stage----------------
            else if (commentTypeSt.equals(DevelopmentalStageCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, developmentalStageCommentRel, commentProperties);
            } //----------miscellaneous----------------
            else if (commentTypeSt.equals(MiscellaneousCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, miscellaneousCommentRel, commentProperties);
            } //----------similarity----------------
            else if (commentTypeSt.equals(SimilarityCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, similarityCommentRel, commentProperties);
            } //----------RNA editing----------------
            else if (commentTypeSt.equals(RnaEditingCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                rnaEditingCommentProperties.put(RnaEditingCommentRel.STATUS_PROPERTY, commentStatusSt);
                rnaEditingCommentProperties.put(RnaEditingCommentRel.EVIDENCE_PROPERTY, commentEvidenceSt);
                rnaEditingCommentProperties.put(RnaEditingCommentRel.TEXT_PROPERTY, commentTextSt);

                List<Element> locationsList = commentElem.getChildren("location");
                for (Element tempLoc : locationsList) {
                    String positionSt = tempLoc.getChild("position").getAttributeValue("position");
                    rnaEditingCommentProperties.put(RnaEditingCommentRel.POSITION_PROPERTY, positionSt);
                    inserter.createRelationship(currentProteinId, commentTypeId, rnaEditingCommentRel, rnaEditingCommentProperties);
                }

            } //----------pharmaceutical----------------
            else if (commentTypeSt.equals(PharmaceuticalCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, pharmaceuticalCommentRel, commentProperties);
            } //----------enzyme regulation----------------
            else if (commentTypeSt.equals(EnzymeRegulationCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {
                inserter.createRelationship(currentProteinId, commentTypeId, enzymeRegulationCommentRel, commentProperties);
            } //----------mass spectrometry----------------
            else if (commentTypeSt.equals(MassSpectrometryCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                String methodSt = commentElem.getAttributeValue("method");
                String massSt = commentElem.getAttributeValue("mass");
                if (methodSt == null) {
                    methodSt = "";
                }
                if (massSt == null) {
                    massSt = "";
                }
                String beginSt = "";
                String endSt = "";
                Element locationElem = commentElem.getChild("location");
                if (locationElem != null) {
                    Element beginElem = commentElem.getChild("begin");
                    Element endElem = commentElem.getChild("end");
                    if (beginElem != null) {
                        beginSt = beginElem.getAttributeValue("position");
                    }
                    if (endElem != null) {
                        endSt = endElem.getAttributeValue("position");
                    }
                }

                massSpectrometryCommentProperties.put(MassSpectrometryCommentRel.STATUS_PROPERTY, commentStatusSt);
                massSpectrometryCommentProperties.put(MassSpectrometryCommentRel.EVIDENCE_PROPERTY, commentEvidenceSt);
                massSpectrometryCommentProperties.put(MassSpectrometryCommentRel.TEXT_PROPERTY, commentTextSt);
                massSpectrometryCommentProperties.put(MassSpectrometryCommentRel.METHOD_PROPERTY, methodSt);
                massSpectrometryCommentProperties.put(MassSpectrometryCommentRel.MASS_PROPERTY, massSt);
                massSpectrometryCommentProperties.put(MassSpectrometryCommentRel.BEGIN_PROPERTY, beginSt);
                massSpectrometryCommentProperties.put(MassSpectrometryCommentRel.END_PROPERTY, endSt);

                inserter.createRelationship(currentProteinId, commentTypeId, massSpectrometryCommentRel, massSpectrometryCommentProperties);

            }

        }


    }

    private static String getProteinFullName(Element proteinElement) {
        if (proteinElement == null) {
            return "";
        } else {
            Element recElem = proteinElement.getChild(CommonData.PROTEIN_RECOMMENDED_NAME_TAG_NAME);
            if (recElem == null) {
                return "";
            } else {
                return recElem.getChildText(CommonData.PROTEIN_FULL_NAME_TAG_NAME);
            }
        }
    }

    private static String getProteinShortName(Element proteinElement) {
        if (proteinElement == null) {
            return "";
        } else {
            Element recElem = proteinElement.getChild(CommonData.PROTEIN_RECOMMENDED_NAME_TAG_NAME);
            if (recElem == null) {
                return "";
            } else {
                return recElem.getChildText(CommonData.PROTEIN_SHORT_NAME_TAG_NAME);
            }
        }
    }

    private static long createIsoformNode(Map<String, Object> isoformProperties,
            BatchInserter inserter,
            BatchInserterIndex isoformIdIndex,
            BatchInserterIndex nodeTypeIndex) {

        long isoformId = inserter.createNode(isoformProperties);
        isoformIdIndex.add(isoformId, MapUtil.map(IsoformNode.ISOFORM_ID_INDEX, isoformProperties.get(IsoformNode.ID_PROPERTY)));
        //---adding isoform node to node_type index----
        nodeTypeIndex.add(isoformId, MapUtil.map(Bio4jManager.NODE_TYPE_INDEX_NAME, IsoformNode.NODE_TYPE));

        return isoformId;
    }

    private static long createPersonNode(Map<String, Object> personProperties,
            BatchInserter inserter,
            BatchInserterIndex index,
            BatchInserterIndex nodeTypeIndex) {

        long personId = inserter.createNode(personProperties);
        index.add(personId, MapUtil.map(PersonNode.PERSON_NAME_FULL_TEXT_INDEX, personProperties.get(PersonNode.NAME_PROPERTY)));
        //---adding person node to node_type index----
        nodeTypeIndex.add(personId, MapUtil.map(Bio4jManager.NODE_TYPE_INDEX_NAME, PersonNode.NODE_TYPE));

        return personId;
    }

    private static long createConsortiumNode(Map<String, Object> consortiumProperties,
            BatchInserter inserter,
            BatchInserterIndex index,
            BatchInserterIndex nodeTypeIndex) {

        long consortiumId = inserter.createNode(consortiumProperties);
        index.add(consortiumId, MapUtil.map(ConsortiumNode.CONSORTIUM_NAME_INDEX, consortiumProperties.get(ConsortiumNode.NAME_PROPERTY)));
        //---adding consortium node to node_type index----
        nodeTypeIndex.add(consortiumId, MapUtil.map(Bio4jManager.NODE_TYPE_INDEX_NAME, ConsortiumNode.NODE_TYPE));

        return consortiumId;
    }

    private static long createInstituteNode(Map<String, Object> instituteProperties,
            BatchInserter inserter,
            BatchInserterIndex index,
            BatchInserterIndex nodeTypeIndex) {

        long instituteId = inserter.createNode(instituteProperties);
        index.add(instituteId, MapUtil.map(InstituteNode.INSTITUTE_NAME_INDEX, instituteProperties.get(InstituteNode.NAME_PROPERTY)));
        //---adding institute node to node_type index----
        nodeTypeIndex.add(instituteId, MapUtil.map(Bio4jManager.NODE_TYPE_INDEX_NAME, InstituteNode.NODE_TYPE));

        return instituteId;
    }

    private static long createCountryNode(Map<String, Object> countryProperties,
            BatchInserter inserter,
            BatchInserterIndex index,
            BatchInserterIndex nodeTypeIndex) {

        long countryId = inserter.createNode(countryProperties);
        index.add(countryId, MapUtil.map(CountryNode.COUNTRY_NAME_INDEX, countryProperties.get(CountryNode.NAME_PROPERTY)));
        //---adding country node to node_type index----
        nodeTypeIndex.add(countryId, MapUtil.map(Bio4jManager.NODE_TYPE_INDEX_NAME, CountryNode.NODE_TYPE));

        return countryId;
    }

    private static long createCityNode(Map<String, Object> cityProperties,
            BatchInserter inserter,
            BatchInserterIndex index,
            BatchInserterIndex nodeTypeIndex) {

        long cityId = inserter.createNode(cityProperties);
        index.add(cityId, MapUtil.map(CityNode.CITY_NAME_INDEX, cityProperties.get(CityNode.NAME_PROPERTY)));
        //---adding city node to node_type index----
        nodeTypeIndex.add(cityId, MapUtil.map(Bio4jManager.NODE_TYPE_INDEX_NAME, CityNode.NODE_TYPE));

        return cityId;
    }

    private static long createDbNode(Map<String, Object> dbProperties,
            BatchInserter inserter,
            BatchInserterIndex index,
            BatchInserterIndex nodeTypeIndex) {

        long dbId = inserter.createNode(dbProperties);
        index.add(dbId, MapUtil.map(DBNode.DB_NAME_INDEX, dbProperties.get(DBNode.NAME_PROPERTY)));
        //---adding db node to node_type index----
        nodeTypeIndex.add(dbId, MapUtil.map(Bio4jManager.NODE_TYPE_INDEX_NAME, DBNode.NODE_TYPE));

        return dbId;
    }

    private static long createSubcellularLocationNode(Map<String, Object> subcellularLocationProperties,
            BatchInserter inserter,
            BatchInserterIndex index,
            BatchInserterIndex nodeTypeIndex) {

        long subcellularLocationId = inserter.createNode(subcellularLocationProperties);
        index.add(subcellularLocationId, MapUtil.map(SubcellularLocationNode.SUBCELLULAR_LOCATION_NAME_INDEX, subcellularLocationProperties.get(SubcellularLocationNode.NAME_PROPERTY)));
        //---adding subcellular location node to node_type index----
        nodeTypeIndex.add(subcellularLocationId, MapUtil.map(Bio4jManager.NODE_TYPE_INDEX_NAME, SubcellularLocationNode.NODE_TYPE));

        return subcellularLocationId;
    }

    private static void importProteinCitations(XMLElement entryXMLElem,
            BatchInserter inserter,
            BatchInserterIndexProvider indexProvider,
            long currentProteinId,
            UniprotDataXML uniprotDataXML) {

        //-----------------create batch indexes----------------------------------
        //----------------------------------------------------------------------
        BatchInserterIndex personNameIndex = indexProvider.nodeIndex(PersonNode.PERSON_NAME_FULL_TEXT_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, FULL_TEXT_ST));
        BatchInserterIndex consortiumNameIndex = indexProvider.nodeIndex(ConsortiumNode.CONSORTIUM_NAME_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex thesisTitleIndex = indexProvider.nodeIndex(ThesisNode.THESIS_TITLE_FULL_TEXT_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, FULL_TEXT_ST));
        BatchInserterIndex instituteNameIndex = indexProvider.nodeIndex(InstituteNode.INSTITUTE_NAME_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex countryNameIndex = indexProvider.nodeIndex(CountryNode.COUNTRY_NAME_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex cityNameIndex = indexProvider.nodeIndex(CityNode.CITY_NAME_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex patentNumberIndex = indexProvider.nodeIndex(PatentNode.PATENT_NUMBER_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex bookNameIndex = indexProvider.nodeIndex(BookNode.BOOK_NAME_FULL_TEXT_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, FULL_TEXT_ST));
        BatchInserterIndex publisherNameIndex = indexProvider.nodeIndex(PublisherNode.PUBLISHER_NAME_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex onlineArticleTitleIndex = indexProvider.nodeIndex(OnlineArticleNode.ONLINE_ARTICLE_TITLE_FULL_TEXT_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, FULL_TEXT_ST));
        BatchInserterIndex onlineJournalNameIndex = indexProvider.nodeIndex(OnlineJournalNode.ONLINE_JOURNAL_NAME_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex articleTitleIndex = indexProvider.nodeIndex(ArticleNode.ARTICLE_TITLE_FULL_TEXT_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, FULL_TEXT_ST));
        BatchInserterIndex articleDoiIdIndex = indexProvider.nodeIndex(ArticleNode.ARTICLE_DOI_ID_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex articlePubmedIdIndex = indexProvider.nodeIndex(ArticleNode.ARTICLE_PUBMED_ID_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex articleMedlineIdIndex = indexProvider.nodeIndex(ArticleNode.ARTICLE_MEDLINE_ID_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex journalNameIndex = indexProvider.nodeIndex(JournalNode.JOURNAL_NAME_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex nodeTypeIndex = indexProvider.nodeIndex(Bio4jManager.NODE_TYPE_INDEX_NAME,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        BatchInserterIndex dbNameIndex = indexProvider.nodeIndex(DBNode.DB_NAME_INDEX,
                MapUtil.stringMap(PROVIDER_ST, LUCENE_ST, TYPE_ST, EXACT_ST));
        //----------------------------------------------------------------------
        //----------------------------------------------------------------------


        List<Element> referenceList = entryXMLElem.asJDomElement().getChildren(CommonData.REFERENCE_TAG_NAME);

        for (Element reference : referenceList) {
            List<Element> citationsList = reference.getChildren(CommonData.CITATION_TAG_NAME);
            for (Element citation : citationsList) {

                String citationType = citation.getAttributeValue(CommonData.DB_REFERENCE_TYPE_ATTRIBUTE);

                List<Long> authorsPersonNodesIds = new ArrayList<Long>();
                List<Long> authorsConsortiumNodesIds = new ArrayList<Long>();

                List<Element> authorPersonElems = citation.getChild("authorList").getChildren("person");
                List<Element> authorConsortiumElems = citation.getChild("authorList").getChildren("consortium");

                for (Element person : authorPersonElems) {
                    //long personId = indexService.getSingleNode(PersonNode.PERSON_NAME_INDEX, person.getAttributeValue("name"));
                    long personId = -1;
                    IndexHits<Long> personNameIndexHits = personNameIndex.get(PersonNode.PERSON_NAME_FULL_TEXT_INDEX, person.getAttributeValue("name"));
                    if (personNameIndexHits.hasNext()) {
                        personId = personNameIndexHits.getSingle();
                    }
                    if (personId < 0) {
                        personProperties.put(PersonNode.NAME_PROPERTY, person.getAttributeValue("name"));
                        personId = createPersonNode(personProperties, inserter, personNameIndex, nodeTypeIndex);
                        //flushing person name index
                        personNameIndex.flush();
                    }
                    authorsPersonNodesIds.add(personId);
                }

                for (Element consortium : authorConsortiumElems) {
                    //long consortiumId = indexService.getSingleNode(ConsortiumNode.CONSORTIUM_NAME_INDEX, consortium.getAttributeValue("name"));
                    long consortiumId = -1;
                    IndexHits<Long> consortiumIdIndexHits = consortiumNameIndex.get(ConsortiumNode.CONSORTIUM_NAME_INDEX, consortium.getAttributeValue("name"));
                    if (consortiumIdIndexHits.hasNext()) {
                        consortiumId = consortiumIdIndexHits.getSingle();
                    }
                    if (consortiumId < 0) {
                        consortiumProperties.put(ConsortiumNode.NAME_PROPERTY, consortium.getAttributeValue("name"));
                        consortiumId = createConsortiumNode(consortiumProperties, inserter, consortiumNameIndex, nodeTypeIndex);
                        //---flushing consortium name index--
                        consortiumNameIndex.flush();
                    }
                    authorsConsortiumNodesIds.add(consortiumId);
                }

                //----------------------------------------------------------------------------
                //-----------------------------THESIS-----------------------------------------
                if (citationType.equals(ThesisNode.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                    if (uniprotDataXML.getThesis()) {
                        String dateSt = citation.getAttributeValue("date");
                        String titleSt = citation.getChildText("title");
                        if (dateSt == null) {
                            dateSt = "";
                        }
                        if (titleSt == null) {
                            titleSt = "";
                        }

                        long thesisId = -1;
                        IndexHits<Long> thesisTitleIndexHits = thesisTitleIndex.get(ThesisNode.THESIS_TITLE_FULL_TEXT_INDEX, titleSt);
                        if (thesisTitleIndexHits.hasNext()) {
                            thesisId = thesisTitleIndexHits.getSingle();
                        }
                        if (thesisId < 0) {
                            thesisProperties.put(ThesisNode.DATE_PROPERTY, dateSt);
                            thesisProperties.put(ThesisNode.TITLE_PROPERTY, titleSt);
                            //---thesis node creation and indexing
                            thesisId = inserter.createNode(thesisProperties);
                            //indexService.index(thesisId, ThesisNode.THESIS_TITLE_FULL_TEXT_INDEX, titleSt);
                            thesisTitleIndex.add(thesisId, MapUtil.map(ThesisNode.THESIS_TITLE_FULL_TEXT_INDEX, titleSt));
                            //flushing thesis title index
                            thesisTitleIndex.flush();
                            //---authors association-----
                            for (long personId : authorsPersonNodesIds) {
                                inserter.createRelationship(thesisId, personId, thesisAuthorRel, null);
                            }

                            //-----------institute-----------------------------
                            String instituteSt = citation.getAttributeValue("institute");
                            String countrySt = citation.getAttributeValue("country");
                            if (instituteSt != null) {
                                //long instituteId = indexService.getSingleNode(InstituteNode.INSTITUTE_NAME_INDEX, instituteSt);
                                long instituteId = -1;
                                IndexHits<Long> instituteNameIndexHits = instituteNameIndex.get(InstituteNode.INSTITUTE_NAME_INDEX, instituteSt);
                                if (instituteNameIndexHits.hasNext()) {
                                    instituteId = instituteNameIndexHits.getSingle();
                                }
                                if (instituteId < 0) {
                                    instituteProperties.put(InstituteNode.NAME_PROPERTY, instituteSt);
                                    instituteId = createInstituteNode(instituteProperties, inserter, instituteNameIndex, nodeTypeIndex);
                                    //flushing institute name index
                                    instituteNameIndex.flush();
                                }
                                if (countrySt != null) {
                                    //long countryId = indexService.getSingleNode(CountryNode.COUNTRY_NAME_INDEX, countrySt);
                                    long countryId = -1;
                                    IndexHits<Long> countryNameIndexHits = countryNameIndex.get(CountryNode.COUNTRY_NAME_INDEX, countrySt);
                                    if (countryNameIndexHits.hasNext()) {
                                        countryId = countryNameIndexHits.getSingle();
                                    }
                                    if (countryId < 0) {
                                        countryProperties.put(CountryNode.NAME_PROPERTY, countrySt);
                                        countryId = createCountryNode(countryProperties, inserter, countryNameIndex, nodeTypeIndex);
                                        //flushing country name index
                                        countryNameIndex.flush();
                                    }
                                    inserter.createRelationship(instituteId, countryId, instituteCountryRel, null);
                                }
                                inserter.createRelationship(thesisId, instituteId, thesisInstituteRel, null);
                            }
                        }

                        //--protein citation relationship
                        inserter.createRelationship(thesisId, currentProteinId, thesisProteinCitationRel, null);

                    }

                    //----------------------------------------------------------------------------
                    //-----------------------------PATENT-----------------------------------------
                } else if (citationType.equals(PatentNode.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                    if (uniprotDataXML.getPatents()) {
                        String numberSt = citation.getAttributeValue("number");
                        String dateSt = citation.getAttributeValue("date");
                        String titleSt = citation.getChildText("title");
                        if (dateSt == null) {
                            dateSt = "";
                        }
                        if (titleSt == null) {
                            titleSt = "";
                        }
                        if (numberSt == null) {
                            numberSt = "";
                        }

                        if (!numberSt.equals("")) {
                            //long patentId = indexService.getSingleNode(PatentNode.PATENT_NUMBER_INDEX, numberSt);
                            long patentId = -1;
                            IndexHits<Long> patentNumberIndexHits = patentNumberIndex.get(PatentNode.PATENT_NUMBER_INDEX, numberSt);
                            if (patentNumberIndexHits.hasNext()) {
                                patentId = patentNumberIndexHits.getSingle();
                            }

                            if (patentId < 0) {
                                patentProperties.put(PatentNode.NUMBER_PROPERTY, numberSt);
                                patentProperties.put(PatentNode.DATE_PROPERTY, dateSt);
                                patentProperties.put(PatentNode.TITLE_PROPERTY, titleSt);
                                //---patent node creation and indexing
                                patentId = inserter.createNode(patentProperties);
                                //indexService.index(patentId, PatentNode.PATENT_NUMBER_INDEX, numberSt);
                                patentNumberIndex.add(patentId, MapUtil.map(PatentNode.PATENT_NUMBER_INDEX, numberSt));
                                //---flushing patent number index---
                                patentNumberIndex.flush();
                                //---authors association-----
                                for (long personId : authorsPersonNodesIds) {
                                    inserter.createRelationship(patentId, personId, patentAuthorRel, null);
                                }
                            }

                            //--protein citation relationship
                            inserter.createRelationship(patentId, currentProteinId, patentProteinCitationRel, null);
                        }
                    }

                    //----------------------------------------------------------------------------
                    //-----------------------------SUBMISSION-----------------------------------------
                } else if (citationType.equals(SubmissionNode.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                    if (uniprotDataXML.getSubmissions()) {
                        String dateSt = citation.getAttributeValue("date");
                        String titleSt = citation.getChildText("title");
                        String dbSt = citation.getAttributeValue("db");
                        if (dateSt == null) {
                            dateSt = "";
                        }
                        if (titleSt == null) {
                            titleSt = "";
                        }

                        submissionProperties.put(SubmissionNode.DATE_PROPERTY, dateSt);
                        submissionProperties.put(SubmissionNode.TITLE_PROPERTY, titleSt);

                        //---submission node creation and indexing
                        long submissionId = inserter.createNode(submissionProperties);
                        //---authors association-----
                        for (long personId : authorsPersonNodesIds) {
                            inserter.createRelationship(submissionId, personId, submissionAuthorRel, null);
                        }
                        //---authors consortium association-----
                        for (long consortiumId : authorsConsortiumNodesIds) {
                            inserter.createRelationship(submissionId, consortiumId, submissionAuthorRel, null);
                        }

                        if (dbSt != null) {
                            long dbId = -1;
                            IndexHits<Long> dbNameIndexHits = dbNameIndex.get(DBNode.DB_NAME_INDEX, dbSt);
                            if (dbNameIndexHits.hasNext()) {
                                dbId = dbNameIndexHits.getSingle();
                            }
                            if (dbId < 0) {
                                dbProperties.put(DBNode.NODE_TYPE_PROPERTY, DBNode.NODE_TYPE);
                                dbProperties.put(DBNode.NAME_PROPERTY, dbSt);
                                dbId = createDbNode(dbProperties, inserter, dbNameIndex, nodeTypeIndex);
                                dbNameIndex.flush();
                            }
                            //-----submission db relationship-----
                            inserter.createRelationship(submissionId, dbId, submissionDbRel, null);
                        }

                        //--protein citation relationship
                        inserter.createRelationship(submissionId, currentProteinId, submissionProteinCitationRel, null);

                    }

                    //----------------------------------------------------------------------------
                    //-----------------------------BOOK-----------------------------------------
                } else if (citationType.equals(BookNode.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                    if (uniprotDataXML.getBooks()) {
                        String nameSt = citation.getAttributeValue("name");
                        String dateSt = citation.getAttributeValue("date");
                        String titleSt = citation.getChildText("title");
                        String publisherSt = citation.getAttributeValue("publisher");
                        String firstSt = citation.getAttributeValue("first");
                        String lastSt = citation.getAttributeValue("last");
                        String citySt = citation.getAttributeValue("city");
                        String volumeSt = citation.getAttributeValue("volume");
                        if (nameSt == null) {
                            nameSt = "";
                        }
                        if (dateSt == null) {
                            dateSt = "";
                        }
                        if (titleSt == null) {
                            titleSt = "";
                        }
                        if (publisherSt == null) {
                            publisherSt = "";
                        }
                        if (firstSt == null) {
                            firstSt = "";
                        }
                        if (lastSt == null) {
                            lastSt = "";
                        }
                        if (citySt == null) {
                            citySt = "";
                        }
                        if (volumeSt == null) {
                            volumeSt = "";
                        }

                        //long bookId = indexService.getSingleNode(BookNode.BOOK_NAME_FULL_TEXT_INDEX, nameSt);
                        long bookId = -1;
                        IndexHits<Long> bookNameIndexHits = bookNameIndex.get(BookNode.BOOK_NAME_FULL_TEXT_INDEX, nameSt);
                        if (bookNameIndexHits.hasNext()) {
                            bookId = bookNameIndexHits.getSingle();
                        }

                        if (bookId < 0) {
                            bookProperties.put(BookNode.NAME_PROPERTY, nameSt);
                            bookProperties.put(BookNode.DATE_PROPERTY, dateSt);
                            //---book node creation and indexing
                            bookId = inserter.createNode(bookProperties);
                            //indexService.index(bookId, BookNode.BOOK_NAME_FULL_TEXT_INDEX, nameSt);
                            bookNameIndex.add(bookId, MapUtil.map(BookNode.BOOK_NAME_FULL_TEXT_INDEX, nameSt));
                            //--flushing book name index---
                            bookNameIndex.flush();
                            //---authors association-----
                            for (long personId : authorsPersonNodesIds) {
                                inserter.createRelationship(bookId, personId, bookAuthorRel, null);
                            }

                            //---editor association-----
                            Element editorListElem = citation.getChild("editorList");
                            if (editorListElem != null) {
                                List<Element> editorsElems = editorListElem.getChildren("person");
                                for (Element person : editorsElems) {
                                    //long editorId = indexService.getSingleNode(PersonNode.PERSON_NAME_INDEX, person.getAttributeValue("name"));
                                    long editorId = -1;
                                    IndexHits<Long> personNameIndexHits = personNameIndex.get(PersonNode.PERSON_NAME_FULL_TEXT_INDEX, person.getAttributeValue("name"));
                                    if (personNameIndexHits.hasNext()) {
                                        editorId = personNameIndexHits.getSingle();
                                    }
                                    if (editorId < 0) {
                                        personProperties.put(PersonNode.NAME_PROPERTY, person.getAttributeValue("name"));
                                        editorId = createPersonNode(personProperties, inserter, personNameIndex, nodeTypeIndex);
                                    }
                                    //---flushing person name index---
                                    personNameIndex.flush();
                                    //editor association
                                    inserter.createRelationship(bookId, editorId, bookEditorRel, null);
                                }
                            }


                            //----publisher--
                            if (!publisherSt.equals("")) {
                                //long publisherId = indexService.getSingleNode(PublisherNode.PUBLISHER_NAME_INDEX, publisherSt);
                                long publisherId = -1;
                                IndexHits<Long> publisherNameIndexHits = publisherNameIndex.get(PublisherNode.PUBLISHER_NAME_INDEX, publisherSt);
                                if (publisherNameIndexHits.hasNext()) {
                                    publisherId = publisherNameIndexHits.getSingle();
                                }
                                if (publisherId < 0) {
                                    publisherProperties.put(PublisherNode.NAME_PROPERTY, publisherSt);
                                    publisherId = inserter.createNode(publisherProperties);
                                    //indexService.index(publisherId, PublisherNode.PUBLISHER_NAME_INDEX, publisherSt);
                                    publisherNameIndex.add(publisherId, MapUtil.map(PublisherNode.PUBLISHER_NAME_INDEX, publisherSt));
                                    //--flushing publisher name index--
                                    publisherNameIndex.flush();
                                }
                                inserter.createRelationship(bookId, publisherId, bookPublisherRel, null);
                            }

                            //-----city-----
                            if (!citySt.equals("")) {
                                //long cityId = indexService.getSingleNode(CityNode.CITY_NAME_INDEX, citySt);
                                long cityId = -1;
                                IndexHits<Long> cityNameIndexHits = cityNameIndex.get(CityNode.CITY_NAME_INDEX, citySt);
                                if (cityNameIndexHits.hasNext()) {
                                    cityId = cityNameIndexHits.getSingle();
                                }
                                if (cityId < 0) {
                                    cityProperties.put(CityNode.NAME_PROPERTY, citySt);
                                    cityId = createCityNode(cityProperties, inserter, cityNameIndex, nodeTypeIndex);
                                    //-----flushing city name index---
                                    cityNameIndex.flush();
                                }
                                inserter.createRelationship(bookId, cityId, bookCityRel, null);
                            }
                        }

                        bookProteinCitationProperties.put(BookProteinCitationRel.FIRST_PROPERTY, firstSt);
                        bookProteinCitationProperties.put(BookProteinCitationRel.LAST_PROPERTY, lastSt);
                        bookProteinCitationProperties.put(BookProteinCitationRel.VOLUME_PROPERTY, volumeSt);
                        bookProteinCitationProperties.put(BookProteinCitationRel.TITLE_PROPERTY, titleSt);
                        //--protein citation relationship
                        inserter.createRelationship(bookId, currentProteinId, bookProteinCitationRel, bookProteinCitationProperties);

                    }

                    //----------------------------------------------------------------------------
                    //-----------------------------ONLINE ARTICLE-----------------------------------------
                } else if (citationType.equals(OnlineArticleNode.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                    if (uniprotDataXML.getOnlineArticles()) {
                        String locatorSt = citation.getChildText("locator");
                        String nameSt = citation.getAttributeValue("name");
                        String titleSt = citation.getChildText("title");

                        if (titleSt == null) {
                            titleSt = "";
                        }
                        if (nameSt == null) {
                            nameSt = "";
                        }
                        if (locatorSt == null) {
                            locatorSt = "";
                        }

                        //long onlineArticleId = indexService.getSingleNode(OnlineArticleNode.ONLINE_ARTICLE_TITLE_FULL_TEXT_INDEX, titleSt);
                        long onlineArticleId = -1;
                        IndexHits<Long> onlineArticleTitleIndexHits = onlineArticleTitleIndex.get(OnlineArticleNode.ONLINE_ARTICLE_TITLE_FULL_TEXT_INDEX, titleSt);
                        if (onlineArticleTitleIndexHits.hasNext()) {
                            onlineArticleId = onlineArticleTitleIndexHits.getSingle();
                        }
                        if (onlineArticleId < 0) {
                            onlineArticleProperties.put(OnlineArticleNode.TITLE_PROPERTY, titleSt);
                            onlineArticleId = inserter.createNode(onlineArticleProperties);
                            if (!titleSt.equals("")) {
                                //indexService.index(onlineArticleId, OnlineArticleNode.ONLINE_ARTICLE_TITLE_FULL_TEXT_INDEX, titleSt);
                                onlineArticleTitleIndex.add(onlineArticleId, MapUtil.map(OnlineArticleNode.ONLINE_ARTICLE_TITLE_FULL_TEXT_INDEX, titleSt));
                                //-----flushing online article title index---
                                onlineArticleTitleIndex.flush();
                            }

                            //---authors person association-----
                            for (long personId : authorsPersonNodesIds) {
                                inserter.createRelationship(onlineArticleId, personId, onlineArticleAuthorRel, null);
                            }
                            //---authors consortium association-----
                            for (long consortiumId : authorsConsortiumNodesIds) {
                                inserter.createRelationship(onlineArticleId, consortiumId, onlineArticleAuthorRel, null);
                            }

                            //------online journal-----------
                            if (!nameSt.equals("")) {

                                long onlineJournalId = -1;
                                IndexHits<Long> onlineJournalNameIndexHits = onlineJournalNameIndex.get(OnlineJournalNode.ONLINE_JOURNAL_NAME_INDEX, nameSt);
                                if (onlineJournalNameIndexHits.hasNext()) {
                                    onlineJournalId = onlineJournalNameIndexHits.getSingle();
                                }
                                if (onlineJournalId < 0) {
                                    onlineJournalProperties.put(OnlineJournalNode.NAME_PROPERTY, nameSt);
                                    onlineJournalId = inserter.createNode(onlineJournalProperties);
                                    //indexService.index(onlineJournalId, OnlineJournalNode.ONLINE_JOURNAL_NAME_INDEX, nameSt);
                                    onlineJournalNameIndex.add(onlineJournalId, MapUtil.map(OnlineJournalNode.ONLINE_JOURNAL_NAME_INDEX, nameSt));
                                    //---flushing online journal name index---
                                    onlineJournalNameIndex.flush();
                                }

                                onlineArticleJournalProperties.put(OnlineArticleJournalRel.LOCATOR_PROPERTY, locatorSt);
                                inserter.createRelationship(onlineArticleId, onlineJournalId, onlineArticleJournalRel, onlineArticleJournalProperties);
                            }
                            //----------------------------
                        }
                        //protein citation
                        inserter.createRelationship(onlineArticleId, currentProteinId, onlineArticleProteinCitationRel, null);

                    }

                    //----------------------------------------------------------------------------
                    //-----------------------------ARTICLE-----------------------------------------
                } else if (citationType.equals(ArticleNode.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                    if (uniprotDataXML.getArticles()) {
                        String journalNameSt = citation.getAttributeValue("name");
                        String dateSt = citation.getAttributeValue("date");
                        String titleSt = citation.getChildText("title");
                        String firstSt = citation.getAttributeValue("first");
                        String lastSt = citation.getAttributeValue("last");
                        String volumeSt = citation.getAttributeValue("volume");
                        String doiSt = "";
                        String medlineSt = "";
                        String pubmedSt = "";

                        if (journalNameSt == null) {
                            journalNameSt = "";
                        }
                        if (dateSt == null) {
                            dateSt = "";
                        }
                        if (firstSt == null) {
                            firstSt = "";
                        }
                        if (lastSt == null) {
                            lastSt = "";
                        }
                        if (volumeSt == null) {
                            volumeSt = "";
                        }
                        if (titleSt == null) {
                            titleSt = "";
                        }

                        List<Element> dbReferences = citation.getChildren("dbReference");
                        for (Element tempDbRef : dbReferences) {
                            if (tempDbRef.getAttributeValue("type").equals("DOI")) {
                                doiSt = tempDbRef.getAttributeValue("id");
                            } else if (tempDbRef.getAttributeValue("type").equals("MEDLINE")) {
                                medlineSt = tempDbRef.getAttributeValue("id");
                            } else if (tempDbRef.getAttributeValue("type").equals("PubMed")) {
                                pubmedSt = tempDbRef.getAttributeValue("id");
                            }
                        }

                        //long articleId = indexService.getSingleNode(ArticleNode.ARTICLE_TITLE_FULL_TEXT_INDEX, titleSt);
                        long articleId = -1;
                        IndexHits<Long> articleTitleIndexHits = articleTitleIndex.get(ArticleNode.ARTICLE_TITLE_FULL_TEXT_INDEX, titleSt);
                        if (articleTitleIndexHits.hasNext()) {
                            articleId = articleTitleIndexHits.getSingle();
                        }
                        if (articleId < 0) {
                            articleProperties.put(ArticleNode.TITLE_PROPERTY, titleSt);
                            articleProperties.put(ArticleNode.DOI_ID_PROPERTY, doiSt);
                            articleProperties.put(ArticleNode.MEDLINE_ID_PROPERTY, medlineSt);
                            articleProperties.put(ArticleNode.PUBMED_ID_PROPERTY, pubmedSt);
                            articleId = inserter.createNode(articleProperties);
                            if (!titleSt.equals("")) {
                                //indexService.index(articleId, ArticleNode.ARTICLE_TITLE_FULL_TEXT_INDEX, titleSt);
                                articleTitleIndex.add(articleId, MapUtil.map(ArticleNode.ARTICLE_TITLE_FULL_TEXT_INDEX, titleSt));
                                //--flushing article title index---
                                articleTitleIndex.flush();
                            }

                            //---indexing by medline, doi and pubmed--
                            if (!doiSt.isEmpty()) {
                                articleDoiIdIndex.add(articleId, MapUtil.map(ArticleNode.ARTICLE_DOI_ID_INDEX, doiSt));
                            }
                            if (!medlineSt.isEmpty()) {
                                articleMedlineIdIndex.add(articleId, MapUtil.map(ArticleNode.ARTICLE_MEDLINE_ID_INDEX, medlineSt));
                            }
                            if (!pubmedSt.isEmpty()) {
                                articlePubmedIdIndex.add(articleId, MapUtil.map(ArticleNode.ARTICLE_PUBMED_ID_INDEX, pubmedSt));
                            }

                            //---authors person association-----
                            for (long personId : authorsPersonNodesIds) {
                                inserter.createRelationship(articleId, personId, articleAuthorRel, null);
                            }
                            //---authors consortium association-----
                            for (long consortiumId : authorsConsortiumNodesIds) {
                                inserter.createRelationship(articleId, consortiumId, articleAuthorRel, null);
                            }

                            //------journal-----------
                            if (!journalNameSt.equals("")) {
                                //long journalId = indexService.getSingleNode(JournalNode.JOURNAL_NAME_INDEX, journalNameSt);
                                long journalId = -1;
                                IndexHits<Long> journalNameIndexHits = journalNameIndex.get(JournalNode.JOURNAL_NAME_INDEX, journalNameSt);
                                if (journalNameIndexHits.hasNext()) {
                                    journalId = journalNameIndexHits.getSingle();
                                }
                                if (journalId < 0) {
                                    journalProperties.put(JournalNode.NAME_PROPERTY, journalNameSt);
                                    journalId = inserter.createNode(journalProperties);
                                    //indexService.index(journalId, JournalNode.JOURNAL_NAME_INDEX, journalNameSt);
                                    journalNameIndex.add(journalId, MapUtil.map(JournalNode.JOURNAL_NAME_INDEX, journalNameSt));
                                    //----flushing journal name index----
                                    journalNameIndex.flush();
                                }

                                articleJournalProperties.put(ArticleJournalRel.DATE_PROPERTY, dateSt);
                                articleJournalProperties.put(ArticleJournalRel.FIRST_PROPERTY, firstSt);
                                articleJournalProperties.put(ArticleJournalRel.LAST_PROPERTY, lastSt);
                                articleJournalProperties.put(ArticleJournalRel.VOLUME_PROPERTY, volumeSt);
                                inserter.createRelationship(articleId, journalId, articleJournalRel, articleJournalProperties);
                            }
                            //----------------------------
                        }
                        //protein citation
                        inserter.createRelationship(articleId, currentProteinId, articleProteinCitationRel, null);

                    }

                    //----------------------------------------------------------------------------
                    //----------------------UNPUBLISHED OBSERVATIONS-----------------------------------------
                } else if (citationType.equals(UnpublishedObservationNode.UNIPROT_ATTRIBUTE_TYPE_VALUE)) {

                    if (uniprotDataXML.getUnpublishedObservations()) {
                        String dateSt = citation.getAttributeValue("date");
                        if (dateSt == null) {
                            dateSt = "";
                        }

                        unpublishedObservationProperties.put(UnpublishedObservationNode.DATE_PROPERTY, dateSt);
                        long unpublishedObservationId = inserter.createNode(unpublishedObservationProperties);
                        //---authors person association-----
                        for (long personId : authorsPersonNodesIds) {
                            inserter.createRelationship(unpublishedObservationId, personId, unpublishedObservationAuthorRel, null);
                        }
                    }

                }
            }
        }


    }

    private static String[] convertToStringArray(List<String> list) {
        String[] result = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}
