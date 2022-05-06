package de.uni_mannheim.informatik.dws.gollum.extractLinks;

import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.Counter;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.DefaultExtensions;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WikiWithLinks {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiWithLinks.class);
    
    private static final String NIF_SUPER_STRING = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#superString";
    private static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String INTER_WIKI_LINK = "http://dbkwik.webdatacommons.org/ontology/InterWikiLink";
    private static final String TA_IDENT_REF = "http://www.w3.org/2005/11/its/rdf#taIdentRef";
    
    private static final String DISAMBIGUATION = "disambiguation";
    private static final Pattern REFER_TO_PATTERN = Pattern.compile("(can|could|may|might)\\s*refer\\s*to");
    private static final Pattern DOT_OR_COLON_PATTERN = Pattern.compile("[.:]");
    private static final String CATEGORY = "/resource/category:";
    private static final int CATEGORY_LENGTH = CATEGORY.length();
    
    private static final Pattern DBPEDIA_PATTERN = Pattern.compile("http://dbpedia\\.org/resource/https?://en\\.wikipedia\\.org/wiki/", Pattern.CASE_INSENSITIVE);
    private static final String DBPEDIA_REPLACEMENT = "http://dbpedia.org/resource/";
    private static final String COMMUNITY_WIKI_PATTERN = "http://dbkwik.webdatacommons.org/community.wikia.com/resource/C:";
    private static final int COMMUNITY_WIKI_PATTERN_LENGTH = COMMUNITY_WIKI_PATTERN.length();
    
    private static final String EXTENSION_SECTION_LABEL = DefaultExtensions.MeltExtensions.CONFIGURATION_BASE + "sectionLabel";
    
    
    
    //private Alignment allInterLanguageLinks;
    private Set<String> allPages;
    private Set<String> disambiguationPages;
    private Map<String, String> redirects;
    private Alignment bestInterWikiLinks;
    private String wikiName;
    
    public WikiWithLinks(File tarGzFileWikiFile){
        LOGGER.info("parsing {}", tarGzFileWikiFile.getName());
        //parse file
        this.allPages = new HashSet<>();
        this.disambiguationPages = new HashSet<>();
        this.redirects = new HashMap<>();
        
        Map<String, String> superStringMap = new HashMap<>();
        Map<String, String> labelMap = new HashMap<>();
        Map<String, String> nifToLink = new HashMap<>();
        Set<String> interWikiLinks = new HashSet<>();
        Counter<String> wikiNames = new Counter<>();
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
                            this.allPages.add(nx[0].getLabel());
                            wikiNames.add(DBkWikUtil.getWikiName(nx[0].getLabel()));                            
                            if(nx[2].getLabel().toLowerCase(Locale.ENGLISH).contains(DISAMBIGUATION)){
                                this.disambiguationPages.add(nx[0].getLabel());
                            }
                        }
                        break;
                    }
                    case "transitive-redirects.ttl":{
                        NxParser nxp = new NxParser();
                        nxp.parse(tarInput, StandardCharsets.UTF_8);      
                        for (Node[] nx : nxp) {
                            this.redirects.put(nx[0].getLabel(), nx[2].getLabel());
                        }
                        break;
                    }
                    case "nif-page-structure.ttl":{
                        NxParser nxp = new NxParser();
                        nxp.parse(tarInput, StandardCharsets.UTF_8);      
                        for (Node[] nx : nxp) {
                            if(nx[1].getLabel().equals(NIF_SUPER_STRING)){
                                superStringMap.put(nx[0].getLabel(), nx[2].getLabel());
                            }else if(nx[1].getLabel().equals(RDFS_LABEL)){
                                labelMap.put(nx[0].getLabel(), nx[2].getLabel());
                            }
                        }
                        break;
                    }
                    case "nif-text-links.ttl":{
                        NxParser nxp = new NxParser();
                        nxp.parse(tarInput, StandardCharsets.UTF_8);      
                        for (Node[] nx : nxp) {
                            if(nx[1].getLabel().equals(NIF_SUPER_STRING)){
                                superStringMap.put(nx[0].getLabel(), nx[2].getLabel());
                            }else if(nx[1].getLabel().equals(RDF_TYPE) && nx[2].getLabel().equals(INTER_WIKI_LINK)){
                                interWikiLinks.add(nx[0].getLabel());
                            }else if(nx[1].getLabel().equals(TA_IDENT_REF)){
                                nifToLink.put(nx[0].getLabel(), nx[2].getLabel());
                            }
                        }
                        break;
                    }
                    //case "disambiguations.ttl":{
                    //    NxParser nxp = new NxParser();
                    //    nxp.parse(tarInput, StandardCharsets.UTF_8);      
                    //    for (Node[] nx : nxp) {
                    //        this.disambiguationPages.add(nx[0].getLabel());
                    //    }
                    //    break;
                    //}
                    case "short-abstracts.ttl":{
                        NxParser nxp = new NxParser();
                        nxp.parse(tarInput, StandardCharsets.UTF_8);
                        for (Node[] nx : nxp) {
                            if(isDisambiguationPageBasedOnComment(nx[0].getLabel(), nx[2].getLabel())){
                                this.disambiguationPages.add(nx[0].getLabel());
                            }
                        }
                        break;
                    }
                    case "article-categories.ttl":{
                        NxParser nxp = new NxParser();
                        nxp.parse(tarInput, StandardCharsets.UTF_8);
                        for (Node[] nx : nxp) {
                            String cat = nx[2].getLabel().toLowerCase(Locale.ENGLISH);
                            int index = cat.indexOf(CATEGORY);
                            if(index != -1){
                                if(cat.substring(index + CATEGORY_LENGTH).contains(DISAMBIGUATION)){
                                    this.disambiguationPages.add(nx[0].getLabel());
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.info("Could not read file " + tarGzFileWikiFile.getName(), ex);
        }
        
        updateDisambiguationPages();
        
        this.wikiName = wikiNames.mostCommonElement();
        
        Alignment allInterWikiLinks = extractAllInterWikiLinks(superStringMap, labelMap, nifToLink, interWikiLinks);
        String sectionTitle = getBestSectionNameForSameEntities(allInterWikiLinks);
        this.bestInterWikiLinks = getAllFilteredInterWikiLinks(allInterWikiLinks, sectionTitle);
        LOGGER.info("parsing {} finished: {} pages, {} disambiguations, {} general interwiki links, {} best section interwiki links (section:{})", 
                tarGzFileWikiFile.getName(), this.allPages.size(), this.disambiguationPages.size(), allInterWikiLinks.size(), this.bestInterWikiLinks.size(), sectionTitle);
        /*
        System.out.println(this.bestInterWikiLinks.toStringMultiline());
        System.out.println("\n\nDIFFERENT");
        for(Correspondence c : this.bestInterWikiLinks){
            if(DBkWikUtil.getResourceNameOrFragment(c.getEntityOne()).equals(DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo())) == false){
                System.out.println(c.toString());
            }
        }
        */
    }

    public Set<String> getAllPages() {
        return allPages;
    }

    public Set<String> getDisambiguationPages() {
        return disambiguationPages;
    }

    public Map<String, String> getRedirects() {
        return redirects;
    }

    public Alignment getBestInterWikiLinks() {
        return bestInterWikiLinks;
    }

    public String getWikiName() {
        return wikiName;
    }
    
    private void updateDisambiguationPages(){
        for(String resourceURL : this.allPages){
            if(DBkWikUtil.getResourceNameOrFragment(resourceURL).toLowerCase(Locale.ENGLISH).contains(DISAMBIGUATION)){
                this.disambiguationPages.add(resourceURL);
            }
        }
    }
    
    private static boolean isDisambiguationPageBasedOnComment(String uri, String comment){
        //in case the label contains dot or colon, we skip it by start
        int beginningLength = (DBkWikUtil.getResourceNameOrFragment(uri).length() * 2) + 15; //plus 15 because of text "could refer to"
        Matcher dotMatcher = DOT_OR_COLON_PATTERN.matcher(comment);
        int subStringLength = 0;
        while(dotMatcher.find()) {
            int start = dotMatcher.start();
            if(start > beginningLength){
                subStringLength = start;
                break;
            }
        }
        if(subStringLength == 0)
            subStringLength = comment.length();
        String reducedComment = comment.substring(0, subStringLength).toLowerCase(Locale.ENGLISH);

        return REFER_TO_PATTERN.matcher(reducedComment).find();
    }
    
    private static Alignment extractAllInterWikiLinks(Map<String, String> superStringMap, Map<String, String> labelMap, 
            Map<String, String> nifToLink, Set<String> interWikiLinks){
        Alignment allExtractedInterwikiLinks = new Alignment();
        int introSection = 0;
        int missingSection = 0;
        int reflexiveEdge = 0;
        int mainPage = 0;
        int noPageName = 0;
        for(String interWikiLink : interWikiLinks){
            String paragraph = superStringMap.get(interWikiLink);
            String link = nifToLink.get(interWikiLink);
            int queryPartIndex = interWikiLink.indexOf('?');
            if(paragraph == null || link == null || queryPartIndex < 0){
                LOGGER.debug("did not find paragraph link or query part. interWikiLink:{} paragraph:{} link:{} queryPartIndex: {}", 
                        interWikiLink, paragraph, link, queryPartIndex);
                missingSection++;
                continue;
            }
            String resourceURL = interWikiLink.substring(0, queryPartIndex);
            String section = superStringMap.get(paragraph);
            if(section == null){
                LOGGER.debug("did not find section: {}", paragraph);
                missingSection++;
                continue;
            }
            String sectionLabel = labelMap.get(section);
            if(sectionLabel == null && section.contains("&char=0,")){
                introSection++;
                continue;
                //or use it:
                //sectionLabel = "Intro Section";
            }
            if(sectionLabel == null){
                LOGGER.debug("did not find sectionLabel: {} (link: {}  or   {})", section, interWikiLink, link);
                missingSection++;
                continue;
            }
            
            //remove reflexive edges:
            if(resourceURL.equals(link)){
                LOGGER.debug("removed reflexive edge: {}", resourceURL);
                reflexiveEdge++;
                continue;
            }
            if(DBkWikUtil.isMainPage(resourceURL) || DBkWikUtil.isMainPage(link)){
                LOGGER.debug("removed due to main page: {} <=> {}", resourceURL, link);
                mainPage++;
                continue;
            }
            
            //postprocess link due to extraction failures:
            link = DBPEDIA_PATTERN.matcher(link).replaceFirst(DBPEDIA_REPLACEMENT);
            if(link.startsWith(COMMUNITY_WIKI_PATTERN)){
                int colonIndex = link.indexOf(':', COMMUNITY_WIKI_PATTERN_LENGTH);
                if(colonIndex == -1){
                    noPageName++;
                    continue;
                }
                
                //if(colonIndex != -1){
                String wikiURL = link.substring(COMMUNITY_WIKI_PATTERN_LENGTH, colonIndex) + ".wikia.com";
                String pageName = link.substring(colonIndex + 1, link.length());
                if(pageName.length() == 0){
                    noPageName++;
                    continue;
                }
                link = "http://dbkwik.webdatacommons.org/" + wikiURL + "/resource/" + pageName;
                //}
            }
            
            Correspondence c = new Correspondence(resourceURL, link);
            c.addExtensionValue(EXTENSION_SECTION_LABEL, sectionLabel);
            allExtractedInterwikiLinks.add(c);
        }
        LOGGER.info("From {} interwiki links, extracted {} links. Removed due to missing section {}, intro section {},  reflexive {}, main pages {}, no page name {}.",
                interWikiLinks.size(), allExtractedInterwikiLinks.size(), missingSection, introSection, reflexiveEdge, mainPage, noPageName);
        return allExtractedInterwikiLinks;
    }
    
    private static Alignment getAllFilteredInterWikiLinks(Alignment allInterWikiLinks, String sectionTitle){
        Alignment alignment = new Alignment();
        for(Correspondence c : allInterWikiLinks){
            if(c.getExtensionValueAsString(EXTENSION_SECTION_LABEL).toLowerCase().contains(sectionTitle)){
                alignment.add(new Correspondence(
                        DBkWikUtil.resolveHostInDBkWikURL(c.getEntityOne()), 
                        DBkWikUtil.resolveHostInDBkWikURL(c.getEntityTwo()))
                );
                //alignment.add(c);
            }
        }
        return alignment;
    }
    
    private static String getBestSectionNameForSameEntities(Alignment allInterWikiLinks){
        Counter<String> positiveSectionLabels = new Counter<>();
        int sameLinks = 0;
        for(Correspondence c : allInterWikiLinks){
            if(DBkWikUtil.getResourceNameOrFragment(c.getEntityOne()).equals(
                    DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo()))){
                positiveSectionLabels.add(c.getExtensionValueAsString(EXTENSION_SECTION_LABEL));
                sameLinks++;
            }
        }
        
        if(positiveSectionLabels.isEmpty() || sameLinks < 5){
            LOGGER.info("bestsection: too less links");
            //default section label to use
            return "link";
        }
        
        //Set<String> sourcePages = new HashSet<>();
        //for(Correspondence c : allInterWikiLinks){
        //    if(positiveSectionLabels.getCount(c.getExtensionValueAsString(EXTENSION_SECTION_LABEL)) > 0)
        //        sourcePages.add(c.getEntityOne());
        //}
                
        //LOGGER.info("Fraction same links: {} / {} = {}   ", 
        //        positiveSectionLabels.getCount(), allInterWikiLinks.size(), (double)positiveSectionLabels.getCount() /  allInterWikiLinks.size());
        //        positiveSectionLabels.getCount(), sourcePages.size(), (double)positiveSectionLabels.getCount() /  sourcePages.size());
        
        //LOGGER.info("positive section links: {}", positiveSectionLabels.getCount());
        
        Counter<String> substringCounter = new Counter<>();
        for(Map.Entry<String, Integer> candidate : positiveSectionLabels.mostCommon()){
            //create unique substrings with length starting from 2
            Set<String> uniqueSubstrings = new HashSet<>();
            String candidateText = candidate.getKey().toLowerCase();
            for (int i=0; i < candidateText.length(); ++i) {
                for (int j=i+2; j <= candidateText.length(); ++j) {
                    uniqueSubstrings.add(candidateText.substring(i, j));
                }
            }
            //update counter
            Integer candidateCoverage = candidate.getValue();
            for(String substring : uniqueSubstrings){
                substringCounter.add(substring, candidateCoverage);
            }
        }
        
        int longestText = positiveSectionLabels.getDistinctElements().stream().mapToInt(s->s.length()).max().orElse(0);
        long allExamples = positiveSectionLabels.getCount();
        
        double bestValue = 0.0d;
        String bestText = "";
        for(Map.Entry<String, Integer> candidate : substringCounter.mostCommon()){
            double coverageFraction = (double)candidate.getValue() / allExamples;
            double textFraction = (double)candidate.getKey().length() / longestText;
            
            double harmonicMean = (2.0d * coverageFraction * textFraction) / (coverageFraction + textFraction);
            if(harmonicMean > bestValue){
                bestValue = harmonicMean;
                bestText = candidate.getKey();
            }
        }
        
        int equalLinks = 0;
        int allLinks = 0;
        for(Correspondence c : allInterWikiLinks){
            if(c.getExtensionValueAsString(EXTENSION_SECTION_LABEL).toLowerCase().contains(bestText)){
                if(DBkWikUtil.getResourceNameOrFragment(c.getEntityOne()).equals(
                    DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo()))){
                    equalLinks++;
                }
                allLinks++;
            }
        }
        double fraction = (double)equalLinks / allLinks;
        LOGGER.info("bestsection: {} | {} / {} = {}", bestText, equalLinks, allLinks, fraction);
        if(fraction < 0.2){
            return "link";
        }
        //LOGGER.info("bllllllll: {} / {} = {}   ", equalLinks, allLinks, (double)equalLinks / allLinks);
        
        
        return bestText;
    }
}
