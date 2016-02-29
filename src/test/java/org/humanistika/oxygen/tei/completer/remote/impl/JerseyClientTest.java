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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.humanistika.ns.tei_completer.Suggestion;
import org.humanistika.ns.tei_completer.Suggestions;
import org.humanistika.oxygen.tei.completer.configuration.beans.RequestInfo;
import org.humanistika.oxygen.tei.completer.configuration.beans.ResponseAction;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXB;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for {@link org.humanistika.oxygen.tei.completer.remote.impl.JerseyClient}
 *
 * @author Adam Retter, Evolved Binary Ltd <adam.retter@googlemail.com>
 * @version 1.0
 * @serial 20160126
 */
public class JerseyClientTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(MockServer.class)
                .register(RolesAllowedDynamicFeature.class)
                .register(new MockSecurityFilter(), Priorities.AUTHENTICATION)
                .register(JerseyClient.createMoxyJsonResolver());
    }

    @Path("multext")
    @PermitAll
    public static class MockServer {

        @GET
        @Path("getlemma/xml/{selection}")
        @Produces({MediaType.APPLICATION_XML})
        public Suggestions getLemmaSelection_Xml(@PathParam("selection") final String selection) {
            return getTestSuggestions(selection, null);
        }

        @GET
        @Path("getlemma/xml/{selection}/{dependent}")
        @Produces({MediaType.APPLICATION_XML})
        public Suggestions getLemmaSelectionWithDependent_Xml(@PathParam("selection") final String selection, @PathParam("dependent") final String dependent) {
            return getTestSuggestions(selection, dependent);
        }

        /**
         * Same as {@link MockServer#getLemmaSelection_Xml(String)} except the
         * output is forcefully encoded as GZIP
         */
        @GET
        @Path("getlemma/gzip/xml/{selection}")
        public Response getLemmaSelection_Gzip_Xml(@PathParam("selection") final String selection) throws IOException {
            try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                try(final GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                    JAXB.marshal(getTestSuggestions(selection, null), gzos);
                }

                return Response.status(Response.Status.OK)
                        .entity(baos.toByteArray())
                        .encoding("gzip")
                        .build();
            }
        }

        /**
         * Same as {@link MockServer#getLemmaSelectionWithDependent_Xml(String, String)} except the
         * output is forcefully encoded as GZIP
         */
        @GET
        @Path("getlemma/gzip/xml/{selection}/{dependent}")
        @Produces({MediaType.APPLICATION_XML})
        public Response getLemmaSelectionWithDependent_Gzip_Xml(@PathParam("selection") final String selection, @PathParam("dependent") final String dependent) throws IOException {
            try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                try(final GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                    JAXB.marshal( getTestSuggestions(selection, dependent), gzos);
                }

                return Response.status(Response.Status.OK)
                        .entity(baos.toByteArray())
                        .encoding("gzip")
                        .build();
            }
        }

        @GET
        @Path("getlemma/custom/xml/{selection}")
        @Produces({MediaType.APPLICATION_XML})
        public String getLemmaSelection_Custom_Xml(@PathParam("selection") final String selection) {
            return getTestSuggestions_CustomXml(selection, null);
        }

        @GET
        @Path("getlemma/custom/xml/{selection}/{dependent}")
        @Produces({MediaType.APPLICATION_XML})
        public String getLemmaSelectionWithDependent_Custom_Xml(@PathParam("selection") final String selection, @PathParam("dependent") final String dependent) {
            return getTestSuggestions_CustomXml(selection, dependent);
        }


        /* resources which produce JSON responses */

        @GET
        @Path("getlemma/json/{selection}")
        @Produces({MediaType.APPLICATION_JSON})
        public Suggestions getLemmaSelection_Json(@PathParam("selection") final String selection) {
            return getTestSuggestions(selection, null);
        }

        @GET
        @Path("getlemma/json/{selection}/{dependent}")
        @Produces({MediaType.APPLICATION_JSON})
        public Suggestions getLemmaSelectionWithDependent_Json(@PathParam("selection") final String selection, @PathParam("dependent") final String dependent) {
            return getTestSuggestions(selection, dependent);
        }

        @GET
        @Path("getlemma/custom/json/{selection}")
        @Produces({MediaType.APPLICATION_JSON})
        public String getLemmaSelection_Custom_Json(@PathParam("selection") final String selection) {
            return getTestSuggestions_CustomJson(selection, null);
        }

        @GET
        @Path("getlemma/custom/json/{selection}/{dependent}")
        @Produces({MediaType.APPLICATION_JSON})
        public String getLemmaSelectionWithDependent_Custom_Json(@PathParam("selection") final String selection, @PathParam("dependent") final String dependent) {
            return getTestSuggestions_CustomJson(selection, dependent);
        }


        /* resources for tests with security */

        @GET
        @Path("secure/getlemma/xml/{selection}")
        @Produces({MediaType.APPLICATION_XML})
        @RolesAllowed(AUTHENTICATED_ROLE)
        public Suggestions secure_GetLemmaSelection_Xml(@PathParam("selection") final String selection) {
            return getTestSuggestions(selection, null);
        }

        @GET
        @Path("secure/getlemma/xml/{selection}/{dependent}")
        @Produces({MediaType.APPLICATION_XML})
        @RolesAllowed(AUTHENTICATED_ROLE)
        public Suggestions secure_GetLemmaSelectionWithDependent_Xml(@PathParam("selection") final String selection, @PathParam("dependent") final String dependent) {
            return getTestSuggestions(selection, dependent);
        }
    }

    @Test
    public void getLemmaSelection_Xml() {
        final String selection = "some-selection";
        final String dependent = null;

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/getlemma/xml/" + RequestInfo.UrlVar.SELECTION.var(), null, null);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, null);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    @Test
    public void getLemmaSelectionDependent_Xml() {
        final String selection = "some-selection";
        final String dependent = "some-dependent";

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/getlemma/xml/" + RequestInfo.UrlVar.SELECTION.var() + "/" + RequestInfo.UrlVar.DEPENDENT.var(), null, null);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, null);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    @Test
    public void getLemmaSelection_Custom_Xml() throws URISyntaxException {
        final String selection = "some-selection";
        final String dependent = null;

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/getlemma/custom/xml/" + RequestInfo.UrlVar.SELECTION.var(), null, null);
        final java.nio.file.Path testTransform = Paths.get(getClass().getResource("custom-transform-test.xslt").toURI());
        final ResponseAction responseAction = new ResponseAction(testTransform);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, responseAction);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    @Test
    public void getLemmaSelectionDependent_Custom_Xml() throws URISyntaxException {
        final String selection = "some-selection";
        final String dependent = "some-dependent";

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/getlemma/custom/xml/" + RequestInfo.UrlVar.SELECTION.var() + "/" + RequestInfo.UrlVar.DEPENDENT.var(), null, null);
        final java.nio.file.Path testTransform = Paths.get(getClass().getResource("custom-transform-test.xslt").toURI());
        final ResponseAction responseAction = new ResponseAction(testTransform);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, responseAction);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    @Test
    public void getLemmaSelection_Gzip_Xml() {
        final String selection = "some-selection";
        final String dependent = null;

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/getlemma/gzip/xml/" + RequestInfo.UrlVar.SELECTION.var(), null, null);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, null);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    @Test
    public void getLemmaSelectionDependent_Gzip_Xml() {
        final String selection = "some-selection";
        final String dependent = "some-dependent";

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/getlemma/gzip/xml/" + RequestInfo.UrlVar.SELECTION.var() + "/" + RequestInfo.UrlVar.DEPENDENT.var(), null, null);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, null);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    @Test
    public void getLemmaSelection_Json() {
        final String selection = "some-selection";
        final String dependent = null;

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/getlemma/json/" + RequestInfo.UrlVar.SELECTION.var(), null, null);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, null);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    @Test
    public void getLemmaSelectionDependent_Json() {
        final String selection = "some-selection";
        final String dependent = "some-dependent";

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/getlemma/json/" + RequestInfo.UrlVar.SELECTION.var() + "/" + RequestInfo.UrlVar.DEPENDENT.var(), null, null);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, null);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    @Test
    public void getLemmaSelection_Custom_Json() throws URISyntaxException {
        final String selection = "some-selection";
        final String dependent = null;

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/getlemma/custom/json/" + RequestInfo.UrlVar.SELECTION.var(), null, null);
        final java.nio.file.Path testTransform = Paths.get(getClass().getResource("custom-transform-test.js").toURI());
        final ResponseAction responseAction = new ResponseAction(testTransform);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, responseAction);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    @Test
    public void getLemmaSelectionDependent_Custom_Json() throws URISyntaxException {
        final String selection = "some-selection";
        final String dependent = "some-dependent";

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/getlemma/custom/json/" + RequestInfo.UrlVar.SELECTION.var() + "/" + RequestInfo.UrlVar.DEPENDENT.var(), null, null);
        final java.nio.file.Path testTransform = Paths.get(getClass().getResource("custom-transform-test.js").toURI());
        final ResponseAction responseAction = new ResponseAction(testTransform);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, responseAction);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    @Ignore("Requires non-preemptive support in Jersey Server. See https://java.net/jira/browse/JERSEY-2908")
    @Test
    public void secure_GetLemmaSelection_Xml() {
        final String selection = "some-selection";
        final String dependent = null;

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/secure/getlemma/xml/" + RequestInfo.UrlVar.SELECTION.var(), TEST_USERNAME, TEST_PASSWORD);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, null);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    @Ignore("Requires non-preemptive support in Jersey Server. See https://java.net/jira/browse/JERSEY-2908")
    @Test
    public void secure_GetLemmaSelectionDependent_Xml() {
        final String selection = "some-selection";
        final String dependent = "some-dependent";

        final RequestInfo requestInfo = new RequestInfo(getBaseUri() + "multext/secure/getlemma/xml/" + RequestInfo.UrlVar.SELECTION.var() + "/" + RequestInfo.UrlVar.DEPENDENT.var(), TEST_USERNAME, TEST_PASSWORD);
        final Suggestions suggestions = new JerseyClient(client()).getSuggestions(requestInfo, selection, dependent, null);

        final Suggestions expectedSuggestions = getTestSuggestions(selection, dependent);

        assertEquals(expectedSuggestions.getSuggestion(), suggestions.getSuggestion());
    }

    public static Suggestions getTestSuggestions(final String selection, @Nullable final String dependent) {
        final Suggestions suggestions = new Suggestions();

        final Suggestion suggestion1 = new Suggestion();
        suggestion1.setValue("suggestion1");
        if(dependent == null) {
            suggestion1.setDescription(selection);
        } else {
            suggestion1.setDescription(selection + ":" + dependent);
        }
        suggestions.getSuggestion().add(suggestion1);

        final Suggestion suggestion2 = new Suggestion();
        suggestion2.setValue("suggestion2");
        if(dependent == null) {
            suggestion2.setDescription(selection);
        } else {
            suggestion2.setDescription(selection + ":" + dependent);
        }
        suggestions.getSuggestion().add(suggestion2);

        return suggestions;
    }

    public static String getTestSuggestions_CustomXml(final String selection, @Nullable final String dependent) {
        final String xml =
                "<sgns>\n" +
                "    <sgn>\n" +
                "        <v>suggestion1</v>\n" +
                "        <d>" + (dependent == null ? selection : (selection + ":" + dependent)) + "</d>\n" +
                "    </sgn>\n" +
                "    <sgn>\n" +
                "        <v>suggestion2</v>\n" +
                "        <d>" + (dependent == null ? selection : (selection + ":" + dependent)) + "</d>\n" +
                "    </sgn>\n" +
                "</sgns>";

        return xml;
    }

    public static String getTestSuggestions_CustomJson(final String selection, @Nullable final String dependent) {
        final String json =
                "{\n" +
                "    \"sgns\": [\n" +
                "        {\n" +
                "            \"v\" : \"suggestion1\",\n" +
                "            \"d\" : \"" + (dependent == null ? selection : (selection + ":" + dependent)) + "\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"v\" : \"suggestion2\",\n" +
                "            \"d\" : \"" + (dependent == null ? selection : (selection + ":" + dependent)) + "\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        return json;
    }

    /* supporting classes below for security based tests */
    private final static String AUTHENTICATED_ROLE = "authenticated-user";
    private final static String TEST_USERNAME = "user1";
    private final static String TEST_PASSWORD = "pass1";

    public static class MockSecurityFilter implements ContainerRequestFilter {
        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(new MockSecurityContext(requestContext));
        }
    }

    public static class MockSecurityContext implements SecurityContext {
        private final static String BASIC = "Basic";
        private final static String DIGEST = "Digest";
        private final ContainerRequestContext requestContext;

        public MockSecurityContext(final ContainerRequestContext requestContext) {
            this.requestContext = requestContext;
        }

        @Override
        public Principal getUserPrincipal() {
            String auth = requestContext.getHeaderString("Authorization");
            if(auth != null && auth.startsWith("Basic ")) {
                auth = auth.replace("Basic ", "");
                try {
                    final String userPassDecoded = new String(Base64.decodeBase64(auth), "UTF-8");
                    final String userPass[] = userPassDecoded.split(":");
                    if(userPass.length == 2) {
                        if(userPass[0].equals(TEST_USERNAME) && userPass[1].equals(TEST_PASSWORD)) {
                            return new AuthenticatedPrincipal(userPass[0]);
                        }
                    }
                } catch(final UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            return null;
        }

        @Override
        public boolean isUserInRole(final String role) {
            final Principal principal = getUserPrincipal();
            return principal != null && role.equals(AUTHENTICATED_ROLE) && principal instanceof AuthenticatedPrincipal;
        }

        @Override
        public boolean isSecure() {
            final String scheme = requestContext.getUriInfo().getBaseUri().getScheme();
            return scheme.equals("https");
        }

        @Override
        public String getAuthenticationScheme() {
            final String auth = requestContext.getHeaderString("Authorization");
            if(auth != null && !auth.isEmpty()) {
                if (auth.startsWith(BASIC)) {
                    return SecurityContext.BASIC_AUTH;
                } else if (auth.startsWith(DIGEST)) {
                    return SecurityContext.DIGEST_AUTH;
                }
            }
            return null;
        }
    }

    public static class AuthenticatedPrincipal implements UserPrincipal {

        private final String username;

        public AuthenticatedPrincipal(final String username) {
            this.username = username;
        }

        @Override
        public String getName() {
            return username;
        }
    }
}
