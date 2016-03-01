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

import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.humanistika.ns.tei_completer.Suggestions;
import org.humanistika.oxygen.tei.completer.configuration.beans.RequestInfo;
import org.humanistika.oxygen.tei.completer.configuration.beans.ResponseAction;
import org.humanistika.oxygen.tei.completer.response.ResponseTransformer;
import org.humanistika.oxygen.tei.completer.response.TransformationException;
import org.humanistika.oxygen.tei.completer.response.impl.JSONTransformer;
import org.humanistika.oxygen.tei.completer.response.impl.XMLTransformer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
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
    private final static Map<String, String> namespacePrefixMapper = new HashMap<>();
    static {
        namespacePrefixMapper.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        namespacePrefixMapper.put("http://humanistika.org/ns/tei-completer", "tc");
    }
    private final static ResponseTransformer jsonTransformer = new JSONTransformer();
    private final static ResponseTransformer xmlTransformer = new XMLTransformer();

    private final Client client;

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
        moxyJsonConfig.setNamespacePrefixMapper(namespacePrefixMapper).setNamespaceSeparator(':');
        return moxyJsonConfig.resolver();
    }

    @Override
    public Suggestions getSuggestions(final RequestInfo requestInfo, final String selection, final String dependent, @Nullable final ResponseAction responseAction) {
        try {
            final URL url = getUrl(requestInfo, selection, dependent);
            Invocation.Builder requestBuilder = client
                    .target(url.getProtocol() + "://" + url.getAuthority())
                    .path(url.getPath())
                    .request()
                    .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON);

            if(requestInfo.getUsername() != null) {
                requestBuilder = requestBuilder
                        .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, requestInfo.getUsername())
                        .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, requestInfo.getPassword());
            }

            if(responseAction == null) {
                //response does not require transformation
                return requestBuilder.get(Suggestions.class);
            } else {
                //we need to transform the custom XML or JSON response into Suggestions format
                final Response response = requestBuilder.get();
                try(final InputStream is = response.readEntity(InputStream.class)) {
                    final MediaType mediaType = response.getMediaType();
                    final Path transformation = responseAction.getTransformation();
                    if (mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)
                            || mediaType.isCompatible(MediaType.TEXT_XML_TYPE)) {
                        //custom XML response
                        LOGGER.debug("Transforming XML response from: {} using: {}", url, transformation);
                        return transformXmlResponse(is, transformation);
                    } else if (mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                        LOGGER.debug("Transforming JSON response from: {} using: {}", url, transformation);
                        //custom JSON response
                        return transformJsonResponse(is, transformation);
                    } else {
                        LOGGER.error("Response from {} has unsupported Content-Type: {}", url, mediaType); //TODO(AR) maybe something more visible to the user
                        return new Suggestions();
                    }
                }
            }
        } catch(final IOException | TransformationException e) {
            LOGGER.error(e.getMessage(), e); //TODO(AR) maybe something more visible to the user
            return new Suggestions();
        }
    }

    private Suggestions transformXmlResponse(final InputStream is, final Path transformation) throws IOException, TransformationException {
        try(final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            xmlTransformer.transform(is, transformation, os);

            try(final InputStream resultIs = new ByteArrayInputStream(os.toByteArray())) {
                final JAXBContext context = JAXBContext.newInstance(Suggestions.class);
                final Unmarshaller unmarshaller = context.createUnmarshaller();
                return (Suggestions)unmarshaller.unmarshal(resultIs);
            } catch(final JAXBException e) {
                throw new TransformationException(e);
            }
        }
    }

    private Suggestions transformJsonResponse(final InputStream is, final Path transformation) throws IOException, TransformationException {
        try(final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            jsonTransformer.transform(is, transformation, os);

            try(final InputStream resultIs = new ByteArrayInputStream(os.toByteArray())) {
                final JAXBContext context = org.eclipse.persistence.jaxb.JAXBContextFactory.createContext(new Class[]{Suggestions.class}, null);

                final Unmarshaller unmarshaller = context.createUnmarshaller();
                unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, org.eclipse.persistence.oxm.MediaType.APPLICATION_JSON);
                unmarshaller.setProperty(UnmarshallerProperties.JSON_ATTRIBUTE_PREFIX, null);
                unmarshaller.setProperty(UnmarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, false);
                unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, false);
                unmarshaller.setProperty(UnmarshallerProperties.JSON_NAMESPACE_PREFIX_MAPPER, namespacePrefixMapper);
                unmarshaller.setProperty(UnmarshallerProperties.JSON_NAMESPACE_SEPARATOR, ':');

                final JAXBElement<Suggestions> jaxbElement = unmarshaller.unmarshal(new StreamSource(resultIs), Suggestions.class);
                return jaxbElement.getValue();
            } catch(final JAXBException e) {
                throw new TransformationException(e);
            }
        }
    }
}
