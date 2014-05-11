package com.bio4j.model.uniprot.relationships.references;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;

// properties
import com.bio4j.model.properties.DoId;
import com.bio4j.model.properties.MedlineId;
import com.bio4j.model.properties.PubmedId;
import com.bio4j.model.properties.Title;

import com.bio4j.model.uniprot.nodes.references.Articles;
import com.bio4j.model.uniprot.nodes.references.Reference;

/**
 * 
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface Article extends Relationship <

  Reference, Reference.Type,
  Article, Article.Type,
  Articles, Articles.Type

>,
  // properties
  Title<Article, Article.Type>, // TODO maybe common to all references?
  PubmedId<Article, Article.Type>,
  MedlineId<Article, Article.Type>, 
  DoId<Article, Article.Type>

{

  public static Type TYPE = Type.article;

  public static enum Type implements RelationshipType<

    Reference, Reference.Type, 
    Article, Article.Type, 
    Articles, Articles.Type
    
  >
  {

    article;

    // there is only one Articles node => many to one.
    public Arity arity() {

      return Arity.manyToOne;
    }

    public Reference.Type sourceType()  { return Reference.TYPE;  }
    public Articles.Type targetType()   { return Articles.TYPE;   }

    public Type value() { return article; }
  }
}
