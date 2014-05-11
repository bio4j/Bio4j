package com.bio4j.model.uniprot.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;
import com.bio4j.model.properties.Locator;
import com.bio4j.model.uniprot.nodes.OnlineArticle;
import com.bio4j.model.uniprot.nodes.OnlineJournal;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface OnlineArticleJournal extends Relationship <
    OnlineArticle, OnlineArticle.Type,
    OnlineArticleJournal, OnlineArticleJournal.Type,
    OnlineJournal, OnlineJournal.Type
    >,
    Locator<OnlineArticleJournal,OnlineArticleJournal.Type>{
    
    public static enum Type implements RelationshipType <
        OnlineArticle, OnlineArticle.Type,
        OnlineArticleJournal, OnlineArticleJournal.Type,
        OnlineJournal, OnlineJournal.Type
    > {
        OnlineArticleJournal;
        public Type value() { return OnlineArticleJournal; }
        public Arity arity() { return Arity.manyToOne; }
        public OnlineArticle.Type sourceType() { return OnlineArticle.TYPE; }
        public OnlineJournal.Type targetType() { return OnlineJournal.TYPE; }
    }
    
    public OnlineArticle source();
    public OnlineJournal target();

}
