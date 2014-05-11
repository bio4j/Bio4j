package com.bio4j.model.uniprot_isoforms.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;
import com.bio4j.model.go.nodes.GoTerm.Type;
import com.bio4j.model.isoforms.nodes.Isoform;
import com.bio4j.model.uniprot.nodes.Protein;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface ProteinIsoform extends Relationship <

  Protein, Protein.Type,
  ProteinIsoform, ProteinIsoform.Type,
  Isoform, Isoform.Type

> {
    
  public Protein source();
  public Isoform target();


  public static Type TYPE = Type.proteinIsoform;
  public default Type type() { return TYPE; }

  public static enum Type implements RelationshipType <

    Protein, Protein.Type,
    ProteinIsoform, ProteinIsoform.Type,
    Isoform, Isoform.Type
  
  > {

    proteinIsoform;

    public Arity arity() { return Arity.manyToMany; }

    public Type value() { return proteinIsoform; }
    public Protein.Type sourceType() { return Protein.TYPE; }
    public Isoform.Type targetType() { return Isoform.TYPE; }
  }


}
