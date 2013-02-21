package com.griddynamics.genesis.notification.template;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ResourceNotFoundException;

import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

public class VelocityTemplateEngine implements TemplateEngine {


    private final String templateFolder;

    public VelocityTemplateEngine(String templatesFolder) {
       this.templateFolder = templatesFolder;
       init();
    }

    public void init() {
        Properties p = new Properties();
        p.put("resource.loader", "file,class");
        p.put("file.resource.loader.path", templateFolder);
        p.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        p.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(p);
    }

    @Override
    public String renderText(String templateName, Map<String, String> params) {
        Template t = loadTemplate(templateName);
        StringWriter writer = new StringWriter();
        t.merge(new VelocityContext(params),writer);
        writer.flush();
        return writer.toString();
    }

    private Template loadTemplate(String templateName) {
        try {
            return Velocity.getTemplate(templateName, "UTF-8");
        } catch (ResourceNotFoundException e) {
            //prepend template name with path for classpath loader
            return Velocity.getTemplate(templateFolder + "/" + templateName, "UTF-8");
        }
    }
}
