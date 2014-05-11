package com.bio4j.model.uniprot.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;
import com.bio4j.model.properties.First;
import com.bio4j.model.properties.Last;
import com.bio4j.model.properties.Title;
import com.bio4j.model.properties.Volume;
import com.bio4j.model.uniprot.nodes.Protein;
import com.bio4j.model.uniprot.nodes.Book;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface BookProteinCitation extends Relationship <
    Book, Book.Type,
    BookProteinCitation, BookProteinCitation.Type,
    Protein, Protein.Type
    >,
    Title<BookProteinCitation, BookProteinCitation.Type>,
    Volume<BookProteinCitation, BookProteinCitation.Type>,
    First<BookProteinCitation, BookProteinCitation.Type>,
    Last<BookProteinCitation, BookProteinCitation.Type>
    {

    public static enum Type implements RelationshipType <
        Book, Book.Type,
        BookProteinCitation, BookProteinCitation.Type,
        Protein, Protein.Type
    > {
        BookProteinCitation;
        public Type value() { return BookProteinCitation; }
        public Arity arity() { return Arity.manyToMany; }
        public Book.Type sourceType() { return Book.TYPE; }
        public Protein.Type targetType() { return Protein.TYPE; }

    }
    
    public Book source();
    public Protein target();


}
