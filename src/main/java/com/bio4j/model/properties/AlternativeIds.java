package com.bio4j.model.properties;

import com.ohnosequences.typedGraphs.Property;
import com.ohnosequences.typedGraphs.PropertyType;

import com.ohnosequences.typedGraphs.Element;
import com.ohnosequences.typedGraphs.ElementType;

public interface AlternativeIds <N extends Element<N,NT>, NT extends Enum<NT> & ElementType<N,NT>> 
  extends Property<N, NT> {

  // the property method
  public String[] alternativeIds();

  // static type method
  public static <
    N extends Element<N,NT> & AlternativeIds<N,NT>, 
    NT extends Enum<NT> & ElementType<N,NT>
  > Type<N,NT> TYPE(NT elementType) { return new Type<N,NT>(elementType); }

  // convenience type
  public class Type <N extends Element<N,NT> & AlternativeIds<N,NT>, NT extends Enum<NT> & ElementType<N,NT>> 
    extends PropertyType<N, NT, AlternativeIds<N,NT>, Type<N,NT>, String[]> {

    public Type(NT elementType) {
      super(elementType, "alternativeIds");
    }

    public Class<String[]> valueClass() { return String[].class; }
  }
}