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

import com.evolvedbinary.xpath.parser.ast.*;

import java.util.*;

import org.humanistika.ns.tei_completer.Suggestion;
import org.humanistika.ns.tei_completer.Suggestions;
import org.humanistika.oxygen.tei.completer.GUI.newSuggestionForm;
import org.humanistika.oxygen.tei.completer.configuration.beans.Authentication;
import org.humanistika.oxygen.tei.completer.configuration.beans.AutoComplete;
import org.humanistika.oxygen.tei.completer.configuration.Configuration;
import org.humanistika.oxygen.tei.completer.configuration.ConfigurationFactory;
import org.humanistika.oxygen.tei.completer.configuration.beans.Dependent;
import org.humanistika.oxygen.tei.completer.remote.Client;
import org.humanistika.oxygen.tei.completer.remote.ClientFactory;
import org.humanistika.oxygen.tei.completer.remote.ClientFactory.AuthenticationType;
import org.humanistika.oxygen.tei.completer.remote.impl.JerseyClientFactory;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import ro.sync.contentcompletion.xml.CIAttribute;
import ro.sync.contentcompletion.xml.CIElement;
import ro.sync.contentcompletion.xml.CIValue;
import ro.sync.contentcompletion.xml.Context;
import ro.sync.contentcompletion.xml.SchemaManagerFilter;
import ro.sync.contentcompletion.xml.WhatAttributesCanGoHereContext;
import ro.sync.contentcompletion.xml.WhatElementsCanGoHereContext;
import ro.sync.contentcompletion.xml.WhatPossibleValuesHasAttributeContext;

import static org.humanistika.oxygen.tei.completer.XPathUtil.isSubset;
import static org.humanistika.oxygen.tei.completer.XPathUtil.parseXPath;


import java.awt.*;
import java.util.List;

/**
 * TEI-Completer
 *
 * Oxygen XML Editor plugin for customizable attribute and value completion
 * for TEI P5 documents
 *
 * Works in conjunction with TEI P5 documents, by suggesting content values
 * for various attributes.
 * Content values are retrieved from a remote server.
 *
 * @author Adam Retter, Evolved Binary Ltd
 * @version 1.0
 * @serial 20160126
 */
public class TeiCompleter implements SchemaManagerFilter {
    private final static Logger LOGGER = LoggerFactory.getLogger(TeiCompleter.class);
    private final static Map<AuthenticationType, Client> clientsWithAuth = new EnumMap<>(AuthenticationType.class);
    private final static Map<AutoComplete, AutoCompleteXPaths> cachedAutoCompleteXPaths = new HashMap<>();

    protected Configuration<? extends AutoComplete> configuration = null;

    @Override
    public String getDescription() {
        return "BCDH TEI-Completer for TEI P5";
    }

    /**
     * Performs content completion for the TEI P5 configured attributes
     * by resolving possible attribute values from a server
     */
    @Override
    public List<CIValue> filterAttributeValues(final List<CIValue> list, final WhatPossibleValuesHasAttributeContext context) {
        if (context != null) {
            final AutoCompleteSuggestions<AutoComplete> autoCompleteSuggestions = getAutoCompleteSuggestions(context);
            if(autoCompleteSuggestions != null) {
                list.addAll(autoCompleteSuggestions.getSuggestions());
            }
            if(autoCompleteSuggestions != null && autoCompleteSuggestions.getSuggestions().size() == 0) {
                // the value needs to be prefixed with a space character to bump it to the top of the list
                list.add(new CustomCIValue(" Custom Entry...", this));
            }

        }
        return list;
    }

