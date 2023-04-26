/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2020 Ioannis Moutsatsos, Bruno P. Kinoshita
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.biouno.unochoice;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;

import hudson.model.*;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.biouno.unochoice.model.Script;
import org.biouno.unochoice.util.Utils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * <p>A choice parameter, that gets updated when another parameter changes. The simplest
 * use case for this, would be having a list of states, and when the user selected a
 * state it would trigger an update of the city fields.</p>
 *
 * <p>The state parameter would be a choice parameter, and the city parameter would be a
 * cascade choice parameter, that referenced the former.</p>
 *
 * <p>Its options are retrieved from the evaluation of a Groovy script.</p>
 *
 * @author Bruno P. Kinoshita
 * @since 0.1
 */
public class CascadeChoiceParameter extends AbstractCascadableParameter {

    /*
     * Serial UID.
     */
    private static final long serialVersionUID = 4524790278642708107L;

    /**
     * Choice type.
     */
    private final String choiceType;

    /**
     * Filter flag.
     */
    private final Boolean filterable;

    /**
     * Filter length. Defines a minimum number of characters that must be entered before the filter
     * is activated.
     */
    private final Integer filterLength;

    /**
     * Author: Kenny
     */
    private Map<Object, Object> choices;



    /**
     * Constructor called from Jelly with parameters.
     *
     * @param name name
     * @param description description
     * @param script script
     * @param choiceType choice type
     * @param referencedParameters referenced parameters
     * @param filterable filter flag
     * @deprecated see JENKINS-32149
     */
    public CascadeChoiceParameter(String name, String description, Script script,
            String choiceType, String referencedParameters, Boolean filterable) {
        super(name, description, script, referencedParameters);
        this.choiceType = StringUtils.defaultIfBlank(choiceType, PARAMETER_TYPE_SINGLE_SELECT);
        this.filterable = filterable;
        this.filterLength = null;
    }

    /**
     * Constructor called from Jelly with parameters.
     *
     * @param name name
     * @param description description
     * @param randomName parameter random generated name (uuid)
     * @param script script
     * @param choiceType choice type
     * @param referencedParameters referenced parameters
     * @param filterable filter flag
     * @deprecated see JENKINS-31625
     */
    public CascadeChoiceParameter(String name, String description, String randomName, Script script,
            String choiceType, String referencedParameters, Boolean filterable) {
        super(name, description, randomName, script, referencedParameters);
        this.choiceType = StringUtils.defaultIfBlank(choiceType, PARAMETER_TYPE_SINGLE_SELECT);
        this.filterable = filterable;
        this.filterLength = null;
    }

    /**
     * Constructor called from Jelly with parameters.
     *
     * @param name name
     * @param description description
     * @param randomName parameter random generated name (uuid)
     * @param script script
     * @param choiceType choice type
     * @param referencedParameters referenced parameters
     * @param filterable filter flag
     * @param filterLength filter length
     */
    @DataBoundConstructor
    public CascadeChoiceParameter(String name, String description, String randomName, Script script,
            String choiceType, String referencedParameters, Boolean filterable, Integer filterLength) {
        super(name, description, randomName, script, referencedParameters);
        this.choiceType = StringUtils.defaultIfBlank(choiceType, PARAMETER_TYPE_SINGLE_SELECT);
        this.filterable = filterable;
        this.filterLength = filterLength;
    }

    /**
     * Author: Kenny
     * @return
     */
    @Override
    public Map<Object, Object> getChoices() {
        if(isRebuilding()) {
            if(choices == null) {
                choices = super.getChoices();
            }
        }
        else {
            choices = super.getChoices();
        }
        return choices;
    }

    /**
     * Author: Kenny
     * @return
     */
    @Override
    public Map<Object, Object> getChoicesToRebuild() {
        setRebuilding(true);
        return getChoices();
    }

    /**
     * Author: Kenny
     * @param map
     */
    public void setChoices(Map<Object, Object> map) {
        LOGGER.log(Level.INFO, "setChoices called");
        choices = map;
    }

    public <T> T instantiate(final String className, final Class<T> type){
        try{
            return type.cast(Class.forName(className).newInstance());
        } catch(InstantiationException
            | IllegalAccessException
            | ClassNotFoundException e){
            throw new IllegalStateException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.biouno.unochoice.AbstractUnoChoiceParameter#getChoiceType()
     */
    @Override
    public String getChoiceType() {
        return choiceType;
    }

    /**
     * Get the filter flag.
     * @return filter flag
     */
    public Boolean getFilterable() {
        return filterable;
    }

    /**
     * Get the filter length.
     * @return filter length
     */
     public Integer getFilterLength() {
         return filterLength == null ? (Integer) 1 : filterLength;
     }

    // --- descriptor

    @Extension
    public static final class DescriptImpl extends UnoChoiceParameterDescriptor {

        @Override
        public String getDisplayName() {
            return "Active Choices Reactive Parameter";
        }

    }

}
