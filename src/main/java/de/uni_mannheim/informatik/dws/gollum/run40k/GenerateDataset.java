package de.uni_mannheim.informatik.dws.gollum.run40k;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
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
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.jena.atlas.lib.CharSpace;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GenerateDataset {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateDataset.class);
    
    public static void main(String[] args) throws FileNotFoundException{
        cli(args);
        //runFolder(new File("C:\\dev\\dbkwik_two\\files\\extracted"), new File("./output"));
        
        /*
        File wiki = new File("C:\\dev\\dbkwik_two\\files\\extracted_example\\130547~en~justdance.tar.gz");
        File output = new File("C:\\dev\\dbkwik_two\\files\\extracted_example");
        createTDB(wiki, output);
        */
    }
    
    private static void cli(String[] args){
        Options options = new Options();

        options.addOption(Option.builder("i")
                .longOpt("input")
                .hasArg()
                .required()
                .desc("file or folder to process as input. The file/folder should contain tar.gz files.")
                .build());
        
        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArg()
                .required()
                .desc("path to output folder.")
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
        
        runFolder(new File(cmd.getOptionValue("input")), new File(cmd.getOptionValue("output")));
    }
    
    
    private static void runFolder(File inputFile, File outputDirectory){
        if(inputFile.exists() == false){
            LOGGER.warn("input file does not exist: {}", inputFile);
            return;
        }
        List<File> inputFileList = new ArrayList<>();
        if(inputFile.isDirectory()){
            inputFileList.addAll(Arrays.asList(inputFile.listFiles((File dir, String name) -> name.endsWith(".tar.gz"))));
        }else if(inputFile.getName().endsWith(".tar.gz")){
            inputFileList.add(inputFile);
        }
        
        if(inputFileList.isEmpty()){
            LOGGER.warn("No input files exists");
            return;
        }
        
        outputDirectory.mkdirs();
        if(outputDirectory.isDirectory() == false){
            LOGGER.warn("output is not a directory: {}", outputDirectory);
            return;
        }
        
        //process
        int inputFileListSize = inputFileList.size();
        int i = 1;
        for(File tarGzFile : inputFileList){
            LOGGER.info("Processing file {} ({}/{})", tarGzFile, i, inputFileListSize);
            //process one file
            createRdfFile(tarGzFile, outputDirectory);
            i++;
        }
    }
    
    
    private static final Set<String> FILE_TYPES = new HashSet<>(Arrays.asList(
            "anchor-text.ttl","article-categories.ttl","category-labels.ttl",
            "disambiguations.ttl", "external-links.ttl", "images.ttl",
            "infobox-properties.ttl", "infobox-property-definitions.ttl",
            "infobox-template-type.ttl", "infobox-template-type-definitions.ttl",
            "labels.ttl", "long-abstracts.ttl", "short-abstracts.ttl", 
            "skos-categories.ttl", "template-type.ttl", "template-type-definitions.ttl",
            "page-links.ttl"
        ));
    private static void createRdfFile(File targzFile, File outputFolder){
        File ntripleFile = new File(outputFolder, targzFile.getName().substring(0, targzFile.getName().length() - 7) + ".nt");
        ntripleFile.getParentFile().mkdirs();
        
        StreamRDF writer;
        try {
            writer = StreamRDFWriter.getWriterStream(new FileOutputStream(ntripleFile), RDFFormat.NTRIPLES, null);
            /*
            RDFParserBuilder.create()
            //.checking(true)
            //.strict(true)
            .errorHandler(ErrorHandlerFactory.errorHandlerWarn)
            //.forceLang(Lang.NTRIPLES)
            .lang(Lang.NTRIPLES)
            .source("./bla.nt")
            .parse(writer);
             */
        } catch (FileNotFoundException ex) {
            LOGGER.error("Output file not found.", ex);
            return;
        }
        
        Set<String> notparseable = new HashSet<>();
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(targzFile)))){
            TarArchiveEntry tarEntry;
            while ((tarEntry = tarInput.getNextTarEntry()) != null) {
                String[] splitted = tarEntry.getName().split("-");
                if(splitted.length < 2){
                    continue;
                }
                String lastPart = String.join("-", Arrays.copyOfRange(splitted, 2, splitted.length));
                if(FILE_TYPES.contains(lastPart)){
                    //tarInput
                    try{
                        RDFParserBuilder.create()
                                    .errorHandler(ErrorHandlerFactory.errorHandlerWarning(ErrorHandlerFactory.noLogger))
                                    //.errorHandler(ErrorHandlerFactory.errorHandlerWarn)
                                    .lang(Lang.NTRIPLES)
                                    .source(new CloseShieldInputStream(tarInput))
                                    .parse(writer);
                    } catch(RiotException ex){
                        notparseable.add(lastPart);
                    }
                    /*
                    BufferedReader br = new BufferedReader(new InputStreamReader(tarInput));
                    //do not close the reader
                    String line = null;
                    int i = 0;
                    while ((line = br.readLine()) != null){
                        if(i % 10000 == 0)
                            LOGGER.info("Process line {}", i);
                        i++;
                        try{
                            RDFParserBuilder.create()
                                    //.checking(true)
                                    //.strict(true)
                                    .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                                    //.forceLang(Lang.NTRIPLES)
                                    .lang(Lang.NTRIPLES)
                                    .fromString(line)
                                    .parse(sink);
                        }catch(RiotException ex){} //skip
                    }
                    */
                }
            }
        } catch (IOException ex) {
            LOGGER.info("Could not read file " + targzFile.getName(), ex);
        }
        
        if(notparseable.isEmpty() == false){
            LOGGER.info("Not parsable: {}", notparseable);
            createRdfFile(targzFile, outputFolder, notparseable);
        }
    }
    
    
    
    private static void createRdfFile(File targzFile, File outputFolder, Set<String> notParseable){
        File ntripleFile = new File(outputFolder, targzFile.getName().substring(0, targzFile.getName().length() - 7) + ".nt");
        ntripleFile.getParentFile().mkdirs();
        
        StreamRDF writer;
        try {
            writer = StreamRDFWriter.getWriterStream(new FileOutputStream(ntripleFile), RDFFormat.NTRIPLES, null);
            /*
            RDFParserBuilder.create()
            //.checking(true)
            //.strict(true)
            .errorHandler(ErrorHandlerFactory.errorHandlerWarn)
            //.forceLang(Lang.NTRIPLES)
            .lang(Lang.NTRIPLES)
            .source("./bla.nt")
            .parse(writer);
             */
        } catch (FileNotFoundException ex) {
            LOGGER.error("Output file not found.", ex);
            return;
        }
        
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(targzFile)))){
            TarArchiveEntry tarEntry;
            while ((tarEntry = tarInput.getNextTarEntry()) != null) {
                String[] splitted = tarEntry.getName().split("-");
                if(splitted.length < 2){
                    continue;
                }
                String lastPart = String.join("-", Arrays.copyOfRange(splitted, 2, splitted.length));
                if(FILE_TYPES.contains(lastPart)){
                    
                    if(notParseable.contains(lastPart)){
                        //do it linewise
                        BufferedReader br = new BufferedReader(new InputStreamReader(tarInput));
                        //do not close the reader
                        String line = null;
                        while ((line = br.readLine()) != null){
                            try{
                                RDFParserBuilder.create()
                                        //.checking(true)
                                        //.strict(true)
                                        .errorHandler(ErrorHandlerFactory.errorHandlerWarning(ErrorHandlerFactory.noLogger))
                                        //.errorHandler(ErrorHandlerFactory.errorHandlerWarn)
                                        //.forceLang(Lang.NTRIPLES)
                                        .lang(Lang.NTRIPLES)
                                        .fromString(line)
                                        .parse(writer);
                            }catch(RiotException ex){} //skip
                        }
                    }else{
                        //do it normal
                        RDFParserBuilder.create()
                                    .errorHandler(ErrorHandlerFactory.errorHandlerWarning(ErrorHandlerFactory.noLogger))
                                    //.errorHandler(ErrorHandlerFactory.errorHandlerWarn)
                                    .lang(Lang.NTRIPLES)
                                    .source(new CloseShieldInputStream(tarInput))
                                    .parse(writer);
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.info("Could not read file " + targzFile.getName(), ex);
        }
    }
}
