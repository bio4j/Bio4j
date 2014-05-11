package com.bio4j.model.uniprot.relationships.references;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;

import com.bio4j.model.uniprot.nodes.references.Reference;
import com.bio4j.model.uniprot.nodes.references.Publication;

import com.bio4j.model.properties.Date;
import com.bio4j.model.properties.Volume;
import com.bio4j.model.properties.First;
import com.bio4j.model.properties.Last;

/*

  A reference can be published in a publication: an `Article` in a `Journal`.
*/
public interface Published extends Relationship <
  
  Reference, Reference.Type, // source
  Published, Published.Type, // rel
  Publication, Publication.Type // target

>,

  // properties
  Date<Published, Published.Type>, // TODO here again?
  Volume<Published, Published.Type>,
  First<Published, Published.Type>, // TODO what? pages?
  Last<Published, Published.Type> // TODO what? pages?

{

  public static Type TYPE = Type.cited;

  public static enum Type implements RelationshipType <

    Reference, Reference.Type,
    Published, Published.Type,
    Publication, Publication.Type

  >
  {
    cited;

    public Arity arity() { return Arity.manyToOne; }

    public Reference.Type sourceType() { return Reference.TYPE; }
    public Publication.Type targetType() { return Publication.TYPE; }

    public Type value() { return cited; }
  }
}