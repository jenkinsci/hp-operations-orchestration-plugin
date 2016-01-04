// (c) Copyright 2013 Hewlett-Packard Development Company, L.P. 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package com.hp.mercury.ci.jenkins.plugins;

import com.hp.mercury.ci.jenkins.plugins.oo.core.OORunRequest;
import com.hp.mercury.ci.jenkins.plugins.oo.core.OORunResponse;
import hudson.model.Action;

import java.net.URL;


public class OOBuildStepResultReportAction implements Action {

//    private OORunResponse runResult;
//    private OORunRequest runRequest;
//
//    public OOBuildStepResultReportAction(OORunRequest request, OORunResponse ooBuildStepResult) {
//        this.runRequest = request;
//        this.runResult = ooBuildStepResult;
//    }
//
//  //  @Override
//    public String getIconFileName() {
//        return "document.png";
//    }
//
//  //  @Override
//    public String getDisplayName() {
//        return "Build Step Result for " + runRequest.getFlow().getId();
//    }
//
//  //  @Override
//    public String getUrlName() {
//
//        return this.runResult.getReportUrl();
//    }



    private String flowName;
    private String runUrl;

    public OOBuildStepResultReportAction(String runUrl) {
        this.runUrl=runUrl;
    }

//    @Override
    public String getIconFileName() {
        return "document.png";
    }

    //  @Override
    public String getDisplayName() {
        return "Link to run in OO";
    }

//    @Override
    public String getUrlName() {
        return this.runUrl;
    }

}
