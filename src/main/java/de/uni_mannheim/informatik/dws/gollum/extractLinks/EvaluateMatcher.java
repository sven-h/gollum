/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_mannheim.informatik.dws.gollum.extractLinks;

import de.uni_mannheim.informatik.dws.melt.matching_data.GoldStandardCompleteness;
import de.uni_mannheim.informatik.dws.melt.matching_eval.evaluator.metric.cm.ConfusionMatrix;
import de.uni_mannheim.informatik.dws.melt.matching_eval.evaluator.metric.cm.ConfusionMatrixMetric;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.TransitiveClosure;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


public class EvaluateMatcher {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateMatcher.class);
    
    public static void main(String[] args) throws SAXException, IOException{
        //System.out.println(reduceGoldStandardToKnownWikis(new Alignment(new File("./alignment.xml")), new File("final/files_goldStandard.txt")));
        /*
        Alignment gold = new Alignment();
        gold.addAll(new Alignment(new File("unsupervised_combined.xml")));
        
        Alignment systemSimple = new Alignment(new File("/work-ceph/shertlin/full_run_oaei_matcher/"));
        eval(new Alignment(new File("./alignment.xml")), new Alignment(new File("./all_wikis_alignment_____somewikis.xml")));
        
        
*/
        
        Alignment gold = new Alignment(new File("./final/unsupervised_combined.xml"));
        gold.addAll(new Alignment(new File("./final/unsupervised_schemaAlignmentMin.xml")));
        Alignment reduced = reduceGoldStandardToKnownWikis(gold, new File("./final/files_goldStandard.txt"));
        reduced.serialize(new File("./final/unsupervised_for_gold_files.xml"));
        
        
        /*
        Alignment gold = new Alignment(new File("unsupervised_for_gold_files.xml"));
        LOGGER.info("===alignment_simple_string=====");
        evalOneSystem(gold, "/work-ceph/shertlin/full_run_oaei_matcher/alignment_simple_string.xml");
        LOGGER.info("===alignment_alod2vec=====");
        evalOneSystem(gold, "/work-ceph/shertlin/full_run_oaei_matcher/alignment.xml");
        */
    }
    
    private static void evalOneSystem(Alignment gold, String systemPath){
        try {
            eval(gold, new Alignment(new File(systemPath)));
        } catch (IOException | SAXException ex) {
            LOGGER.error("Could not read alignment", ex);
        }
    }
    
    
    private static void eval(Alignment goldStandard, Alignment systemAlignment){
        Alignment goldInstance = new Alignment();
        Alignment goldProperty = new Alignment();
        Alignment goldClass = new Alignment();
        
        Alignment systemInstance = new Alignment();
        Alignment systemProperty = new Alignment();
        Alignment systemClass = new Alignment();
        
        
        for(Correspondence c : goldStandard){
            switch(DBkWikUtil.getConceptType(c)){
                case CLASS:{
                    goldClass.add(c);
                    break;
                }
                case RDF_PROPERTY:{
                    goldProperty.add(c);
                    break;
                }
                case INSTANCE:{
                    goldInstance.add(c);
                    break;
                }
                default:{
                    LOGGER.error("Check correspondence {}", c);
                    break;
                }
            }
        }
        for(Correspondence c : systemAlignment){
            switch(DBkWikUtil.getConceptType(c)){
                case CLASS:{
                    systemClass.add(c);
                    break;
                }
                case RDF_PROPERTY:{
                    systemProperty.add(c);
                    break;
                }
                case INSTANCE:{
                    systemInstance.add(c);
                    break;
                }
                default:{
                    LOGGER.error("Check correspondence {}", c);
                    break;
                }
            }
        }
        //ConfusionMatrixMetric metric = new ConfusionMatrixMetric();
        //LOGGER.info("OVERALL: {}", metric.compute(goldStandard, systemAlignment, GoldStandardCompleteness.PARTIAL_SOURCE_COMPLETE_TARGET_COMPLETE));
        //LOGGER.info("CLASS: {}", metric.compute(goldClass, systemClass, GoldStandardCompleteness.PARTIAL_SOURCE_COMPLETE_TARGET_COMPLETE));
        //LOGGER.info("PROP : {}", metric.compute(goldProperty, systemProperty, GoldStandardCompleteness.PARTIAL_SOURCE_COMPLETE_TARGET_COMPLETE));
        //LOGGER.info("INST : {}", metric.compute(goldInstance, systemInstance, GoldStandardCompleteness.PARTIAL_SOURCE_COMPLETE_TARGET_COMPLETE));
        
        evalWithTransClosure(goldClass, systemClass, "class");
        evalWithTransClosure(goldProperty, systemProperty, "prop");
        evalWithTransClosure(goldInstance, systemInstance, "inst");
        
        evalWithTransClosure(goldStandard, systemAlignment, "overall");
        
        LOGGER.info("Size overall: gold: {} system: {}", goldStandard.size(), systemAlignment.size());
        logSameDifference(systemAlignment, "overall system ");
        
        LOGGER.info("Size CLASS: gold: {} system: {}", goldClass.size(), systemClass.size());
        logSameDifference(systemClass, "CLASS system ");
        
        LOGGER.info("Size PROP: gold: {} system: {}", goldProperty.size(), systemProperty.size());
        logSameDifference(systemProperty, "PROP system ");
        
        LOGGER.info("Size INST: gold: {} system: {}", goldInstance.size(), systemInstance.size());
        logSameDifference(systemInstance, "INST system ");
    }
    
    private static void evalWithTransClosure(Alignment gold, Alignment system, String text){
        TransitiveClosure<String> closure = new TransitiveClosure<>();
        for(Correspondence c : system){
            closure.add(c.getEntityOne(), c.getEntityTwo());
        }
        
        int tp = 0;
        int fp = 0;
        int fn = 0;
        for (Correspondence referenceCell : gold) {            
            Set<String> oneIdentity = closure.getIdentitySetForElement(referenceCell.getEntityOne());
            Set<String> twoIdentity = closure.getIdentitySetForElement(referenceCell.getEntityTwo());
            
            if(oneIdentity == null || twoIdentity == null){
                fn++;
            }else{
                if(oneIdentity.equals(twoIdentity)){
                    tp++;
                }else{
                    fn++;
                }
            }
            
            if(oneIdentity != null){
                String twoWiki = DBkWikUtil.getWikiName(referenceCell.getEntityTwo());
                for(String sameOne : oneIdentity){
                    if(DBkWikUtil.getWikiName(sameOne).equals(twoWiki)){
                        if(sameOne.equals(referenceCell.getEntityOne()))
                           continue;
                        if(sameOne.equals(referenceCell.getEntityTwo()))
                            continue;
                        fp++;
                    }
                }
            }
            
            if(twoIdentity != null){
                String oneWiki = DBkWikUtil.getWikiName(referenceCell.getEntityOne());
                for(String sameTwo : twoIdentity){
                    if(DBkWikUtil.getWikiName(sameTwo).equals(oneWiki)){
                        if(sameTwo.equals(referenceCell.getEntityOne()))
                           continue;
                        if(sameTwo.equals(referenceCell.getEntityTwo()))
                            continue;
                        fp++;                    
                    }
                }
            }
        }
        double precision = divideWithTwoDenominators(tp, tp, fp);
        double recall = divideWithTwoDenominators(tp, tp, fn);
        double fmeasure = (2.0*precision*recall)/(precision+recall);
        
        LOGGER.info("{} tp: {} fp: {} fn: {} prec: {} rec: {} f1: {}", text, tp, fp, fn, precision, recall, fmeasure);
    }
    
    private static double divideWithTwoDenominators(double numerator, double denominatorOne, double denominatorTwo) {
        if ((denominatorOne + denominatorTwo) > 0.0) {
            return numerator / (denominatorOne + denominatorTwo);
        } else {
            return 0.0;
        }
    }
    

    private static void logSameDifference(Alignment alignment, String text){
        int same = 0;
        int different = 0;
        for(Correspondence c : alignment){
            if(DBkWikUtil.getResourceNameOrFragment(c.getEntityOne()).equals(
                    DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo()))){
                same++;
            }else{
                different++;
            }
        }
        LOGGER.info("{}  same: {} different:  {}", text, same, different);
    }
    
    private static Alignment reduceGoldStandardToKnownWikis(Alignment goldStandard, File fileWithFilenames){
        Alignment reducedAlignment = new Alignment();
        Set<String> wikiNames = DBkWikUtil.getWikiNamesFromFileNames(fileWithFilenames);
        for(Correspondence c : goldStandard){
            if(wikiNames.contains(DBkWikUtil.getWikiName(c.getEntityOne())) &&
                 wikiNames.contains(DBkWikUtil.getWikiName(c.getEntityTwo()))){
                reducedAlignment.add(c);
            }
            else{
                LOGGER.info("{} - {}", DBkWikUtil.getWikiName(c.getEntityOne()), DBkWikUtil.getWikiName(c.getEntityTwo()));
            }
        }
        return reducedAlignment;
    }
}
