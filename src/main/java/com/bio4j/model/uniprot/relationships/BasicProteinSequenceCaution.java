package com.bio4j.model.uniprot.relationships;

import com.bio4j.model.uniprot.nodes.Protein;
import com.bio4j.model.uniprot.nodes.SequenceCaution;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface BasicProteinSequenceCaution{
    
  public String getText();
  public String getStatus();
  public String getEvidence();
  public String getId();
  public String getPosition();
  public String getResource();
  public String getVersion();

  public Protein getProtein();
  public SequenceCaution getSequenceCaution();

  public void setText(String value);
  public void setStatus(String value);
  public void setEvidence(String value);
  public void setId(String value);
  public void setPosition(String value);
  public void setResource(String value);
  public void setVersion(String value);
}
