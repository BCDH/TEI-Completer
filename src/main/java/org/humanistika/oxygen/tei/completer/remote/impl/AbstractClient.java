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
package org.humanistika.oxygen.tei.completer.remote.impl;

import org.humanistika.oxygen.tei.completer.configuration.beans.RequestInfo;
import org.humanistika.oxygen.tei.completer.remote.Client;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for server clients
 *
 * @author Adam Retter, Evolved Binary Ltd <adam.retter@googlemail.com>
 * @version 1.0
 * @serial 20160126
 */
public abstract class AbstractClient implements Client {

    /**
     * Get the URL for connecting to the server
     *
     * Completes the URL from autoComplete with the selection and dependent
     *
     * @param requestInfo The base request info
     * @param selection The selection
     * @param dependent The dependent or null
     *
     * @return The URL for connecting to the server
     */
    protected URL getUrl(final RequestInfo requestInfo, final String selection, final @Nullable String dependent) throws MalformedURLException {
        final Map<RequestInfo.UrlVar, String> substitutions = new HashMap<>();
        substitutions.put(RequestInfo.UrlVar.SELECTION, selection);
        if(dependent != null) {
            substitutions.put(RequestInfo.UrlVar.DEPENDENT, dependent);
        }

        return requestInfo.getUrl(substitutions);
    }
}
