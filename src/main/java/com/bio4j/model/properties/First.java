package com.bio4j.model.properties;

import com.ohnosequences.typedGraphs.Element;
import com.ohnosequences.typedGraphs.ElementType;
import com.ohnosequences.typedGraphs.Property;
import com.ohnosequences.typedGraphs.PropertyType;

public interface First <N extends Element<N,NT>, NT extends Enum<NT> & ElementType<N,NT>> 
	extends Property<N, NT> {
		
	// the property method
	public Integer first();
	
	// static type method
	public static <
	  N extends Element<N,NT> & First<N,NT>, 
	  NT extends Enum<NT> & ElementType<N,NT>
	> Type<N,NT> TYPE(NT elementType) { return new Type<N,NT>(elementType); }
	
	// convenience type
	public class Type <N extends Element<N,NT> & First<N,NT>, NT extends Enum<NT> & ElementType<N,NT>> 
	  extends PropertyType<N, NT, First<N,NT>, Type<N,NT>, Integer> {
	
	  public Type(NT elementType) {
	    super(elementType, "first");
	  }
	
	  public Class<Integer> valueClass() { return Integer.class; }
	}
}