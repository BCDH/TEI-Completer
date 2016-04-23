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

/**
 * Configuration details for authenticating a request
 *
 * @author Adam Retter, Evolved Binary Ltd
 * @version 1.0
 * @serial 20160315
 */
public class Authentication {
    public enum AuthenticationType {
        PREEMPTIVE_BASIC,
        NON_PREEMPTIVE_BASIC,
        DIGEST,
        NON_PREEMPTIVE_BASIC_DIGEST
    }

    private final AuthenticationType authenticationType;
    private final String username;
    private final String password;

    public Authentication(final AuthenticationType authenticationType, final String username, final String password) {
        this.authenticationType = authenticationType;
        this.username = username;
        this.password = password;
    }

    public AuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
