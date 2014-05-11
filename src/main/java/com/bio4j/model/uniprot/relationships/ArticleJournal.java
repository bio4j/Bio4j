package com.bio4j.model.uniprot.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;
import com.bio4j.model.properties.Date;
import com.bio4j.model.properties.First;
import com.bio4j.model.properties.Last;
import com.bio4j.model.properties.Volume;
import com.bio4j.model.uniprot.nodes.Journal;
import com.bio4j.model.uniprot.nodes.references.Reference;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
// inJournal
public interface ArticleJournal extends Relationship <
    Reference, Reference.Type,
    ArticleJournal, ArticleJournal.Type,
    Journal, Journal.Type
    >,
    Date<ArticleJournal, ArticleJournal.Type>,
    Volume<ArticleJournal, ArticleJournal.Type>,
    First<ArticleJournal, ArticleJournal.Type>,
    Last<ArticleJournal, ArticleJournal.Type>{
    
    public static Type TYPE = Type.articleJournal;    
    public static enum Type implements RelationshipType <
    	Reference, Reference.Type,
        ArticleJournal, ArticleJournal.Type,
        Journal, Journal.Type
    > {
        articleJournal;
        public Type value() { return articleJournal; }
        public Arity arity() { return Arity.manyToOne; }
        public Reference.Type sourceType() { return Reference.TYPE; }
        public Journal.Type targetType() { return Journal.TYPE; }

    }
    
    public Reference source();
    public Journal target();

}
