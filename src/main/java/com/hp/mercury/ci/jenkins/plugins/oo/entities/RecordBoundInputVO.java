package com.hp.mercury.ci.jenkins.plugins.oo.entities;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * User: maromg
 * Date: 22/04/2014
 */
@SuppressWarnings("UnusedDeclaration")
public class RecordBoundInputVO {
    private String name;
    private String termName;
    private String value;

    public RecordBoundInputVO(String name, String termName, String value) {
        this.name = name;
        this.termName = termName;
        this.value = value;
    }

    public RecordBoundInputVO() {}

    public String getName() {
        return name;
    }

    public String getTermName() {
        return termName;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecordBoundInputVO)) return false;

        RecordBoundInputVO that = (RecordBoundInputVO) o;

        return new EqualsBuilder()
                .append(this.name, that.name)
                .append(this.termName, that.termName)
                .append(this.value, that.value)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.name)
                .append(this.termName)
                .append(this.value)
                .toHashCode();
    }
}

