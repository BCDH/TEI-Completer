package org.humanistika.oxygen.tei.completer;


import org.jetbrains.annotations.Nullable;

import java.util.List;

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
