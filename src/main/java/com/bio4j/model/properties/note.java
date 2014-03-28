package com.bio4j.model.properties;

import com.bio4j.model.Property;
import com.bio4j.model.PropertyType;

public interface note extends Property {

  public static enum type implements PropertyType<type, String> {
    note;
    public type value() { return note; }
    public Class<String> getValueClass() { return String.class; }
  }

  public static type TYPE = type.note;

  public String note();
}