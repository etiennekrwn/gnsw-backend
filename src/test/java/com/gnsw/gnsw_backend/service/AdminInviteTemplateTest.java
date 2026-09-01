package com.gnsw.gnsw_backend.service;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the admin invite onboarding email.
 *
 * The template previously used the invalid default expression
 * {@code ${acceptMail:''}} (a single colon, which is not valid Thymeleaf/SpEL
 * default syntax). {@code templateEngine.process(...)} throws on that, so the
 * invite email could never render and inviting an admin failed/emailed nothing.
 * Rendering it here guards against that regressing, and confirms the recipient
 * address is populated in the "This link is for … only" line.
 */
class AdminInviteTemplateTest {

    private TemplateEngine engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Test
    void adminInviteTemplateRendersWithRecipientAndLink() {
        Context ctx = new Context();
        ctx.setVariable("displayName", "Jane Admin");
        ctx.setVariable("roleLabel", "Administrator (full access)");
        ctx.setVariable("modulesLabel", "");
        ctx.setVariable("acceptMail", "jane.admin@example.com");
        ctx.setVariable("acceptUrl",
                "http://localhost:5174/accept-invite?token=abc123&email=jane.admin%40example.com");

        String html = engine().process("admin-invite", ctx);

        assertThat(html)
                .contains("You've been invited to the admin console")
                .contains("jane.admin@example.com")
                .contains("Administrator (full access)")
                .contains("http://localhost:5174/accept-invite?token=abc123");
    }
}
