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

import org.humanistika.oxygen.tei.completer.configuration.impl.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Factory for creating instances of TeiCompleter
 *
 * @author Adam Retter, Evolved Binary Ltd <adam.retter@googlemail.com>
 * @version 1.0
 * @serial 20160126
 */
public class ConfigurationFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(ConfigurationFactory.class);
    private final static ConfigurationFactory instance = new ConfigurationFactory();
    private final static String WIN32_USER_APPLICATION_DATA_FOLDER_NAME = "Application Data";
    private final static String CONFIG_FOLDER_NAME = ".bcdh-tei-completer";
    private final static String CONFIG_FILE_NAME_PREFIX = "config";

    private final File configDir;

    private ConfigurationFactory() {
        this.configDir = getConfigDir();
    }

    public final static ConfigurationFactory getInstance() {
        return instance;
    }

    /**
     * Loads the Configuration
     *
     * @return The loaded Configuration
     */
    public Configuration loadConfiguration() {
        return new XmlConfiguration(new File(configDir, CONFIG_FILE_NAME_PREFIX + ".xml"));
    }

    /**
     * Gets the path to the configuration directory for for the current operating system
     *
     * Configuration is user specific and will reside in the Users profile
     *
     * @return The path to the configuration directory
     */
    private File getConfigDir() {
        final String userHome = System.getProperty("user.home");
        final String platform = System.getProperty("platform");

        final File configDir;
        if(platform != null && platform.startsWith("WIN")) {
            final File appData = new File(userHome,  WIN32_USER_APPLICATION_DATA_FOLDER_NAME);
            if(!(appData.exists() && appData.canWrite())) {
                LOGGER.error("Windows Application Data folder is not accessible: " + appData.getAbsolutePath());
            }
            configDir = new File(appData, CONFIG_FOLDER_NAME);
        } else {
            configDir = new File(userHome, CONFIG_FOLDER_NAME);
        }

        if(!configDir.exists()) {
            if(!configDir.mkdirs()) {
                LOGGER.error("Unable to create configuration directory: " + configDir.getAbsolutePath());
            }
        }

        return configDir;
    }
}