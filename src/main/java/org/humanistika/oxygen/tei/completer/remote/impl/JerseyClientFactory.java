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

import org.humanistika.oxygen.tei.completer.remote.Client;
import org.humanistika.oxygen.tei.completer.remote.ClientFactory;

/**
 * Factory for creating instances of JerseyClient
 *
 * @author Adam Retter, Evolved Binary Ltd
 * @version 1.0
 * @serial 20160126
 */
public class JerseyClientFactory implements ClientFactory {

    private final static JerseyClientFactory instance = new JerseyClientFactory();

    private JerseyClientFactory() {
    }

    public final static JerseyClientFactory getInstance() {
        return instance;
    }

    @Override
    public Client createClient(final AuthenticationType authenticationType) {
        return new JerseyClient(authenticationType);
    }
}