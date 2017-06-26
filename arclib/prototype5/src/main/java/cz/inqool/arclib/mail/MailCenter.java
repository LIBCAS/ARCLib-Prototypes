package cz.inqool.arclib.mail;

import cz.inqool.arclib.Utils;
import cz.inqool.arclib.exception.GeneralException;
import cz.inqool.arclib.service.Templater;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static cz.inqool.arclib.Utils.extractDate;
import static cz.inqool.arclib.Utils.extractTime;

@Slf4j
@Component
public class MailCenter {

    private AsyncMailSender sender;

    private Templater templater;

    private String senderEmail;

    private String senderName;

    private String appName;

    public void sendIngestResultNotification(String email, String sipId, String result, Instant created) {
        sendNotificationInternal(email, sipId, result, created, "templates/ingestResultNotification.vm");
    }

    public void sendAipSavedNotification(String email, String sipId, String result, Instant created) {
        sendNotificationInternal(email, sipId, result, created, "templates/aipSavedNotification.vm");
    }

    private MimeMessageHelper generalMessage(String emailTo, @Nullable String subject, boolean hasAttachment) throws MessagingException {
        MimeMessage message = sender.create();

        // use the true flag to indicate you need a multipart message
        MimeMessageHelper helper = new MimeMessageHelper(message, hasAttachment);

        if (emailTo != null) {
            helper.setTo(emailTo);
        }

        if (subject != null) {
            helper.setSubject(subject);
        }

        try {
            helper.setFrom(senderEmail, senderName);
        } catch (UnsupportedEncodingException ex) {
            log.warn("Can not set email 'from' encoding, fallbacking.");
            helper.setFrom(senderEmail);
        }

        return helper;
    }

    private Map<String, Object> generalArguments() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("appName", appName);
        arguments.put("senderEmail", senderEmail);
        return arguments;
    }

    private void transformAndSend(InputStream template, Map<String, Object> arguments, MimeMessageHelper helper)
            throws MessagingException, IOException {

        String text = templater.transform(template, arguments);
        helper.setText(text, true);

        MimeMessage message = helper.getMimeMessage();

        if (message.getAllRecipients() != null && message.getAllRecipients().length > 0) {
            sender.send(message);
        } else {
            log.warn("Mail message was silently consumed because there were no recipients.");
        }
    }

    private void sendNotificationInternal(String email, String sipId, String result, Instant created, String templateName) {
        try {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd. MM. yyyy");

            MimeMessageHelper message = generalMessage(email, appName, false);

            Map<String, Object> params = generalArguments();
            params.put("sipId", sipId);
            params.put("result", result);
            params.put("createdDate", extractDate(created).format(dateFormatter));
            params.put("createdTime", extractTime(created).format(timeFormatter));

            InputStream template = Utils.resource(templateName);
            transformAndSend(template, params, message);
        } catch (MessagingException | IOException ex) {
            throw new GeneralException(ex);
        }
    }

    @Inject
    public void setSenderEmail(@Value("${mail.sender.email}") String senderEmail) {
        this.senderEmail = senderEmail;
    }

    @Inject
    public void setSenderName(@Value("${mail.sender.name}") String senderName) {
        this.senderName = senderName;
    }

    @Inject
    public void setSender(AsyncMailSender sender) {
        this.sender = sender;
    }

    @Inject
    public void setTemplater(Templater templater) {
        this.templater = templater;
    }

    @Inject
    public void setAppName(@Value("${mail.app.name}") String appName) {
        this.appName = appName;
    }
}
