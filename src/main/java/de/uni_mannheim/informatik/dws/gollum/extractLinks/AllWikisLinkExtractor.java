package de.uni_mannheim.informatik.dws.gollum.extractLinks;

import com.googlecode.cqengine.query.QueryFactory;
import com.googlecode.cqengine.query.option.DeduplicationStrategy;
import com.googlecode.cqengine.resultset.ResultSet;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.multisource.clustering.ComputeErrDegree;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.StringProcessing;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.TransitiveClosure;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.AlignmentSerializer;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.CorrespondenceRelation;
import it.uniroma1.lcl.jlt.util.Files;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.simmetrics.metrics.Levenshtein;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AllWikisLinkExtractor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AllWikisLinkExtractor.class);
    
    private Set<String> allPages;
    private Set<String> disambiguationPages;
    private Map<String, String> redirects;
    private Alignment bestInterWikiLinks;
    
    private Set<String> internalWikiNames; //wiki names which we parsed and where we have redirects etc (this excludes external wiki which are not part of wikia.com etc)
    
    public AllWikisLinkExtractor(Iterable<File> tarGzFiles){
        this.allPages = new HashSet<>();
        this.disambiguationPages = new HashSet<>();
        this.redirects = new HashMap<>();
        this.bestInterWikiLinks = new Alignment();
        this.internalWikiNames = new HashSet<>();
        
        for(File tarGzFile : tarGzFiles){
            WikiWithLinks wiki = new WikiWithLinks(tarGzFile);
            allPages.addAll(wiki.getAllPages());
            disambiguationPages.addAll(wiki.getDisambiguationPages());
            redirects.putAll(wiki.getRedirects());
            internalWikiNames.add(wiki.getWikiName());

            bestInterWikiLinks.addAll(wiki.getBestInterWikiLinks());            
            //addToWikiLinks(wiki.getBestInterWikiLinks());
        }
        
        writeAnalysisToFile(bestInterWikiLinks, "./alignment_analysis_01_after_extraction.txt");   
        
        LOGGER.info("resolve redirects, normalize, and ensure injectivity");
        bestInterWikiLinks = resolveRedirectsNormalizeAndEnsureInjectivity(bestInterWikiLinks, redirects);
        writeAnalysisToFile(bestInterWikiLinks, "./alignment_analysis_02_after_redirectsandinjectivity.txt");
        
        LOGGER.info("remove disambiguation");        
        bestInterWikiLinks = removeDisambiguationPages(bestInterWikiLinks, this.disambiguationPages);  
        writeAnalysisToFile(bestInterWikiLinks, "./alignment_analysis_03_after_disambiguation.txt");
        
        LOGGER.info("remove based on existing pages and hash links");
        bestInterWikiLinks = removeNonExistentPagesAndHashLinks(bestInterWikiLinks, this.allPages, this.internalWikiNames);
        writeAnalysisToFile(bestInterWikiLinks, "./alignment_analysis_04_after_non_existent_pages.txt");
        
        LOGGER.info("remove based on transitive closure");
        bestInterWikiLinks = removeCorrespondencesBasedOnTransitiveClosure(bestInterWikiLinks);
        writeAnalysisToFile(bestInterWikiLinks, "./alignment_analysis_05_after_remove_by_closure.txt");
        
        Alignment withClosure = addTransitiveClosure(bestInterWikiLinks);
        writeAnalysisToFile(withClosure, "./alignment_analysis_06_after_add_closure.txt");
        
        LOGGER.info("Write alignment to files");
        
        try{
            bestInterWikiLinks.serialize(new File("all_wikis_alignment.xml"));
            withClosure.serialize(new File("all_wikis_alignment_closure.xml"));
            restrictToKnownWikis(bestInterWikiLinks, this.internalWikiNames).serialize(new File("known_wikis_alignment.xml"));
            restrictToKnownWikis(withClosure, this.internalWikiNames).serialize(new File("known_wikis_alignment_closure.xml"));
        }catch(IOException e){
            LOGGER.warn("Could not write alignments to file", e);
        }
        
        //writeAlignmentToZipFile(bestInterWikiLinks, new File("all_wikis_alignment.zip"));
        //writeAlignmentToZipFile(withClosure, new File("all_wikis_alignment_closure.zip"));
        //writeAlignmentToZipFile(restrictToKnownWikis(bestInterWikiLinks, this.internalWikiNames), new File("known_wikis_alignment.zip"));
        //writeAlignmentToZipFile(restrictToKnownWikis(withClosure, this.internalWikiNames), new File("known_wikis_alignment_closure.zip"));
        
        LOGGER.info("finished");
        //addToWikiLinks();
        
        
        
        
        //TODO: check pages if existent and if not, make http call?
        //TODO: adjust confidence
        //TODO: check after closure ( introduce algorithm)
                
        /*
        Map<Entry<String, String>,Alignment> transitiveLinks = computeTransitiveClosure(interWikiLinks, this.bestInterWikiLinks);
        
        for(Entry<Entry<String, String>, Alignment> entry : interWikiLinks.entrySet()){
            Alignment original = transitiveLinks.get(entry.getKey());
            if(original == null){
                LOGGER.info("Did not find {}", entry.getKey());
                continue;
            }
                
            Alignment a = new Alignment(original, true);
            a.removeAll(entry.getValue());
            System.out.println("Diff to transitive links " + entry.getKey().toString() + " :");
            System.out.println(a.toStringMultiline());
        }
        */
        
        
        
        
        //bestInterWikiLinks
        
        //TODO: if one page links to multiple pages
        //BUT one has the same name, then use this, otherwise use none!!!!
        
        
        //TODO: remove links with # after all transitive etc
        
        
        
        
    }
    
    /*
    private static Map<Entry<String, String>,Alignment> reduceWrongMappingsByTransitiveClosure(Map<Entry<String, String>,Alignment> map){
        
    }
    */
    
    
    private static void writeAlignmentToZipFile(Alignment alignment, File file){
        
        Map<Entry<String, String>,Alignment> wikiPairToAlignment = new HashMap<>();
        for(Correspondence c : alignment){            
            Entry<String, String> key = new SimpleEntry<>(DBkWikUtil.getWikiName(c.getEntityOne()), DBkWikUtil.getWikiName(c.getEntityTwo()));
            wikiPairToAlignment.computeIfAbsent(key, __-> new Alignment()).add(c);
        }
        
        try(ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(file)){
            for(Entry<Entry<String, String>, Alignment> entry : wikiPairToAlignment.entrySet()){
                //zipArchiveOutputStream
                String name = entry.getKey().getKey() + "-" + entry.getKey().getValue() + ".xml";
                zipArchiveOutputStream.putArchiveEntry(new ZipArchiveEntry(name));                
                AlignmentSerializer.serialize(entry.getValue(), zipArchiveOutputStream);
                zipArchiveOutputStream.closeArchiveEntry();
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not write alignment to zip file", ex);
        }
    }
    
    private static Alignment restrictToKnownWikis(Alignment alignment, Set<String> internalWikiNames){
        Alignment result = new Alignment();
        for(Correspondence c : alignment){   
            String leftWiki = DBkWikUtil.getWikiName(c.getEntityOne());
            String rightWiki = DBkWikUtil.getWikiName(c.getEntityTwo());            
            if(internalWikiNames.contains(leftWiki) && internalWikiNames.contains(rightWiki)){
                result.add(c);
            }
        }
        return result;
    }
    
    public static Alignment removeNonExistentPagesAndHashLinks(Alignment alignment, Set<String> allPages, Set<String> internalWikiNames){
        Alignment result = new Alignment();
        int hashLink = 0;
        int nonExistent = 0;
        Alignment removed = new Alignment();
        for(Correspondence c : alignment){            
            if(DBkWikUtil.isHashLink(c.getEntityOne()) || DBkWikUtil.isHashLink(c.getEntityTwo())){
                removed.add(c);
                hashLink++;
                continue;
            }
        
            String leftWiki = DBkWikUtil.getWikiName(c.getEntityOne());
            String rightWiki = DBkWikUtil.getWikiName(c.getEntityTwo());            
            if(internalWikiNames.contains(leftWiki)){
                //we can make the page existing check:
                if(allPages.contains(c.getEntityOne()) == false){
                    removed.add(c);
                    //System.out.println("not found in our URIs left: " + c.getEntityOne() + "   corresponding mapping: " + c.getEntityTwo());
                    nonExistent++;
                    continue;
                }
            }
            if(internalWikiNames.contains(rightWiki)){
                //we can make the page existing check:
                if(allPages.contains(c.getEntityTwo()) == false){
                    removed.add(c);
                    //System.out.println("not found in our URIs right: " + c.getEntityTwo() + "   corresponding mapping: " + c.getEntityOne());
                    nonExistent++;
                    continue;
                }
            }
            result.add(c);
        }
        
        try {
            removed.serializeToCSV(new File("removedDueToNonExistent.csv"));
        } catch (IOException ex) {
            LOGGER.warn("Could not write alignment file (removedDueToNonExistent)", ex);
        }
        LOGGER.info("Removed {} non existent pages and {} due to hashtag in their uris.", nonExistent, hashLink);
        return result;
    }
    
    
    //public static Map<Entry<String, String>,Alignment> getWikiSpecificLinks(Alignment bestInterWikiLinks, Map<String, String> redirects){
        
    //}
    
    public static Alignment resolveRedirectsNormalizeAndEnsureInjectivity(Alignment bestInterWikiLinks, Map<String, String> redirects){
        
        //normalize links (if both way, then confidence = 1.0, if only one way, then confidence = 0.5)
        Map<Entry<String, String>,Alignment> wikiPairToAlignment = new HashMap<>();
        for(Correspondence c : bestInterWikiLinks){
            String leftURI = redirects.getOrDefault(c.getEntityOne(), c.getEntityOne());
            String rightURI = redirects.getOrDefault(c.getEntityTwo(), c.getEntityTwo());
            
            /*
            if(leftURI.equals(c.getEntityOne()) == false){
                System.out.println("solvedredirect " + c.getEntityOne() + " -> " + leftURI);
            }
            if(rightURI.equals(c.getEntityTwo()) == false){
                System.out.println("solvedredirect " + c.getEntityTwo() + " -> " + rightURI);
            }
            */
            
            String leftWiki = DBkWikUtil.getWikiName(leftURI);
            String rightWiki = DBkWikUtil.getWikiName(rightURI);
            
            if(leftWiki.equals("$1.wikia.com") || rightWiki.equals("$1.wikia.com")) // due to extraction errors
                continue;
            
            int comparison = leftWiki.compareTo(rightWiki);
            if(comparison > 0){
                //switch
                String tmp = leftWiki;
                leftWiki = rightWiki;
                rightWiki = tmp;

                tmp = leftURI;
                leftURI = rightURI;
                rightURI = tmp;
            }else if(comparison == 0){
                //equal
                LOGGER.debug("Found link from wiki to itself: {} {}", c.getEntityOne(), c.getEntityTwo());
                continue;
            }
            Entry<String, String> key = new SimpleEntry<>(leftWiki, rightWiki);
            Alignment a = wikiPairToAlignment.computeIfAbsent(key, __-> new Alignment());
            Correspondence corres = a.getCorrespondence(leftURI, rightURI, CorrespondenceRelation.EQUIVALENCE);
            if(corres == null){
                a.add(leftURI, rightURI, 0.5);
            }else{
                //System.out.println("Found twice");
                a.remove(corres); // remove and add to update the indices of alignment
                corres.setConfidence(1.0);
                a.add(corres);
            }
        }
        
        Alignment result = new Alignment();
        Alignment allRemoved = new Alignment();
        for(Entry<Entry<String, String>, Alignment> entry : wikiPairToAlignment.entrySet()){
            Alignment a = entry.getValue();
            Set<Correspondence> toBeRemoved = ensureInjectivity(entry.getValue());
            allRemoved.addAll(toBeRemoved);
            a.removeAll(toBeRemoved);
            result.addAll(a);
        }
        try {
            allRemoved.serializeToCSV(new File("removedDueToInjectivity.csv"));
        } catch (IOException ex) {
            LOGGER.warn("Could not write alignment file (removedDueToInjectivity)", ex);
        }
        LOGGER.info("Removed {} correspondences due to injectivity.", allRemoved.size());
        return result;
        
        
                
/*
        try(BufferedWriter sameBefore = Files.getBufferedWriter("sameBefore.txt"); 
                BufferedWriter sameAfter = Files.getBufferedWriter("sameAfter.txt");
                BufferedWriter differentBefore = Files.getBufferedWriter("differentBefore.txt");
                BufferedWriter differentAfter = Files.getBufferedWriter("differentAfter.txt");
                ){
            
            for(Entry<Entry<String, String>, Alignment> entry : wikiPairToAlignment.entrySet()){
                Alignment same = new Alignment();
                Alignment different = new Alignment();
                for(Correspondence c : entry.getValue()){
                    if(DBkWikUtil.getResourceNameOrFragment(c.getEntityOne()).equals(DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo()))){
                        same.add(c);
                    }
                    else{
                        different.add(c);
                    }
                }
                sameBefore.append(entry.getKey().toString());sameBefore.newLine();
                sameBefore.append(same.toStringMultiline());sameBefore.newLine();sameBefore.newLine();
                
                differentBefore.append(entry.getKey().toString());differentBefore.newLine();
                differentBefore.append(different.toStringMultiline());differentBefore.newLine();differentBefore.newLine();

                ensureInjectivity(entry.getValue());
                
                
                
                same = new Alignment();
                different = new Alignment();
                for(Correspondence c : entry.getValue()){
                    if(DBkWikUtil.getResourceNameOrFragment(c.getEntityOne()).equals(DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo()))){
                        same.add(c);
                    }
                    else{
                        different.add(c);
                    }
                }
                sameAfter.append(entry.getKey().toString());sameAfter.newLine();
                sameAfter.append(same.toStringMultiline());sameAfter.newLine();sameAfter.newLine();
                
                differentAfter.append(entry.getKey().toString());differentAfter.newLine();
                differentAfter.append(different.toStringMultiline());differentAfter.newLine();differentAfter.newLine();
                
                //entry.getValue()

                //this.interWikiLinks.put(entry.getKey(), ...)

            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(AllWikisLinkExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
    }
    
    private static Alignment removeCorrespondencesBasedOnTransitiveClosure(Alignment alignment){
        TransitiveClosure<String> closure = closureFromAlignment(alignment);
        int problematicSets = 0;
        Alignment toBeRemovedAll = new Alignment();
        try(BufferedWriter writer = Files.getBufferedWriter("removedDueToTransitiveClosure.txt")){
            for(Set<String> sameEntities : closure.getClosure()){
                if(isProblematicSameAsSet(sameEntities)){
                    problematicSets++;
                    Alignment toBeRemoved = resolveProblematicSameAsSet(alignment, sameEntities);

                    ResultSet<Correspondence> result = alignment.retrieve(
                            QueryFactory.or(
                                QueryFactory.in(Correspondence.SOURCE, sameEntities),
                                QueryFactory.in(Correspondence.TARGET, sameEntities)
                            ),
                            QueryFactory.queryOptions(QueryFactory.deduplicate(DeduplicationStrategy.MATERIALIZE))
                    );
                    writer.write("[");writer.newLine();
                    for(Correspondence c : result){
                        writer.write("  " + c);
                        if(toBeRemoved.contains(c)){
                            writer.write("  (removed)");
                        }
                        writer.newLine();
                    }
                    writer.write("]");writer.newLine();
                    toBeRemovedAll.addAll(toBeRemoved);
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not write analysis file (transitive closure)", ex);
        }
        try {
            toBeRemovedAll.serializeToCSV(new File("removedDueToTransitiveClosure.csv"));
        } catch (IOException ex) {
            LOGGER.warn("Could not write alignment file (removedDueToTransitiveClosure)", ex);
        }
        alignment.removeAll(toBeRemovedAll);
        LOGGER.info("Removed {} correspondences from {} inconsistent sameAs sets.", toBeRemovedAll.size(), problematicSets);
        return alignment;
        
        
            
            /*
            List<Entry<String, String>> entities = new ArrayList<>(sameEntities.size());
            Set<String> alreadySeenWikis = new HashSet<>();
            boolean problematicSameAsSet = false; // set has correspondences which transitivly points to itself
            for(String s : sameEntities){
                String wikiName = DBkWikUtil.getWikiName(s);
                if(alreadySeenWikis.contains(wikiName)){
                    problematicSameAsSet = true;
                }else{
                    alreadySeenWikis.add(wikiName);
                }   
                entities.add(new SimpleEntry<>(DBkWikUtil.getWikiName(s), s));
            }
            if(problematicSameAsSet){                
                
                
                
                
            }
            
            entities.sort(Comparator.comparing(entry -> entry.getKey()));
            for(int i=0; i < entities.size(); i++){
                Entry<String, String> first = entities.get(i);
                for(int j=i+1; j < entities.size(); j++){
                    Entry<String, String> second = entities.get(j);
                    if(first.getKey().equals(second.getKey())){
                        LOGGER.warn("After transitive closure a correspondences points from a wiki to itself: {} - {} closure: {}", 
                                first.getValue(), second.getValue(), sameEntities);
                        problematicSameAsSets.add(sameEntities);
                    }
                    cloureAlignment.computeIfAbsent(new SimpleEntry<>(first.getKey(), second.getKey()), __-> new Alignment())
                            .add(new Correspondence(first.getValue(), second.getValue()));
                }
            }
            */
    }
    
    public static Alignment addTransitiveClosure(Alignment alignment){
        TransitiveClosure<String> closure = new TransitiveClosure<>();
        for(Correspondence c : alignment){
            closure.add(c.getEntityOne(), c.getEntityTwo());
        }
        Alignment closureAlignment = new Alignment();
        for(Set<String> sameEntities : closure.getClosure()){
            List<Entry<String, String>> entities = new ArrayList<>(sameEntities.size());
            Set<String> alreadySeenWikis = new HashSet<>();
            boolean problematicSameAsSet = false; // set has correspondences which transitivly points to itself
            for(String s : sameEntities){
                String wikiName = DBkWikUtil.getWikiName(s);
                if(alreadySeenWikis.contains(wikiName)){
                    problematicSameAsSet = true;
                }else{
                    alreadySeenWikis.add(wikiName);
                }   
                entities.add(new SimpleEntry<>(DBkWikUtil.getWikiName(s), s));
            }
            if(problematicSameAsSet){
                LOGGER.warn("Contains still problematic sets - should not happen");
            }
            
            Alignment subset = getSubset(alignment, sameEntities);
            AlignmentGraph subsetGraph = new AlignmentGraph(subset);
            
            entities.sort(Comparator.comparing(entry -> entry.getKey()));
            for(int i=0; i < entities.size(); i++){
                Entry<String, String> first = entities.get(i);
                for(int j=i+1; j < entities.size(); j++){
                    Entry<String, String> second = entities.get(j);
                    if(alignment.contains(new Correspondence(first.getValue(), second.getValue())) == false){
                        closureAlignment.add(new Correspondence(first.getValue(), second.getValue(), subsetGraph.getConnectivity(first.getValue(), second.getValue())));
                    }
                }
            }
        }
        return closureAlignment;
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
    
    
    /*
    private static Alignment addTransitiveClosure(Alignment alignment){
        TransitiveClosure<String> closure = new TransitiveClosure<>();
        for(Correspondence c : alignment){
            closure.add(c.getEntityOne(), c.getEntityTwo());
        }
        Alignment closureAlignment = new Alignment(alignment);
        for(Set<String> sameEntities : closure.getClosure()){
            List<Entry<String, String>> entities = new ArrayList<>(sameEntities.size());
            Set<String> alreadySeenWikis = new HashSet<>();
            boolean problematicSameAsSet = false; // set has correspondences which transitivly points to itself
            for(String s : sameEntities){
                String wikiName = DBkWikUtil.getWikiName(s);
                if(alreadySeenWikis.contains(wikiName)){
                    problematicSameAsSet = true;
                }else{
                    alreadySeenWikis.add(wikiName);
                }   
                entities.add(new SimpleEntry<>(DBkWikUtil.getWikiName(s), s));
            }
            if(problematicSameAsSet){
                System.out.println("problmematic set: ");
                ResultSet<Correspondence> result = tmp.retrieve(
                        QueryFactory.or(
                            QueryFactory.in(Correspondence.SOURCE, sameEntities),
                            QueryFactory.in(Correspondence.TARGET, sameEntities)
                        )
                );
                System.out.println("digraph alllinks {");
                for(Correspondence c : result){
                    System.out.println("  " + DotGraphUtil.makeQuotedNodeID(c.getEntityOne()) + " -- " + DotGraphUtil.makeQuotedNodeID(c.getEntityTwo()) + 
                            " [ label=\"" + String.format("%.3f", c.getConfidence()) + "\"];");
                }
                System.out.println("}");
                
                
                System.out.println("digraph selectedlinks {");
                for(Alignment a : map.values()){
                    for(Correspondence c : a.retrieve(
                            QueryFactory.or(
                                QueryFactory.in(Correspondence.SOURCE, sameEntities),
                                QueryFactory.in(Correspondence.TARGET, sameEntities)
                            ),
                            QueryFactory.queryOptions(QueryFactory.deduplicate(DeduplicationStrategy.MATERIALIZE))
                    )){
                        System.out.println("  " + DotGraphUtil.makeQuotedNodeID(c.getEntityOne()) + " -- " + DotGraphUtil.makeQuotedNodeID(c.getEntityTwo()) + 
                            " [ label=\"" + String.format("%.3f", c.getConfidence()) + "\"];");
                    }
                }
                System.out.println("}");
                
            }
            
            entities.sort(Comparator.comparing(entry -> entry.getKey()));
            for(int i=0; i < entities.size(); i++){
                Entry<String, String> first = entities.get(i);
                for(int j=i+1; j < entities.size(); j++){
                    Entry<String, String> second = entities.get(j);
                    if(first.getKey().equals(second.getKey())){
                        LOGGER.warn("After transitive closure a correspondences points from a wiki to itself: {} - {} closure: {}", 
                                first.getValue(), second.getValue(), sameEntities);
                        problematicSameAsSets.add(sameEntities);
                    }
                    cloureAlignment.computeIfAbsent(new SimpleEntry<>(first.getKey(), second.getKey()), __-> new Alignment())
                            .add(new Correspondence(first.getValue(), second.getValue()));
                }
            }
        }
        return cloureAlignment;
    }
    */
    
    public static Alignment resolveProblematicSameAsSet(Alignment allLinks, Set<String> problematicSameAsSet){
        
        //retrive all important links:
        ResultSet<Correspondence> result = allLinks.retrieve(
                QueryFactory.or(
                    QueryFactory.in(Correspondence.SOURCE, problematicSameAsSet),
                    QueryFactory.in(Correspondence.TARGET, problematicSameAsSet)
                ),
                QueryFactory.queryOptions(QueryFactory.deduplicate(DeduplicationStrategy.MATERIALIZE))
        );
        
        //call the algorithm:
        ComputeErrDegree<String> errorDegree = new ComputeErrDegree<>();
        Alignment reducedAlignment = new Alignment();
        //System.out.println("digraph problematicSameAsSet {");
        for(Correspondence c : result){
            double weight = c.getConfidence();
            String nameEntityOne = DBkWikUtil.getResourceNameOrFragment(c.getEntityOne());
            String nameEntityTwo = DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo());
            if(nameEntityOne.equals(nameEntityTwo)){
                weight += 0.5; // if they share the same name/label, 
            }else if(StringProcessing.normalize(nameEntityOne).equals(StringProcessing.normalize(nameEntityTwo))){
                weight = 0.25;
            }
            errorDegree.addEdge(c.getEntityOne(), c.getEntityTwo(), weight);
            //System.out.println("  " + DotGraphUtil.makeQuotedNodeID(c.getEntityOne()) + " -- " + DotGraphUtil.makeQuotedNodeID(c.getEntityTwo()) + 
            //            " [ label=\"" + String.format("%.3f", weight) + "\"];");
            reducedAlignment.add(new Correspondence(c));
        }
        //System.out.println("}");
        
        //
        
        updateErrorValues(errorDegree.computeLinkError(), reducedAlignment);
        updateAmountOfLinksToBeRemoved(reducedAlignment);
        updateLevenshtein(reducedAlignment);
        
        List<Correspondence> sortedCorrespondences = new ArrayList<>(reducedAlignment);
        sortedCorrespondences.sort(
                Comparator.<Correspondence>comparingDouble(c->c.getAdditionalConfidenceOrDefault("linksToBeRemoved", Double.MAX_VALUE))
                .thenComparing(c->c.getConfidence())
                .thenComparing(c->c.getAdditionalConfidenceOrDefault("errorValue", Double.MAX_VALUE), Comparator.reverseOrder())
                .thenComparing(c->c.getAdditionalConfidenceOrDefault("levenshtein", Double.MAX_VALUE), Comparator.reverseOrder())
                .thenComparing(c -> c.getEntityOne() + c.getEntityTwo()) //to make it deterministic
        );
        /*
        for(Correspondence c : sortedCorrespondences){
            System.out.println(c.getEntityOne() + "," + 
                    c.getEntityTwo() + "," + 
                    c.getConfidence() + "," + 
                    c.getAdditionalConfidence("linksToBeRemoved") + "," + 
                    c.getAdditionalConfidence("errorValue") + "," + 
                    c.getAdditionalConfidence("levenshtein"));
        }
        */
        
        
        Alignment toBeRemoved = new Alignment();
        for(Correspondence c : sortedCorrespondences){
            c.removeExtensions();
            reducedAlignment.remove(c);
            toBeRemoved.add(c);
            if(isProblematicTransitiveClosure(closureFromAlignment(reducedAlignment)) == false){
                break;
            }
        }
        return toBeRemoved;
        
        
        
        
        
        /*
        Levenshtein levenshtein = new Levenshtein();
        
        
        Map<Entry<String, String>, Double> linkError = errorDegree.computeLinkError();
        Map<Entry<String, String>, Integer> howManyLinksRemoved = computeAmountofLinksToBeRemoved(linkError.keySet());
        
        List<Entry<Entry<String, String>, Double>> list = new ArrayList<>(linkError.entrySet());
        
        
        list.sort(new Comparator<Entry<Entry<String, String>, Double>>() {
            @Override
            public int compare(Entry<Entry<String, String>, Double> o1, Entry<Entry<String, String>, Double> o2) {
                //first have a look at how many links needs to be removed: smaller is better
                int res = howManyLinksRemoved.getOrDefault(o1.getKey(), Integer.MAX_VALUE).compareTo(
                        howManyLinksRemoved.getOrDefault(o2.getKey(), Integer.MAX_VALUE));
                if(res != 0)
                    return res;
                
                //compare the error degree of the edge - decreasing - thus the other way around
                res = o2.getValue().compareTo(o1.getValue());
                if(res != 0)
                    return res;
                
                //compute the levenstein distance - decreasing 
                res = getLevenshteinDistance(o2).compareTo(getLevenshteinDistance(o1));
                if(res != 0)
                    return res;
                
                return concatURIs(o1).compareTo(concatURIs(o2));
            }
            private Float getLevenshteinDistance(Entry<Entry<String, String>, Double> entry){
                return levenshtein.distance(
                        DBkWikUtil.getResourceNameOrFragment(entry.getKey().getKey()), 
                        DBkWikUtil.getResourceNameOrFragment(entry.getKey().getValue()));
            }
            private String concatURIs(Entry<Entry<String, String>, Double> entry){
                return entry.getKey().getKey() + entry.getKey().getValue();
            }
        });
        
        
        //list.sort(
        //        // compare by error degree
        //        Entry.<Entry<String, String>, Double>comparingByValue(Comparator.reverseOrder())
        //        //compare by levenshtein distance of name name 
        //        .thenComparing(x -> levenshtein.distance(
        //                DBkWikUtil.getResourceNameOrFragment(x.getKey().getKey()), 
        //                DBkWikUtil.getResourceNameOrFragment(x.getKey().getValue())
        //        ), Comparator.reverseOrder())
        //        //compare by full uri lexicographically (to make it deterministic)
        //        .thenComparing(x -> x.getKey().getKey() + x.getKey().getValue())
        //);
        
        
        
        Alignment toBeRemoved = new Alignment();
        for(Entry<Entry<String, String>, Double> entry : list){
            //if(reducedAlignment.isEmpty())
            //    break;
            //remove from alignment (and find the order (source target or target source):
            if(reducedAlignment.remove(new Correspondence(entry.getKey().getKey(), entry.getKey().getValue()))){
                toBeRemoved.add(new Correspondence(entry.getKey().getKey(), entry.getKey().getValue()));
            }else if(reducedAlignment.remove(new Correspondence(entry.getKey().getValue(), entry.getKey().getKey()))){
                toBeRemoved.add(new Correspondence(entry.getKey().getValue(), entry.getKey().getKey()));
            }else{
                LOGGER.warn("Correspondence not found in alignment.");
            }
            
            if(isProblematicTransitiveClosure(closureFromAlignment(reducedAlignment)) == false){
                break;
            }
        }
        return toBeRemoved;
        */
    }
    
    
    private static void updateErrorValues(Map<Entry<String, String>, Double> errorValues, Alignment alignment){
        for(Correspondence c : alignment){
            Double confidence = errorValues.get(new SimpleEntry<>(c.getEntityOne(), c.getEntityTwo()));
            if(confidence == null){
                //try the other way around
                confidence = errorValues.getOrDefault(new SimpleEntry<>(c.getEntityTwo(), c.getEntityOne()), Double.MAX_VALUE);
            }
            c.addAdditionalConfidence("errorValue", confidence);
        }
    }
    
    private static void updateAmountOfLinksToBeRemoved(Alignment alignment){
        for(Correspondence c : alignment){
            Alignment reducedAlignment = new Alignment(alignment); // copy the alignment
            reducedAlignment.remove(c);
            if(isProblematicTransitiveClosure(closureFromAlignment(reducedAlignment))){
                c.addAdditionalConfidence("linksToBeRemoved", Double.MAX_VALUE); //need more than one removal of a correspondence to make it non problematic
            }else{
                c.addAdditionalConfidence("linksToBeRemoved", 1.0);
            }
        }
    }
    
    private static void updateLevenshtein(Alignment alignment){
        Levenshtein levenshtein = new Levenshtein();
        for(Correspondence c : alignment){
            c.addAdditionalConfidence("levenshtein", levenshtein.distance(
                        DBkWikUtil.getResourceNameOrFragment(c.getEntityOne()), 
                        DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo())
            ));
        }
    }
    
    private static Map<Entry<String, String>, Integer> computeAmountofLinksToBeRemoved(Set<Entry<String, String>> correspondences){
        Alignment alignment = new Alignment();
        for(Entry<String, String> cor : correspondences){
            alignment.add(cor.getKey(), cor.getValue());
        }
        Map<Entry<String, String>, Integer> map = new HashMap<>();
        for(Correspondence c : alignment){
            Alignment reducedAlignment = new Alignment(alignment); // copy the alignment
            reducedAlignment.remove(c);
            Entry<String, String> key = new SimpleEntry<>(c.getEntityOne(), c.getEntityTwo());
            if(isProblematicTransitiveClosure(closureFromAlignment(reducedAlignment))){
                map.put(key, Integer.MAX_VALUE); //need more than one removal of a correspondence to make it non problematic
            }else{
                map.put(key, 1);
            }
        }
        return map;
    }
        
    
    /**
     * Ensures injectivity by removing all correspondences where a source points to multiple targets or multiple sources point to the same target.
     * @param alignment the alignment to be analyzed
     * @return the set of correspondences which should be removed.
     */
    public static Set<Correspondence> ensureInjectivity(Alignment alignment){
        Map<String, Alignment> sameSource = new HashMap<>();
        Map<String, Alignment> sameTarget = new HashMap<>();
        
        for(Correspondence c : alignment){
            sameSource.computeIfAbsent(c.getEntityOne(), __-> new Alignment()).add(c);
            sameTarget.computeIfAbsent(c.getEntityTwo(), __-> new Alignment()).add(c);
        }
        Set<Correspondence> toBeRemoved = new HashSet<>();
        for(Alignment sameSourceAlignment : sameSource.values()){
            if(sameSourceAlignment.size() > 1){
                //check if one value is exactly the same -> then choose it, otherwise remove all
                //remove all but not the one with the exact same label / name
                for(Correspondence c : sameSourceAlignment){
                    if(DBkWikUtil.getResourceNameOrFragment(c.getEntityOne()).equals(
                        DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo())) == false){
                        toBeRemoved.add(c);
                    }
                }
            }
        }
        
        for(Alignment sameTargetAlignment : sameTarget.values()){
            if(sameTargetAlignment.size() > 1){
                //check if one value is exactly the same -> then choose it, otherwise remove all
                //remove all but not the one with the exact same label / name
                for(Correspondence c : sameTargetAlignment){
                    if(DBkWikUtil.getResourceNameOrFragment(c.getEntityOne()).equals(
                        DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo())) == false){
                        toBeRemoved.add(c);
                    }
                }
            }
        }
        return toBeRemoved;
    }
    
    public static Alignment removeDisambiguationPages(Alignment alignment, Set<String> disambiguationPages){
        ResultSet<Correspondence> result = alignment.retrieve(
                QueryFactory.or(
                    QueryFactory.in(Correspondence.SOURCE, disambiguationPages),
                    QueryFactory.in(Correspondence.TARGET, disambiguationPages)
                )
        );
        Alignment toBeRemoved = new Alignment();        
        for(Correspondence c : result){
            alignment.remove(c);
            toBeRemoved.add(c);
        }
        try {
            toBeRemoved.serializeToCSV(new File("removedDueToDisambiguations.csv"));
        } catch (IOException ex) {
            LOGGER.warn("Could not write alignment file (removedDueToDisambiguations)", ex);
        }
        LOGGER.info("Removed {} correspondences due to disambiguation pages", toBeRemoved.size());
        return alignment;
    }
    
    
    
    private static TransitiveClosure<String> closureFromAlignment(Alignment a){
        TransitiveClosure<String> tc = new TransitiveClosure<>();
        for(Correspondence c : a){
            tc.add(c.getEntityOne(), c.getEntityTwo());
        }
        return tc;
    }
    
    private static boolean isProblematicTransitiveClosure(TransitiveClosure<String> transitiveClosure){
        for(Set<String> sameAsSet : transitiveClosure.getClosure()){
            if(isProblematicSameAsSet(sameAsSet))
                return true;
        }
        return false;
    }
    
    private static boolean isProblematicSameAsSet(Set<String> sameAsSet){
        Set<String> alreadySeenWikis = new HashSet<>();
        for(String s : sameAsSet){
            String wikiName = DBkWikUtil.getWikiName(s);
            if(alreadySeenWikis.contains(wikiName)){
                return true;
            }else{
                alreadySeenWikis.add(wikiName);
            }
        }
        return false;
    }
    
    
    public static void writeAnalysisToFile(Alignment a, String filename){
        Map<Entry<String, String>,Entry<Alignment, Alignment>> wikiPairToAlignemnt = new HashMap<>();
        int overallSame = 0;
        int overallDifferent = 0;
        for(Correspondence c : a){            
            String leftWiki = DBkWikUtil.getWikiName(c.getEntityOne());
            String rightWiki = DBkWikUtil.getWikiName(c.getEntityTwo());
            
            String leftResource = DBkWikUtil.getResourceNameOrFragment(c.getEntityOne());
            String rightResource = DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo());
            
            Entry<String, String> key = new SimpleEntry<>(leftWiki, rightWiki);
            if(leftResource.equals(rightResource)){
                wikiPairToAlignemnt.computeIfAbsent(key, __-> new SimpleEntry<>(new Alignment(), new Alignment())).getKey().add(c);
                overallSame++;
            }else{
                wikiPairToAlignemnt.computeIfAbsent(key, __-> new SimpleEntry<>(new Alignment(), new Alignment())).getValue().add(c);
                overallDifferent++;
            }
        }
        
        try(BufferedWriter writer = Files.getBufferedWriter(filename)){
            writer.append("number of correspondences: " + a.size() + "   same: " + overallSame + " different:" + overallDifferent); writer.newLine();
            
            List<Entry<Entry<String, String>,Entry<Alignment, Alignment>>> listWiki = new ArrayList<>(wikiPairToAlignemnt.entrySet());
            listWiki.sort((Entry<Entry<String, String>, Entry<Alignment, Alignment>> o1, Entry<Entry<String, String>, Entry<Alignment, Alignment>> o2) -> 
                    Integer.compare(o2.getValue().getKey().size() + o2.getValue().getValue().size(), o1.getValue().getKey().size() + o1.getValue().getValue().size()));
            for(Entry<Entry<String, String>,Entry<Alignment, Alignment>> entry : listWiki){
                int same = entry.getValue().getKey().size();
                int different = entry.getValue().getValue().size();
                writer.append(entry.getKey().toString() + " overall: " + (same + different) + " same: " + same + " different: " + different);
                writer.newLine();
            }
            writer.newLine();
            writer.append("SAME CORRESPONDENCES");writer.newLine();
            for(Entry<Entry<String, String>,Entry<Alignment, Alignment>> entry : wikiPairToAlignemnt.entrySet()){
                if(entry.getValue().getKey().size() > 0){
                    writer.append(entry.getKey().toString());writer.newLine();
                    writer.append(entry.getValue().getKey().toStringMultiline());
                    writer.newLine();
                }
            }
            
            writer.newLine();
            writer.newLine();
            writer.append("DIFFERENT CORRESPONDENCES / NOT SAME");            
            for(Entry<Entry<String, String>,Entry<Alignment, Alignment>> entry : wikiPairToAlignemnt.entrySet()){
                if(entry.getValue().getValue().size() > 0){
                    writer.append(entry.getKey().toString());writer.newLine();
                    writer.append(entry.getValue().getValue().toStringMultiline());
                    writer.newLine();
                }
            }
            
            //DEBUG
            writer.newLine();
            writer.newLine();
            Set<String> s = new HashSet<>();
            
            s.add("http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Korea");
            s.add("http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/South_Korea");
            
            s.add("http://dbpedia.org/resource/Korea");
            s.add("http://dbpedia.org/resource/South_Korea");
            
            s.add("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/Korea");
            s.add("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/South_Korea");//safe
            
            s.add("http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/South_Korea");
            s.add("http://dbkwik.webdatacommons.org/stexpanded.wikia.com/resource/Korea"); //safe
            
            
            
            
            ResultSet<Correspondence> result = a.retrieve(
                QueryFactory.or(
                    QueryFactory.in(Correspondence.SOURCE, s),
                    QueryFactory.in(Correspondence.TARGET, s)
                ),
                QueryFactory.queryOptions(QueryFactory.deduplicate(DeduplicationStrategy.MATERIALIZE))
            );
            for(Correspondence c : result){
                writer.write("  " + c.toString());writer.newLine();
            }
            
            
        } catch (IOException ex) {
            LOGGER.warn("could not write to file", ex);
        }
    }
}
