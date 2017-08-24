package com.deinersoft.timeteller;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Properties;

public class TimeTeller {

    public static void main(String [] args) {

        System.out.println(getResult(TimeZone.LOCAL,TimeFormatting.NUMERIC,false));
        System.out.println(getResult(TimeZone.UTC, TimeFormatting.NUMERIC,false));
        System.out.println(getResult(TimeZone.LOCAL, TimeFormatting.APPROXIMATE_WORDING,false));
        System.out.println(getResult(TimeZone.UTC, TimeFormatting.APPROXIMATE_WORDING,false));
        System.out.println(getResult(TimeZone.LOCAL,TimeFormatting.NUMERIC,true));
        System.out.println(getResult(TimeZone.UTC,TimeFormatting.NUMERIC,true));
        System.out.println(getResult(TimeZone.LOCAL, TimeFormatting.APPROXIMATE_WORDING,true));
        System.out.println(getResult(TimeZone.UTC, TimeFormatting.APPROXIMATE_WORDING,true));

    }

    public static final int SECONDS_IN_A_HALF_MINUTE = 30;
    public static final int HOURS_IN_A_QUARTER_OF_A_DAY = 6;
    public static final int MINUTE_TO_START_FUZZY_WORDING = 3;
    public static final int MINUTE_TO_START_FUZZING_INTO_NEXT_HOUR = 35;

    public static String getResult(TimeZone whichTimeZone, TimeFormatting typeOfFormatting, boolean special) {

        String formattedTime = "";
        int hour = 0;
        int minute = 0;
        int second = 0;

        switch (whichTimeZone) {
            case LOCAL:
                hour = LocalDateTime.now().getHour();
                minute = LocalDateTime.now().getMinute();
                second = LocalDateTime.now().getSecond();
                break;
            case UTC:
                LocalDateTime t = LocalDateTime.now(Clock.systemUTC());
                hour = t.getHour();
                minute = t.getMinute();
                second = t.getSecond();
                break;
        }

        switch (typeOfFormatting) {
            case NUMERIC:
                formattedTime = String.format("%02d:%02d:%02d", hour, minute, second);
                if (whichTimeZone == TimeZone.UTC) {
                    formattedTime += "Z";
                }
                break;
            case APPROXIMATE_WORDING:
                String[] namesOfTheHours = {"twelve", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven"};
                String[] fuzzyTimeWords = {"", "almost ten after", "ten after", "a quarter after", "twenty after", "almost half past", "half past", "almost twenty before", "twenty before", "a quarter of", "ten of", "almost"};
                String[] quadrantOfTheDay = {"at night", "in the morning", "in the afternoon", "in the evening"};

                if (second >= SECONDS_IN_A_HALF_MINUTE) minute++;

                if (minute >= MINUTE_TO_START_FUZZY_WORDING) {
                    formattedTime += fuzzyTimeWords[(minute+2)/5] + " ";
                }
                if (minute < MINUTE_TO_START_FUZZING_INTO_NEXT_HOUR) {
                    formattedTime += namesOfTheHours[hour % namesOfTheHours.length];
                }  else {
                    formattedTime += namesOfTheHours[(hour+1) % namesOfTheHours.length];
                }

                formattedTime += " " + quadrantOfTheDay[hour/HOURS_IN_A_QUARTER_OF_A_DAY];

                if (whichTimeZone == TimeZone.UTC) {
                    formattedTime += " Zulu";
                }

                break;
        }

        if (special) {
            Properties localProperties = new Properties();
            InputStream input = null;
            try {
                input = new FileInputStream("config.properties");
                localProperties.load(input);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            Properties systemProperties = System.getProperties();

            systemProperties.put("mail.smtp.auth", localProperties.getProperty("smtp.authentication.enabled"));
            systemProperties.put("mail.smtp.starttls.enable", localProperties.getProperty("smtp.starttls.enabled"));
            systemProperties.put("mail.smtp.host", localProperties.getProperty("smtp.host.to.use"));
            systemProperties.put("mail.smtp.port", localProperties.getProperty("smtp.port.to.use"));

            Session session = Session.getInstance(systemProperties,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(localProperties.getProperty("smtp.username.to.use"), localProperties.getProperty("smtp.password.to.use"));
                        }
                    });

            try {

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(localProperties.getProperty("email.sender")));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(localProperties.getProperty("email.recipient")));
                message.setSubject(localProperties.getProperty("email.subject"));
                message.setText(localProperties.getProperty("email.message") + " " + formattedTime);

                Transport.send(message);

            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }

        return formattedTime;
    }
}
