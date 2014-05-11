package com.bio4j.model.uniprot.nodes;

import java.util.List;

import com.ohnosequences.typedGraphs.Node;
import com.ohnosequences.typedGraphs.NodeType;
import com.bio4j.model.go.nodes.GoTerm.Type;
import com.bio4j.model.properties.Name;
import com.bio4j.model.properties.Id;
import com.bio4j.model.uniprot.relationships.ProteinPfam;

public interface Pfam extends Node<Pfam, Pfam.Type>,

	Id<Pfam, Pfam.Type>, Name<Pfam, Pfam.Type> {

	public static Type TYPE = Type.pfam;
	public default Type type() { return TYPE; }

	public static enum Type implements NodeType<Pfam, Pfam.Type> {

		pfam;
		public Type value() {
			return pfam;
		}
	}

	// proteinPfam
    // ingoing
    public List<ProteinPfam> proteinPfam_in(); 
    public List<Protein> proteinPfam_inNodes();
}
