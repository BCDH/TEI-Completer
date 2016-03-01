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
import org.humanistika.oxygen.tei.completer.configuration.beans.AutoComplete;
import org.humanistika.oxygen.tei.completer.configuration.beans.Dependent;
import org.humanistika.oxygen.tei.completer.configuration.beans.RequestInfo;
import org.humanistika.oxygen.tei.completer.configuration.beans.ResponseAction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
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
 * @author Adam Retter, Evolved Binary Ltd <adam.retter@googlemail.com>
 * @version 1.0
 * @serial 20160126
 */
public class XmlConfiguration implements Configuration {
    private final static Logger LOGGER = LoggerFactory.getLogger(XmlConfiguration.class);
    private final Path configFile;
    private List<AutoComplete> autoCompletes = null;

    public XmlConfiguration(final Path configFile) {
        this.configFile = configFile;
    }

    @Override
    public List<AutoComplete> getAutoCompletes() {
        synchronized(this) {
            if(this.autoCompletes == null) {
                this.autoCompletes = loadAutoCompletes();
            }
        }
        return this.autoCompletes;
    }

    @Nullable
    private List<AutoComplete> loadAutoCompletes() {
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

    private final List<AutoComplete> expandConfig(final Config config) {
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
                        autoComplete.getDependent().getValue()
                );
            }

            final String username = resolveUsername(config.getServer(), autoComplete.getRequest().getServer());
            final String password = resolvePassword(config.getServer(), autoComplete.getRequest().getServer());

            final RequestInfo requestInfo = new RequestInfo(
                    expandUrl(config.getServer(), autoComplete.getRequest(), i+1, username, password),
                    username,
                    password
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
                    autoComplete.getSelection(),
                    requestInfo,
                    responseAction
            ));
        }

        return autoCompletes;
    }

    private String expandUrl(final Server global, final Request specific, final int index, final String username, final String password) {
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
        if(username != null) {
            url = url.replace(USERNAME.var(), username);
        }
        if(password != null) {
            url = url.replace(PASSWORD.var(), password);
        }
        return url;
    }

    @Nullable
    private String resolveUsername(final Server global, final Server specific) {
        if(specific != null) {
            return specific.getUsername();
        } else if(global != null) {
            return global.getUsername();
        } else {
            return null;
        }
    }

    @Nullable
    private String resolvePassword(final Server global, final Server specific) {
        if(specific != null) {
            return specific.getUsername();
        } else if(global != null) {
            return global.getUsername();
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
