/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description: Continuous Delivery Platform
 */
package com.griddynamics.genesis.notification.utils;

import org.apache.commons.lang.StringUtils;

public class ConfigReaderUtils {
    private ConfigReaderUtils(){}

    public static String getRequiredParameter(java.util.Map<String, String> config, String parameterName) {
        String value = config.get(parameterName);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException(String.format("'%s' cannot be empty", parameterName));
        }
        return value;
    }

    public static Integer getIntParameter(java.util.Map<String, String> config, String paramName, int maxValue) {
        String paramValue = config.get(paramName);
        if (StringUtils.isNotEmpty(paramValue) && StringUtils.isNumeric(paramValue)) {
            try {
                int i = Integer.parseInt(paramValue);
                if (i > maxValue) {
                    throw new IllegalArgumentException(String.format("%s is too big: %s", paramName, paramValue));
                }
                return i;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("%s is is too big: %s", paramName, paramValue));
            }
        } else {
            throw new IllegalArgumentException(paramName + String.format("%s is not a number: '%s'", paramName, paramValue));
        }
    }
}
