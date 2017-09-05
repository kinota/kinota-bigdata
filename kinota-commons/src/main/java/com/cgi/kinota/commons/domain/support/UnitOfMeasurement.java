/**
 * Kinota (TM) Copyright (C) 2017 CGI Group Inc.
 *
 * Licensed under GNU Lesser General Public License v3.0 (LGPLv3);
 * you may not use this file except in compliance with the License.
 *
 * This software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * v3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License v3.0 for more details.
 *
 * You can receive a copy of the GNU Lesser General Public License
 * from:
 *
 * https://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 */

package com.cgi.kinota.commons.domain.support;

import java.net.URI;

/**
 * Created by bmiles on 2/28/17.
 */
public class UnitOfMeasurement {

    private String name;
    private String symbol;
    private URI definition;

    public UnitOfMeasurement() {}

    public UnitOfMeasurement(String name, String symbol, URI definition) {
        this.name = name;
        this.symbol = symbol;
        this.definition = definition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public URI getDefinition() {
        return definition;
    }

    public void setDefinition(URI definition) {
        this.definition = definition;
    }

    @Override
    public String toString() {
        return "UnitOfMeasurement{" +
                "name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", definition=" + definition +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnitOfMeasurement)) return false;

        UnitOfMeasurement that = (UnitOfMeasurement) o;

        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getSymbol() != null ? !getSymbol().equals(that.getSymbol()) : that.getSymbol() != null) return false;
        return getDefinition() != null ? getDefinition().equals(that.getDefinition()) : that.getDefinition() == null;
    }

    @Override
    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (getSymbol() != null ? getSymbol().hashCode() : 0);
        result = 31 * result + (getDefinition() != null ? getDefinition().hashCode() : 0);
        return result;
    }
}
