package com.bio4j.model.uniprot.relationships;

import com.ohnosequences.typedGraphs.Relationship;
import com.ohnosequences.typedGraphs.RelationshipType;
import com.bio4j.model.uniprot.nodes.Protein;
import com.bio4j.model.uniprot.nodes.Submission;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public interface SubmissionProteinCitation extends Relationship <
	Submission, Submission.Type,
	SubmissionProteinCitation, SubmissionProteinCitation.Type,
	Protein, Protein.Type
	> {
	
	enum Type implements RelationshipType <
		Submission, Submission.Type,
		SubmissionProteinCitation, SubmissionProteinCitation.Type,
	    Protein, Protein.Type
	> {
	    SubmissionProteinCitation;
	    public Type value() { return SubmissionProteinCitation; }
	    public Arity arity() { return Arity.manyToMany; }
	    public Submission.Type sourceType() { return Submission.TYPE; }
	    public Protein.Type targetType() { return Protein.TYPE; }
	
	}
    
    //----------GETTERS----------------
    public Protein getProtein();
    public Submission getSubmission();

    
}
