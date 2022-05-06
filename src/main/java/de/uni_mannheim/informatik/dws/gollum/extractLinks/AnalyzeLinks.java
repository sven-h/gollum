package de.uni_mannheim.informatik.dws.gollum.extractLinks;

import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.Counter;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.TransitiveClosure;
import de.uni_mannheim.informatik.dws.melt.matching_ml.python.PythonServer;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


public class AnalyzeLinks {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) throws Exception{
        /*        
        List<File> alignmentFiles = new ArrayList<>();
        //alignmentFiles.add(new File("all_wikis_alignment.xml"));
        //alignmentFiles.add(new File("all_wikis_alignment_closure.xml"));
        alignmentFiles.add(new File("known_wikis_alignment_closure.xml"));
        Alignment a = new Alignment();
        for(File f : alignmentFiles){
            try {
                a.addAll(new Alignment(f));
            } catch (SAXException | IOException ex) {
                LOGGER.error("Cannot load alignment", ex);
                return;
            }
        }
        */
        
        //AllWikisLinkExtractor.writeAnalysisToFile(a, "alignment_analysis_07.txt");
        //LOGGER.info("{}", sampleSize(351110, 0.9, 0.1));
        //writeHistogramOfTransitiveClosureSize(a);
        //writeHistogramOfTransitiveClosureConfidence(a);
        //uniqueKGs();
        //onlyenglish();
        
                
        //uniqueKGsFileNames();
        
        //createSupervisedSubsets(Arrays.asList(new File("alignment.xml")), 0.8);
        //createSupervisedSubsetsWikiDistinct(Arrays.asList(new File("known_wikis_alignment_closure.xml")), 0.8);
        //combine();
        