    /**
     * Get the autocomplete suggestions from the server if
     * we have a configured XPath
     *
     * @param context The attribute context
     * @return The list of possible suggestions or null if there was no configured XPath
     */
    @Nullable
    protected final AutoCompleteSuggestions<AutoComplete> getAutoCompleteSuggestions(final WhatPossibleValuesHasAttributeContext context) {
        final String elemXPath = context.computeContextXPathExpression();
        final String attrXPath = elemXPath + "/@" + context.getAttributeName();
        Expr attributeExpr;
        try {
            attributeExpr = parseXPath(attrXPath);
        } catch (Exception e) {
            return null;
        }


        for (final AutoComplete autoComplete : getConfiguration().getAutoCompletes()) {
            final AutoCompleteXPaths autoCompleteXPaths = getXPaths(autoComplete);

            if(autoCompleteXPaths != null) {
                //check if attributeExpr addresses a subset of autoCompleteXPaths.attributeXPath
                if (isSubset(attributeExpr, autoCompleteXPaths.getAttributeXPath())) {
                    final String selection = getSelection(context, elemXPath, autoComplete);

                    final String dependent;
                    if(autoComplete.getDependent() != null) {
                        dependent = getDependent(context, elemXPath, autoComplete.getDependent());
                    } else {
                        dependent = null;
                    }

                    //TODO(AR) should we consider all options?
                    return new AutoCompleteSuggestions<>(autoComplete, new AutoCompleteContext(selection, dependent), requestAutoComplete(autoComplete, selection, dependent));
                } else {
                    LOGGER.debug("Attribute XPath '{}' is not a subset of configured auto-complete XPath '{}'", attrXPath, getAutoCompleteAttributeXPath(autoComplete));
                }
            }
        }

        return null;
    }

    public List<CIValue> requestAutoComplete(final AutoComplete autoComplete, final String selection, @Nullable final String dependent) {
        final Authentication.AuthenticationType authenticationType = autoComplete.getRequestInfo().getAuthentication() == null ? null : autoComplete.getRequestInfo().getAuthentication().getAuthenticationType();
        //TODO USE a constant for this value
        if(selection.length() > 3 && dependent.length() > 3) {
            final Suggestions suggestions = getClient(authenticationType).getSuggestions(autoComplete.getRequestInfo(), selection, dependent, autoComplete.getResponseAction());
            final List<CIValue> results = new ArrayList<>();
            for(final Suggestion suggestion : suggestions.getSuggestion()) {
                results.add(new CIValue(suggestion.getValue(), suggestion.getDescription()));
            }
            return results;
        }
        return Collections.emptyList();

        //TODO(AR) consider some visual warnings/errors in Oxygen such as  JOptionPane.showMessageDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), "some error message here");
    }


     public Configuration<? extends AutoComplete> getConfiguration() {
        if(configuration == null) {
            synchronized(this) {
                if(configuration == null) {
                    this.configuration = ConfigurationFactory.getInstance().loadConfiguration();
                }
            }
        }
        return configuration;
    }

    protected ClientFactory getClientFactory() {
        return JerseyClientFactory.getInstance();
    }

    /**
     * Will get a client which is suitable for the authenticationType
     *
     * Clients are reused pre-authentication type
     *
     * @param authenticationType The type of authentication that a client is needed for
     *
     * @return A client which provides the requested authentication type
     */
    protected final Client getClient(final Authentication.AuthenticationType authenticationType) {
        final AuthenticationType cfAuthenticationType = asClientFactoryAuthenticationType(authenticationType);
        Client client = clientsWithAuth.get(cfAuthenticationType);
        if(client == null) {
            client = getClientFactory().createClient(cfAuthenticationType);
            clientsWithAuth.put(cfAuthenticationType, client);
        }
        return client;
    }

    private AuthenticationType asClientFactoryAuthenticationType(@Nullable final Authentication.AuthenticationType authenticationType) {
        if(authenticationType == null) {
            return AuthenticationType.NONE;
        } else {
            final AuthenticationType clientAuthenticationType;
            switch (authenticationType) {
                case PREEMPTIVE_BASIC:
                    clientAuthenticationType = AuthenticationType.PREEMPTIVE_BASIC;
                    break;

                case NON_PREEMPTIVE_BASIC:
                    clientAuthenticationType = AuthenticationType.NON_PREEMPTIVE_BASIC;
                    break;

                case DIGEST:
                    clientAuthenticationType = AuthenticationType.DIGEST;
                    break;

                case NON_PREEMPTIVE_BASIC_DIGEST:
                    clientAuthenticationType = AuthenticationType.NON_PREEMPTIVE_BASIC_DIGEST;
                    break;

                default:
                    throw new IllegalStateException("Unknown authentication type: " + authenticationType);
            }
            return clientAuthenticationType;
        }
    }

