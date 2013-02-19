/*
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.notification.template;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;

import java.util.Map;

public class StringTemplateEngine implements TemplateEngine {

  private STGroup group;
  private String path;

  public StringTemplateEngine(String templateFolder) {
    path = templateFolder;
    group = new STGroupDir(templateFolder);
  }

  @Override
  public String renderText(String templateName, Map<String, String> params) {
    String result;
    ST template = group.getInstanceOf(templateName);
    if (template != null) {
        //Have no idea why for (String attr : template.getAttributes().keySet()) produce compilation error in java compiler 7 with target=1.6
        Map<String, Object> attributes = template.getAttributes();
        if(attributes != null) {
            for (Object attr : attributes.keySet()) {
              String attrStr = attr.toString();
              template.add(attrStr, params.get(attrStr) == null ? "" : params.get(attrStr));
            }
        }
        result = template.render();
    } else {
        throw new IllegalArgumentException(String.format("Template %s is not found at path %s", templateName, path));
    }
    return result;
  }

}
