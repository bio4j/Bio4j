package com.bio4j.model.uniprot.nodes;

import com.ohnosequences.typedGraphs.Node;
import com.ohnosequences.typedGraphs.NodeType;
import com.bio4j.model.go.nodes.GoTerm.Type;
import com.bio4j.model.properties.Name;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface Dataset extends Node<Dataset, Dataset.Type>,
  
  // properties
	Name<Dataset,Dataset.Type>
{
	
	public static Type TYPE = Type.dataset;
	public default Type type() { return TYPE; }
	
  public static enum Type implements NodeType<Dataset, Dataset.Type> {

    dataset;
    public Type value() { return dataset; }
  }

    
}
