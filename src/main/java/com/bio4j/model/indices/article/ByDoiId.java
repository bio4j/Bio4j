package com.bio4j.model.indices.article;

import com.bio4j.model.indices.NodeUniqueIndex;
import com.bio4j.model.uniprot.nodes.Article;

public interface ByDoiId extends NodeUniqueIndex<Article, Article.type, String> {}