    protected class AutoCompleteXPaths {
        private final Expr attributeXPath;

        public AutoCompleteXPaths(final Expr attributeXPath) {
            this.attributeXPath = attributeXPath;
        }

        public Expr getAttributeXPath() {
            return attributeXPath;
        }
    }

    protected AutoCompleteXPaths getXPaths(final AutoComplete autoComplete) {
        synchronized(cachedAutoCompleteXPaths) {
            AutoCompleteXPaths autoCompleteXPaths = cachedAutoCompleteXPaths.get(autoComplete);
            if(autoCompleteXPaths == null) {
                autoCompleteXPaths = createXPaths(autoComplete);
                if(autoCompleteXPaths != null) {
                    cachedAutoCompleteXPaths.put(autoComplete, autoCompleteXPaths);
                }
            }
            return autoCompleteXPaths;
        }
    }

    protected String getAutoCompleteAttributeXPath(final AutoComplete autoComplete) {
        return autoComplete.getContext() + "/" + autoComplete.getAttribute();
    }

    @Nullable
    private AutoCompleteXPaths createXPaths(final AutoComplete autoComplete) {
        final Expr attributeExpr = parseXPath(getAutoCompleteAttributeXPath(autoComplete));

        //check the xpath is absolute, i.e. starts `/` or `//`
        if(attributeExpr.getExprSingles().size() > 0 && attributeExpr.getExprSingles().get(0) instanceof ValueExpr) {
            final ValueExpr valueExpr = (ValueExpr)attributeExpr.getExprSingles().get(0);
            final PathExpr pathExpr = (PathExpr)valueExpr.getPathExpr();
            if(pathExpr.getSteps().size() > 0) {
                final StepExpr firstStep = pathExpr.getSteps().get(0);
                if(firstStep.equals(PathExpr.SLASH_ABBREV) || firstStep.equals(PathExpr.SLASH_SLASH_ABBREV)) {
                    return new AutoCompleteXPaths(attributeExpr);
                }
            }
        }

        LOGGER.error("Invalid config detected for autoComplete, path must be absolute, will skip. context='{}' attribute='{}' attributeExpr={}", autoComplete.getContext(), autoComplete.getAttribute(), attributeExpr);
        return null;
    }

    protected String getSelection(final Context context, final String elemXPath, final AutoComplete autoComplete) {
        //get the selection by evaluating an XPath
        final String selectionXPath = getAutoCompleteSelectionXPath(elemXPath, autoComplete);
        LOGGER.info("Using selection XPath: {}", selectionXPath);
        final List results = context.executeXPath(selectionXPath, null, true);

        if(results.size() > 0 && results.get(0) instanceof Node) {
            final StringBuilder builder = new StringBuilder();
            for(final Object result : results) {
                final Node selectionNode = (Node)result;
                builder.append(selectionNode.getTextContent());
            }
            return builder.toString();
        } else {
            LOGGER.error("Could not find selection from XPath: {}", selectionXPath);
            return "";
        }
    }

    @Nullable
    protected String getDependent(final Context context, final String elemXPath, final Dependent dependent) {
        //get the dependent by evaluating an XPath
        final String selectionXPath = getAutoCompleteDependentXPath(elemXPath, dependent);
        LOGGER.info("Using dependent XPath: {}", selectionXPath);
        final List results = context.executeXPath(selectionXPath, null, true);

        final StringBuilder builder = new StringBuilder();

        if(results.size() > 0 && results.get(0) instanceof Node) {
            for(final Object result : results) {
                final Node selectionNode = (Node)result;
                builder.append(selectionNode.getTextContent());
            }
        }

        final String dependentStr = builder.toString().trim();
        if(dependentStr.length() > 0) {
            return dependentStr;
        } else {
            if(dependent.getDefault() != null) {
                LOGGER.error("Could not find dependent from XPath: {}. Using default value: {}", selectionXPath, dependent.getDefault());
                return dependent.getDefault();
            } else {
                LOGGER.warn("Could not find dependent from XPath: {}", selectionXPath);
                return null;
            }
        }
    }

