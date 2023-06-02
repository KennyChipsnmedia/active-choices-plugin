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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.biouno.unochoice;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.*;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.biouno.unochoice.model.Script;
import org.biouno.unochoice.util.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import hudson.Extension;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

/**
 * <p>Provides a <b>dynamic reference parameter</b> for users. This is a not so elegant
 * solution, since we are using a ParameterDefinition extension point, but it 
 * actually <b>doesn't provide any parameter value</b>.</p>
 *
 * <p>This kind of parameter is only for reference. An use case is when you have several
 * job parameters, but your input values may vary depending on previous executions. You 
 * can get the previous executions by accessing from your Groovy code the jenkinsProject
 * variable.</p>
 *
 * <p>Its options are retrieved from the evaluation of a Groovy script.</p>
 *
 * @author Bruno P. Kinoshita
 * @since 0.1
 */
public class DynamicReferenceParameter extends AbstractCascadableParameter {

    /*
     * Serial UID.
     */
    private static final long serialVersionUID = 8261526672604361397L;

    /**
     * Choice type.
     */
    private final String choiceType;

    private final Boolean omitValueField;

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
     * @param omitValueField used in the UI to decide whether to include a hidden empty &lt;input name=value&gt;.
     * <code>false</code> by default.
     * @deprecated see JENKINS-32149
     */
    public DynamicReferenceParameter(String name, String description, Script script, 
            String choiceType, String referencedParameters, Boolean omitValueField) {
        super(name, description, script, referencedParameters);
        this.choiceType = StringUtils.defaultIfBlank(choiceType, PARAMETER_TYPE_SINGLE_SELECT);
        this.omitValueField = BooleanUtils.toBooleanDefaultIfNull(omitValueField, Boolean.FALSE);
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
     * @param omitValueField used in the UI to decide whether to include a hidden empty &lt;input name=value&gt;.
     * <code>false</code> by default.
     */
    @DataBoundConstructor
    public DynamicReferenceParameter(String name, String description, String randomName, Script script,
            String choiceType, String referencedParameters, Boolean omitValueField) {
        super(name, description, randomName, script, referencedParameters);
        this.choiceType = StringUtils.defaultIfBlank(choiceType, PARAMETER_TYPE_SINGLE_SELECT);
        this.omitValueField = BooleanUtils.toBooleanDefaultIfNull(omitValueField, Boolean.FALSE);
    }

    /*
     * (non-Javadoc)
     * @see org.biouno.unochoice.AbstractUnoChoiceParameter#getChoiceType()
     */
    @Override
    public String getChoiceType() {
        return this.choiceType;
    }

    public Boolean getOmitValueField() {
        return omitValueField;
    }

    @JavaScriptMethod
    public String getChoicesAsStringForUI() {
        String result = getChoicesAsString(getParameters());

        if(isRebuilding()) {
            if(choices == null || choices.isEmpty()) {
                return result;
            }
            LOGGER.log(Level.FINEST, "rebuilding with content result:" + result);

            // Kenny, FIXME, now only dealing with choiceType of ET_FORMATTED_HTML.
            if(getChoiceType().equals("ET_FORMATTED_HTML")) {
                Document doc = Jsoup.parse(result);

                // Kenny, FIXME, now only dealing with first occurence of textarea tag.
                Element textarea = doc.selectFirst("textarea");
                if(textarea != null) {
                    // choices should have rebuild value injected by Cbuilder.
                    String choiceString = choices.entrySet().iterator().next().getValue().toString();
                    textarea.text(choiceString);
                    LOGGER.log(Level.FINEST, "choices as string doc:" + doc.html());
                    return doc.html();
                }
            }

            return result;

        }
        return result;
    }

    @JavaScriptMethod
    @Override
    public void setRebuilding(Boolean rebuilding) {
        LOGGER.log(Level.FINEST, "DynamicReferenceParameter set rebuilding to:" + rebuilding);
        super.setRebuilding(rebuilding);
    }

    /**
     * Author: Kenny
     * @return
     */
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

    public void setChoices(Map<Object, Object> map) {
        choices = map;
    }

    // --- descriptor

    @Extension
    public static final class DescriptorImpl extends UnoChoiceParameterDescriptor {

        private AbstractProject<?, ?> project;

        /*
         * Used to store a reference to the Jenkins project related to this parameter.
         * A bit hacky, probably using another extension point would be a good idea.
         */
        @Override
        public ParameterDefinition newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
            if (req != null) {
                List<Ancestor> ancestors = req.getAncestors();
                AbstractProject<?, ?> project = null;
                for (Ancestor ancestor : ancestors) {
                    Object object = ancestor.getObject();
                    if (object instanceof AbstractProject<?, ?>) {
                        project = (AbstractProject<?, ?>) object;
                        break;
                    }
                }
                this.project = project;
            }
            return super.newInstance(req, formData);
        }

        public AbstractProject<?, ?> getProject() {
            return project;
        }

        @Override
        public String getDisplayName() {
            return "Active Choices Reactive Reference Parameter";
        }

        public FormValidation doCheckRequired(@QueryParameter String value) {
            String strippedValue = StringUtils.strip(value);
            if ("".equals(strippedValue)) {
                return FormValidation.error("This field is required.");
            }
            return FormValidation.ok();
        }

    }

}
