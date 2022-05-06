package de.uni_mannheim.informatik.dws.gollum.extractLinks;

import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.Counter;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class ComputeSchemaMatches {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args){
        
        //args = new String[]{
        //    "-i", "C:\\dev\\dbkwik_two\\files\\extracted_example",
        //    "-a", "C:\\dev\\OntMatching\\runOAEImatcherOnFull\\all_wikis_alignment.xml",
        //};
        
        Options options = new Options();

        options.addOption(Option.builder("i")
                .longOpt("input")
                .hasArgs()
                .valueSeparator(' ')
                .required()
                .desc("Folder(s) to process as input. The folder should contain tar.gz files with wiki content. Multiple directories are possible (separated with space).")
                .build());
        
        options.addOption(Option.builder("a")
                .longOpt("alignment")
                .hasArg()
                .required()
                .desc("Instance alignment to use.")
                .build());
        
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar ", options);
            System.exit(1);
        }
        
        if(cmd.hasOption("help")){
            formatter.printHelp("java -jar ", options);
            System.exit(1);
        }
        
        List<File> filesToProcess = getFiles(cmd);
        LOGGER.info("Process {} files.", filesToProcess.size());        
        
        try {
            new ComputeSchemaMatches().extractSchemaMatches(filesToProcess, new File(cmd.getOptionValue("alignment")));
        } catch (SAXException | IOException ex) {
            LOGGER.error("Could not parse file", ex);
        }
        
        
    }
    
    
    private static List<File> getFiles(CommandLine cmd){
        List<File> files = new ArrayList<>();
        for(String input : cmd.getOptionValues("input")){
            File inputDir = new File(input);
            if(inputDir.isDirectory() == false){
                LOGGER.warn("input is not a directory or does not exist: {}", inputDir);
                continue;
            }
            files.addAll(Arrays.asList(inputDir.listFiles((File dir, String name) -> name.endsWith("tar.gz"))));
        }
        //always keep a fixed order.
        Collections.sort(files, (File o1, File o2) -> o1.getName().compareTo(o2.getName()));
        return files;
    }
    
    private static final String OWL_THING = "http://www.w3.org/2002/07/owl#Thing";
    private void extractSchemaMatches(List<File> files, File instanceAlignmentFile) throws SAXException, IOException{
        Alignment finalSchemaAlignmentDice = new Alignment();
        Alignment finalSchemaAlignmentMin = new Alignment();
        
        Map<String, Set<String>> instanceToClass = new HashMap<>();
        Map<String, Set<String>> classToInstance = new HashMap<>();
        
        
        Map<String, Map<String, Set<String>>> subjectToLiteralToProperty = new HashMap<>();
        Map<String, Map<String, Set<String>>> subjectToResourceToProperty = new HashMap<>();        
        Counter<String> propertyUsedCounter = new Counter<>();
        for(File tarGzFileWikiFile : files){
            LOGGER.info("parsing {}", tarGzFileWikiFile.getName());
            try (TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tarGzFileWikiFile))))){
                TarArchiveEntry tarEntry;
                while ((tarEntry = tarInput.getNextTarEntry()) != null) {
                    String[] splitted = tarEntry.getName().split("-");
                    String lastPart = String.join("-", Arrays.copyOfRange(splitted, 2, splitted.length));
                    switch(lastPart){
                        //case "template-type.ttl":
                        case "infobox-template-type.ttl":
                        {
                            NxParser nxp = new NxParser();
                            nxp.parse(tarInput, StandardCharsets.UTF_8);      
                            for (Node[] nx : nxp) {
                                if(nx[2].getLabel().equals(OWL_THING))
                                    continue;
                                instanceToClass.computeIfAbsent(nx[0].getLabel(), __-> new HashSet<>()).add(nx[2].getLabel());
                                classToInstance.computeIfAbsent(nx[2].getLabel(), __-> new HashSet<>()).add(nx[0].getLabel());
                            }
                            break;
                        }
                        case "infobox-properties.ttl":{
                            NxParser nxp = new NxParser();
                            nxp.parse(tarInput, StandardCharsets.UTF_8);      
                            for (Node[] nx : nxp) {
                                if(nx[2] instanceof Literal){
                                    subjectToLiteralToProperty.computeIfAbsent(nx[0].getLabel(), __-> new HashMap<>())
                                            .computeIfAbsent(nx[2].getLabel(), __->new HashSet<>()).add(nx[1].getLabel());
                                }else{
                                    subjectToResourceToProperty.computeIfAbsent(nx[0].getLabel(), __-> new HashMap<>())
                                            .computeIfAbsent(nx[2].getLabel(), __->new HashSet<>()).add(nx[1].getLabel());
                                }
                                propertyUsedCounter.add(nx[1].getLabel());
                            }
                            break;
                        }
                            
                    }
                }
            } catch (IOException ex) {
                LOGGER.info("Could not read file " + tarGzFileWikiFile.getName(), ex);
            }
        }
        
        Alignment instanceAlignment = new Alignment(instanceAlignmentFile);
        
        Map<Correspondence, ClassMatchInfo> classAlignment = new HashMap<>(); // a map from (class source, class target) to number of shared/matched instances

        for(Correspondence c : instanceAlignment){
            Set<String> sourceTypes = instanceToClass.getOrDefault(c.getEntityOne(), new HashSet<>());
            Set<String> targetTypes = instanceToClass.getOrDefault(c.getEntityTwo(), new HashSet<>());
            for(String sourceType : sourceTypes){
                for(String targetType : targetTypes){
                    Correspondence classCorrespondence = new Correspondence(sourceType, targetType);
                    classAlignment.computeIfAbsent(classCorrespondence, x->new ClassMatchInfo()).addInstanceMatch(c.getEntityOne(), c.getEntityTwo());
                }
            }
        }
        
        for(Map.Entry<Correspondence, ClassMatchInfo> t : classAlignment.entrySet()){
            int instancesOne = classToInstance.getOrDefault(t.getKey().getEntityOne(), new HashSet<>()).size();
            int instancesTwo = classToInstance.getOrDefault(t.getKey().getEntityTwo(), new HashSet<>()).size();
            int instancesOverlap = t.getValue().getOverlap();
            
            if(instancesOverlap < 10 | instancesOne < 10 | instancesTwo < 10)
                continue;
            
            double simValueMin = 0.0;
            int min = Math.min(instancesOne, instancesTwo);
            if(min != 0)
                simValueMin = (double)instancesOverlap / (double) min;
            
            double simValueDice = 0.0;
            if(instancesOne + instancesTwo != 0)
                simValueDice = (double)(2 * instancesOverlap) / (double)(instancesOne + instancesTwo);
            
            if(simValueMin >= 0.2){
                Correspondence c = new Correspondence(t.getKey());
                c.setConfidence(simValueMin);
                c.addAdditionalConfidence("simValueMin", simValueMin);
                c.addAdditionalConfidence("simValueDice", simValueDice);
                finalSchemaAlignmentMin.add(c);
            }
            
            if(simValueDice >= 0.2){
                Correspondence c = new Correspondence(t.getKey());
                c.setConfidence(simValueDice);
                c.addAdditionalConfidence("simValueMin", simValueMin);
                c.addAdditionalConfidence("simValueDice", simValueDice);
                finalSchemaAlignmentDice.add(c);
            }
        }
        
        
        //properties:        
        Counter<Correspondence> counter = new Counter<>();
        for(Correspondence c : instanceAlignment){
            
            Map<String, Set<String>> literalOne = subjectToLiteralToProperty.get(c.getEntityOne());
            Map<String, Set<String>> resourceOne = subjectToResourceToProperty.get(c.getEntityOne());
            
            Map<String, Set<String>> literalTwo = subjectToLiteralToProperty.get(c.getEntityTwo());
            Map<String, Set<String>> resourceTwo = subjectToResourceToProperty.get(c.getEntityTwo());
            
            //match literals:
            if(literalOne != null && literalTwo != null){
                for(Entry<String, Set<String>> literalOneEntry : literalOne.entrySet()){
                    Set<String> propertiesTwo = literalTwo.get(literalOneEntry.getKey());
                    if(propertiesTwo!= null){
                        Set<String> propertiesOne = literalOneEntry.getValue();
                        for(String propertyOne : propertiesOne){
                            for(String propertyTwo : propertiesTwo){
                                Correspondence propCorrespondence = new Correspondence(propertyOne, propertyTwo);
                                counter.add(propCorrespondence);
                                //counter.put(propCorrespondence, counter.getOrDefault(propCorrespondence, 0) + 1);
                            }
                        }
                    }
                }
            }
            
            //match resources
            if(resourceOne != null && resourceTwo != null){
                for(Entry<String, Set<String>> resourceOneEntry : resourceOne.entrySet()){
                    for(String matchedResourceTwo : getMatches(instanceAlignment, resourceOneEntry.getKey())){
                        Set<String> propertiesTwo = resourceTwo.get(matchedResourceTwo);
                        if(propertiesTwo != null){
                            Set<String> propertiesOne = resourceOneEntry.getValue();
                            for(String propertyOne : propertiesOne){
                                for(String propertyTwo : propertiesTwo){
                                    Correspondence propCorrespondence = new Correspondence(propertyOne, propertyTwo);
                                    counter.add(propCorrespondence);
                                    //counter.put(propCorrespondence, counter.getOrDefault(propCorrespondence, 0) + 1);
                                }
                            }
                        }
                    }
                }
            }
            
        }
        
        for(Entry<Correspondence, Integer> entry : counter.mostCommon()){
            int instancesOverlap = entry.getValue();
            int instancesOne = propertyUsedCounter.getCount(entry.getKey().getEntityOne());
            int instancesTwo = propertyUsedCounter.getCount(entry.getKey().getEntityTwo());
            if(instancesOverlap < 10 | instancesOne < 10 | instancesTwo < 10)
                continue;
            
            
            double simValueMin = 0.0;
            int min = Math.min(instancesOne, instancesTwo);
            if(min != 0)
                simValueMin = (double)instancesOverlap / (double) min;
            
                        
            double simValueDice = 0.0;
            if(instancesOne + instancesTwo != 0)
                simValueDice = (double)(2 * instancesOverlap) / (double)(instancesOne + instancesTwo);
            
            
            if(simValueMin >= 0.2){
                Correspondence c = new Correspondence(entry.getKey());
                c.setConfidence(simValueMin);
                c.addAdditionalConfidence("simValueMin", simValueMin);
                c.addAdditionalConfidence("simValueDice", simValueDice);
                finalSchemaAlignmentMin.add(c);
            }
            
            if(simValueDice >= 0.2){
                Correspondence c = new Correspondence(entry.getKey());
                c.setConfidence(simValueDice);
                c.addAdditionalConfidence("simValueMin", simValueMin);
                c.addAdditionalConfidence("simValueDice", simValueDice);
                finalSchemaAlignmentDice.add(c);
            }
            
            //LOGGER.info("| {} | {} | {} | {}", c.getEntityOne(), c.getEntityTwo(), simValueMin, simValueDice);
        }
        
        finalSchemaAlignmentMin.serialize(new File("schemaAlignmentMin.xml"));
        finalSchemaAlignmentDice.serialize(new File("schemaAlignmentDice.xml"));
    }
    
    private static Set<String> getMatches(Alignment a, String element){
        Set<String> matches = new HashSet<>();
        for(Correspondence c : a.getCorrespondencesSource(element)){
            matches.add(c.getEntityTwo());
        }
        for(Correspondence c : a.getCorrespondencesTarget(element)){
            matches.add(c.getEntityOne());
        }
        return matches;
    }
    
    class ClassMatchInfo{
        private final Set<String> sourceInstances;
        private final Set<String> targetInstances;
        
        public ClassMatchInfo(){
            this.sourceInstances = new HashSet<>();
            this.targetInstances = new HashSet<>();
        }
        
        public void addInstanceMatch(String source, String target){
            sourceInstances.add(source);
            targetInstances.add(target);
        }
        
        public int getOverlap(){
            //in case of n:m instance mappings only the minimum amount of resource is the number of the intersection
            return Math.min(sourceInstances.size(), targetInstances.size());
        }
    }
}
