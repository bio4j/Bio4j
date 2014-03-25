
package com.bio4j.model.nodes.refseq;

import com.bio4j.model.Node;
import com.bio4j.model.NodeType;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public interface Gene extends Node<Gene, Gene.type> {
    
  enum type implements NodeType<Gene, Gene.type> {

    gene;
    public type value() { return gene; }
  }
    
    //---------GETTERS------------
    public String getPositions();
    public String getNote();
    public GenomeElement getGenomeElement();


    //---------SETTERS-------
    public void setPositions(String value);
    public void setNote(String value);

}