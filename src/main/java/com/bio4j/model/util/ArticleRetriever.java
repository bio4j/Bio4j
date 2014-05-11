package com.bio4j.model.util;

import java.util.List;

import com.bio4j.model.uniprot.relationships.references.Article;

public interface ArticleRetriever extends RelationshipRetriever<Article> {

  public List<? extends Article> getArticlesByTitle(String articleTitle);    
  public Article getArticleByMedlineId(String articleMedlineId);
  public Article getArticleByDoiId(String articleDoiId);    
  public Article getArticleByPubmedId(String articlePubmedId);

}
