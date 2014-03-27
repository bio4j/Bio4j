package com.bio4j.model.util;

import com.bio4j.model.nodes.FeatureType;

public interface FeatureTypeRetriever extends NodeRetriever<FeatureType> {

  public FeatureType getFeatureTypeByName(String featureTypeName);

}