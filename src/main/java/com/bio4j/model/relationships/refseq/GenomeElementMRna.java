package com.bio4j.model.relationships.refseq;

import com.bio4j.model.nodes.refseq.GenomeElement;
import com.bio4j.model.nodes.refseq.rna.MRNA;
import com.bio4j.model.Relationship;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public interface GenomeElementMRna extends Relationship {
    
  public GenomeElement getGenomeElement();    
  public MRNA getMRNA();
}
