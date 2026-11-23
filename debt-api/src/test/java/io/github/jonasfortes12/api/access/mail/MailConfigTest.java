package io.github.jonasfortes12.api.access.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.github.jonasfortes12.api.access.AccessConfig;

class MailConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MailConfig.class, AccessConfig.class)
            .withPropertyValues("debt.access.terms-version=v1");

    @Test
    void blankHostSelectsLoggingSender() {
        runner.withPropertyValues("debt.mail.host=").run(context ->
                assertThat(context).getBean(AccessCodeSender.class).isInstanceOf(LoggingAccessCodeSender.class));
    }

    @Test
    void hostSelectsSmtpSender() {
        runner.withPropertyValues("debt.mail.host=smtp.example.test", "debt.mail.port=587",
                "debt.mail.from=noreply@example.test").run(context ->
                assertThat(context).getBean(AccessCodeSender.class).isInstanceOf(SmtpAccessCodeSender.class));
    }
}
