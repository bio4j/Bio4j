package com.bio4j.model.uniprot.nodes;

import java.util.List;

import com.ohnosequences.typedGraphs.Node;
import com.ohnosequences.typedGraphs.NodeType;
import com.bio4j.model.go.nodes.GoTerm.Type;
import com.bio4j.model.properties.Id;
import com.bio4j.model.properties.Name;
import com.bio4j.model.uniprot.relationships.ProteinInterpro;

/**
 * 
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface Interpro extends Node<Interpro, Interpro.Type>,

	// properties
		Id<Interpro, Interpro.Type>, Name<Interpro, Interpro.Type> {

	public static Type TYPE = Type.interpro;
	public default Type type() { return TYPE; }

	public static enum Type implements NodeType<Interpro, Interpro.Type> {

		interpro;
		public Type value() {
			return interpro;
		}
	}

	// proteinInterpro
	// ingoing
	public List<ProteinInterpro> proteinInterpro_in();
	public List<Protein> proteinInterpro_inNodes();

}
