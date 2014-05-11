package com.bio4j.model.uniprot.relationships;
import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;
import com.ohnosequences.typedGraphs.RelationshipType.Arity;
import com.bio4j.model.uniprot.nodes.Person;
import com.bio4j.model.uniprot.nodes.Thesis;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface ThesisAuthor extends Relationship <
	Thesis, Thesis.Type,
	ThesisAuthor, ThesisAuthor.Type,
	Person, Person.Type
	> {
	
	public static Type TYPE = Type.thesisAuthor;
	enum Type implements RelationshipType <
		Thesis, Thesis.Type,
		ThesisAuthor, ThesisAuthor.Type,
		Person, Person.Type
		> {
		thesisAuthor;
		  public Type value() { return thesisAuthor; }
		  public Arity arity() { return Arity.manyToMany; }
		  public Thesis.Type sourceType() { return Thesis.TYPE; }
		  public Person.Type targetType() { return Person.TYPE; }
	
	}
	
	public Thesis source();
	public Person target();

}
