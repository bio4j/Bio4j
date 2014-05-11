package com.bio4j.model.proteinInteractions.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;

import com.bio4j.model.uniprot.nodes.Protein;
import com.bio4j.model.isoforms.nodes.Isoform;

// properties
import com.bio4j.model.properties.Experiments;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 * @author <a href="mailto:eparejatobes@ohnosequences.com">Eduardo Pareja-Tobes</a>
 */
public interface ProteinIsoformInteraction extends Relationship <
  Protein, Protein.Type,
  ProteinIsoformInteraction, ProteinIsoformInteraction.Type,
  Isoform, Isoform.Type
>,

  // properties
  Experiments<ProteinIsoformInteraction, ProteinIsoformInteraction.Type>
{

  public Protein source();
  public Isoform target();

  // TODO migrate to properties
  public String getOrganismsDiffer(); // TODO ??
  public String getIntactId2(); // TODO ??
  public String getIntactId1(); // TODO ??

  public static Type TYPE = Type.proteinIsoformInteraction;
  public static enum Type implements RelationshipType <
    Protein, Protein.Type,
    ProteinIsoformInteraction, ProteinIsoformInteraction.Type,
    Isoform, Isoform.Type
  > {

    proteinIsoformInteraction;
    
    public Arity arity() { return Arity.manyToMany; }

    public Type value() { return proteinIsoformInteraction; }
    public Protein.Type sourceType() { return Protein.TYPE; }
    public Isoform.Type targetType() { return Isoform.TYPE; }
  }  
}
