package de.uni_mannheim.informatik.dws.melt.gollumi.extractLinks;

import de.uni_mannheim.informatik.dws.gollum.extractLinks.AlignmentGraph;
import de.uni_mannheim.informatik.dws.gollum.extractLinks.DBkWikUtil;
import com.googlecode.cqengine.query.QueryFactory;
import com.googlecode.cqengine.query.option.DeduplicationStrategy;
import com.googlecode.cqengine.resultset.ResultSet;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.DotGraphUtil;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.TransitiveClosure;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.CorrespondenceRelation;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.nd4j.shade.guava.io.Files;
import org.xml.sax.SAXException;

public class AlignmentGraphTest {
    
    //@Test
    public void getMaximumFlowTest(){        
        Alignment a = new Alignment();
        a.add("A", "B", 1.0);
        a.add("A", "C", 1.0);
        
        a.add("B", "D", 1.0);
        a.add("C", "D", 1.0);
        
        
        AlignmentGraph g = new AlignmentGraph(a);        
        assertEquals(2.0, g.getMaximumFlow("A", "D"));
        
        a.remove(new Correspondence("A", "C"));
        
        g = new AlignmentGraph(a);        
        assertEquals(1.0, g.getMaximumFlow("A", "D"));
    }
    
    
    //@Test
    public void getShortestPathWeightTest(){        
        Alignment a = new Alignment();
        a.add("A", "B", 0.5);
        a.add("B", "C", 0.5);
        a.add("C", "D", 0.5);
        a.add("D", "E", 0.5);
        
        AlignmentGraph g = new AlignmentGraph(a);        
        assertEquals(2.0, g.getShortestPathWeight("A", "E"));
        
        a = new Alignment();
        a.add("A", "B", 1.0);
        a.add("B", "C", 1.0);
        a.add("C", "D", 1.0);
        a.add("D", "E", 1.0);
        g = new AlignmentGraph(a);     
        assertEquals(4.0, g.getShortestPathWeight("A", "E"));
    }
    
    
    public static void main(String[] args) throws IOException, SAXException{
        Alignment alignment = new Alignment(new File("all_wikis_alignment.xml"));
        TransitiveClosure<String> closure = new TransitiveClosure<>();
        for(Correspondence c : alignment){
            closure.add(c.getEntityOne(), c.getEntityTwo());
        }
        Alignment closureAlignment = new Alignment(alignment);
        
        try(BufferedWriter w = Files.newWriter(new File("comparison.csv"), StandardCharsets.UTF_8)){
            for(Set<String> sameEntities : closure.getClosure()){
                if(sameEntities.size() <= 2)
                    continue;
                if(sameEntities.contains("http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/Rigel_class") == false)
                    continue;
                
                List<Map.Entry<String, String>> entities = new ArrayList<>(sameEntities.size());
                for(String s : sameEntities){
                    entities.add(new AbstractMap.SimpleEntry<>(DBkWikUtil.getWikiName(s), s));
                }
                double confidence = 1.0d / (double)sameEntities.size();
                entities.sort(Comparator.comparing(entry -> entry.getKey()));
                
                Alignment subset = getSubset(alignment, sameEntities);
                AlignmentGraph subsetGraph = new AlignmentGraph(subset);
                
                //boolean written = false;
                System.out.println("graph problematicSameAsSet {");
                for(int i=0; i < entities.size(); i++){
                    Map.Entry<String, String> first = entities.get(i);
                    for(int j=i+1; j < entities.size(); j++){
                        Map.Entry<String, String> second = entities.get(j);
                        
                        
                        if(alignment.contains(new Correspondence(first.getValue(), second.getValue())) == false){
                            //written = true;
                            /*
                            w.write(first.getValue() + ";" + 
                                    second.getValue() + ";" +
                                    confidence + ";" + 
                                    subsetGraph.getMaximumFlow(first.getValue(), second.getValue()) + ";" + 
                                    subsetGraph.getShortestPathWeight(first.getValue(), second.getValue()) + ";" + 
                                    
                                    subsetGraph.getFlowValue(first.getValue(), second.getValue()) + ";" + 
                                    subsetGraph.getPathValue(first.getValue(), second.getValue()) + ";" + 
                                    
                                    subsetGraph.getConnectivity(first.getValue(), second.getValue())+ ";" + 
                                    subsetGraph.getConnectivityAverage(first.getValue(), second.getValue())
                                    );
                            w.newLine();
                            */
                            
                            
                            
                            //written = true;
                            //w.write(first.getValue() + ";" + 
                            //        second.getValue() + ";" +
                            //        confidence + ";" + 
                            //        subsetGraph.getMaximumFlow(first.getValue(), second.getValue()) + ";" + 
                            //        subsetGraph.getShortestPathWeight(first.getValue(), second.getValue()) + ";" + 
                            //        subsetGraph.getConnectivity(first.getValue(), second.getValue()));
                            //w.newLine();
                            System.out.println("  " + DotGraphUtil.makeQuotedNodeID(first.getValue()) + " -- " + DotGraphUtil.makeQuotedNodeID(second.getValue()) + 
                                    " [ label=\"closure:" + String.format("%.2f", confidence) + ";" + 
                                            subsetGraph.getMaximumFlow(first.getValue(), second.getValue()) + ";" + 
                                            subsetGraph.getShortestPathWeight(first.getValue(), second.getValue()) + ";" + 
                                            subsetGraph.getConnectivity(first.getValue(), second.getValue()) + "\"];");
                        }else{
                            double weight = subset.getCorrespondence(first.getValue(), second.getValue(), CorrespondenceRelation.EQUIVALENCE).getConfidence();
                            System.out.println("  " + DotGraphUtil.makeQuotedNodeID(first.getValue()) + " -- " + DotGraphUtil.makeQuotedNodeID(second.getValue()) + 
                                    " [ label=\"" + String.format("%.2f", weight) + "\" color=\"red\"];");
                        }
                    }
                }
                //System.out.println("}");
                //if(written){
                //    w.newLine();w.newLine();
                //}
            }
        }
    }
    
    private static Alignment getSubset(Alignment alignment, Set<String> entities){
        //retrive all important links:
        ResultSet<Correspondence> result = alignment.retrieve(
                QueryFactory.or(
                    QueryFactory.in(Correspondence.SOURCE, entities),
                    QueryFactory.in(Correspondence.TARGET, entities)
                ),
                QueryFactory.queryOptions(QueryFactory.deduplicate(DeduplicationStrategy.MATERIALIZE))
        );
        Alignment reducedAlignment = new Alignment();
        for(Correspondence c : result){
            reducedAlignment.add(new Correspondence(c));
        }
        return reducedAlignment;
    }
}
