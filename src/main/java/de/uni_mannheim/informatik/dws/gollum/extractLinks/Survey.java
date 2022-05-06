/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_mannheim.informatik.dws.gollum.extractLinks;

import com.googlecode.cqengine.query.QueryFactory;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.filter.ConceptType;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


public class Survey {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Survey.class);
    
    public static void main(String[] args) throws SAXException, IOException{
        //int numberOfTaskPerHit = 10;
        //generateTemplate(numberOfTaskPerHit, new File("./mturk_template.html"));
        //Alignment a = new Alignment(new File("known_wikis_alignment.xml"));
        //writeAlignment(a.sample(25), new File("./mturk_out.csv"), numberOfTaskPerHit);
        //replaceValues(new File("./mturk_template.html"), new File("./mturk_out.csv"), new File("./mturk_replaced.html"));
        
        //writeSimpleCSV(new Alignment(new File("./final/unsupervised_direct.xml")),
        //        new Alignment(new File("./final/unsupervised_transitive.xml")),
        //        new File("./final/survey.csv"));
        writeSimpleCSVForSchema(new Alignment(new File("./final/unsupervised_schemaAlignmentMin.xml")), new File("./final/schema_survey.csv"));
    }    
    
    private static void writeSimpleCSVForSchema(Alignment schemaAlignment, File out) throws IOException{
        
        Alignment clazz = new Alignment();
        Alignment prop = new Alignment();
        for(Correspondence c : schemaAlignment){
            ConceptType t = DBkWikUtil.getConceptType(c);
            if(t.equals(ConceptType.RDF_PROPERTY)){
                prop.add(c);
            }else if(t.equals(ConceptType.CLASS)){
                clazz.add(c);
            }
        }
        int sampleClazz = sampleSize(clazz.size(), 0.9, 0.1);
        int sampleProp = sampleSize(prop.size(), 0.9, 0.1);
        LOGGER.info("class:{}/{} properties:{}/{}", 
                sampleClazz, clazz.size(), 
                sampleProp, prop.size());
        
        List<List<String>> records = new ArrayList<>();        
        for(Correspondence c : clazz.sample(sampleClazz)){
            records.add(Arrays.asList("class", c.getEntityOne(), c.getEntityTwo()));
        }        
        for(Correspondence c : prop.sample(sampleProp)){
            records.add(Arrays.asList("prop", c.getEntityOne(), c.getEntityTwo()));
        }
        
        try(CSVPrinter csvPrinter = CSVFormat.DEFAULT.print(out, StandardCharsets.UTF_8)){
            for(List<String> record : records){
                csvPrinter.printRecord(record);
            }
        }
    }
    private static void writeSimpleCSV(Alignment alignmentDirect, Alignment alignmentTransitive, File out) throws IOException{
        
        Alignment bothWays = new Alignment(alignmentDirect.retrieve(QueryFactory.equal(Correspondence.CONFIDENCE, 1.0d)));
        Alignment oneWay = new Alignment(alignmentDirect.retrieve(QueryFactory.equal(Correspondence.CONFIDENCE, 0.5d)));
        
        Alignment transAboveFour = new Alignment(alignmentTransitive.retrieve(QueryFactory.greaterThan(Correspondence.CONFIDENCE, 0.4d)));
        Alignment transEqualFour = new Alignment(alignmentTransitive.retrieve(QueryFactory.equal(Correspondence.CONFIDENCE, 0.4d)));
        Alignment transBelowFour = new Alignment(alignmentTransitive.retrieve(QueryFactory.lessThan(Correspondence.CONFIDENCE, 0.4d)));
        
        int sampleBothWays = sampleSize(bothWays.size(), 0.9, 0.1);
        int sampleOneWay = sampleSize(oneWay.size(), 0.9, 0.1);
        int sampleTransAboveFour = sampleSize(transAboveFour.size(), 0.9, 0.1);
        int sampleTransEqualFour = sampleSize(transEqualFour.size(), 0.9, 0.1);
        int sampleTransBelowFour = sampleSize(transBelowFour.size(), 0.9, 0.1);
        
        LOGGER.info("bothWays:{}/{} oneWay:{}/{} abovefour:{}/{} equalfour:{}/{} belowfour:{}/{}", 
                sampleBothWays, bothWays.size(), 
                sampleOneWay, oneWay.size(),
                sampleTransAboveFour, transAboveFour.size(),
                sampleTransEqualFour, transEqualFour.size(),
                sampleTransBelowFour, transBelowFour.size());
        
        List<List<String>> records = new ArrayList<>();
        
        for(Correspondence c : bothWays.sample(sampleBothWays)){
            records.add(Arrays.asList("both", getCSVHyperlink(c.getEntityOne()), getCSVHyperlink(c.getEntityTwo())));
        }        
        for(Correspondence c : oneWay.sample(sampleOneWay)){
            records.add(Arrays.asList("one", getCSVHyperlink(c.getEntityOne()), getCSVHyperlink(c.getEntityTwo())));
        }        
        for(Correspondence c : transAboveFour.sample(sampleTransAboveFour)){
            records.add(Arrays.asList("transAboveFour", getCSVHyperlink(c.getEntityOne()), getCSVHyperlink(c.getEntityTwo())));
        }
        for(Correspondence c : transEqualFour.sample(sampleTransEqualFour)){
            records.add(Arrays.asList("transEqualFour", getCSVHyperlink(c.getEntityOne()), getCSVHyperlink(c.getEntityTwo())));
        }
        for(Correspondence c : transBelowFour.sample(sampleTransBelowFour)){
            records.add(Arrays.asList("transBelowFour", getCSVHyperlink(c.getEntityOne()), getCSVHyperlink(c.getEntityTwo())));
        }
        Collections.shuffle(records);
        
        try(CSVPrinter csvPrinter = CSVFormat.DEFAULT.print(out, StandardCharsets.UTF_8)){
            for(List<String> record : records){
                csvPrinter.printRecord(record);
            }
        }
    }
    private static String getCSVHyperlink(String url){
        String real = DBkWikUtil.getRealURL(url);
        return "=HYPERLINK(\"" + real + "\")";
    }
    
    private static void writeAlignment(Alignment alignment, File out, int matchesPerHit) throws IOException{
        
        try(CSVPrinter csvPrinter = CSVFormat.DEFAULT.print(out, StandardCharsets.UTF_8)){
            List<String> header = new ArrayList<>();
            for(int i=1; i <= matchesPerHit; i++){
                header.add("left_resource_" + i);
                header.add("right_resource_" + i);
                header.add("left_url_" + i);
                header.add("right_url_" + i);
            }
            csvPrinter.printRecord(header);
            
            int counter = 0;
            List<Object> record = new ArrayList<>();
            for(Correspondence cell : alignment){
                record.add(cell.getEntityOne());
                record.add(cell.getEntityTwo());
                record.add(DBkWikUtil.getRealURL(cell.getEntityOne()));
                record.add(DBkWikUtil.getRealURL(cell.getEntityTwo()));
                counter++;
                
                if(counter >= matchesPerHit){
                     csvPrinter.printRecord(record);
                     record = new ArrayList<>();
                     counter = 0;
                }
            }
            if(record.isEmpty() == false){
                LOGGER.info("number of correspondences does not directly fit to hits");
                csvPrinter.printRecord(record);
            }
        }
    }
    
    private static void replaceValues(File template, File csvFile, File out) throws IOException{
        String fileContent = new String(Files.readAllBytes(template.toPath()), "UTF-8");
        for (CSVRecord row : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(csvFile))) {
            for(Entry<String, String> entry : row.toMap().entrySet()){
                fileContent = fileContent.replace("${" + entry.getKey() + "}", entry.getValue());
            }
            break;
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)){
            writer.write(fileContent);
        }
    }
    
    private static void generateTemplate(int matchesPerHit, File out){
        VelocityContext context = new VelocityContext();
        context.put("matches_per_hit", matchesPerHit);
        context.put("d", "$");
        
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loaders", "classpath");
        velocityEngine.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());  
        velocityEngine.init();
        
        Template template = velocityEngine.getTemplate("survey.vm");
        try(Writer writer = new FileWriter(out)){
            template.merge( context, writer );
        } catch (IOException ex) {
            LOGGER.error("Could not write to file.", ex);
        }
    }
    
    
    
    
    
   /**
    * Computes sample size based on confidence of 85%, 90%, 95%, 97% or 99%
    * From <a href="https://gist.github.com/scarton/40ff03eee1f505e95cdceedf27271520">this gist</a>.
    * E.g., sampleSize(650000,.95,.03) -> sample size of 1,066
    * </p>
    * @param populationSize - overall population size
    * @param confidence - confidence - 85%, 90%, 95%, 97% or 99% - uses 95%  if anything else is sent.
    * @param error - Margin of error.
    * @return - the number of samples.
    */
    private static int sampleSize(long populationSize, double confidence, double error) {
        double z = confidence==0.85?1.44:confidence==.90?1.65:confidence==.95?1.96:confidence==.97?2.17:confidence==.99?2.58:1.96;
        double n = 0.25*Math.pow((z/error), 2);
        double size = Math.ceil((populationSize*n)/(n+populationSize-1));
        return (int)(size+.5);
   }
}
