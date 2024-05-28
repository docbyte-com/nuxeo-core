/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.nuxeo.mail;

import java.util.List;

import jakarta.activation.DataHandler;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Helper to convert {@link MailMessage}s to {@link MimeMessage}.
 * 
 * @since 2023.4
 */
public class MimeMessageHelper {

    public static MimeMessage composeMimeMessage(MailMessage msg) throws MessagingException {
        return composeMimeMessage(msg, null);
    }

    public static MimeMessage composeMimeMessage(MailMessage msg, Session session) throws MessagingException {
        var mail = new MimeMessage(session);
        Object content = msg.getContent();
        String contentType = msg.getContentType();
        if (msg.hasAttachments()) {
            MimeBodyPart body = null;
            if (content != null) {
                body = new MimeBodyPart();
                body.setContent(msg.getContent(), contentType);
            }
            MimeMultipart bodyParts = assembleMultiPart(body, msg.getAttachments());
            mail.setContent(bodyParts);
        } else if (content != null) {
            mail.setContent(msg.getContent(), contentType);
        }
        return fillDetails(msg, mail);
    }

    protected static MimeMultipart assembleMultiPart(MimeBodyPart body, List<Blob> attachments)
            throws MessagingException {
        var mp = new MimeMultipart();
        if (body != null) {
            mp.addBodyPart(body);
        }
        for (Blob blob : attachments) {
            var a = new MimeBodyPart();
            a.setDataHandler(new DataHandler(new BlobDataSource(blob)));
            a.setFileName(blob.getFilename());
            mp.addBodyPart(a);
        }
        return mp;
    }

    protected static MimeMessage fillDetails(MailMessage msg, MimeMessage mimeMsg) throws MessagingException {
        mimeMsg.addFrom(toAddresses(msg.getFroms()));
        mimeMsg.setRecipients(MimeMessage.RecipientType.TO, toAddresses(msg.getTos()));
        mimeMsg.setRecipients(MimeMessage.RecipientType.CC, toAddresses(msg.getCcs()));
        mimeMsg.setRecipients(MimeMessage.RecipientType.BCC, toAddresses(msg.getBccs()));
        mimeMsg.setReplyTo(toAddresses(msg.getReplyTos()));
        mimeMsg.setSentDate(msg.getDate());
        mimeMsg.setSubject(msg.getSubject(), msg.getSubjectCharset().name());

        return mimeMsg;
    }

    protected static Address[] toAddresses(List<String> list) {
        return list.stream().map(a -> {
            try {
                return new InternetAddress(a);
            } catch (AddressException e) {
                throw new MailException("Could not parse mail address: " + a, e);
            }
        }).toArray(InternetAddress[]::new);
    }

}
