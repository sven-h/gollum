package de.uni_mannheim.informatik.dws.melt.gollumi.extractLinks;

import de.uni_mannheim.informatik.dws.gollum.extractLinks.AllWikisLinkExtractor;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AllWikiLinksExtractorTest {
    
    //@Test
    public void addToWikiLinksTest(){
        /*
        Alignment a = new Alignment();
        a.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/one");
        a.add("http://dbkwik.webdatacommons.org/bbwiki/resource/two", "http://dbkwik.webdatacommons.org/aawiki/resource/two");
        a.add("http://dbkwik.webdatacommons.org/aawiki/resource/three", "http://dbpedia.org/resource/three");
        a.add("http://dbkwik.webdatacommons.org/aawiki/resource/four", "http://dbpedia.org/resource/four");
        
        AllWikisLinkExtractor ex = new AllWikisLinkExtractor(new File("./"));
        ex.addToWikiLinks(a, new HashMap<>());
        */
    }
    
    @Test
    public void resolveProblematicSameAsSetTest(){
        Alignment a = new Alignment();
        a.add("http://dbpedia.org/resource/South_Korea", "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/South_Korea", 0.5);
        a.add("http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Korea", "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/South_Korea", 0.5);
        a.add("http://dbpedia.org/resource/South_Korea", "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/South_Korea", 0.5);
        a.add("http://dbpedia.org/resource/Korea", "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Korea", 0.5);
        
        Alignment toBeRemoved = AllWikisLinkExtractor.resolveProblematicSameAsSet(a, getAllSourcesAndTargets(a));
        
        assertEquals(1, toBeRemoved.size());
        
        assertTrue(toBeRemoved.contains(new Correspondence(
                "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Korea", 
                "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/South_Korea")
        ));
    }
    
    @Test
    public void resolveProblematicSameAsSetTestTwo(){
        //real example
        Alignment a = new Alignment();
        a.add("http://dbpedia.org/resource/South_Korea", "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/South_Korea", 0.5);
        a.add("http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Korea", "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/South_Korea", 0.5);
        a.add("http://dbpedia.org/resource/South_Korea", "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/South_Korea", 0.5);
        a.add("http://dbpedia.org/resource/Korea", "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Korea", 0.5);
        
        a.add("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/Korea", "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/South_Korea", 0.5);
        a.add("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/Korea", "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/South_Korea", 0.5);
        Alignment toBeRemoved = AllWikisLinkExtractor.resolveProblematicSameAsSet(a, getAllSourcesAndTargets(a));
        
        assertEquals(1, toBeRemoved.size());
        
        assertTrue(toBeRemoved.contains(new Correspondence(
                "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Korea", 
                "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/South_Korea")
        ));
    }
    /*
    @Test
    public void resolveProblematicSameAsSetTestThree(){
        Alignment a = new Alignment();
        a.add("http://dbkwik.webdatacommons.org/one/resource/A", "http://dbkwik.webdatacommons.org/two/resource/B", 0.5);
        a.add("http://dbkwik.webdatacommons.org/one/resource/A", "http://dbkwik.webdatacommons.org/three/resource/C", 0.5);
        a.add("http://dbkwik.webdatacommons.org/two/resource/B", "http://dbkwik.webdatacommons.org/three/resource/C", 0.5);
        a.add("http://dbkwik.webdatacommons.org/three/resource/C", "http://dbkwik.webdatacommons.org/one/resource/D", 0.5);
        
        Alignment toBeRemoved = AllWikisLinkExtractor.resolveProblematicSameAsSet(a, getAllSourcesAndTargets(a));
        
        assertEquals(1, toBeRemoved.size());
        
        assertTrue(toBeRemoved.contains(new Correspondence("http://dbkwik.webdatacommons.org/three/resource/C", "http://dbkwik.webdatacommons.org/one/resource/D")));
    }
    */
    
    
    @Test
    public void resolveProblematicSameAsSetTestFour(){
        Alignment a = new Alignment();
        a.add("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/Commandant", "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/Commandant", 0.5);
        a.add("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/Commandant", "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Commandant_of_Starfleet_Academy", 0.5);
        a.add("http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Commandant", "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/Commandant", 0.5);
        
        Alignment toBeRemoved = AllWikisLinkExtractor.resolveProblematicSameAsSet(a, getAllSourcesAndTargets(a));
        
        assertEquals(1, toBeRemoved.size());
        assertTrue(toBeRemoved.contains(new Correspondence("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/Commandant", "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Commandant_of_Starfleet_Academy")));
    }
    
    @Test
    public void resolveProblematicSameAsSetTestFive(){
        Alignment a = new Alignment();
        a.add("http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Chicago", "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/Chicago", 0.5);
        a.add("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/Chicago", "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/Chicago", 0.5);
        a.add("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/Chicago", "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Chicago", 1.0);
        a.add("http://dbpedia.org/resource/Chicago", "http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/Chicago", 0.5);
        a.add("http://dbpedia.org/resource/Chicago,_Illinois", "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Chicago", 0.5);
        
        Alignment toBeRemoved = AllWikisLinkExtractor.resolveProblematicSameAsSet(a, getAllSourcesAndTargets(a));
        
        assertEquals(1, toBeRemoved.size());
        assertTrue(toBeRemoved.contains(new Correspondence("http://dbpedia.org/resource/Chicago,_Illinois", "http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Chicago")));
    }
    
    private Set<String> getAllSourcesAndTargets(Alignment a){
        Set<String> result = new HashSet<>();
        result.addAll(a.getDistinctSourcesAsSet());
        result.addAll(a.getDistinctTargetsAsSet());
        return result;
    }
    
    //@Test
    public void ensureInjectivityTest(){
        Alignment alignment = new Alignment();
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/a");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/b");
                
        AllWikisLinkExtractor.ensureInjectivity(alignment);
        assertTrue(alignment.isEmpty());
        
        alignment = new Alignment();
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/a", "http://dbkwik.webdatacommons.org/bbwiki/resource/one");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/b", "http://dbkwik.webdatacommons.org/bbwiki/resource/one");
        
        AllWikisLinkExtractor.ensureInjectivity(alignment);
        assertTrue(alignment.isEmpty());
        
        alignment = new Alignment();
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/one");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/a");
        AllWikisLinkExtractor.ensureInjectivity(alignment);
        assertEquals(1, alignment.size());
        
        alignment = new Alignment();
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/one");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/a", "http://dbkwik.webdatacommons.org/bbwiki/resource/one");
        AllWikisLinkExtractor.ensureInjectivity(alignment);
        assertEquals(1, alignment.size());
        
        
        alignment = new Alignment();
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/a");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/b");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/two", "http://dbkwik.webdatacommons.org/bbwiki/resource/c");
        AllWikisLinkExtractor.ensureInjectivity(alignment);
        assertEquals(1, alignment.size());
        
        
        alignment = new Alignment();
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/one");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/a");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/b");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/two", "http://dbkwik.webdatacommons.org/bbwiki/resource/c");
        AllWikisLinkExtractor.ensureInjectivity(alignment);
        assertEquals(2, alignment.size());
        
        
        alignment = new Alignment();
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/one");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/a");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/one", "http://dbkwik.webdatacommons.org/bbwiki/resource/b");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/a", "http://dbkwik.webdatacommons.org/bbwiki/resource/a");
        alignment.add("http://dbkwik.webdatacommons.org/aawiki/resource/two", "http://dbkwik.webdatacommons.org/bbwiki/resource/a");
        AllWikisLinkExtractor.ensureInjectivity(alignment);
        assertEquals(2, alignment.size());
    }
}
