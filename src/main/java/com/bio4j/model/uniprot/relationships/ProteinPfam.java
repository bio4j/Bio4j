package com.bio4j.model.uniprot.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;
import com.ohnosequences.typedGraphs.RelationshipType.Arity;
import com.bio4j.model.uniprot.nodes.Pfam;
import com.bio4j.model.uniprot.nodes.Protein;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface ProteinPfam extends Relationship <
	Protein, Protein.Type,
	ProteinPfam, ProteinPfam.Type,
	Pfam, Pfam.Type
	> {
	
	public static Type TYPE = Type.proteinPfam;
	public static enum Type implements RelationshipType <
		Protein, Protein.Type,
		ProteinPfam, ProteinPfam.Type,
		Pfam, Pfam.Type
		  > {
		
		   proteinPfam;
		   public Type value() { return proteinPfam; }
		   public Arity arity() { return Arity.manyToMany; }
		   public Protein.Type sourceType() { return Protein.TYPE; }
		   public Pfam.Type targetType() { return Pfam.TYPE; }
	}
		
	public Protein source();
	public Pfam target();
	
}
