package com.bio4j.model.uniprot.relationships.references;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;

import com.bio4j.model.properties.Title;
// properties
import com.bio4j.model.uniprot.nodes.references.OnlineArticles;
import com.bio4j.model.uniprot.nodes.references.Reference;

/**
 * 
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface OnlineArticle extends Relationship<

	Reference, Reference.Type, 
	OnlineArticle, OnlineArticle.Type,
	OnlineArticles, OnlineArticles.Type

>,

	// properties
	Title<OnlineArticle, OnlineArticle.Type>  
{

	public static Type TYPE = Type.onlineArticle;

	public static enum Type implements RelationshipType<

		Reference, Reference.Type, 
		OnlineArticle, OnlineArticle.Type, 
		OnlineArticles, OnlineArticles.Type

	>
	{
		onlineArticle;

		// there is only one OnlineArticles node => many to one.
		public Arity arity() {
			return Arity.manyToOne;
		}

		public Reference.Type sourceType() {
			return Reference.TYPE;
		}

		public OnlineArticles.Type targetType() {
			return OnlineArticles.TYPE;
		}

		public Type value() {
			return onlineArticle;
		}
	}
}
