/**
 * TEI Completer
 * An Oxygen XML Editor plugin for customizable attribute and value completion for TEI P5 documents
 * Copyright (C) 2016 Belgrade Center for Digital Humanities
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.humanistika.oxygen.tei.completer.configuration.beans;

import javax.annotation.Nullable;

/**
 * Configuration Details of an optional dependency of the {@link AutoComplete#context}
 */
public class Dependent {
    @Nullable private final String def;  //default
    private final String attribute;

    private final String label;

    public Dependent(@Nullable final String def, final String attribute, final String label) {
        this.def = def;
        this.attribute = attribute;
        this.label = label;
    }

    @Nullable
    public String getDefault() {
        return def;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getLabel() {
        return label;
    }
}
