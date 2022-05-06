package de.uni_mannheim.informatik.dws.gollum.extractLinks;

import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.jgrapht.Graph;
import org.jgrapht.alg.flow.EdmondsKarpMFImpl;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * Graph util methods based on alignments.
 */
public class AlignmentGraph {
    
    private Graph<String, DefaultWeightedEdge> graph;
    private EdmondsKarpMFImpl<String, DefaultWeightedEdge> maxFlow;
    private ShortestPathAlgorithm<String, DefaultWeightedEdge> shortestPath;
    
    public AlignmentGraph(Alignment a){
        this.graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for(Correspondence c : a){
            this.graph.addVertex(c.getEntityOne());
            this.graph.addVertex(c.getEntityTwo());
            this.graph.addEdge(c.getEntityOne(), c.getEntityTwo());
            this.graph.setEdgeWeight(c.getEntityOne(), c.getEntityTwo(), c.getConfidence());
        }
        
        this.maxFlow = new EdmondsKarpMFImpl<>(graph);
        
        Graph<String, DefaultWeightedEdge> reverse = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for(Correspondence c : a){
            reverse.addVertex(c.getEntityOne());
            reverse.addVertex(c.getEntityTwo());
            reverse.addEdge(c.getEntityOne(), c.getEntityTwo());
            //switch confidence for shortest path (confidence is only 0.5 or 1.0)
            double conf = 1.0;
            if(c.getConfidence() == 1.0)
                conf = 0.5;
            reverse.setEdgeWeight(c.getEntityOne(), c.getEntityTwo(), conf);
        }
        this.shortestPath = new DijkstraShortestPath<>(reverse); //new FloydWarshallShortestPaths<>(reverse);
    }
    
    public double getMaximumFlow(String source, String target){
        return this.maxFlow.calculateMaximumFlow(source, target);
    }
    
    public double getShortestPathWeight(String source, String target){
        return this.shortestPath.getPathWeight(source, target);
    }
    
    public double getConnectivity(String source, String target){
        double flow = getMaximumFlow(source, target);
        double path = getShortestPathWeight(source, target);
        
        double pathValue = 1.0d / path;
        double flowValue = 1.0d - (1.0d/(1.0d + flow));  
        
        double harmonicMean = (2.0d * pathValue * flowValue) / (pathValue + flowValue);        
        return harmonicMean;
    }
}
