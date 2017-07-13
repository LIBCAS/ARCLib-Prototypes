package cz.inqool.arclib;

import cz.inqool.arclib.mail.AsyncMailSender;
import cz.inqool.arclib.mail.MailCenter;
import cz.inqool.arclib.service.Templater;
import helper.MockMailSender;
import org.apache.velocity.app.VelocityEngine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MailCenterTest {
    @InjectMocks
    protected MockMailSender mockMailSender = new MockMailSender();

    @InjectMocks
    protected AsyncMailSender sender = new AsyncMailSender();

    @InjectMocks
    protected MailCenter mailCenter = new MailCenter();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Templater templater = new Templater();
        templater.setEngine(new VelocityEngine());

        sender.setSender(mockMailSender);
        mailCenter.setSender(sender);
        mailCenter.setTemplater(templater);
        mailCenter.setSenderEmail("noreply@test.cz");
        mailCenter.setSenderName("cro");
        mailCenter.setAppName("arclib");
    }

    @Test
    public void sendAipSavedNotificationTest() {
        mailCenter.sendAipSavedNotification("test@test.cz", "1", "test text", Instant.now());

        Object content = mockMailSender.getJavaMailProperties().get("mailContent");
        mockMailSender.getJavaMailProperties().remove("mailContent");

        assertThat(content,is(not(nullValue())));
        assertThat(content.toString(), containsString("test text"));
    }

    @Test
    public void sendIngestResultNotificationTest() {
        mailCenter.sendIngestResultNotification("test@test.cz", "1", "test text", Instant.now());

        Object content = mockMailSender.getJavaMailProperties().get("mailContent");
        mockMailSender.getJavaMailProperties().remove("mailContent");

        assertThat(content,is(not(nullValue())));
        assertThat(content.toString(), containsString("test text"));
    }
}