        createSupervisedSubsets(Arrays.asList(new File("./final/unsupervised_direct_and_transitive.xml"), new File("./final/unsupervised_schemaAlignmentMin.xml")), 0.8);
        createSupervisedSubsetsWikiDistinct(Arrays.asList(new File("./final/unsupervised_direct_and_transitive.xml"), new File("./final/unsupervised_schemaAlignmentMin.xml")), 0.8);
    }
    
    
    private static void combine(){
        List<File> alignmentFiles = new ArrayList<>();
        alignmentFiles.add(new File("./final/unsupervised_direct.xml"));
        alignmentFiles.add(new File("./final/unsupervised_transitive.xml"));
        Alignment a = new Alignment();
        for(File f : alignmentFiles){
            try {
                a.addAll(new Alignment(f));
            } catch (SAXException | IOException ex) {
                LOGGER.error("Cannot load alignment", ex);
                return;
            }
        }
        try {
            a.serialize(new File("./final/unsupervised_combined.xml"));
        } catch (IOException ex) {
            LOGGER.error("Cannot write alignment", ex);
        }
    }
    
    private static void writeHistogramOfTransitiveClosureSize(Alignment a){
        TransitiveClosure<String> tc = new TransitiveClosure<>();
        for(Correspondence c : a){
            tc.add(c.getEntityOne(), c.getEntityTwo());
        }
        
        DescriptiveStatistics stat = new DescriptiveStatistics();
        Set<String> largestIdentitySet = new HashSet<>();
        int largestIdentitySetNumber = 0;
        try(BufferedWriter bw = Files.newBufferedWriter(Paths.get("closureSize.csv"), StandardCharsets.UTF_8)){
            for(Set<String> identitySet : tc.getClosure()){
                bw.write(""+identitySet.size());
                bw.write(";");
                stat.addValue(identitySet.size());
                if(identitySet.size() > largestIdentitySetNumber){
                    largestIdentitySetNumber = identitySet.size();
                    largestIdentitySet = identitySet;
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Cannot write file", ex);
        }
        
        LOGGER.info("Transitive closure size stat: {}", stat.toString());
        LOGGER.info("Largest identity set: {}", largestIdentitySet);
        
    }
    
    private static final DecimalFormat DF = new DecimalFormat("#.000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static void writeHistogramOfTransitiveClosureConfidence(Alignment a){
        try(BufferedWriter bw = Files.newBufferedWriter(Paths.get("transitive_confidences.csv"), StandardCharsets.UTF_8)){
            for(Correspondence c : a){
                bw.write(DF.format(c.getConfidence()));
                bw.write(";");
            }
        } catch (IOException ex) {
            LOGGER.error("Cannot write file", ex);
        }
    }
    
    private static void onlyenglish(){
        Set<String> kgs = new HashSet<>();
        LOGGER.info("load...");
        Alignment a;
        try {
            a = new Alignment(new File("known_wikis_alignment_closure.xml"));
        } catch (SAXException | IOException ex) {
            LOGGER.warn("Could not lead alignment", ex);
            return;
        }
        LOGGER.info("process...");
        int count = 0;
        for(Correspondence c : a){
            if(DBkWikUtil.getWikiLanguage(c.getEntityOne()).equals("en") && DBkWikUtil.getWikiLanguage(c.getEntityTwo()).equals("en")){
                count++;
            }
        }
        LOGGER.info("englisch mapping: {} / all mapping: {}", count, a.size());
        //englisch mapping: 318204 / all mapping: 351110
    }
    
    private static Set<String> uniqueKGs(){
        Set<String> kgs = new HashSet<>();
        LOGGER.info("load...");
        Alignment a;
        try {
            a = new Alignment(new File("./final/unsupervised_direct.xml"));
        } catch (SAXException | IOException ex) {
            LOGGER.warn("Could not lead alignment", ex);
            return kgs;
        }
        LOGGER.info("process...");
        for(Correspondence c : a){
            kgs.add(DBkWikUtil.getWikiName(c.getEntityOne()));
            kgs.add(DBkWikUtil.getWikiName(c.getEntityTwo()));
        }
        LOGGER.info("Unique KGs: {}", kgs.size());
        return kgs;
    }
    private static void uniqueKGsFileNames(){
        Map<String, String> wikiToFileName = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("files_names.txt")))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] lineSplit = line.split("~");
                String wikiName = lineSplit[2];
                wikiName = wikiName.substring(0, wikiName.indexOf(".tar.gz")) + ".wikia.com";                
                wikiToFileName.put(wikiName, line);
                wikiToFileName.put(lineSplit[1] + "." + wikiName, line);
            }
        } catch (IOException ex) {
            LOGGER.error("Could not read", ex);
        }
        
        Set<String> uniqueKGs = uniqueKGs();
        try(BufferedWriter bw = Files.newBufferedWriter(Paths.get("goldStandardFiles.txt"), StandardCharsets.UTF_8)){
            for(String kg : uniqueKGs){
                String fileName = wikiToFileName.get(kg);
                if(fileName == null){
                    LOGGER.info("Missing: {}", kg);
                }
                else{
                    bw.write(fileName);
                    bw.newLine();
                }
            }
        }catch (IOException ex) {
            LOGGER.error("could not write", ex);
        }
    }
    
    
    private static void createSupervisedSubsets(List<File> alignmentFiles, double trainPercentage){
        if(trainPercentage < 0.0 || trainPercentage > 1.0)
            throw new IllegalArgumentException("not a percentage value");
        
        Alignment a = new Alignment();
        for(File f : alignmentFiles){
            try {
                a.addAll(new Alignment(f));
            } catch (SAXException | IOException ex) {
                LOGGER.error("Cannot load alignment", ex);
                return;
            }
        }
        
        TransitiveClosure<String> tc = new TransitiveClosure<>();
        for(Correspondence c : a){
            tc.add(c.getEntityOne(), c.getEntityTwo());
        }
        
        List<Correspondence> fixedOder = new ArrayList<>(a);
        
        List<Integer> groups = new ArrayList<>();
        for(Correspondence cell : fixedOder){
            groups.add(tc.getIdentityID(cell.getEntityOne()));
        }
        
        List<Integer> indices = new ArrayList<>();
        try {
            indices = PythonServer.getInstance().runGroupShuffleSplit(groups, trainPercentage);
        } catch (Exception ex) {
            LOGGER.error("Python exception", ex);
        }
        Set<Integer> indicesSet = new HashSet<>(indices);
        
        Alignment train = new Alignment();
        Alignment test = new Alignment();
        
        for(int i=0; i < fixedOder.size(); i++){
            if(indicesSet.contains(i)){
                train.add(fixedOder.get(i));
            }else{
                test.add(fixedOder.get(i));
            }
        }
        
        try {
            train.serialize(new File("supervised_samewiki_train.xml"));
        } catch (IOException ex) {
            LOGGER.error("Canot write file", ex);
        }
        try {
            test.serialize(new File("supervised_samewiki_test.xml"));
        } catch (IOException ex) {
            LOGGER.error("Canot write file", ex);
        }
    }
    
    
    
    private static void createSupervisedSubsetsWikiDistinct(List<File> alignmentFiles, double trainPercentage){
        if(trainPercentage < 0.0 || trainPercentage > 1.0)
            throw new IllegalArgumentException("not a percentage value");
        
        Alignment a = new Alignment();
        for(File f : alignmentFiles){
            try {
                a.addAll(new Alignment(f));
            } catch (SAXException | IOException ex) {
                LOGGER.error("Cannot load alignment", ex);
                return;
            }
        }
        
        TransitiveClosure<String> tc = new TransitiveClosure<>();
        Set<String> knownWikis = new HashSet<>();
        for(Correspondence c : a){
            tc.add(c.getEntityOne(), c.getEntityTwo());
            //knownWikis.add(DBkWikUtil.getWikiName(c.getEntityOne()));
            //knownWikis.add(DBkWikUtil.getWikiName(c.getEntityTwo()));
            //tcWiki.add(DBkWikUtil.getWikiName(c.getEntityOne()), DBkWikUtil.getWikiName(c.getEntityTwo()));
        }
        
        
        Counter<Entry<String, String>> counter = new Counter<>();
        for(Set<String> identitySet : tc.getClosure()){
            List<Entry<String, String>> entities = new ArrayList<>(identitySet.size());
            for(String s : identitySet){
                String wikiName = DBkWikUtil.getWikiName(s);
                entities.add(new SimpleEntry<>(DBkWikUtil.getWikiName(s), s));
            }
            entities.sort(Comparator.comparing(entry -> entry.getKey()));
            for(int i=0; i < entities.size(); i++){
                Entry<String, String> first = entities.get(i);
                for(int j=i+1; j < entities.size(); j++){
                    Entry<String, String> second = entities.get(j);
                    counter.add(new SimpleEntry<>(first.getKey(),second.getKey()));
                }
            }
        }
        System.out.println(counter.toStringMultiline());
        
        int c = 100;
        
        TransitiveClosure<String> tcWiki = new TransitiveClosure<>();
        for(Entry<Entry<String, String>, Integer> entry : counter.mostCommon()){
            if(entry.getValue() < 200)
                break;
            tcWiki.add(entry.getKey().getKey(), entry.getKey().getValue());
        }
        
        for(Set<String> x : tcWiki.getClosure()){
            System.out.println(Integer.toString(x.size()) + " " + x.toString());
        }
        int counterId = tcWiki.getClosure().size() + 1;
        
        List<Correspondence> fixedOder = new ArrayList<>(a);
        List<Integer> groups = new ArrayList<>();
        Map<String, Integer> wikiToId = new HashMap<>();
        for(Correspondence d : fixedOder){
            String wikiOne = DBkWikUtil.getWikiName(d.getEntityOne());
            String wikiTwo = DBkWikUtil.getWikiName(d.getEntityTwo());
            
            Integer wikiId = tcWiki.getIdentityID(wikiOne);
            if(wikiId != null){
                groups.add(wikiId);
                continue;
            }
            wikiId = tcWiki.getIdentityID(wikiTwo);
            if(wikiId != null){
                groups.add(wikiId);
                continue;
            }
            
            String wikiKey = wikiOne + wikiTwo;
            wikiId = wikiToId.get(wikiKey);
            if(wikiId == null){
                counterId++;
                wikiToId.put(wikiKey, counterId);
                groups.add(counterId);
            }else{
                groups.add(wikiId);
            }
            
        }
       
        //System.out.println(new Counter<>(groups).toStringMultiline());
        
        
        List<Integer> indices = new ArrayList<>();
        try {
            indices = PythonServer.getInstance().runGroupShuffleSplit(groups, trainPercentage);
        } catch (Exception ex) {
            LOGGER.error("Python exception", ex);
        }
        Set<Integer> indicesSet = new HashSet<>(indices);
        
        Alignment train = new Alignment();
        Alignment test = new Alignment();
        
        for(int i=0; i < fixedOder.size(); i++){
            if(indicesSet.contains(i)){
                test.add(fixedOder.get(i));
            }else{
                train.add(fixedOder.get(i));
            }
        }
        
        try {
            train.serialize(new File("supervised_distinct_wiki_train.xml"));
        } catch (IOException ex) {
            LOGGER.error("Canot write file", ex);
        }
        try {
            test.serialize(new File("supervised_distinct_wiki_test.xml"));
        } catch (IOException ex) {
            LOGGER.error("Canot write file", ex);
        }
        
        /*
        //compute overlap between wikis
        List<String> knownWikisList = new ArrayList<>(knownWikis);
        for(int i =0 ; i < knownWikisList.size(); i++){
            String wikiOne = knownWikisList.get(i);
            for(int j = i + 1; j < knownWikisList.size(); j++){
                String wikiTwo = knownWikisList.get(j);
                
                LOGGER.info("overlap {}-{}: {}", wikiOne, wikiTwo, overlap(tc, wikiOne, wikiTwo));
                
            }
        }
        */
        
        /*
        List<Set<String>> tcWikiClosure = new ArrayList<>(tcWiki.getClosure());
        for(Set<String> wikiIdentitySet : tcWikiClosure){
            int count = 0;
            for(Correspondence c : a){
                if(wikiIdentitySet.contains(DBkWikUtil.getWikiName(c.getEntityOne())) ||
                        wikiIdentitySet.contains(DBkWikUtil.getWikiName(c.getEntityTwo()))){
                    count++;
                }
            }
            LOGGER.info("wikiIdentitySet: {} count: {}", wikiIdentitySet.size(), count);
        }
*/
        
    }
    private static int overlap(TransitiveClosure<String> tc, String wikiOne, String wikiTwo){
        int count = 0;
        for(Set<String> identitySet : tc.getClosure()){
            
            int one = 0;
            int two = 0;
            for(String s : identitySet){
                String name = DBkWikUtil.getWikiName(s);
                if(name.equals(wikiOne)){
                    one++;
                }else if(name.equals(wikiTwo)){
                    two++;
                }
            }
            if(one > 0 && two > 0){
                count++;
            }
        }
        return count;
        
    }
    
}
