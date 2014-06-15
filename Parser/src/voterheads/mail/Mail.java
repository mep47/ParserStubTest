package voterheads.mail;

/**
 * Created by chasedaigle on 6/6/14.
 */
public class Mail
{

    /**
     * Object that holds information about an email
     * to be passed to the MailManager sendEmail Function
     */
    String subject;
    String body;
    String to;
    String from;

    public String getSubject ()
    {
        return subject;
    }

    public void setSubject (String subject)
    {
        this.subject = subject;
    }

    public String getBody ()
    {
        return body;
    }

    public void setBody (String body)
    {
        this.body = body;
    }

    public String getTo ()
    {
        return to;
    }

    public void setTo (String to)
    {
        this.to = to;
    }

    public String getFrom ()
    {
        return from;
    }

    public void setFrom (String from)
    {
        this.from = from;
    }

    public void appendBody (String what)
    {
        this.body += what;
    }

    public String toString ()
    {
        String thisMail;
        thisMail = "Subject: " + this.getSubject() + "\n";
        thisMail += "To: " + this.getTo() + "\n";
        thisMail += "From: " + this.getFrom() + "\n";
        thisMail += "Body: \n" + this.getBody();
        return thisMail;
    }

}

