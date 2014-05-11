package com.bio4j.model.uniprot.relationships.references;

import java.util.List;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;

// properties
import com.bio4j.model.properties.Name;

import com.bio4j.model.uniprot.nodes.references.Consortiums;
import com.bio4j.model.uniprot.nodes.references.Author;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface Consortium extends Relationship <

  Author, Author.Type,
  Consortium, Consortium.Type,
  Consortiums, Consortiums.Type

>,
  
  // properties
  Name<Consortium, Consortium.Type>
{
  
  public static Type TYPE = Type.consortium;
  
  public static enum Type implements RelationshipType <
    
    Author, Author.Type,
    Consortium, Consortium.Type,
    Consortiums, Consortiums.Type
    
  >
  {

    consortium;

    // there is only one Consortiums node => many to one.
    public Arity arity() { return Arity.manyToOne; }

    public Author.Type sourceType() { return Author.TYPE; }
    public Consortiums.Type targetType() { return Consortiums.TYPE; }

    public Type value() { return consortium; }
  }
}
