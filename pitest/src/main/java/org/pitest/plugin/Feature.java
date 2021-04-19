package org.pitest.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Feature {

  private final boolean onByDefault;
  private final String  name;
  private final String  description;
  private final List<FeatureParameter> params;

  private Feature(boolean onByDefault, String name, String description, List<FeatureParameter> params) {
    this.onByDefault = onByDefault;
    this.name = name;
    this.description = description;
    this.params = params;
  }

  public static Feature named(String name) {
    return new Feature(false, name, "", Collections.<FeatureParameter>emptyList());
  }

  public Feature withOnByDefault(boolean onByDefault) {
    return new Feature(onByDefault, this.name, this.description, this.params);
  }

  public Feature withDescription(String description) {
    return new Feature(this.onByDefault, this.name, description, this.params);
  }


  public Feature withParameter(FeatureParameter param) {
    final List<FeatureParameter> params = new ArrayList<>(this.params);
    params.add(param);
    return new Feature(this.onByDefault, this.name, this.description, params);
  }

  public String name() {
    return this.name;
  }

  public String description() {
    return this.description;
  }

  public boolean isOnByDefault() {
    return this.onByDefault;
  }

  public List<FeatureParameter> params() {
    return this.params;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Feature other = (Feature) obj;
    return Objects.equals(name, other.name);
  }
}
