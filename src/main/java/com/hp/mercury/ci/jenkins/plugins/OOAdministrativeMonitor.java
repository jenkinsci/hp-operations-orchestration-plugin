// (c) Copyright 2013 Hewlett-Packard Development Company, L.P. 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package com.hp.mercury.ci.jenkins.plugins;


import com.hp.mercury.ci.jenkins.plugins.oo.utils.*;
import hudson.Extension;
import hudson.model.AdministrativeMonitor;


import java.util.Collection;
import java.util.Collections;


@Extension
public final class OOAdministrativeMonitor extends AdministrativeMonitor {

    public static final String MONITOR_ID = "ooConfigMonitor";

    public OOAdministrativeMonitor() {
        super(MONITOR_ID);
        urlConfigurationErrors = Collections.emptyList();
    }

    private Collection<Exception> urlConfigurationErrors;

    public void setUrlConfigurationErrors(Collection<Exception> urlConfigurationErrors) {
        this.urlConfigurationErrors = urlConfigurationErrors;
    }

    public boolean isActivated() {
        return !urlConfigurationErrors.isEmpty();
    }

    public String getMessage() {

        //should never reach here
        if (urlConfigurationErrors == null) {
            return "unexpected problem in configuration of Operations Orchestration";
        }

        Collection<String> errors = CollectionUtils.map(urlConfigurationErrors,
                new Handler<String, Exception>() {

                  //  @Override
                    public String apply(Exception node) {

                        return node.getMessage() + "\n";
                    }
                });

        return "Configuration of HP Operations Orchestration failed, the following URLS were invalid: \r\n" + errors;
    }
}
