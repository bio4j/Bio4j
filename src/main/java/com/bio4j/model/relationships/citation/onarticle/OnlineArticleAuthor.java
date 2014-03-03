
package com.bio4j.model.relationships.citation.onarticle;

import com.bio4j.model.nodes.Person;
import com.bio4j.model.nodes.citation.OnlineArticle;
import com.bio4j.model.Relationship;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public interface OnlineArticleAuthor extends Relationship {
    
    //-------GETTERS--------
    public OnlineArticle getOnlineArticle();
    public Person getAuthor();
    
}
