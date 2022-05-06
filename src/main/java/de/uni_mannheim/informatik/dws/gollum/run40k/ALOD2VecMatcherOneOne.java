package de.uni_mannheim.informatik.dws.gollum.run40k;

import de.uni_mannheim.informatik.dws.Alod2vecMatcher.Matcher;
import de.uni_mannheim.informatik.dws.melt.matching_jena.MatcherYAAAJena;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.filter.extraction.NaiveDescendingExtractor;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import java.util.Properties;
import org.apache.jena.ontology.OntModel;


public class ALOD2VecMatcherOneOne extends MatcherYAAAJena{
    @Override
    public Alignment match(OntModel source, OntModel target, Alignment inputAlignment, Properties properties) throws Exception {
        return NaiveDescendingExtractor.filter(new Matcher().match(source, target, inputAlignment, properties));
    }
}
