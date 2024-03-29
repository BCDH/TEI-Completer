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
package org.humanistika.oxygen.tei.completer.configuration.impl;

import org.humanistika.ns.tei_completer.Config;
import org.humanistika.ns.tei_completer.NamespaceBindings;
import org.humanistika.ns.tei_completer.Request;
import org.humanistika.ns.tei_completer.Server;
import org.humanistika.oxygen.tei.completer.configuration.Configuration;
import org.humanistika.oxygen.tei.completer.configuration.beans.*;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.humanistika.oxygen.tei.completer.configuration.beans.RequestInfo.UrlVar.*;

/**
 * Load the TEI-Completer Configuration from an XML
 * file in the users home directory
 *
 * On Unix/Linux platform the properties file will be loaded from ~/.bcdh-tei-completer/config.xml
 *
 * On Windows platforms the properties file will be loaded from %USER_PROFILE%/Application Data/.bcdh-tei-completer/config.xml
 *
 * @author Adam Retter, Evolved Binary Ltd
 * @version 1.0
 * @serial 20160126
 */
public class XmlConfiguration<T extends AutoComplete> implements Configuration<T> {
    private final static Logger LOGGER = LoggerFactory.getLogger(XmlConfiguration.class);
    protected final Path configFile;
    private List<T> autoCompletes = null;

    public XmlConfiguration(final Path configFile) {
        this.configFile = configFile;
    }

    @Override
    public List<T> getAutoCompletes() {
        synchronized(this) {
            if(this.autoCompletes == null) {
                this.autoCompletes = loadAutoCompletes();
            }
        }
        return this.autoCompletes;
    }

    @Nullable
    protected List<T> loadAutoCompletes() {
        if(Files.notExists(configFile)) {
            LOGGER.error("Configuration file does not exist: {}", configFile.toAbsolutePath());
            return null;
        }

        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Config.class);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            final Config config = (Config)unmarshaller.unmarshal(configFile.toFile());
            return expandConfig(config);
        } catch(final JAXBException e) {
            LOGGER.error("Unable to load config: " + configFile.toAbsolutePath(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<T> expandConfig(final Config config) {
        final List<AutoComplete> autoCompletes = new ArrayList<>();
        for(int i = 0; i <  config.getAutoComplete().size(); i++) {
            final org.humanistika.ns.tei_completer.AutoComplete autoComplete  = config.getAutoComplete().get(i);
            final Map<String, String> namespaceBindings = mergeNamespaceBindings(
                    config.getNamespaceBindings(), autoComplete.getNamespaceBindings());
            final Dependent dependent;
            if(autoComplete.getDependent() == null) {
                dependent = null;
            } else {
                dependent = new Dependent(
                        autoComplete.getDependent().getDefault(),
                        autoComplete.getDependent().getValue(),
                        autoComplete.getDependent().getLabel()
                );
            }

            final Selection selection;
            if(autoComplete.getSelection() == null) {
                selection = null;
            } else {
                selection = new Selection(
                        autoComplete.getSelection().getValue(),
                        autoComplete.getSelection().getLabel()
                );
            }

            final Authentication requestAuthentication = resolveAuthentication(config.getServer(), autoComplete.getRequest().getServer());
            final RequestInfo requestInfo = new RequestInfo(
                    expandUrl(config.getServer(), autoComplete.getRequest(), i+1, requestAuthentication),
                    requestAuthentication
            );

            final ResponseAction responseAction;
            if(autoComplete.getResponse() == null) {
                responseAction = null;
            } else {
                responseAction = new ResponseAction(configFile.resolveSibling(autoComplete.getResponse().getTransformation()));
            }

            autoCompletes.add(new AutoComplete(
                    namespaceBindings,
                    autoComplete.getContext(),
                    autoComplete.getAttribute(),
                    dependent,
                    selection,
                    requestInfo,
                    responseAction
            ));
        }

        return (List<T>)autoCompletes;
    }

    private String expandUrl(final Server global, final Request specific, final int index, final Authentication authentication) {
        final String baseUrl;
        if(specific.getServer() != null) {
            baseUrl = specific.getServer().getBaseUrl();
        } else if(global != null) {
            baseUrl = global.getBaseUrl();
        } else {
            LOGGER.warn("No base URL specified for auto-complete: {}", index);
            baseUrl = "";
        }

        String url = specific.getUrl();
        url = url.replace(BASE_URL.var(), baseUrl);
        if(authentication != null) {
            url = url.replace(USERNAME.var(), authentication.getUsername());
            url = url.replace(PASSWORD.var(), authentication.getPassword());
        }
        return url;
    }

    @Nullable
    private Authentication resolveAuthentication(final Server global, final Server specific) {
        final org.humanistika.ns.tei_completer.Authentication configAuth;
        if(specific != null) {
            configAuth = specific.getAuthentication();
        } else if(global != null) {
            configAuth = global.getAuthentication();
        } else {
            configAuth = null;
        }

        if(configAuth != null) {
            final Authentication.AuthenticationType authenticationType;
            switch(configAuth.getType()) {
                case PREEMPTIVE_BASIC:
                    authenticationType = Authentication.AuthenticationType.PREEMPTIVE_BASIC;
                    break;

                case BASIC:
                    authenticationType = Authentication.AuthenticationType.NON_PREEMPTIVE_BASIC;
                    break;

                case DIGEST:
                    authenticationType = Authentication.AuthenticationType.DIGEST;
                    break;

                case BASIC_DIGEST:
                    authenticationType = Authentication.AuthenticationType.NON_PREEMPTIVE_BASIC_DIGEST;
                    break;

                default:
                    throw new IllegalStateException("Unknown authentication type: " + configAuth.getType());
            }

            return new Authentication(authenticationType, configAuth.getUsername(), configAuth.getPassword());
        } else {
            return null;
        }
    }

    private Map<String,String> mergeNamespaceBindings(@Nullable final NamespaceBindings global, @Nullable final NamespaceBindings specific) {
        final Map<String, String> namespaceBindings = new HashMap<>();
        if(global != null) {
            addBindings(namespaceBindings, global.getBinding());
        }
        if(specific != null) {
            addBindings(namespaceBindings, specific.getBinding());
        }
        return namespaceBindings;
    }

    private void addBindings(final Map<String, String> namespaceBindings, final List<NamespaceBindings.Binding> bindings) {
        for(final NamespaceBindings.Binding binding : bindings) {
            namespaceBindings.put(binding.getPrefix(), binding.getNamespace());
        }
    }
}
