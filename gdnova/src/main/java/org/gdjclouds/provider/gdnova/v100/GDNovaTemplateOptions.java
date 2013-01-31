package org.gdjclouds.provider.gdnova.v100;

import org.jclouds.compute.options.TemplateOptions;

public class GDNovaTemplateOptions extends org.jclouds.compute.options.TemplateOptions {
    private String keyPair;

    @Override
    public void copyTo(TemplateOptions to) {
        super.copyTo(to);
        if (to instanceof GDNovaTemplateOptions) {
            ((GDNovaTemplateOptions) to).keyPair(keyPair);
        }
    }

    public String getKeyPair() {
        return keyPair;
    }

    public GDNovaTemplateOptions keyPair(String keyPair) {
        this.keyPair = keyPair;
        return this;
    }
}
