package voterheads.mail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import com.cribbstechnologies.clients.mandrill.exception.RequestFailedException;
import com.cribbstechnologies.clients.mandrill.model.MandrillHtmlMessage;
import com.cribbstechnologies.clients.mandrill.model.MandrillMessageRequest;
import com.cribbstechnologies.clients.mandrill.model.MandrillRecipient;
import com.cribbstechnologies.clients.mandrill.request.MandrillMessagesRequest;
import com.cribbstechnologies.clients.mandrill.request.MandrillRESTRequest;
import com.cribbstechnologies.clients.mandrill.util.MandrillConfiguration;

public class MailManager
{

    private static Logger                  logger          = Logger.getLogger(MailManager.class);

    private static MandrillRESTRequest     request         = new MandrillRESTRequest();
    private static MandrillConfiguration   config          = new MandrillConfiguration();
    private static MandrillMessagesRequest messagesRequest = new MandrillMessagesRequest();
    private static HttpClient              client;
    private static ObjectMapper            mapper          = new ObjectMapper();

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // TODO Auto-generated method stub

    }

    public static void sendEmail(String text, String to, String from,
            String subject)
    {

        logger.info("### Sending Email ###");
        config.setApiKey("K8DAVgcw9DjYrNU2FAqq5g");
        config.setApiVersion("1.0");
        config.setBaseURL("https://mandrillapp.com/api");
        request.setConfig(config);
        request.setObjectMapper(mapper);
        messagesRequest.setRequest(request);

        client = new DefaultHttpClient();
        request.setHttpClient(client);
        final MandrillMessageRequest mmr = new MandrillMessageRequest();
        final MandrillHtmlMessage message = new MandrillHtmlMessage();
        final Map<String, String> headers = new HashMap<String, String>();
        message.setFrom_email(from);
        message.setFrom_name(from);
        message.setHeaders(headers);

        final DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
        final Calendar cal = Calendar.getInstance();
        final String mailDate = dateFormat.format(cal.getTime());
        message.setSubject(subject);
        message.setHtml(text);

        final String[] emailAddresses = to.split(",");

        final MandrillRecipient[] recipients = new MandrillRecipient[emailAddresses.length];
        int idx = 0;
        for (final String str : emailAddresses)
        {
            final MandrillRecipient recip = new MandrillRecipient("", str);
            recipients[idx++] = recip;
        }
        message.setTo(recipients);
        // message.setTrack_clicks(true);
        // message.setTrack_opens(true);

        final String[] tags = new String[1];
        message.setTags(tags);
        mmr.setMessage(message);

        try
        {
            messagesRequest.sendMessage(mmr);
        }
        catch (final RequestFailedException e)
        {
            e.printStackTrace();
            // fail(e.getMessage());
        }
        finally
        {
            logger.info("Message Sent");
        }

    }

}
