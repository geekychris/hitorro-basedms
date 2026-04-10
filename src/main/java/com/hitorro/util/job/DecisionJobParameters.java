/*
 * Copyright (c) 2006-2025 Chris Collins
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
package com.hitorro.util.job;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.core.CommandArgs;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.json.keys.propaccess.PropaccessError;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**

 * Allow one to make a choice from a listFiles of decision paths.
 */
@TypeClassMetaInfo(shortTypeName = "DecisionJobParameters",
        isView = false,
        isPersisted = false,
        schemaVersion = DecisionJobParameters.SerializationVersion)
public class DecisionJobParameters extends JobParameters {
    public static final int SerializationVersion = 3;

    private List<String> choicesDisplayStrings = new ArrayList<String>();
    private List<String> choices = new ArrayList<String>();
    private String choice;
    private String description;
    private String payloadGuid;

    private String reasonFromUI;
    private String appStepClass;

    private String appStepParameters;

    private String fromUser;


    public String getJobName() {
        return NoOpJob.Name;
    }


    public int getSerializationVersion() {
        return DecisionJobParameters.SerializationVersion;
    }


    public boolean isPersisted() {
        return false;
    }


    public boolean hasGuid() {
        return false;
    }


    public boolean hasSoftGuid() {
        return false;
    }


    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(DecisionJobParameters.SerializationVersion);
        super.serialize(os);
        // part of version 2.
        os.writeString(fromUser);
        os.writeString(appStepParameters);
        // pars of version 1
        os.writeListOfString(choicesDisplayStrings);
        os.writeListOfString(choices);
        os.writeString(choice);
        os.writeString(description);
        os.writeString(payloadGuid);
        os.writeString(reasonFromUI);
        os.writeString(appStepClass);

    }


    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 3:
                fromUser = os.readString();
            case 2:
                appStepParameters = os.readString();
            case 1:
                choicesDisplayStrings = os.readListOfStrings();
                choices = os.readListOfStrings();
                choice = os.readString();
                description = os.readString();
                payloadGuid = os.readString();
                reasonFromUI = os.readString();
                appStepClass = os.readString();
        }

    }


    public List<String> getChoicesDisplayStrings() {
        return choicesDisplayStrings;
    }


    public void setChoicesDisplayStrings(List<String> choicesDisplayStrings) {
        this.choicesDisplayStrings = choicesDisplayStrings;
    }


    public List<String> getChoices() {
        return choices;
    }


    public void setChoices(List<String> choices) {
        this.choices = choices;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public String getPayloadGuid() {
        return payloadGuid;
    }


    public void setPayloadGuid(String payloadGuid) {
        this.payloadGuid = payloadGuid;
    }


    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String user) {
        fromUser = user;
    }

    public String getReasonFromUI() {
        return reasonFromUI;
    }


    public void setReasonFromUI(String reasonFromUI) {
        this.reasonFromUI = reasonFromUI;
    }


    public String getAppStepClass() {
        return appStepClass;
    }


    public void setAppStepClass(String appStepClass) {
        this.appStepClass = appStepClass;
    }


    public String getAppStepParameters() {
        return appStepParameters;
    }

    public void setAppStepParameters(String appStepParameters) {
        this.appStepParameters = appStepParameters;
    }

    public JVS getAppStepParametersAsMap() {
        if (StringUtil.nullOrEmptyOrBlankString(appStepParameters)) {
            return new JVS();
        }
        try {
            return new JVS(CommandArgs.getParameters(appStepParameters, true));
        } catch (ParseException | PropaccessError e) {
            Log.statemachine.error("Unable to parse parameters%s %e", e, e);
            return new JVS();
        }
    }

    public void setAppStepParametersFromMap(Map<String, String> map) {
        if (map != null) {
            appStepParameters = CommandArgs.getArgString(map);
        }
    }
}
