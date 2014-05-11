package com.bio4j.model.uniprot_go.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;
import com.bio4j.model.go.nodes.GoTerm;
import com.bio4j.model.uniprot.nodes.Protein;

// properties
import com.bio4j.model.properties.Evidence;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 * @author <a href="mailto:eparejatobes@ohnosequences.com">Eduardo Pareja-Tobes</a> 
 */
public interface GoAnnotation extends Relationship <
  Protein, Protein.Type,
  GoAnnotation, GoAnnotation.Type,
  GoTerm, GoTerm.Type
>,
  
  // properties
  Evidence<GoAnnotation, GoAnnotation.Type>

{

  @Override
  public Protein source();
  @Override
  public GoTerm target();


  public static Type TYPE = Type.goAnnotation;
  public static enum Type implements RelationshipType <
    Protein, Protein.Type,
    GoAnnotation, GoAnnotation.Type,
    GoTerm, GoTerm.Type
  > {
    goAnnotation;
    public Type value() { return goAnnotation; }
    public Arity arity() { return Arity.manyToMany; }
    public Protein.Type sourceType() { return Protein.TYPE; }
    public GoTerm.Type targetType() { return GoTerm.TYPE; }
  }
}
