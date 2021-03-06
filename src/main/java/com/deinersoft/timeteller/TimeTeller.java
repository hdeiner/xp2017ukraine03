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
                String[] fuzzyTimeWords = {"about", "a little after", "about ten after", "about a quarter after", "about twenty after", "almost half past", "about half past", "almost twenty before", "about twenty before", "about a quarter of", "about ten of", "almost", "about"};
                String[] quadrantOfTheDay = {"at night", "in the morning", "in the afternoon", "in the evening"};

                if (second >= SECONDS_IN_A_HALF_MINUTE) minute++;

                formattedTime += fuzzyTimeWords[(minute+2)/5] + " ";

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
            Properties configuration = new Properties();
            InputStream input = null;
            try {
                input = new FileInputStream("config.properties");
                configuration.load(input);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            Properties systemProperties = System.getProperties();

            systemProperties.put("mail.smtp.auth", configuration.getProperty("smtp.authentication.enabled"));
            systemProperties.put("mail.smtp.starttls.enable", configuration.getProperty("smtp.starttls.enabled"));
            systemProperties.put("mail.smtp.host", configuration.getProperty("smtp.host.to.use"));
            systemProperties.put("mail.smtp.port", configuration.getProperty("smtp.port.to.use"));

            Session eMailSession = Session.getInstance(systemProperties,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(configuration.getProperty("smtp.username.to.use"), configuration.getProperty("smtp.password.to.use"));
                        }
                    });

            try {

                Message message = new MimeMessage(eMailSession);
                message.setFrom(new InternetAddress(configuration.getProperty("email.sender")));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(configuration.getProperty("email.recipient")));
                message.setSubject(configuration.getProperty("email.subject"));
                message.setText(configuration.getProperty("email.message") + " " + formattedTime);

                Transport.send(message);

            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }

        return formattedTime;
    }
}
