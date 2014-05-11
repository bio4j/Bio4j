package com.bio4j.model.uniprot.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;
import com.bio4j.model.uniprot.nodes.Taxon;

public interface TaxonParent extends Relationship <
  Taxon, Taxon.Type,
  TaxonParent, TaxonParent.Type,
  Taxon, Taxon.Type
> {
  
  public static Type TYPE = Type.taxonParent;   
  public static enum Type implements RelationshipType <
    Taxon, Taxon.Type,
    TaxonParent, TaxonParent.Type,
    Taxon, Taxon.Type
  > {

    taxonParent;

    public Type value() { return taxonParent; }
    public Arity arity() { return Arity.manyToOne; }
    public Taxon.Type sourceType() { return Taxon.TYPE; }
    public Taxon.Type targetType() { return Taxon.TYPE; }
  }
}