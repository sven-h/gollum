package de.uni_mannheim.informatik.dws.gollum.extractLinks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


public class CountEntities {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CountEntities.class);
    
    public static void main(String[] args) throws SAXException, IOException{
        
        List<File> files = new ArrayList<>();

        File inputDir = new File(args[0]);
        if(inputDir.isDirectory() == false){
            LOGGER.warn("input is not a directory or does not exist: {}", inputDir);
            return;
        }
        files.addAll(Arrays.asList(inputDir.listFiles((File dir, String name) -> name.endsWith("tar.gz"))));
        Set<String> entities = new HashSet<>();
        for(File tarGzFileWikiFile : files){
            try (TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tarGzFileWikiFile))))){
                TarArchiveEntry tarEntry;
                while ((tarEntry = tarInput.getNextTarEntry()) != null) {
                    String[] splitted = tarEntry.getName().split("-");
                    String lastPart = String.join("-", Arrays.copyOfRange(splitted, 2, splitted.length));
                    switch(lastPart){
                        case "labels.ttl":{
                            NxParser nxp = new NxParser();
                            nxp.parse(tarInput, StandardCharsets.UTF_8);      
                            for (Node[] nx : nxp) {
                                entities.add(nx[0].getLabel());
                            }
                            break;
                        }
                    }
                }
            } catch (IOException ex) {
                LOGGER.info("Could not read file " + tarGzFileWikiFile.getName(), ex);
            }
        }
        
        LOGGER.info("entities: {} ", entities.size());
    }
}
