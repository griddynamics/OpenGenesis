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
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.ImageBuilder;
import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.domain.Credentials;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class GDNovaImageToImage implements Function<org.gdjclouds.provider.gdnova.v100.Image, Image> {
private final Function<org.gdjclouds.provider.gdnova.v100.Image, OperatingSystem> imageToOs;

    @Inject
    GDNovaImageToImage(Function<org.gdjclouds.provider.gdnova.v100.Image, OperatingSystem> imageToOs) {
        this.imageToOs = imageToOs;
    }

    public Image apply(org.gdjclouds.provider.gdnova.v100.Image from) {
        ImageBuilder builder = new ImageBuilder();
        builder.ids(from.getId());
        builder.name(from.getName() != null ? from.getName() : "unspecified");
        builder.description(from.getName() != null ? from.getName() : "unspecified");
        builder.version(from.getUpdated().getTime() + "");
        builder.operatingSystem(imageToOs.apply(from)); //image name may not represent the OS type
        builder.defaultCredentials(new Credentials("root", null));
        builder.uri(from.getURI());
        Image image = builder.build();
        return image;
    }
}
