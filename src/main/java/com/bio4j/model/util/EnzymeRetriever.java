package com.bio4j.model.util;

import com.bio4j.model.enzymedb.nodes.Enzyme;

public interface EnzymeRetriever extends NodeRetriever<Enzyme>{

  public Enzyme getEnzymeById(String id);

}
