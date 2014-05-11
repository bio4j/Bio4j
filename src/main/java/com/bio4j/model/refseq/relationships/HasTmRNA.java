package com.bio4j.model.refseq.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;

// source and target
import com.bio4j.model.refseq.nodes.GenomeElement;
import com.bio4j.model.refseq.nodes.TmRNA;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 * @author <a href="mailto:eparejatobes@ohnosequences.com">Eduardo Pareja-Tobes</a>
 */
public interface HasTmRNA extends HasGenomicFeature <
  HasTmRNA,  HasTmRNA.Type,
  TmRNA, TmRNA.Type
> {

  public GenomeElement source();
  public TmRNA target();

  public static Type TYPE = Type.hasTmRNA;
  public static enum Type implements HasGenomicFeatureType <
    HasTmRNA,  HasTmRNA.Type,
    TmRNA, TmRNA.Type
  > {
    hasTmRNA;
    public Type value() { return hasTmRNA; }
    public Arity arity() { return Arity.manyToMany; } // TODO review this
    public GenomeElement.Type sourceType() { return GenomeElement.TYPE; }
    public TmRNA.Type targetType() { return TmRNA.TYPE; }
  }
}
