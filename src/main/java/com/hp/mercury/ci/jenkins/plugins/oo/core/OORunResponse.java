// (c) Copyright 2013 Hewlett-Packard Development Company, L.P. 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package com.hp.mercury.ci.jenkins.plugins.oo.core;

import com.hp.mercury.ci.jenkins.plugins.oo.utils.CollectionUtils;
import com.hp.mercury.ci.jenkins.plugins.oo.utils.Criteria;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;

 /* response format:
<?xml version="1.0" encoding="UTF-8"?>
<runResponse>
    <runReturn>
        <item>
            <name>runId</name>
            <value>23</value>
        </item>
        ... (more items)
    </runReturn>
</runResponse>
*/
@XmlRootElement(name = "runResponse")
public class OORunResponse {

    Collection<OORunReturnItem> items;
    private String reportUrl;

    //for jaxb
    public OORunResponse() {}

    @XmlElementWrapper(name = "runReturn")
    @XmlElement(name = "item")
    public Collection<OORunReturnItem> getItems() {
        return items;
    }

    public void setItems(Collection<OORunReturnItem> items) {
        this.items = items;
    }

    public String getReportUrl() {

        if (this.reportUrl == null) {
            final ArrayList<OORunReturnItem> itemsCopy = new ArrayList<OORunReturnItem>(items);
            CollectionUtils.filter(itemsCopy, new Criteria<OORunReturnItem>() {
                //@Override
                public boolean isSuccessful(OORunReturnItem tested) {
                    return tested.getName().equals("displayRunReportUrl");
                }
            });
            if (!itemsCopy.isEmpty()) {
                this.reportUrl = itemsCopy.get(0).getValue();
            }
        }

        return this.reportUrl;
    }

    public boolean hasReport() {

        return getReportUrl() != null;
    }

    public static class OORunReturnItem {

        private String name;
        private String value;

        //for jaxb
        public OORunReturnItem() {}

        @XmlElement(name = "name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlElement(name = "value")
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
