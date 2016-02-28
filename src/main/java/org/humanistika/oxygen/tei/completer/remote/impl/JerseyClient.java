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

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.humanistika.ns.tei_completer.Suggestions;
import org.humanistika.oxygen.tei.completer.configuration.beans.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Server client implemented using Jersey
 *
 * @author Adam Retter, Evolved Binary Ltd <adam.retter@googlemail.com>
 * @version 1.0
 * @serial 20160126
 */
public class JerseyClient extends AbstractClient {
    private final static Logger LOGGER = LoggerFactory.getLogger(JerseyClient.class);

    final Client client;

    public JerseyClient() {
        this(ClientBuilder.newClient());
    }

    /**
     * Used for injecting a test client
     * in unit tests
     */
    JerseyClient(final Client client) {
        final HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.universalBuilder().build();
//        final HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basicBuilder().build();
        this.client = client
                .register(GZipEncoder.class)
                .register(EncodingFilter.class)
                .register(authFeature)
                .register(createMoxyJsonResolver());
    }

    public static ContextResolver<MoxyJsonConfig> createMoxyJsonResolver() {
        final MoxyJsonConfig moxyJsonConfig = new MoxyJsonConfig();
        final Map<String, String> namespacePrefixMapper = new HashMap<>();
        namespacePrefixMapper.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        namespacePrefixMapper.put("http://humanistika.org/ns/tei-completer", "tc");
        moxyJsonConfig.setNamespacePrefixMapper(namespacePrefixMapper).setNamespaceSeparator(':');
        return moxyJsonConfig.resolver();
    }

    @Override
    public Suggestions getSuggestions(final RequestInfo requestInfo, final String selection, final String dependent) {

        try {
            final URL url = getUrl(requestInfo, selection, dependent);
            Invocation.Builder requestBuilder = client
                    .target(url.getProtocol() + "://" + url.getAuthority())
                    .path(url.getPath())
                    .request()
                    .accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON);

            if(requestInfo.getUsername() != null) {
                requestBuilder = requestBuilder
                        .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, requestInfo.getUsername())
                        .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, requestInfo.getPassword());
            }

            return requestBuilder.get(Suggestions.class);
        } catch(final MalformedURLException e) {
            LOGGER.error(e.getMessage(), e); //TODO(AR) maybe something more visible to the user
            return new Suggestions();
        }
    }
}
