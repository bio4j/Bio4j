package com.bio4j.model.uniprot.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;
import com.bio4j.model.uniprot.nodes.Protein;
import com.bio4j.model.uniprot.nodes.references.Reference;

/**
 * 
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
// articleCitesProtein
public interface ArticleProteinCitation
		extends
		Relationship<Reference, Reference.Type, ArticleProteinCitation, ArticleProteinCitation.Type, Protein, Protein.Type> {

	public Reference source();

	public Protein target();

	public static Type TYPE = Type.articleProteinCitation;

	public static enum Type
			implements
			RelationshipType<Reference, Reference.Type, ArticleProteinCitation, ArticleProteinCitation.Type, Protein, Protein.Type> {
		articleProteinCitation;
		public Type value() {
			return articleProteinCitation;
		}

		public Arity arity() {
			return Arity.manyToMany;
		}

		public Reference.Type sourceType() {
			return Reference.TYPE;
		}

		public Protein.Type targetType() {
			return Protein.TYPE;
		}

	}

}
