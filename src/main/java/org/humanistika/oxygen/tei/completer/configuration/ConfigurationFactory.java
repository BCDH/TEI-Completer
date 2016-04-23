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
package org.humanistika.oxygen.tei.completer.configuration;

import org.humanistika.oxygen.tei.completer.configuration.beans.AutoComplete;
import org.humanistika.oxygen.tei.completer.configuration.impl.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Factory for creating instances of TeiCompleter
 *
 * @author Adam Retter, Evolved Binary Ltd
 * @version 1.0
 * @serial 20160126
 */
public class ConfigurationFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(ConfigurationFactory.class);
    private final static ConfigurationFactory instance = new ConfigurationFactory();
    private final static String WIN32_USER_APPLICATION_DATA_FOLDER_NAME = "Application Data";
    private final static String CONFIG_FOLDER_NAME = ".bcdh-tei-completer";
    protected final static String CONFIG_FILE_NAME_PREFIX = "config";

    protected final Path configDir;

    protected ConfigurationFactory() {
        this.configDir = getConfigDir();
    }

    public static ConfigurationFactory getInstance() {
        return instance;
    }

    /**
     * Loads the Configuration
     *
     * @return The loaded Configuration
     */
    public Configuration<? extends AutoComplete> loadConfiguration() {
        return new XmlConfiguration(configDir.resolve(CONFIG_FILE_NAME_PREFIX + ".xml"));
    }

    protected String getConfigFolderName() {
        return CONFIG_FOLDER_NAME;
    }

    /**
     * Gets the path to the configuration directory for for the current operating system
     *
     * Configuration is user specific and will reside in the Users profile
     *
     * @return The path to the configuration directory
     */
    protected Path getConfigDir() {
        final String userHome = System.getProperty("user.home");
        final String platform = System.getProperty("platform");

        final Path configDir;
        if(platform != null && platform.startsWith("WIN")) {
            final Path appData = Paths.get(userHome,  WIN32_USER_APPLICATION_DATA_FOLDER_NAME);
            if(!(Files.exists(appData) && Files.isWritable(appData))) {
                LOGGER.error("Windows Application Data folder is not accessible: {}", appData.toAbsolutePath());
            }
            configDir = appData.resolve(getConfigFolderName());
        } else {
            configDir = Paths.get(userHome, getConfigFolderName());
        }

        if(!Files.exists(configDir)) {
            try {
                return Files.createDirectories(configDir);
            } catch(final IOException e) {
                LOGGER.error("Unable to create configuration directory: " + configDir.toAbsolutePath(), e);
            }
        }

        return configDir;
    }
}
