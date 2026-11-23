package io.github.jonasfortes12.api.access.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import io.github.jonasfortes12.api.access.AccessProperties;

/**
 * Chooses the email sender: SMTP when {@code debt.mail.host} (env
 * {@code DEBT_MAIL_HOST}) is set,
 * otherwise the dev sender that logs codes. Builds its own
 * {@link JavaMailSenderImpl} and sets no
 * {@code spring.mail.*} property, so Spring Boot's mail auto-configuration
 * stays inactive.
 */
@Configuration
public class MailConfig {
    
    @Bean
    AccessCodeSender accessCodeSender(
            AccessProperties accessProperties,
            @Value("${debt.mail.host:}") String host,
            @Value("${debt.mail.port:587}") int port,
            @Value("${debt.mail.username:}") String username,
            @Value("${debt.mail.password:}") String password,
            @Value("${debt.mail.from:}") String from) {
        if (host.isBlank()) {
            return new LoggingAccessCodeSender(accessProperties.frontendUrl());
        }
        return new SmtpAccessCodeSender(smtp(host, port, username, password), from, accessProperties.frontendUrl());
    }

    private static JavaMailSenderImpl smtp(String host, int port, String username, String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        if (!username.isBlank()) {
            sender.setUsername(username);
            sender.setPassword(password);
            sender.getJavaMailProperties().put("mail.smtp.auth", "true");
        }
        sender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
        sender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "10000");
        sender.getJavaMailProperties().put("mail.smtp.timeout", "10000");
        sender.getJavaMailProperties().put("mail.smtp.writetimeout", "10000");
        return sender;
    }
}
