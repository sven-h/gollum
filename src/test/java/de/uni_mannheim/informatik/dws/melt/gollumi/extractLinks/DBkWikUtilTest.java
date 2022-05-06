package de.uni_mannheim.informatik.dws.melt.gollumi.extractLinks;

import de.uni_mannheim.informatik.dws.gollum.extractLinks.DBkWikUtil;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.filter.ConceptType;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DBkWikUtilTest {
    
    @Test
    public void getWikiNameTest(){
        assertEquals("mywiki", DBkWikUtil.getWikiName("http://dbkwik.webdatacommons.org/mywiki/resource/foo"));
        assertEquals("mywiki.org", DBkWikUtil.getWikiName("http://dbkwik.webdatacommons.org/mywiki.org/resource/foo"));
        assertEquals("mywiki", DBkWikUtil.getWikiName("http://dbkwik.webdatacommons.org/mywiki"));
        
        assertEquals("mywiki", DBkWikUtil.getWikiName("http://mywiki/resource/foo"));
        assertEquals("mywiki", DBkWikUtil.getWikiName("https://mywiki/resource/foo"));
        assertEquals("mywiki.org", DBkWikUtil.getWikiName("https://mywiki.org/resource/foo"));
        
        assertEquals("mywiki", DBkWikUtil.getWikiName("http://mywiki"));
        assertEquals("mywiki", DBkWikUtil.getWikiName("https://mywiki"));
        
        assertEquals("dbpedia.org", DBkWikUtil.getWikiName("http://dbpedia.org/resource/Figwit"));
    }
    
    @Test
    public void getConceptTypeTest(){
        assertEquals(ConceptType.RDF_PROPERTY, DBkWikUtil.getConceptType("http://dbpedia.org/property/formation"));
        assertEquals(ConceptType.RDF_PROPERTY, DBkWikUtil.getConceptType("http://dbkwik.webdatacommons.org/locomotive.wikia.com/property/formations"));
        
        assertEquals(ConceptType.CLASS, DBkWikUtil.getConceptType("http://dbpedia.org/ontology/Person"));
        assertEquals(ConceptType.CLASS, DBkWikUtil.getConceptType("http://dbkwik.webdatacommons.org/henson-alternative.wikia.com/class/wikipedia"));
        
        assertEquals(ConceptType.INSTANCE, DBkWikUtil.getConceptType("http://mywiki/resource/foo"));
        
    }
    
    @Test
    public void getResourceNameOrFragmentTest(){
        String k = DBkWikUtil.getResourceNameOrFragment("http://dbkwik.webdatacommons.org/greenday.wikia.com/property/name");
        assertEquals("name", DBkWikUtil.getResourceNameOrFragment("http://dbkwik.webdatacommons.org/greenday.wikia.com/property/name"));
        assertEquals("name/foo", DBkWikUtil.getResourceNameOrFragment("http://dbkwik.webdatacommons.org/greenday.wikia.com/property/name/foo"));
        
        assertEquals("single", DBkWikUtil.getResourceNameOrFragment("http://dbkwik.webdatacommons.org/greenday.wikia.com/class/single"));
        assertEquals("single/foo", DBkWikUtil.getResourceNameOrFragment("http://dbkwik.webdatacommons.org/greenday.wikia.com/class/single/foo"));
        
        assertEquals("All_the_Time_(Lyrics)", DBkWikUtil.getResourceNameOrFragment("http://dbkwik.webdatacommons.org/greenday.wikia.com/resource/All_the_Time_(Lyrics)"));
        assertEquals("All_the_Time_(Lyrics)/foo/bar", DBkWikUtil.getResourceNameOrFragment("http://dbkwik.webdatacommons.org/greenday.wikia.com/resource/All_the_Time_(Lyrics)/foo/bar"));
        
        //missing property class or resource
        assertEquals("name", DBkWikUtil.getResourceNameOrFragment("http://dbkwik.webdatacommons.org/greenday.wikia.com/foo/name"));
        assertEquals("bar", DBkWikUtil.getResourceNameOrFragment("http://dbkwik.webdatacommons.org/greenday.wikia.com/foo/name/bar"));
        assertEquals("name-test", DBkWikUtil.getResourceNameOrFragment("http://dbkwik.webdatacommons.org/greenday.wikia.com/foo/name-test"));
    }
    
    @Test
    public void getFirstPartOfPathTest() throws URISyntaxException{
        assertEquals("test", DBkWikUtil.getFirstPartOfPath(new URI("http://foo.org///test//")));
        assertEquals("test", DBkWikUtil.getFirstPartOfPath(new URI("http://foo.org/test")));
        assertEquals("test", DBkWikUtil.getFirstPartOfPath(new URI("http://foo.org/test/")));
    }
    
    @Test
    public void isHashLinkTest(){
        assertTrue(DBkWikUtil.isHashLink("http://dbpedia.org/resource/Music_of_The_Lord_of_the_Rings_film_series#Songs"));
        assertTrue(DBkWikUtil.isHashLink("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/Unnamed_Xindi#Xindi-Primate Councilor"));
        
        assertFalse(DBkWikUtil.isHashLink("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/Unnamed_Xindi"));
        assertFalse(DBkWikUtil.isHashLink("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/reso####urce/Unnamed_Xindi"));
    }
    
    //commented due to http request behind
    //@Test
    public void resolveHostTest(){
        assertEquals("memory-beta.wikia.com", DBkWikUtil.resolveHost("memorybeta.wikia.com"));
        assertEquals("de.memory-beta.wikia.com", DBkWikUtil.resolveHost("de.memory-beta.wikia.com"));
        assertEquals("thisisnotexistentfoo.wikia.com", DBkWikUtil.resolveHost("thisisnotexistentfoo.wikia.com"));
        
        assertEquals("memory-beta.wikia.com", DBkWikUtil.resolveHost("http://memorybeta.wikia.com"));
        assertEquals("", DBkWikUtil.resolveHost(null));
        assertEquals("", DBkWikUtil.resolveHost(""));
        assertEquals("", DBkWikUtil.resolveHost("    "));
        
        assertEquals("test", DBkWikUtil.resolveHost("test"));
        
        //wrong extracted URL
        assertEquals("atelieratelier totori.wikia.com", DBkWikUtil.resolveHost("atelieratelier totori.wikia.com"));
    }
    
    //commented due to http request behind
    //@Test
    public void resolveHostInDBkWikURLTest(){
        assertEquals("http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/Eel", DBkWikUtil.resolveHostInDBkWikURL("http://dbkwik.webdatacommons.org/memorybeta.wikia.com/resource/Eel"));
        assertEquals("http://test.com", DBkWikUtil.resolveHostInDBkWikURL("http://test.com"));
        
        //parts of url
        assertEquals("http://dbkwik.webdatacommons.org/memory-beta.wikia.com/resource/", DBkWikUtil.resolveHostInDBkWikURL("http://dbkwik.webdatacommons.org/memorybeta.wikia.com/resource/"));
        assertEquals("http://dbkwik.webdatacommons.org/memory-beta.wikia.com/", DBkWikUtil.resolveHostInDBkWikURL("http://dbkwik.webdatacommons.org/memorybeta.wikia.com/"));
        assertEquals("http://dbkwik.webdatacommons.org/memory-beta.wikia.com", DBkWikUtil.resolveHostInDBkWikURL("http://dbkwik.webdatacommons.org/memorybeta.wikia.com"));
        assertEquals("http://dbkwik.webdatacommons.org/", DBkWikUtil.resolveHostInDBkWikURL("http://dbkwik.webdatacommons.org/"));
        assertEquals("http://dbkwik.webdatacommons.org", DBkWikUtil.resolveHostInDBkWikURL("http://dbkwik.webdatacommons.org"));
    }
    
    
    
    @Test
    public void getWikiLanguageTest(){
        assertEquals("en", DBkWikUtil.getWikiLanguage("http://dbkwik.webdatacommons.org/en.memorybeta.wikia.com/resource/Eel"));
        assertEquals("de", DBkWikUtil.getWikiLanguage("http://dbkwik.webdatacommons.org/de.memorybeta.wikia.com/resource/Eel"));
        assertEquals("fr", DBkWikUtil.getWikiLanguage("http://dbkwik.webdatacommons.org/fr.memorybeta.wikia.com/resource/Eel"));
        
        assertEquals("en", DBkWikUtil.getWikiLanguage("http://dbkwik.webdatacommons.org/memorybeta.wikia.com/resource/Eel"));
    }
    
    @Test
    public void getRealURLTest(){
        assertEquals("http://memorybeta.wikia.com/wiki/Eel", DBkWikUtil.getRealURL("http://dbkwik.webdatacommons.org/memorybeta.wikia.com/resource/Eel"));
        assertEquals("http://memory-alpha.wikia.com/wiki/Unnamed_Xindi", DBkWikUtil.getRealURL("http://dbkwik.webdatacommons.org/memory-alpha.wikia.com/resource/Unnamed_Xindi"));
    }
    
    //commented due to http request behind
    //@Test
    public void isURLActiveTest(){
        assertTrue(DBkWikUtil.isURLActive("http://memorybeta.wikia.com/wiki/Eel"));
        assertFalse(DBkWikUtil.isURLActive("https://memory-beta.fandom.com/wiki/Eeleeee"));
    }
}
