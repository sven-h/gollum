package de.uni_mannheim.informatik.dws.gollum.run40k;

import de.uni_mannheim.informatik.dws.melt.matching_base.FileUtil;
import de.uni_mannheim.informatik.dws.melt.matching_base.ParameterConfigKeys;
import de.uni_mannheim.informatik.dws.melt.matching_base.typetransformer.AlignmentAndParameters;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.multisource.dispatchers.CopyMode;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.multisource.dispatchers.MergeOrder;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.multisource.dispatchers.MultiSourceDispatcherIncrementalMergeByClusterText;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.multisource.dispatchers.clustermerge.ClusterDistance;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.multisource.dispatchers.clustermerge.ClusterLinkage;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.multisource.dispatchers.clustermerge.Clusterer;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.multisource.dispatchers.clustermerge.ClustererSmile;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.tdb.TDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run the OAEI matcher on full KG dataset
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    
    
    public static void main(String[] args) throws MalformedURLException{
        Options options = new Options();

        options.addOption(Option.builder("i")
                .longOpt("input")
                .hasArg()
                .required()
                .desc("file or folder to process as input. The file/folder should contain tar.gz files.")
                .build());
        
        options.addOption(Option.builder("l")
                .longOpt("language")
                .hasArg()
                .desc("language which should be used like en (can be comma separated).")
                .build());
        
        options.addOption(Option.builder("s")
                .longOpt("size")
                .hasArg()
                .desc("the minimal size in KB of the files to be used.")
                .build());
        
        options.addOption(Option.builder("k")
                .longOpt("topk")
                .hasArg()
                .desc("the top k files to process (the files are sorted by size descending).")
                .build());
        
        options.addOption(Option.builder("t")
                .longOpt("tmpFolder")
                .hasArg()
                .desc("Pointing to the folder for temporary files like TDB storage etc. If not given, point to the systems tmp folder.")
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
        
        File[] filesToProcess = getFiles(cmd);
        LOGGER.info("Process {} files.", filesToProcess.length);
        
        //runMatcher(filesToProcess, cmd);
        //runSimilarityTree(filesToProcess);
        
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./filenames.txt"), StandardCharsets.UTF_8))){
            for(File f : filesToProcess){
                bw.write(f.getName());
                bw.newLine();
            }
        } catch (IOException ex) {
            LOGGER.warn("IO Error", ex);
        }
    }
    
    private static File[] getFiles(CommandLine cmd){
        File inputFile = new File(cmd.getOptionValue("input"));
        if(inputFile.isDirectory() == false){
            LOGGER.warn("input is not a directory or does not exist: {}", inputFile);
            return new File[0];
        }
        
        long sizebyte = -1;        
        String s = cmd.getOptionValue("size");
        if(s != null){
            try{
                int sizeKB = Integer.parseInt(s.trim());
                sizebyte = (long)sizeKB * 1000L;
            }catch(NumberFormatException ex){
                LOGGER.warn("Parameter size is not a number: {}", s);
                return new File[0];
            }
        }
        final long finalSizebyte = sizebyte;
        
        final Set<String> languages = new HashSet<>();
        String l = cmd.getOptionValue("language");
        if(l != null){
            for(String lang : l.trim().split(",")){
                languages.add(lang.trim());
            }
        }
        
        File[] files = inputFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if(finalSizebyte > 0){
                    if(f.length() < finalSizebyte)
                        return false;
                }
                if(languages.isEmpty() == false){
                    String[] infos = f.getName().split("~");
                    if(infos.length < 1){
                        return false;
                    }
                    if(languages.contains(infos[1]) == false){
                        return false;
                    }
                }
                return true;
            }
        });
        if(files.length == 0){
            return files;
        }
        
        //apply top k filter.
        String topkText = cmd.getOptionValue("topk");
        if(topkText != null){
            int topK;
            try{
                topK = Integer.parseInt(topkText.trim());
            }catch(NumberFormatException ex){
                LOGGER.warn("input is not a directory or does not exist: {}", inputFile);
                return new File[0];
            }
            Arrays.sort(files, (File o1, File o2) -> Long.compare(o2.length(), o1.length())); // sort descending
            files = Arrays.copyOfRange(files, 0, Math.min(topK, files.length));
        }
        Arrays.sort(files, (File o1, File o2) -> o1.getName().compareTo(o2.getName()));
        return files;
    }
    
    private static void runSimilarityTree(File[] files){
        List<Set<Object>> models = new ArrayList<>();        
        for(File f : files){
            try {
                models.add(new HashSet<>(Arrays.asList(f.toURI().toURL())));
            } catch (MalformedURLException ex) {
                LOGGER.warn("Could not add file URL to list of models", ex);
            }
        }
        Properties parameters = new Properties();
        parameters.put(ParameterConfigKeys.USE_ONTOLOGY_CACHE, false);
        
        MultiSourceDispatcherIncrementalMergeByClusterText imClusterText = new MultiSourceDispatcherIncrementalMergeByClusterText(null);
        imClusterText.setDebug(true);
        imClusterText.setMindf(0.001);
        imClusterText.setMaxdf(0.8);
        imClusterText.setRemoveUnusedJenaModels(true);
        double[][] clusterFeatures = imClusterText.getClusterFeatures(models, parameters);
        saveToFile(clusterFeatures, new File("./clusterFeatures.dat"));
        
        Clusterer clusterer = new ClustererSmile();
        
        MergeOrder order = clusterer.run(clusterFeatures, ClusterLinkage.SINGLE, ClusterDistance.EUCLIDEAN);
        order.serializeToFile(new File("./mergeOrderSingle.dat"));
        order.writeToFile(new File("./mergeOrderSingle_human.txt"));
        LOGGER.info("Single Linkage: cluster height: {}", order.getHeight());
        LOGGER.info("Single Linkage: parallel Executions: {}", order.getCountOfParallelExecutions());
        
        order = clusterer.run(clusterFeatures, ClusterLinkage.AVERAGE, ClusterDistance.EUCLIDEAN);
        order.serializeToFile(new File("./mergeOrderAverage.dat"));
        order.writeToFile(new File("./mergeOrderAverage_human.txt"));
        LOGGER.info("Average Linkage: cluster height: {}", order.getHeight());
        LOGGER.info("Average Linkage: parallel Executions: {}", order.getCountOfParallelExecutions());
        
        order = clusterer.run(clusterFeatures, ClusterLinkage.COMPLETE, ClusterDistance.EUCLIDEAN);
        order.serializeToFile(new File("./mergeOrderComplete.dat"));
        order.writeToFile(new File("./mergeOrderComplete_human.txt"));
        LOGGER.info("Complete Linkage: cluster height: {}", order.getHeight());
        LOGGER.info("Complete Linkage: parallel Executions: {}", order.getCountOfParallelExecutions());
    }
    
    private static void saveToFile(Object o, File f){
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))){
            oos.writeObject(o);
        } catch (IOException ex) {
            LOGGER.warn("Could not save data", ex);
        }
    }
    private static Object readFromFile(File f){
        try(ObjectInputStream iis = new ObjectInputStream(new FileInputStream(f))){
            return iis.readObject();
        } catch (Exception ex) {
            LOGGER.warn("Could not load data", ex);
            return new Object();
        }
    }
    
    private static void runMatcher(File[] files, CommandLine cmd){
        List<Set<Object>> models = new ArrayList<>();        
        for(File f : files){
            try {
                models.add(new HashSet<>(Arrays.asList(f.toURI().toURL())));
            } catch (MalformedURLException ex) {
                LOGGER.warn("Could not add file URL to list of models", ex);
            }
        }
        
        String tmpFolderText = cmd.getOptionValue("tmpFolder");
        if(tmpFolderText != null){
            FileUtil.setUserTmpFolder(new File(tmpFolderText));
        }
        
        //Object matcher = new SimpleMatcher(); // simple string matcher
        //Object matcher = new ALOD2VecMatcherOneOne();//Alod2VecMatcher
        
        MultiSourceDispatcherIncrementalMergeByClusterText multisourceMatcher = 
                //new MultiSourceDispatcherIncrementalMergeByClusterText(matcher);
                new MultiSourceDispatcherIncrementalMergeByClusterText(()->new ALOD2VecMatcherOneOne());
                //new MultiSourceDispatcherIncrementalMergeByOrder(matcher, MultiSourceDispatcherIncrementalMergeByOrder.MODEL_SIZE_DECENDING_NTRIPLES_FAST);
        
        multisourceMatcher.setLinkage(ClusterLinkage.COMPLETE);
        multisourceMatcher.setDistance(ClusterDistance.EUCLIDEAN);
        multisourceMatcher.setClusterer(new ClustererSmile(-1, 700, false));
        multisourceMatcher.setMindf(0.001);
        multisourceMatcher.setMaxdf(0.8);
        
        multisourceMatcher.setNumberOfThreads((int)(Runtime.getRuntime().availableProcessors() * 0.75));
        multisourceMatcher.setCopyMode(CopyMode.createTdbForLargeKg(400000));
        multisourceMatcher.setCacheFile(new File("./cacheMergeOrder.dat"));
        multisourceMatcher.setSerializedTreeFile(new File("mergeTree.txt"));
        multisourceMatcher.setRemoveUnusedJenaModels(true);
                
        Properties parameters = new Properties();
        parameters.put(ParameterConfigKeys.USE_ONTOLOGY_CACHE, false);
        try {
            AlignmentAndParameters alignmentParameters = multisourceMatcher.match(models, null, parameters);   
            alignmentParameters.getAlignment(Alignment.class).serialize(new File("alignment.xml"));
        } catch (Exception ex) {
            LOGGER.error("Error during matching", ex);
        }
        //https://jena.apache.org/documentation/javadoc/tdb/org/apache/jena/tdb/TDBFactory.html#release(org.apache.jena.query.Dataset)
        //TDBFactory.release(dataset);
    }
    
}
