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
package org.humanistika.oxygen.tei.completer.remote;

import org.humanistika.ns.tei_completer.Suggestions;
import org.humanistika.oxygen.tei.completer.configuration.beans.RequestInfo;

/**
 * TEI Completer Client interface for getting auto-complete suggestions from a remote server
 *
 * @author Adam Retter, Evolved Binary Ltd <adam.retter@googlemail.com>
 * @version 1.0
 * @serial 20160126
 */
public interface Client {

    /**
     * Retrieve the suggestions for auto-complete from the server
     *
     * @param selection The selection
     * @param dependent The dependent or null
     *
     * @return A list of auto-complete suggestions from the server
     */
    Suggestions getSuggestions(final RequestInfo requestInfo, final String selection, final String dependent);
}
