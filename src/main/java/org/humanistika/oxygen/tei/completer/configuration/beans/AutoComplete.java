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

import java.util.Map;

/**
 * Configuration details for a element or attribute for which we should
 * attempt to provide auto-complete options
 *
 * @author Adam Retter, Evolved Binary Ltd
 * @version 1.0
 * @serial 20160126
 */
public class AutoComplete {
    private final Map<String, String> namespaceBindings;
    private final String context;
    private final String attribute;
    @Nullable private final Dependent dependent;
    private final Selection selection;
    private final RequestInfo requestInfo;
    @Nullable private final ResponseAction responseAction;

    public AutoComplete(final Map<String, String> namespaceBindings, final String context, final String attribute, final Dependent dependent, final Selection selection, final RequestInfo requestInfo, final ResponseAction responseAction) {
        this.namespaceBindings = namespaceBindings;
        this.context = context;
        this.attribute = attribute;
        this.dependent = dependent;
        this.selection = selection;
        this.requestInfo = requestInfo;
        this.responseAction = responseAction;
    }

    public Map<String, String> getNamespaceBindings() {
        return namespaceBindings;
    }

    public String getContext() {
        return context;
    }

    public String getAttribute() {
        return attribute;
    }

    @Nullable
    public Dependent getDependent() {
        return dependent;
    }

    public Selection getSelection() {
        return selection;
    }

    public RequestInfo getRequestInfo() {
        return requestInfo;
    }

    @Nullable
    public ResponseAction getResponseAction() {
        return responseAction;
    }


}
