package com.bio4j.model.uniprot.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;
import com.bio4j.model.uniprot.nodes.Person;
import com.bio4j.model.uniprot.nodes.OnlineArticle;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface OnlineArticleAuthor extends Relationship <
	  OnlineArticle, OnlineArticle.Type,
	  OnlineArticleAuthor, OnlineArticleAuthor.Type,
	  Person, Person.Type
	  > {
	
	  public static Type TYPE = Type.onlineArticleAuthor;
	  public static enum Type implements RelationshipType <
	    OnlineArticle, OnlineArticle.Type,
	    OnlineArticleAuthor, OnlineArticleAuthor.Type,
	    Person, Person.Type
	    > {
	
		    onlineArticleAuthor;
		    public Type value() { return onlineArticleAuthor; }
		    public Arity arity() { return Arity.manyToMany; }
		    public OnlineArticle.Type sourceType() { return OnlineArticle.TYPE; }
		    public Person.Type targetType() { return Person.TYPE; }
	  }
	  
	  public OnlineArticle source();
	  public Person target();

}