    private String getAutoCompleteSelectionXPath(final String elemXPath, final AutoComplete autoComplete) {
        return elemXPath + "/" + autoComplete.getSelection().getDefault();
    }

    private String getAutoCompleteDependentXPath(final String elemXPath, final Dependent dependent) {
        return elemXPath + "/" + dependent.getAttribute();
    }

    protected class AutoCompleteContext {
        private final String selectedValue;
        @Nullable private final String dependentValue;

        public AutoCompleteContext(final String selectedValue, final String dependentValue) {
            this.selectedValue = selectedValue;
            this.dependentValue = dependentValue;
        }

        public String getSelectedValue() {
            return selectedValue;
        }

        @Nullable
        public String getDependentValue() {
            return dependentValue;
        }
    }

    /**
     * Holds the detail of AutoComplete config and suggestions found for it
     */
    protected class AutoCompleteSuggestions<T> {
        private final T autoComplete;
        private final List<CIValue> suggestions;
        private final AutoCompleteContext autoCompleteContext;

        public AutoCompleteSuggestions(final T autoComplete, final AutoCompleteContext autoCompleteContext, final List<CIValue> suggestions) {
            this.autoComplete = autoComplete;
            this.suggestions = suggestions;
            this.autoCompleteContext = autoCompleteContext;
        }

        public T getAutoComplete() {
            return autoComplete;
        }

        public AutoCompleteContext getAutoCompleteContext() {
            return autoCompleteContext;
        }

        public List<CIValue> getSuggestions() {
            return suggestions;
        }
    }


    /* additional filter functions which we don't need to act on below */

    @Override
    public List<CIElement> filterElements(final List<CIElement> list, final WhatElementsCanGoHereContext context) {
        return list;
    }

    @Override
    public List<CIAttribute> filterAttributes(final List<CIAttribute> list, final WhatAttributesCanGoHereContext context) {
        return list;
    }

    @Override
    public List<CIValue> filterElementValues(final List<CIValue> list, final Context context) {
        return list;
    }


    /**
     * A CIValue labelled "Add New..." which
     * prompts the user to enter a new suggestion
     * via a dialog box
     */
    public class CustomCIValue extends CIValue {
        private TeiCompleter teiCompleter;
        private String suggestion;
        public CustomCIValue(String s, final TeiCompleter teiCompleter) {
            super(s);
            this.teiCompleter = teiCompleter;
        }

        @Override
        public String getInsertString() {
            if(suggestion == null) {
                //Ask the user for an autocomplete dependent and selection
                final SuggestedAutocomplete suggestedAutocomplete = promptUserForNewSuggestion();
                suggestion = suggestedAutocomplete.getSuggestion();
            }

            return suggestion;
        }

        private SuggestedAutocomplete promptUserForNewSuggestion() {
            final KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            final Component comp = keyboardFocusManager.getFocusOwner();
            final Frame parentFrame = getParentFrame(comp);
            final newSuggestionForm newSuggestionForm = new newSuggestionForm(parentFrame, teiCompleter);



            //display the dialog
            newSuggestionForm.setLocationRelativeTo(parentFrame);
            newSuggestionForm.setVisible(true);
            final SuggestedAutocomplete suggestedAutocomplete = newSuggestionForm.getSuggestedAutocomplete();
            newSuggestionForm.dispose();
            return suggestedAutocomplete;
        }

        private Frame getParentFrame(final Component component) {
            if(component == null) {
                return null;
            }

            final Component parent = component.getParent();
            if(parent == null) {
                return null;
            }

            if(parent instanceof Frame) {
                return (Frame)parent;
            }

            return getParentFrame(parent);
        }
    }
}
