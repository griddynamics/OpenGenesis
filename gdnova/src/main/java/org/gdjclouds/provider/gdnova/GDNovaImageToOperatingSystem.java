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
 *   Description:  Continuous Delivery Platform
 */
package org.gdjclouds.provider.gdnova;

import com.google.common.base.Function;
import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.compute.util.ComputeServiceUtils;
import org.jclouds.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class GDNovaImageToOperatingSystem implements
        Function<org.gdjclouds.provider.gdnova.v100.Image, OperatingSystem> {
    public static final Pattern DEFAULT_PATTERN = Pattern.compile("(([^ ]*) ([0-9.]+) ?.*)");
    // Windows Server 2008 R2 x64
    public static final Pattern WINDOWS_PATTERN = Pattern.compile("Windows (.*) (x[86][64])");

    @Resource
    @Named(ComputeServiceConstants.COMPUTE_LOGGER)
    protected Logger logger = Logger.NULL;

    private final Map<OsFamily, Map<String, String>> osVersionMap;

    @Inject
    public GDNovaImageToOperatingSystem(Map<OsFamily, Map<String, String>> osVersionMap) {
        this.osVersionMap = osVersionMap;
    }

    public OperatingSystem apply(final org.gdjclouds.provider.gdnova.v100.Image from) {
        OsFamily osFamily = null;
        String osName = null;
        String osArch = null;
        String osVersion = null;
        String osDescription = from.getName() != null ? from.getName() : "unspecified";

        String name = from.getName() != null ? from.getName() : "unspecified";
        boolean is64Bit = true;
        if (name.indexOf("Red Hat EL") != -1) {
            osFamily = OsFamily.RHEL;
        } else if (name.indexOf("Oracle EL") != -1) {
            osFamily = OsFamily.OEL;
        } else if (name.indexOf("Windows") != -1) {
            osFamily = OsFamily.WINDOWS;
            Matcher matcher = WINDOWS_PATTERN.matcher(from.getName());
            if (matcher.find()) {
                osVersion = ComputeServiceUtils.parseVersionOrReturnEmptyString(osFamily, matcher.group(1), osVersionMap);
                is64Bit = matcher.group(2).equals("x64");
            }
        } else {
            Matcher matcher = DEFAULT_PATTERN.matcher(name);
            if (matcher.find()) {
                try {
                    osFamily = OsFamily.fromValue(matcher.group(2).toLowerCase());
                } catch (IllegalArgumentException e) {
                    logger.debug("<< didn't match os(%s)", matcher.group(2));
                }
                osVersion = ComputeServiceUtils.parseVersionOrReturnEmptyString(osFamily, matcher.group(3), osVersionMap);
            }
        }
        return new OperatingSystem(osFamily, osName, osVersion, osArch, osDescription, is64Bit);
    }
}

