package test.com.deinersoft.timeteller;

import com.deinersoft.timeteller.*;

import com.sun.mail.imap.IMAPFolder;
import org.junit.Before;
import org.junit.Test;

import javax.mail.*;
import javax.mail.Flags.Flag;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

public class TimeTellerTest {

    private TimeTeller timeTeller;

    @Before
    public void initialize(){
         timeTeller = new TimeTeller();
    }

    @Test
    public void localTimeCurrent(){
        assertThat(timeTeller.getResult(TimeZone.LOCAL, TimeFormatting.NUMERIC,false), is(getFormattedTime(LocalDateTime.now())));
    }

    @Test
    public void zuluTimeCurrent(){
        assertThat(timeTeller.getResult(TimeZone.UTC, TimeFormatting.NUMERIC,false), is(getFormattedTime(LocalDateTime.now(Clock.systemUTC()))+"Z"));
    }

    @Test
    public void localTimeInWordsCurrent(){
        assertThat(timeTeller.getResult(TimeZone.LOCAL, TimeFormatting.APPROXIMATE_WORDING,false), matchesPattern("^(\\s|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|twenty|almost|a|quarter|half|of|past|after|before|at|night|in|the|morning|afternoon|evening|night)+$"));
    }

    @Test
    public void zuluTimeInWordsCurrent(){
        assertThat(timeTeller.getResult(TimeZone.UTC, TimeFormatting.APPROXIMATE_WORDING,false), matchesPattern("^(\\s|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|twenty|almost|a|quarter|half|of|past|after|before|at|night|in|the|morning|afternoon|evening|night)+Zulu$"));
    }

    @Test
    public void emailForLocalTime(){
        String localTimeNowFormatted = getFormattedTime(LocalDateTime.now());
        timeTeller.getResult(TimeZone.LOCAL, TimeFormatting.NUMERIC,true);

        boolean receivedEmail = false;
        for (int readAttempts = 1; (readAttempts <= 5) && (!receivedEmail); readAttempts++ ) {
            receivedEmail = lookForTimeTellerEmail(localTimeNowFormatted);
        }
        assertThat(receivedEmail, is(true));
    }

    private String getFormattedTime(LocalDateTime clock){
        int localHour = clock.getHour();
        int localMinute = clock.getMinute();
        int localSecond = clock.getSecond();
        return String.format("%02d:%02d:%02d", localHour, localMinute, localSecond);
    }

    private boolean lookForTimeTellerEmail(String localTimeNowFormatted){
        boolean receivedEmail = false;
        IMAPFolder folder = null;
        Store store = null;
        try {
            Properties props = System.getProperties();
            props.setProperty("mail.store.protocol", "imaps");

            Session session = Session.getDefaultInstance(props, null);
            store = session.getStore("imaps");
            store.connect("imap.googlemail.com","howarddeiner.xyzzy@gmail.com", "birneraccount");

            folder = (IMAPFolder) store.getFolder("inbox");
            if(!folder.isOpen()) {
                folder.open(Folder.READ_WRITE);
                Message[] messages = folder.getMessages();
                for (Message msg : messages) {
                    if (msg.getSubject().equals("TimeTeller")) {
                        if (((String) msg.getContent()).contains(localTimeNowFormatted)) {
                            receivedEmail = true;
                            msg.setFlag(Flag.DELETED, true);
                        }
                    }
                }
            }
        }
        catch (Exception e) { }
        finally {
            try {
                if (folder != null && folder.isOpen()) folder.close(true);
                if (store != null) store.close();
            }
            catch (Exception e) { }
        }

        if (!receivedEmail) {
            try { TimeUnit.SECONDS.sleep(1); }
            catch(InterruptedException e){ }
        }

        return receivedEmail;
    }
}