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
package org.humanistika.oxygen.tei.completer;

import java.util.List;
import javax.annotation.Nullable;

public class SuggestedAutocomplete {

    public static class UserValue {
        private final String name;
        private final String value;

        public UserValue(final String name, final String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    private final String suggestion;
    @Nullable
    private final String description;
    @Nullable private final List<UserValue> userValues;

    public SuggestedAutocomplete(final String suggestion, final String description, final List<UserValue> userValues) {
        this.suggestion = suggestion;
        this.description = description;
        this.userValues = userValues;
    }

    public String getSuggestion() {
        return suggestion;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public List<UserValue> getUserValues() {
        return userValues;
    }
}
