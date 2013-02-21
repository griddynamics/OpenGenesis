package com.griddynamics.genesis.notification.template;


import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class VelocityTemplateEngineTest {

    TemplateEngine templateEngine = new VelocityTemplateEngine("template");

    @Test
    public void testRenderText() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("message1", "test");
        params.put("message2", "12345");
        params.put("message3", "67890");
        String result = templateEngine.renderText("email.vm", params);
        assertEquals("test12345", result);
    }
}
