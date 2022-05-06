package de.uni_mannheim.informatik.dws.gollum.extractLinks;

import de.uni_mannheim.informatik.dws.melt.matching_base.FileUtil;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.filter.ConceptType;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.FileCache;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.util.URIUtil;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.slf4j.LoggerFactory;


public class DBkWikUtil {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DBkWikUtil.class);
    
    private static final String DBKWIK = "dbkwik.webdatacommons.org/";
    private static final int DBKWIK_LENGTH = DBKWIK.length();
    private static final Pattern HTTP_PATTERN = Pattern.compile("https?:\\/\\/");
    
    
    /**
     * Given a full URI like http://dbkwik.webdatacommons.org/lotr.wikia.com/resource/Main_Page it will extract the wiki name.
     * In this example it is lotr.wikia.com. Mainly it extracts everthing after dbkwik.webdatacommons.org/ and before the next following slash (/).
     * @param resourceURI a full resource uri
     * @return the wiki name
     */
    public static String getWikiName(String resourceURI){
        //everthing after 'dbkwik.webdatacommons.org/' and before the next '/'
        int dbkwikStart = resourceURI.indexOf(DBKWIK);
        if(dbkwikStart == -1){
            //just remove the http part in front
            String wiki = HTTP_PATTERN.matcher(resourceURI).replaceFirst("");
            int wikiEnd = wiki.indexOf('/');
            if(wikiEnd == -1)
                return wiki;
            return wiki.substring(0, wikiEnd);
        }
        int wikiStart = dbkwikStart + DBKWIK_LENGTH;
        int wikiEnd = resourceURI.indexOf('/', wikiStart);
        if(wikiEnd == -1)
            wikiEnd = resourceURI.length();
        return resourceURI.substring(wikiStart, wikiEnd);
    }
    
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("\\/(property|class|resource)\\/");
    /**
     * Extracts the name / label of a resource given the uri.
     * Example: given uri http://dbkwik.webdatacommons.org/lotr.wikia.com/resource/Main_Page it will extract Main_Page .
     * Usually it extracts everything after /resource/ or /property/ etc and in case it is not found, then it wil extract the substring after the last slash (/).
     * @param uri the resource uri
     * @return the name / label of a resource
     */
    public static String getResourceNameOrFragment(String uri){
        /*
        int index = uri.indexOf("/resource/");
        if(index != -1)
            return uri.substring(index + 10);
        index = uri.indexOf("/property/");
        if(index != -1)
            return uri.substring(index + 10);
        index = uri.indexOf("/class/");
        if(index != -1)
            return uri.substring(index + 7);
        return URIUtil.getUriFragment(uri);
        */
        Matcher m = RESOURCE_PATTERN.matcher(uri);
        if(m.find()){
            return uri.substring(m.end());
        }else{
            return URIUtil.getUriFragment(uri);
        }
    }
    
    private static final Pattern CONCEPT_TYPE_PATTERN = Pattern.compile("\\/(property|class|resource|ontology)\\/");
    public static ConceptType getConceptType(String uri){
        Matcher m = CONCEPT_TYPE_PATTERN.matcher(uri);
        if(m.find()){
            if(m.group(1).equals("resource")){
                return ConceptType.INSTANCE;
            }else if(m.group(1).equals("class") || m.group(1).equals("ontology")){
                return ConceptType.CLASS;
            }else if(m.group(1).equals("property")){
                return ConceptType.RDF_PROPERTY;
            }
        }
        return ConceptType.UNKNOWN;
    }
    
    public static ConceptType getConceptType(Correspondence c){
        ConceptType one = getConceptType(c.getEntityOne());
        ConceptType two = getConceptType(c.getEntityOne());
        if(one.equals(two))
            return one;
        return ConceptType.UNKNOWN;
    }
    
    private static final Set<String> MAIN_PAGE_NAMES = new HashSet<>(Arrays.asList(
            "main_page", "hauptseite", "portada", "page_principale", "strona_główna", "Заглавная_страница", 
            "pagina_principale", "hoofdpagina", "página_principal"
    ));
    
    /**
     * Returns true if the URL refers to a main page and thus not to a real page in the wiki.
     * @param url the full url 
     * @return true, if it is pointing to a main page, false otherwise
     */
    public static boolean isMainPage(String url){
        return MAIN_PAGE_NAMES.contains(getResourceNameOrFragment(url).toLowerCase());
    }
    
    
    
    private static final Pattern FANDOM_PATTERN = Pattern.compile(".fandom.com");
    
    private static final Set<String> LANGUAGES = new HashSet<>(Arrays.asList(
            "aa", "ab", "ae", "af", "ak", "am", "an", "ar", "as", "av", "ay", "az", "ba", "be", "bg",
            "bh", "bi", "bm", "bn", "bo", "br", "bs", "ca", "ce", "ch", "co", "cr", "cs", "cu", "cv",
            "cy", "da", "de", "dv", "dz", "ee", "el", "en", "eo", "es", "et", "eu", "fa", "ff", "fi",
            "fj", "fo", "fr", "fy", "ga", "gd", "gl", "gn", "gu", "gv", "ha", "he", "hi", "ho", "hr",
            "ht", "hu", "hy", "hz", "ia", "id", "ie", "ig", "ii", "ik", "io", "is", "it", "iu", "ja",
            "jv", "ka", "kg", "ki", "kj", "kk", "kl", "km", "kn", "ko", "kr", "ks", "ku", "kv", "kw",
            "ky", "la", "lb", "lg", "li", "ln", "lo", "lt", "lu", "lv", "mg", "mh", "mi", "mk", "ml",
            "mn", "mr", "ms", "mt", "my", "na", "nb", "nd", "ne", "ng", "nl", "nn", "no", "nr", "nv",
            "ny", "oc", "oj", "om", "or", "os", "pa", "pi", "pl", "ps", "pt", "qu", "rm", "rn", "ro",
            "ru", "rw", "sa", "sc", "sd", "se", "sg", "si", "sk", "sl", "sm", "sn", "so", "sq", "sr",
            "ss", "st", "su", "sv", "sw", "ta", "te", "tg", "th", "ti", "tk", "tl", "tn", "to", "tr",
            "ts", "tt", "tw", "ty", "ug", "uk", "ur", "uz", "ve", "vi", "vo", "wa", "wo", "xh", "yi",
            "yo", "za", "zh", "zu", "zh-tw", "pt-br"
    ));
    
    /**
     * This function resolves the wiki host in a DBkWik URL.
     * E.g. in url 'http://dbkwik.webdatacommons.org/memorybeta.wikia.com/resource/Eel' the wiki host
     * 'memorybeta.wikia.com' is transfored to 'memory-beta.wikia.com'. Thus the returned url is
     * 'http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Eel' such that the full url is still kept.
     * @param resourceURI the full dbkwik url
     * @return the full url where the host wiki is the redirected version
     */
    public static String resolveHostInDBkWikURL(String resourceURI){
        //everthing after 'dbkwik.webdatacommons.org/' and before the next '/'
        int dbkwikStart = resourceURI.indexOf(DBKWIK);
        if(dbkwikStart == -1){
            //not a dbkwik url
            return resourceURI;
        }
        int wikiStart = dbkwikStart + DBKWIK_LENGTH;
        int wikiEnd = resourceURI.indexOf('/', wikiStart);
        if(wikiEnd == -1)
            wikiEnd = resourceURI.length();
        
        String firstPart = resourceURI.substring(0, wikiStart);
        String wikiPart = resourceURI.substring(wikiStart, wikiEnd);
        String lastPart = resourceURI.substring(wikiEnd);
        
        String resolvedWikiPart = resolveHost(wikiPart);
        return firstPart + resolvedWikiPart + lastPart;
        
    }
    
    
    private static final Map<String, String> CACHE = getCache();
    private static Map<String, String> getCache(){
        FileCache<Map<String, String>> cache = new FileCache<>(
            new File(FileUtil.getUserTmpFolder(), "dbkwikResolveCache.dat"), ()-> new HashMap<>());
        cache.saveAtShutdown();
        return cache.get();
    }
    
    private static final int TIMEOUT = 5000; // 5 seconds
    private static final HttpClient CLIENT = HttpClientBuilder.create()
            .setRedirectStrategy(new LaxRedirectStrategy())
            .setDefaultRequestConfig(RequestConfig.custom()
                .setSocketTimeout(TIMEOUT)
                .setConnectTimeout(TIMEOUT)
                .setConnectionRequestTimeout(TIMEOUT)
                .build())
            .build();
    
    /**
     * This function resolves only the host.
     * Thus the input looks like "memoryalpha.wikia.com" (without the schema - http(s)).
     * It will return the redirected version like "memory-alpha.wikia.com".
     * @param url the url (host) without the schema.
     * @return the redirected url
     */
    public static String resolveHost(String url){
        String resolvedURL = CACHE.get(url);
        if(resolvedURL != null)
            return resolvedURL;
        
        if(url == null){
            CACHE.put(null, "");
            return "";
        }
        String processedUrl = url.trim();
        if(processedUrl.length() == 0){
            CACHE.put(url, "");
            return "";
        }        
        if(processedUrl.startsWith("http://") == false && processedUrl.startsWith("https://") == false){
            processedUrl = "http://" + processedUrl;
        }
        
        HttpClientContext context = HttpClientContext.create();
        try {
            HttpHead request = new HttpHead(new URI(processedUrl));            
            CLIENT.execute(request, response -> null, context);
        } catch (IOException | URISyntaxException ex) {
            CACHE.put(url, url);
            return url;
        }
        List<URI> locations = context.getRedirectLocations();
        if(locations == null){
            CACHE.put(url, url);
            return url;
        }
        URI redirectedURI = locations.get(locations.size() - 1);
        String newHost = redirectedURI.getHost();
        if(newHost == null || newHost.equals("community.fandom.com")){
            CACHE.put(url, url);
            return url;
        }
        //convert to wikia url and not fandom
        newHost = FANDOM_PATTERN.matcher(newHost).replaceAll(".wikia.com");
        
        String possibleLanguage = getFirstPartOfPath(redirectedURI);
        if(LANGUAGES.contains(possibleLanguage)){
            String finalURL = possibleLanguage + "." + newHost;
            CACHE.put(url, finalURL);
            return finalURL;
        }else{
            CACHE.put(url, newHost);
            return newHost;
        }
    }
    /**
     * Return the first part of the path. This is usually the wiki or a language e.g.
     * given the url 'http://memory-beta.fandom.com/fr/wiki/El' this function will return 'fr' because it is the first part of the path.
     * @param uri the full url
     * @return the first part of the path or empty string if there is none
     */
    public static String getFirstPartOfPath(URI uri){
        for(String s : uri.getPath().split("/")){
            if(s != null && s.length() != 0)
                return s;
        }
        return "";
    }
    
    public static String getRealURL(String uri){
        String wikiname = getWikiName(uri);
        if(wikiname.equals("dbpedia.org")){
            return uri;
        }else{
            return "http://" + getWikiName(uri) + "/wiki/" + getResourceNameOrFragment(uri);
        }
    }
    
    public static Set<String> getWikiNameFromFileName(String filename){
        String[] lineSplit = filename.split("~");
        String wikiName = lineSplit[2];
        wikiName = wikiName.substring(0, wikiName.indexOf(".tar.gz")) + ".wikia.com"; 
        return new HashSet<>(Arrays.asList(wikiName, lineSplit[1] + "." + wikiName));
    }
    
    public static Set<String> getWikiNamesFromFileNames(File fileWithFilenames){
        Set<String> wikiNames = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileWithFilenames)))) {
            String line;
            while ((line = br.readLine()) != null) {
                wikiNames.addAll(getWikiNameFromFileName(line));
            }
        } catch (IOException ex) {
            LOGGER.error("Could not read", ex);
        }
        return wikiNames;
    }
    
    public static Map<String, Set<String>> getMapFromFileNameToWikiName(File fileWithFilenames){
        Map<String, Set<String>> fileNameToWikiName = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileWithFilenames)))) {
            String line;
            while ((line = br.readLine()) != null) {
                fileNameToWikiName.put(line, getWikiNameFromFileName(line));
            }
        } catch (IOException ex) {
            LOGGER.error("Could not read", ex);
        }
        return fileNameToWikiName;
    }
    
    public static Map<String, String> getMapFromWikiNameToFileName(File fileWithFilenames){
        Map<String, String> wikiToFileName = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileWithFilenames)))) {
            String line;
            while ((line = br.readLine()) != null) {
                for(String s : getWikiNameFromFileName(line)){
                    wikiToFileName.put(s, line);
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Could not read", ex);
        }
        return wikiToFileName;
    }
    
    /**
     * return true if URL is active: meaning if it can be retrived with a 200 response.
     * @param uri the URI to check
     * @return true if URL is active
     */
    public static boolean isURLActive(String uri){        
        try {
            HttpResponse response = CLIENT.execute(new HttpHead(new URI(uri)));
            return HttpStatus.SC_OK == response.getStatusLine().getStatusCode();
        } catch (IOException | URISyntaxException ex) {
            return false;
        }
    }
    
    private static Pattern LANGUAGE_WIKI_PATTERN = Pattern.compile("^(" + LANGUAGES.stream().map(s->s + "\\.").collect(Collectors.joining("|")) + ")");
    public static String getWikiLanguage(String uri){
        String wikiName = getWikiName(uri);
        Matcher m = LANGUAGE_WIKI_PATTERN.matcher(wikiName);
        if(m.find()){
            String group = m.group(0);
            return group.substring(0, group.length() - 1);
        }
        return "en";        
    }
    
    public static Alignment getDifferentLabelSubset(Alignment alignment){
        Alignment result = new Alignment();
        for(Correspondence c : alignment){
            if(DBkWikUtil.getResourceNameOrFragment(c.getEntityOne()).equals(DBkWikUtil.getResourceNameOrFragment(c.getEntityTwo())) == false){
                result.add(c);
            }
        }
        return result;
    }
    
    
    /**
     * Return true if a hashtag is contained after the last slash.
     * This indicates a reference to a part of a website e.g. http://dbpedia.org/resource/Music_of_The_Lord_of_the_Rings_film_series#Songs
     * @param uri the uri to analyze
     * @return true, if the uri contains a hashtag fter the last slash of the uri
     */
    public static boolean isHashLink(String uri){
        int lastSlash = uri.lastIndexOf('/');
        if(lastSlash == -1){
            return uri.contains("#");
        }else{
            return uri.substring(lastSlash).contains("#");
        }
    }
}
