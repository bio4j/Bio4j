package com.bio4j.model.uniprot.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.Node;
import com.ohnosequences.typedGraphs.NodeType;
import com.ohnosequences.typedGraphs.RelationshipType;

import com.bio4j.model.uniprot.nodes.Protein;
import com.bio4j.model.uniprot.nodes.CommentType;

public interface BasicCommentType <
  R extends BasicComment<R,RT>, 
  RT extends Enum<RT> & BasicCommentType<R,RT>
> extends RelationshipType<Protein, Protein.Type, R, RT, CommentType, CommentType.Type> {}