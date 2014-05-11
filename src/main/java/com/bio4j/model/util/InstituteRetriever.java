package com.bio4j.model.util;

import java.util.List;
import com.bio4j.model.uniprot.nodes.Institute;

public interface InstituteRetriever extends NodeRetriever<Institute>{

  public Institute getInstituteByName(String instituteName);

}
