package voterheads.extractor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.PDFOperator;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.PositionWrapper;
import org.apache.pdfbox.util.TextPosition;

import voterheads.CLArguments;
import voterheads.FilenameUrlPair;
import voterheads.Organization;
import voterheads.Voterheads;
import voterheads.index.QueryResult;

import com.google.gson.Gson;

@SuppressWarnings("deprecation")
public class Extractor
{

    private class MyPDFTextStripper extends PDFTextStripper
    {

        private final String  FOOTER_DATE           = "^(January|February|March|April|May|June|July|August|September|October|November|December)\\b";
        private final Pattern footerDatePat         = Pattern
                                                            .compile(FOOTER_DATE);

        boolean               bold                  = false;
        float                 previousCharY         = 0;
        TextLocation          previousCharX         = null;
        String                previousChar          = null;
        float                 prevPrevLineY         = 0;
        float                 prevLineY             = 0;
        boolean               lineIsBold            = false;
        float                 y                     = 0;
        float                 x                     = 0;
        Integer               lineNumberCnt         = 0;
        int                   charCount             = 0;
        StringBuffer          lineBuf               = new StringBuffer(200);

        PDFont                font                  = null;
        String                fontType              = null;
        float                 fontSize              = 0;

        int                   pageNum               = 0;

        float                 firstCharX            = 0;
        float                 lastCharX             = 0;

        boolean               startingNewLine       = true;
        float                 prevFontSizeInPt      = 0;

        boolean               isGettingWord         = false;
        int                   wordCount             = 1;
        boolean               hasPeriod             = false;
        boolean               isMultipleUnderscores = false;
        VoterHeadTextLine     prevPrevLine          = null;
        boolean               terminateDocument     = false;

        public MyPDFTextStripper() throws IOException
        {
            super();
            leftMostTextStartPositon.x = Float.MAX_VALUE;
        }

        private void checkIfMergeWithPrevPrevLine()
        {
            // See if this is that PDF querk where this line is a continuation
            // of the prevPrevline (i.e. their Y's are equal. We have to check
            // for that to know we are at the end of line since the Y may be
            // less than six which keeps us from thinking a superscript is a
            // new line
            boolean merge = false;

            if (textLines.size() > 1)
            {
                prevPrevLine = textLines.get(textLines.size() - 2);
                prevPrevLineY = prevPrevLine.getY();
                prevLineY = textLines.get(textLines.size() - 1).getY();
            }

            if ((y != prevLineY) && (previousCharY == prevPrevLineY))
            {
                merge = true;
            }

            if (!merge)
            {
                prevPrevLine = null;
            }

            // if(lineNumberCnt > 7 && lineNumberCnt < 11)
            // {
            // System.out.println("MERGE WITH PREV PREV LINE = "+merge+" pply="+prevPrevLineY+" ply="+prevLineY+" pcy="+previousCharY);
            // }

        }

        @Override
        protected void endDocument(PDDocument pdf) throws IOException
        {
            System.out.println("##### END OF DOCUMENT REACHED ######### line="
                    + lineNumberCnt);
            if (charCount > 0)
            {
                System.out
                        .println("Force Writing the last line at end of document");
                final VoterHeadTextLine textLine = new VoterHeadTextLine(
                        previousCharY);
                textLine.setLineNumber(lineNumberCnt);
                textLine.setPageNumber(pageNum);
                textLine.setTextStartX(previousCharX);
                textLine.setTextEndX(lastCharX);
                textLine.setBold(lineIsBold);
                textLine.setFirstCharFontType(fontType);
                textLine.setText(lineBuf.toString());
                textLines.add(textLine);//

                // System.out.println("line#="+lineNumberCnt+" "+lineBuf.toString()+" fontType="+fontType);
                if (lineIsBold)
                {
                    // boldLineNumbers.add(lineNumberCnt);
                }
                // startOfText.put(lineNumberCnt, previousCharX);
            }
            lastLine = lineNumberCnt;
            pdf.close(); // memory leak
        }

        @Override
        protected void endPage(PDPage page)
        {
            // pageNum += 1;
        }

        @Override
        // pageContent =
        protected PositionWrapper handleLineSeparation(PositionWrapper current,
                PositionWrapper lastPosition,
                PositionWrapper lastLineStartPosition, float maxHeightForLine)
                throws IOException
        {
            // System.out.println("handleLineSeparation");
            return super.handleLineSeparation(current, lastPosition,
                    lastLineStartPosition, maxHeightForLine);
        }

        private boolean lineIsCentered(float startX, float endX)
        {
            boolean centered = false;

            final float textFollowingSpace = pageWidth - endX;
            final float precedingFollowingDiff = startX - textFollowingSpace;
            // if(lineNumberCnt == 0)
            // {
            // pageWidth = pageWidth + precedingFollowingDiff;
            // }
            if (Math.abs(precedingFollowingDiff) < 15)
            {
                centered = true;
                // System.out.println("++++++++ Line is Centered ++++++++");
            }

            return centered;
        }

        @Override
        protected void processTextPosition(TextPosition text)
        {
            // System.out.println("start processTextPosition linNumberCnt="+lineNumberCnt);

            if (terminateDocument)
            {
                return;
            }
            VoterHeadTextLine textLine = null;

            font = text.getFont();
            fontType = font.getBaseFont();
            fontSize = text.getFontSize();

            String c = text.getCharacter();

            // System.out.println("fontType = "+fontType);
            x = text.getX();
            y = text.getY();

            if ((lineNumberCnt > -1) && (lineNumberCnt < -1))
            {
                final int j = c.charAt(0);
                if (textLines.size() > 1)
                {
                    prevPrevLine = textLines.get(textLines.size() - 1);
                    prevPrevLineY = prevPrevLine.getY();
                }
                System.out.println("c=" + c + "(" + j + ") x=" + x + " y=" + y
                        + " prevPrevLineY=" + prevPrevLineY + " previousCharY="
                        + previousCharY + " fontType=" + fontType
                        + " fontSizeIOnPt=" + text.getFontSizeInPt()
                        + " prevPrevLineText=" + prevPrevLine.getText());
                // System.out.println("PREVIOUS CHAR Y = "+previousCharY+" Y = "+y+" fs="+text.getFontSizeInPt()+" pfs="+prevFontSizeInPt);
                prevPrevLine = null;
                final int[] codePoints = text.getCodePoints();
                if (codePoints != null)
                {
                    System.out.print("CodePoints: ");
                    for (final int pt : codePoints)
                    {
                        System.out.print(pt + " ");
                    }
                    System.out.println(" ");
                }
            }

            if (previousCharY == 0)
            {
                previousCharY = y;
                // font = text.getFont();
                // fontType = font.getBaseFont();
            }

            if (previousCharX == null)
            {
                previousCharX = new TextLocation(x);
                firstCharX = x;
            }

            if (c.equals("."))
            {
                hasPeriod = true;
            }
            else if (c.charAt(0) == 160)
            {
                c = " ";
            }

            if (Character.isWhitespace(c.charAt(0)))
            {
                if (isGettingWord)
                {
                    // System.out.println("Adding 1 to wordCount");
                    wordCount += 1;
                    isGettingWord = false;
                }

            }
            else
            {
                isGettingWord = true;
            }

            /*
             * Detects that we are staring a new line The -6 offset is to avoid
             * erroneously setting new line due to a superscript
             */

            if ((y < (previousCharY - 6)) || (y > (previousCharY + 2)))
            {
                // if(Character.isWhitespace(c.charAt(0)))
                // {
                // return;
                // }
                if (lineBuf.toString().toLowerCase().contains("adjourn"))
                {
                    adjournFound = true;
                }
                textLine = new VoterHeadTextLine(previousCharY);
                if (isGettingWord)
                {
                    // System.out.println("Adding 1 to wordCount");
                    wordCount += 1;
                    isGettingWord = false;
                }

                textLine.setTableOfContents(false);
                if (isMultipleUnderscores
                        && (lineBuf.toString().trim().charAt(0) != '_'))
                {
                    stripMultipleUnderscores(lineBuf);
                    handlingTOC = true;
                    textLine.setTableOfContents(true);
                }
                // System.out.println("Starting New Line");
                startingNewLine = true;
                if (charCount == 0)
                {
                    // System.out.println("##### End of blank Line ########");
                    textLine.setPageNumber(pageNum);
                    textLine.setText(null);
                    textLine.setLineNumber(lineNumberCnt);
                    lineNumberCnt += 1;
                    textLines.add(textLine);

                    previousCharY = y;
                    lineBuf = new StringBuffer(200);
                    charCount = 0;
                }
                else
                {
                    // System.out.println("##### End of Line ########");
                    // System.out.println("line#="+lineNumberCnt+" "+lineBuf.toString()+" fontType="+fontType);

                    // some PDF's have a space with a higher y but then the
                    // following
                    // text is at the previous y and should go with that line.
                    // if(textLines.size() > 1)
                    // {
                    // prevPrevLine = textLines.get(textLines.size() - 1);
                    // System.out.println("ProcessTextPosition - prevPrevY="+prevPrevLine.getY()+" y="+y);
                    // if(prevPrevLine.getY() != y)
                    // {
                    // prevPrevLine = null;
                    // }
                    // }

                    checkIfMergeWithPrevPrevLine();

                    if (prevPrevLine != null)
                    {
                        prevPrevLine.setTextEndX(lastCharX);
                        prevPrevLine.setWordCount(wordCount);
                        prevPrevLine.setHasPeriod(hasPeriod);
                        prevPrevLine.setBold(lineIsBold);
                    }
                    else
                    {
                        textLine.setLineNumber(lineNumberCnt);
                        textLine.setPageNumber(pageNum);
                        textLine.setTextStartX(new TextLocation(firstCharX));
                        textLine.setTextEndX(lastCharX);
                        textLine.setBold(lineIsBold);
                        textLine.setWordCount(wordCount);
                        textLine.setHasPeriod(hasPeriod);
                        if (((lineNumberCnt > 0) && (textLines.get(
                                lineNumberCnt - 1).getText() == null))
                                || isPrecedingBlankLine)
                        {
                            textLine.setHasPrecedingBlankLine(true);
                            isPrecedingBlankLine = false;
                        }
                        if (!adjournFound && (wordCount < 8)
                                && (firstCharX > 40)
                                && (firstCharX < leftMostTextStartPositon.x))
                        {
                            leftMostTextStartPositon.x = firstCharX;
                        }
                    }

                    if (!isMultipleUnderscores && (prevPrevLine == null))
                    {
                        if (lineIsCentered(firstCharX, lastCharX))
                        {
                            VoterHeadTextLine l = null;
                            final int size = textLines.size();
                            if (size == 1)
                            {
                                l = textLines.get(0);
                                l.setCentered(true);
                            }
                            textLine.setCentered(true);
                        }
                    }

                    if (prevPrevLine != null)
                    {
                        final String t = prevPrevLine.getText();
                        if (t != null)
                        {
                            prevPrevLine.setText(t.substring(0, t.length() - 1)
                                    + lineBuf.toString());
                            // textLines.set(textLines.size() - 1,
                            // prevPrevLine);
                        }
                        prevPrevLine = null;
                    }
                    else
                    {
                        textLine.setFirstCharFontType(fontType);
                        lineBuf.append("\n");
                        textLine.setText(lineBuf.toString());
                        // Specific to Charlotte with Table of Contents
                        // Need to get page numbers from footer which is located
                        // physically in the file immediately following the
                        // header.
                        // the footer line has Y of 7 hundred something then the
                        // next line has a Y at the fron of the page such as
                        // around
                        // 70. The footer starts with the date and ends with the
                        // page number which is what we need so we can stop
                        // processing on the last page as shown in the Table of
                        // Contents.

                        if ((prevLineY > y) && (y < 100)) // y < 100 means this
                                                          // line
                        // is near front of page
                        {
                            int pageNumber = 0;

                            final String txt = textLine.getText();

                            Matcher m = footerDatePat.matcher(txt);
                            if (m.find())
                            {
                                m.reset();
                                final int pos = txt.lastIndexOf("  ");
                                String numberStr = null;
                                final Pattern regex = Pattern
                                        .compile("[0-9]{1,4}");
                                m = regex.matcher(txt.substring(pos + 1));
                                if (m.find())
                                {

                                    numberStr = m.group();
                                    pageNumber = Integer.parseInt(numberStr);
                                    ;
                                    if (pageNumber == lastPageToProcess)
                                    {
                                        terminateDocument = true;
                                    }
                                }
                            }
                        }
                        textLines.add(textLine);
                        lineNumberCnt += 1;
                    }

                    // wordCount = 0;
                    // System.out.println("##### "+textLine);
                    previousCharY = y;
                    lineBuf = new StringBuffer(200);
                    lineIsBold = false;
                    hasPeriod = false;
                    charCount = 0;
                    // System.out.println("Adding 1 to lineNumber gives "+lineNumberCnt);
                }
                isMultipleUnderscores = false;
                // System.out.println("Setting wordCount to 1");
                wordCount = 0;
            }
            if (!Character.isWhitespace(c.charAt(0)))
            {
                if (fontType.contains("Bold"))
                {
                    lineIsBold = true;
                }
                if (startingNewLine)
                {
                    firstCharX = x;
                    if (textLines.size() > 0)
                    {
                        prevLineY = textLines.get(textLines.size() - 1).getY();
                        if ((y - prevLineY) > 20)
                        {
                            isPrecedingBlankLine = true;
                        }
                    }
                    startingNewLine = false;
                }
                if (previousChar != null)
                {
                    if (previousChar.equals("."))
                    {
                        // System.out.println("******** PREVIOUS CHAR EQUALS PERIOD prevX="+previousCharX.x+" x="+x+" diff="+diff);
                        if ((x - previousCharX.x) > 9)
                        {
                            lineBuf.append(" ");
                            charCount += 1;
                        }
                    }
                    else if (c.equals("_") && previousChar.equals("_"))
                    {
                        isMultipleUnderscores = true;
                    }
                }
                previousChar = c;
                previousCharX = new TextLocation(x);
                prevFontSizeInPt = text.getFontSizeInPt();
                charCount += 1;
                lastCharX = x;
            }

            lineBuf.append(c);

            super.processTextPosition(text);
        }

        private void stripMultipleUnderscores(StringBuffer sb)
        {
            // changed to not strip underscores but to set line as type table of
            // Contents.
            // Only going to strip multiple underscores if in a Table of
            // Contents
            boolean digitFoundAfterUnderscores = false;
            final int pos2 = sb.lastIndexOf("__") + 3;
            for (int i = pos2; i < sb.toString().length(); i++)
            {
                if (Character.isDigit(sb.charAt(i)))
                {
                    digitFoundAfterUnderscores = true;
                }
            }

            if (digitFoundAfterUnderscores)
            {
                // sb.delete(pos1, pos3);
            }
        }

        @Override
        protected void writeCharacters(TextPosition text)
        {
            text.getCharacter();
        }

        @Override
        protected void writeLineSeparator() throws IOException
        {
            super.writeLineSeparator();
            // System.out.println("WriteLineSeparator");
        }

    }

    public enum OrdinalType
    {
        NUMBER, SMALL_ROMAN, LARGE_ROMAN, UPPER, LOWER, NUMBER_NB, UPPER_NB, MECH_ID, MIXED_1a, MIXED_1a1, CALL_ORDER, BOLD_TIME, BOLD_MIX, BOLD_UPPER, BOLD_CENTERED, BULLET, NONE
    }

    private static Logger       logger     = Logger.getLogger(Extractor.class);

    private static Properties   props      = Voterheads.getProperties();

    public static final String  DATE_REGEX = "January|February|March|April|May|June|July|August|September|October|November|December";
    public static final Pattern pat        = Pattern.compile(DATE_REGEX);

    public static void main(String[] args)
    {
		
	    final String home = System.getProperty("user.home");

        BufferedWriter out = null;
        String topicsFileString = home+"/VoterheadsTest/Topics/Topics.txt";

        Extractor extractor;
        Event event;

        final Organization org = new Organization();
        org.setId("1111");

        extractor = new Extractor();

        for (final VoterHeadTextLine line : extractor.getTextLines())
        {
            System.out.println(line);
        }

        event = extractor.extractAllSections();

        try {
			out = new BufferedWriter(new FileWriter( new File(topicsFileString)));
	        for (final Topic top : event.getTopics())
	        {
	            out.write("\n" + top + "\n");
	        }
	        out.flush();
		} catch (IOException e) {
			logger.error(e, e);
		}
        finally
        {
        	try {
				out.close();
			} catch (IOException e) {
				logger.error(e, e);
			}
        }
    }

    public static void performExtractionParse(FilenameUrlPair pair,
            Organization org, QueryResult queryResult, String statusString)
    {
		
	    final String home = System.getProperty("user.home");

        BufferedWriter out = null;
        String topicsFileString = home+"/VoterheadsTest/Topics/Topics.txt";

        final Extractor extractor = new Extractor();

        extractor.pdfBoxParse(pair.getFilename(), org.getId());

        extractor.orderByPage();
        extractor.sortLines();
        // for(VoterHeadTextLine line: extractor.getTextLines())
        // {
        // System.out.println(line);
        // }
        final Event event = extractor.extractAllSections();
        // extractor.SendJSON(pair.getUrl(), queryResult, statusString);
        if (event.getTopics().size() > 200)
        {
            logger.warn("Topic limit hit: " + event.getTopics().size()
                    + " topics created");
            return;
        }

        logger.info("write Topics.txt file");
        try {
			out = new BufferedWriter(new FileWriter( new File(topicsFileString)));
	        for (final Topic top : event.getTopics())
	        {
	            out.write("\n" + top + "\n");
	        }
	        out.flush();
		} catch (IOException e) {
			logger.error(e, e);
		}
        finally
        {
        	try {
				out.close();
			} catch (IOException e) {
				logger.error(e, e);
			}
        }

        // logger.info("In performExtraction Parse SENDJSON: " +
        // props.getProperty("sendJSON"));
        if (props.getProperty("sendJSON").equalsIgnoreCase("True"))
        {
            extractor.SendJSON(pair.getUrl(), queryResult, statusString);
            // logger.info("got to properties sendJSON true");
        }
        else
        {
            // logger.info("got to properties sendJSON false");
        }

    }

    String                  orgId;
    String                  pageContent               = null;
    String                  pageFilename              = null;
    int                     order                     = 1;
    int                     previousTopicOrderNumber  = 0;
    boolean                 previousIsBold            = false;
    boolean                 documentIsFinished        = false;

    boolean                 nextLineIsName            = false;
    boolean                 isPrecedingBlankLine      = false;
    boolean                 handlingTOC               = false;
    boolean                 processingDocumentHeading = true;
    boolean                 processingTableOfContents = false;
    OrdinalType             previousOrdinalType       = OrdinalType.NONE;

    String                  previousOrdinal           = null;
    // int indentCount = 0;
    TextLocation            textStart                 = null;
    String                  ordinal                   = null;

    String                  name                      = null;
    String                  startTime                 = null;

    Stack<Integer>          parentOrderNumbers        = new Stack<Integer>();

    // Stack<Integer> currentIndentLevel = new Stack<Integer>();
    Stack<TextLocation>     currentIndentLevel        = new Stack<TextLocation>();
    Stack<OrdinalType>      parentOrdinalTypes        = new Stack<OrdinalType>();
    boolean                 parsingSection            = false;
    int                     blankLineCount            = 0;
    StringBuffer            description               = new StringBuffer(100);

    Topic                   topic                     = null;
    Event                   event                     = null;
    List<Event>             events                    = new ArrayList<Event>();             ;
    List<VoterHeadTextLine> textLines                 = new ArrayList<VoterHeadTextLine>();
    // List<Integer> boldLineNumbers = new ArrayList<Integer>();
    // List<Integer> centeredLineNumbers = new ArrayList<Integer>();
    // Map<Integer, TextLocation> startOfText = new HashMap<Integer,
    // TextLocation>();
    TextLocation            previousStartOfText       = null;
    Integer                 lineCounter               = 0;

    int                     lastLine                  = 0;

    float                   pageWidth                 = 0;

    TextLocation            leftMostTextStartPositon  = new TextLocation(
                                                              Float.MAX_VALUE);

    int                     lastPageToProcess         = 0;

    boolean                 adjournFound              = false;

    private void createTopic()
    {
        // System.out.println("@@@@@@@@@@@ Creating Topic");
        topic = new Topic();
        topic.setName(name);
        topic.setEntityOrdinal(ordinal);
        topic.setOrder(order);
        topic.setParentOrderNumber(parentOrderNumbers.peek());
        event.getTopics().add(topic);

    }

    public Event extractAllSections()
    {

        // 05-16-2014 14:00 -0400
        final Calendar cal = Calendar.getInstance();
        final Date date = cal.getTime();
        final SimpleDateFormat format1 = new SimpleDateFormat(
                "MM-dd-yyyy HH:mm Z");
        startTime = format1.format(date);
        processingTableOfContents = false;
        parentOrderNumbers.push(0);
        currentIndentLevel.push(new TextLocation(0));
        parentOrdinalTypes.push(OrdinalType.NONE);

        event = new Event();
        event.setOrgId(orgId);
        event.setDocLink(pageFilename);
        TextLocation textStart = null;

        // BufferedReader reader = new BufferedReader(new
        // StringReader(pageContent));

        String section = null;

        try
        {
            // while((line = reader.readLine()) != null &&
            // lineCounter.intValue() <= lastLine)
            for (final VoterHeadTextLine ln : textLines)
            {
                if (ln.isHeader() || ln.isFooter())
                {
                    continue;
                }
                if ((ln.getText() != null)
                        && (ln.getText().equals("MAYOR") || ln.getText()
                                .equals("MAYOR PRO TEM")))
                {
                    continue;
                }
                if (processingTableOfContents)
                {
                    if (!ln.isTableOfContents())
                    {
                        final int nextLineIndex = ln.getLineNumber() + 1;
                        final int lineIndex = skipBlankLines(nextLineIndex + 1);
                        final int nextNextLineIndex = lineIndex + 1;
                        if (textLines.get(nextLineIndex).isTableOfContents()
                                || textLines.get(nextNextLineIndex)
                                        .isTableOfContents()
                                || (ln.getText() == null))
                        {
                            continue;
                        }
                        else
                        {
                            processingTableOfContents = false;
                        }
                    }
                }
                if (ln.isTableOfContents())
                {
                    processingTableOfContents = true;
                    final String txt = ln.getText();
                    final int p1 = txt.lastIndexOf("_");
                    if (p1 != -1)
                    {
                        txt.charAt(p1);
                        String numberStr = null;
                        final Pattern regex = Pattern.compile("[0-9]{1,4}");
                        final Matcher m = regex.matcher(txt.substring(p1));
                        if (m.find())
                        {
                            numberStr = m.group();
                        }
                        try
                        {
                            final int parseResult = Integer.parseInt(numberStr);
                            lastPageToProcess = parseResult;
                            ln.getPageNumber();
                        }
                        catch (final NumberFormatException nfe)
                        {
                            // ignore exception
                        }
                    }
                    continue;
                }
                textStart = ln.getTextStartX();
                if (processingDocumentHeading)
                {
                    if (textStart == null)
                    {
                        continue;
                    }
                    else
                    {
                        if (textStart.x > (leftMostTextStartPositon.x + 17))
                        {
                            continue;
                        }
                        else
                        {
                            processingDocumentHeading = false;
                        }
                    }
                }
                if (ln.getText() == null)
                {
                    blankLineCount += 1;
                    // System.out.println("@@@@@@@@@@ blankLineCount = "+blankLineCount);
                    // --
                    if (blankLineCount >= 1)
                    {
                        if (parsingSection)
                        {
                            if (description.length() > 0)
                            {
                                topic.setDescription(description.toString());
                                // System.out.println("@@@@@@@@@@@ add description to Topic");
                                description = new StringBuffer(100);
                                parsingSection = false;
                            }
                        }
                    }
                    // --
                }
                else
                {
                    // System.out.println(lineCounter+"==="+ln.getText());
                    blankLineCount = 0;
                    // lineCounter += 1;
                    section = extractSection(ln);
                    if (section != null)
                    {
                        parsingSection = true;
                        // System.out.println("@@@@@@@@@@@ Setting parsingSection to true");
                    }
                    // --
                    else
                    {
                        if (parsingSection)
                        {
                            if (nextLineIsName)
                            {
                                topic.setName(ln.getText());
                                nextLineIsName = false;
                            }
                            else
                            {
                                description.append(ln.getText());
                            }
                            // System.out.println("@@@@@@@@@@ Appending line: "+line);
                        }
                    }
                    lineCounter += 1;
                    // --
                }
                if (documentIsFinished)
                {
                    break;
                }

                // System.out.println("###"+section);
            }

        }
        catch (final Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return event;
    }

    public String extractSection(VoterHeadTextLine ln)
    {

        // System.out.println("extractSection: text="+ln.getText());

        OrdinalType ordType = null;
        int popCount = 0;

        final String subLine = ln.getText().trim();
        textStart = ln.getTextStartX();

        ordType = this.getOrdinal(subLine, ln);
        if (ordType != OrdinalType.NONE)
        {
            if ((ordType != previousOrdinalType)
                    && (ordType != OrdinalType.BOLD_UPPER)
                    && (ordType != OrdinalType.BOLD_CENTERED))
            {
                if (((popCount = ordTypeOnStack(ordType)) == 0)
                // if ordinal is 1 then push but don't push on 10 or 11 etc.
                        || ((subLine.charAt(0) == '1') && !Character
                                .isDigit(subLine.charAt(1))))
                {
                    parentOrdinalTypes.push(previousOrdinalType);
                    currentIndentLevel.push(textStart);
                    parentOrderNumbers.push(previousTopicOrderNumber);
                }
                else
                {
                    for (int i = 0; i < popCount; i++)
                    {
                        parentOrdinalTypes.pop();
                        currentIndentLevel.pop();
                        parentOrderNumbers.pop();
                    }
                }
            }
            else if (!nextInSequence(ordType, previousOrdinal, ordinal))
            {
                if (ordType == OrdinalType.BOLD_CENTERED)
                {
                    while ((parentOrdinalTypes.peek() != OrdinalType.NONE)
                            && (ln.getLineNumber() > 10))
                    {
                        parentOrdinalTypes.pop();
                        currentIndentLevel.pop();
                        parentOrderNumbers.pop();
                    }
                }
                // if(indentCount > currentIndentLevel.peek() )
                else if (textStart.greater(currentIndentLevel.peek()))
                {
                    // currentIndentLevel.push(indentCount);
                    parentOrdinalTypes.push(previousOrdinalType);
                    currentIndentLevel.push(textStart);
                    parentOrderNumbers.push(previousTopicOrderNumber);
                }
                // else if (indentCount < currentIndentLevel.peek())
                else if (textStart.less(currentIndentLevel.peek())
                        || textStart.equals(currentIndentLevel.peek()))
                {
                    // while(ordType != parentOrdinalTypes.peek())
                    popCount = ordTypeOnStack(ordType);
                    for (int i = 0; i < popCount; i++)
                    {
                        currentIndentLevel.pop();
                        parentOrderNumbers.pop();
                        parentOrdinalTypes.pop();
                    }
                }
            }

            createTopic();

            if (description.length() > 0)
            {
                topic.setDescription(description.toString());
                // System.out.println("@@@@@@@@@@@ add description to Topic");
                description = new StringBuffer(100);
            }

            previousTopicOrderNumber = order;
            previousOrdinalType = ordType;
            previousOrdinal = ordinal;
            order += 1;
        }

        if (StringUtils.containsIgnoreCase(ln.getText(), "adjour")
                && (ln.getWordCount() < 2)
                && (topic.getParentOrderNumber() == 0))
        {
            documentIsFinished = true;
        }

        return ordinal;
    }

    public OrdinalType getOrdinal(String subLine, VoterHeadTextLine ln)
    {
        final String[] exp = { "^[(\\d{1,3}\\s-]", "^[ivxl\\s-]",
                "^[IVXL\\s-]", "^[A-Z\\s-]", "^[a-z\\s-]", "^[(\\d{1,3}]",
                "^[A-Z]", "^[0-9]{2}\\-[0-9]{4}",
                "^[(\\d{1,3}\\s-]\\.[a-z\\s-]",
                "^[(\\d{1,3}\\s-]\\.[a-z\\s-]\\.[(\\d{1,3}\\s-]" };
        //
        OrdinalType ordType = OrdinalType.NONE;
        int ordSize = 0;
        ordinal = null;
        Pattern pat = null;
        int digitCount = 0;
        String[] words = null;

        Matcher matcher = null;

        if (ln.getTextStartX().x > 150)
        {
            return ordType;
        }

        for (int i = OrdinalType.NUMBER.ordinal(); i <= OrdinalType.MIXED_1a1
                .ordinal(); i++)
        {
            if ((subLine.length() > 13)
                    && (subLine.substring(0, 14).equals("CONSENT AGENDA") || subLine
                            .substring(0, 14).equals("REGULAR AGENDA")))
            {
                ordType = OrdinalType.NONE;
                ordinal = "";

                return ordType;
            }
            if (i == OrdinalType.MECH_ID.ordinal())
            {
                pat = Pattern.compile(exp[i]);
                matcher = pat.matcher(subLine);
                if (matcher.find())
                {
                    ordType = OrdinalType.values()[i];
                    ordSize = matcher.end();
                    ordinal = subLine.substring(0, matcher.end());
                    break;
                }

            }
            else if ((i == OrdinalType.NUMBER_NB.ordinal())
                    || (i == OrdinalType.UPPER_NB.ordinal()))
            {
                pat = Pattern.compile(exp[i] + "+[\\.\\d{0,2})]");
                matcher = pat.matcher(subLine);
                if (matcher.find())
                {
                    ordSize = matcher.end();
                    if (ordSize < subLine.length())
                    {
                        ordType = OrdinalType.values()[i];
                        ordinal = subLine.substring(0, ordSize + 1);
                    }
                    if (ordType == OrdinalType.NUMBER_NB)
                    {
                        for (final byte b : ordinal.getBytes())
                        {
                            if (Character.isDigit(b))
                            {
                                digitCount += 1;
                            }
                        }
                        if (digitCount > 3)
                        {
                            ordType = OrdinalType.NONE;
                            ordSize = 0;
                            // ordinal = "NONE";
                            ordinal = "";
                        }
                    }

                    break;
                }
            }
            else
            {
                pat = Pattern.compile(exp[i] + "+[\\.)]\\s");
                matcher = pat.matcher(subLine);
                if (matcher.find())
                {

                    ordType = OrdinalType.values()[i];
                    ordSize = matcher.end() - 1;
                    ordinal = subLine.substring(0, matcher.end() - 1);
                    if (ordType == OrdinalType.NUMBER)
                    {
                        for (final byte b : ordinal.getBytes())
                        {
                            if (Character.isDigit(b))
                            {
                                digitCount += 1;
                            }
                        }
                        if (digitCount > 3)
                        {
                            ordType = OrdinalType.NONE;
                            ordSize = 0;
                            // ordinal = "NONE";
                            ordinal = "";
                        }
                    }
                    break;
                }
            }
        }

        if (ln.getWordCount() <= 2)
        {
            words = subLine.replaceAll(" +", " ").split(" ");
            if (ordType != OrdinalType.NONE)
            {

                if ((words.length > 1)
                        && words[1].toLowerCase().startsWith("adjour"))
                {
                    documentIsFinished = true;
                }
            }
            else
            {
                if ((words.length > 0)
                        && words[0].toLowerCase().startsWith("adjour"))
                {
                    documentIsFinished = true;
                }

            }
        }

        String word = null;
        if ((ordType == OrdinalType.NONE) && !nextLineIsName)
        {
            // System.out.println("in getOrdinal lineCounter="+lineCounter+"text="+subLine);

            final int pos = subLine.indexOf(" ");
            if (pos == -1)
            {
                word = subLine;
            }//
            else
            {
                word = subLine.substring(0, pos);
            }
            if (subLine.charAt(0) == 61607)
            {
                ordType = OrdinalType.BULLET;
                // ordinal = "NONE";
                ordinal = "";
            }
            else if (ln.isBold() && subLine.contains("CALL TO ORDER"))
            {
                ordType = OrdinalType.CALL_ORDER;
                ordinal = "CO";
            }
            else if (ln.isBold() && isTime(word))
            {
                ordType = OrdinalType.BOLD_TIME;
                // ordinal = "BT";
                // ordinal = "NONE";
                ordinal = "";
            }
            else if (handlingTOC
                    || (!(ln.getWordCount() > 7) && !ln.isHasPeriod() && ln
                            .isHasPrecedingBlankLine()))
            {
                if (ln.isBold())
                {
                    // System.out.println("IndentCount = "+textStart.x);

                    if (ln.isCentered())
                    {
                        ordType = OrdinalType.BOLD_CENTERED;
                        // ordinal = "BC";
                        // ordinal = "NONE";
                        ordinal = "";
                    }
                    else if (wordIsCap(word))
                    {
                        ordType = OrdinalType.BOLD_UPPER;
                        // ordinal = "BU";
                        // ordinal = "NONE";
                        ordinal = "";
                    }
                    else if (wordsAreCapitalized(ln)
                            && ln.isHasPrecedingBlankLine()) // isHasBlankLine
                                                             // not working
                                                             // above
                    {
                        ordType = OrdinalType.BOLD_MIX;
                        // ordinal = "BM";
                        // ordinal = "NONE";
                        ordinal = "";
                    }
                }
            }
        }

        switch (ordType)
        {

            case NONE:
                break;

            case NUMBER:
                break;

            case SMALL_ROMAN:
                break;

            case LARGE_ROMAN:
                if ((previousOrdinal != null) && ordinal.equals("I.")
                        && previousOrdinal.equals("H."))
                {
                    ordType = OrdinalType.UPPER;
                }
                break;

            case UPPER:
            case LOWER:
                if (ordSize > 2)
                {
                    ordType = OrdinalType.NONE;
                    ordinal = null;
                }
                break;

            case NUMBER_NB:
                break;

            case UPPER_NB:
                break;

            case MECH_ID:
                break;

            case MIXED_1a:
                break;

            case MIXED_1a1:
                break;

            case CALL_ORDER:
                break;

            case BOLD_TIME:
                break;

            case BOLD_MIX:
                break;

            case BOLD_UPPER:
                break;

            case BOLD_CENTERED:
                break;

            case BULLET:
                break;

            default:
                ordType = OrdinalType.NONE;
                ordinal = null;
                break;
        }

        if (ordType != OrdinalType.NONE)
        {
            if ((ordType == OrdinalType.NUMBER_NB)
                    || (ordType == OrdinalType.UPPER_NB)
                    || (ordType == OrdinalType.MECH_ID))
            {
                if (ordinal.length() == subLine.length())
                {
                    nextLineIsName = true;
                }
                else
                {
                    name = subLine.substring(matcher.end());
                }
            }
            else if ((ordType != OrdinalType.BOLD_UPPER)
                    && (ordType != OrdinalType.BOLD_TIME)
                    && (ordType != OrdinalType.BOLD_MIX)
                    && (ordType != OrdinalType.BOLD_CENTERED)
                    && (ordType != OrdinalType.CALL_ORDER)
                    && (ordType != OrdinalType.BULLET))

            {
                name = subLine.substring(matcher.end());
            }
            else
            {
                name = subLine;
            }
        }

        return ordType;

    }

    public List<VoterHeadTextLine> getTextLines()
    {
        return textLines;
    }

    private boolean isTime(String word)
    {
        boolean isTime = false;

        final Pattern pat = Pattern.compile("[0-9]:[0-9]");
        final Matcher matcher = pat.matcher(word);
        if (matcher.find())
        {
            isTime = true;
        }

        return isTime;
    }

    // public String countIndentation(String line)
    // {
    //
    // Pattern pat1 = Pattern.compile("^\\s+");
    // Matcher matcher = pat1.matcher(line);
    //
    // int whiteSpaceEnd = 0;
    // indentCount = 0;
    //
    // if(matcher.find())
    // {
    // int end = matcher.end();
    // int start = matcher.start();
    // indentCount = end - start;
    // whiteSpaceEnd = matcher.end();
    // }
    //
    // String substring = line.substring(whiteSpaceEnd);
    //
    // // System.out.println("indentCount="+indentCount);
    //
    // return substring;
    // }

    private boolean nextInSequence(OrdinalType ordType, String ord1, String ord2)
    {
        boolean inOrder = false;
        int num1 = 0;
        int num2 = 0;

        if (ordType.equals(OrdinalType.UPPER)
                || ordType.equals(OrdinalType.LOWER)
                || ordType.equals(OrdinalType.MIXED_1a)
                || ordType.equals(OrdinalType.MIXED_1a1))
        {
            final int pos1 = ord1.lastIndexOf('.');
            final char c1 = ord1.charAt(pos1 - 1);
            final char c2 = ord2.charAt(pos1 - 1);
            if (c2 == (c1 + 1))
            {
                inOrder = true;
            }
        }
        else if (ordType.equals(OrdinalType.NUMBER))
        {

            final int pos1 = ord1.lastIndexOf('.');
            final int pos2 = ord2.lastIndexOf('.');
            if (pos1 > 1)
            {
                if (Character.isDigit(ord1.charAt(pos1 - 2)))
                {
                    num1 = Integer.parseInt(ord1.substring(pos1 - 2, pos1 - 1));
                }
            }
            if (pos2 > 1)
            {
                if (Character.isDigit(ord2.charAt(pos2 - 2)))
                {
                    num2 = Integer.parseInt(ord2.substring(pos2 - 2, pos2 - 1));
                }
            }
            if (num2 == (num1 + 1))
            {
                inOrder = true;
            }
        }

        return inOrder;
    }

    private void orderByPage()
    {
        float previousY = 0;
        int pageNum = 0;

        int prevPidx = 0;
        int pidx = 0;
        for (final VoterHeadTextLine line : textLines)
        {
            final float currentY = line.getY();
            if ((currentY < previousY) && (currentY < 100))
            {
                pageNum += 1;
                final String tx = textLines.get(prevPidx).getText();
                if ((tx != null) && tx.contains("Page"))
                {
                    textLines.get(prevPidx).setFooter(true);
                }
                if (line.getText() != null)
                {
                    final Matcher matcher = pat.matcher(line.getText());
                    if (matcher.find())
                    {
                        line.setHeader(true);
                    }
                }
            }
            line.setPageNumber(pageNum);
            previousY = currentY;
            prevPidx = pidx;
            pidx += 1;
        }
    }

    private int ordTypeOnStack(OrdinalType ordType)
    {
        int popCount = 0;
        boolean ordTypeFound = false;

        for (int i = parentOrdinalTypes.size() - 1; i > -1; i--)
        {
            popCount += 1;
            final OrdinalType ot = parentOrdinalTypes.elementAt(i);
            if (ot.equals(ordType))
            {
                ordTypeFound = true;
                break;
            }
        }

        if (!ordTypeFound)
        {
            popCount = 0;
        }

        return popCount;
    }

    private void pdfBoxParse(String filename, String orgId)
    {
        this.orgId = orgId;

        PDDocument pdoc = null;
        MyPDFTextStripper pdfs = null;
        this.pageFilename = filename;
        adjournFound = false;

        File file = new File(filename);

        try
        {

            final BufferedWriter PDFOut = new BufferedWriter(new FileWriter(
                    "PDFObjects.txt"));

            pdfs = new MyPDFTextStripper();
            pdoc = new PDDocument();
            pdoc = PDDocument.load(file);
            final List<PDPage> pages = pdoc.getDocumentCatalog().getAllPages();
            for (final PDPage page : pages)
            {
                final PDStream contents = page.getContents();
                final PDFStreamParser parser = new PDFStreamParser(
                        contents.getStream());
                parser.parse();
                final List tokens = parser.getTokens();

                for (int j = 0; j < tokens.size(); j++)
                {
                    final Object next = tokens.get(j);
                    if (next instanceof PDFOperator)
                    {
                        final PDFOperator op = (PDFOperator) next;
                        PDFOut.write(op + " ^^^^^^^^^^^\n");
                    }
                    else if (next instanceof COSString)
                    {
                        final String s = ((COSString) next).getString();
                        PDFOut.write("@@@@@@@@@@@ " + s + " @@@@@@@@@@@@\n");
                    }
                    else if (next instanceof COSInteger)
                    {
                        final String i = ((COSInteger) next).toString();
                        PDFOut.write(i + " ");
                    }
                    else if (next instanceof COSFloat)
                    {
                        final String f = ((COSFloat) next).toString();
                        PDFOut.write(f + " ");
                    }
                    else if (next instanceof COSArray)
                    {
                        final StringBuffer sbuf = new StringBuffer(100);

                        final String a = ((COSArray) next).toString();
                        final Iterator<COSBase> iter = ((COSArray) next)
                                .iterator();
                        while (iter.hasNext())
                        {
                            try
                            {
                                sbuf.append(((COSString) iter.next())
                                        .getString());
                            }
                            catch (final java.lang.ClassCastException cce)
                            {
                            }
                        }
                        PDFOut.write("@@@@@@@@@@ " + a + " >>"
                                + sbuf.toString() + "\n");
                    }
                    else if (next instanceof COSDictionary)
                    {
                        PDFOut.write("&&&&&&&&& ");
                        final Set<Map.Entry<COSName, COSBase>> entries = ((COSDictionary) next)
                                .entrySet();
                        for (final Entry<COSName, COSBase> entry : entries)
                        {
                            PDFOut.write(" " + entry.getKey().getName() + ":"
                                    + entry.getValue().toString());
                        }
                        PDFOut.write(" &&&&&&&&&&&\n");
                    }
                    else
                    {
                        PDFOut.write("$$$$$$$$$$ " + next.getClass().getName()
                                + " $$$$$$$$$$\n");
                    }
                }
            }
            final PDPage page = (PDPage) pdoc.getDocumentCatalog()
                    .getAllPages().get(0);
            page.getCropBox();
            pageWidth = 612;
            pageContent = pdfs.getText(pdoc);

        }
        catch (final IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            try
            {
                pdoc.close();
                logger.info("Closed PDF File");
            }
            catch (final IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                logger.warn("Failed to Close PDF File");
            } // added to solve warning about not closing pdf document potential
              // memory leak

        }

    }

    public void SendJSON(String url, QueryResult queryResult,
            String statusString)
    {
    	String sendJSONUrlString = CLArguments.getArgValue(CLArguments.RETURN_URL);
        final StringBuffer jsonString = new StringBuffer(300);
        System.out.println("SendJSON");
        final Gson gson = new Gson();

        final JsonEvent jsonEvent = new JsonEvent();
        
        //Variables used by JSON result transmit code
        OutputStream os = null;
        BufferedReader input = null;
        byte[] bytes = null;
        String type = null;
        URL u = null;
        HttpURLConnection conn   ;
        StringBuffer response = null;
        String str = null;
        
        if (queryResult == null)
        {
            jsonEvent.setDescription("<No Description>");
            jsonEvent.setTitle("<No Title>");
        }
        else
        {
            jsonEvent.setDescription(queryResult.getQueryResult());
            jsonEvent.setTitle(statusString + " hit count "
                    + queryResult.getHitCount());
        }
        if (props.getProperty("production").equalsIgnoreCase("Testing"))
        {
            jsonEvent.setTitle(jsonEvent.getTitle() + "::TEST");

        }
        else if (props.getProperty("production").equalsIgnoreCase("Staging"))
        {
            jsonEvent.setTitle(jsonEvent.getTitle() + "::STAGE");
        }
        jsonEvent.setDoc_link(url);
        jsonEvent.setStart_time(startTime);
        jsonEvent.setOrganization_ids(orgId);

        // String json = gson.toJson(event);
        jsonString.append("{\"event\": ");
        jsonString.append(gson.toJson(jsonEvent));
        final int topicCount = event.getTopics().size();
        // int topicCount = 2;
        jsonString.append(",\"topic_count\":\"" + topicCount
                + "\",\"topics\": [");

        // jsonString.append(twoTopics);
        // Topic topic = event.getTopics().get(0);
        int i = 0;
        for (final Topic t : event.getTopics())
        {
            i = i + 1;
            final JsonTopic jsonTopic = new JsonTopic();

            jsonTopic.setOrder(Integer.toString(t.getOrder()));
            jsonTopic.setName(t.getName());
            if (t.getDescription() == null)
            {
                topic.setDescription("");
            }
            jsonTopic.setDescription(t.getDescription());
            // jsonTopic.setDescription("test");
            jsonTopic.setTopic_type("Non-Voteable");
            jsonTopic.setEntity_ordinal(t.getEntityOrdinal());
            if (t.parentOrderNumber == 0)
            {
                jsonTopic.setParent(null);
            }
            else
            {
                jsonTopic.setParent(Integer.toString(t.parentOrderNumber));
            }
            jsonString.append(gson.toJson(jsonTopic));
            if (i < topicCount)
            // if(i < 2)
            {
                jsonString.append(",");
            }
            //
            // if(i == 2)
            // {
            // break;
            // }
        }

        // jsonString.append(secondTopic);
        jsonString.append("]}");

        try
        {
        	logger.info("Sending JSON Results to "+sendJSONUrlString);
        	
	        bytes = jsonString.toString().getBytes();
	        type = "application/x-www-form-urlencoded";
	        u = new URL(sendJSONUrlString);
	        conn = (HttpURLConnection) u.openConnection();
	        conn.setDoOutput(true);
	        conn.setRequestMethod("POST");
	        conn.setRequestProperty( "Content-Type", type );
	        conn.setRequestProperty( "Content-Length", String.valueOf(bytes.length));
	        os = conn.getOutputStream();
	        os.write(bytes);
	        os.flush();

	        response = new StringBuffer(1000);
	        input = new BufferedReader(new InputStreamReader(conn.getInputStream ()));
	        while (null != ((str = input.readLine())))
	        {
		        response.append (str + "\n");
	        }
	        
	        logger.info("Response Received from JSON Post ="+response);

        }
        catch(Exception e)
        {
        	logger.error(e, e);
        }
        finally
        {
	        try {
				os.close();
		        input.close ();
			} catch (IOException e) {
				logger.error(e, e);
			}
        }

//        String content = null;
//
//        HttpResponse response = null;
//
//        final HttpClient httpClient = new DefaultHttpClient();
//        final HttpPost httpPost = new HttpPost(sendJSONUrlString);
//        // Request parameters and other properties.
//        // List<NameValuePair> params = new ArrayList<NameValuePair>();
//        // params.add(new BasicNameValuePair("Content-Type",
//        // "application/json"));
//        // params.add(new BasicNameValuePair("token",
//        // "583d42b69ae461f6e65a3577059ba06a"));
//        // params.add(new BasicNameValuePair("Authorization", "Token"));
//        httpPost.addHeader("Content-Type", "application/json");
//        httpPost.addHeader("token", "583d42b69ae461f6e65a3577059ba06a");
//        httpPost.addHeader("Authorization", "Token");
//        try
//        {
//            // httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
//
//            // HttpEntity postEntity = new
//            // ByteArrayEntity(p2String.getBytes("UTF-8"));
//            final HttpEntity postEntity = new ByteArrayEntity(jsonString
//                    .toString().getBytes("UTF-8"));
//            httpPost.setEntity(postEntity);
//
//        }
//        catch (final UnsupportedEncodingException e)
//        {
//            // writing error to Log
//            e.printStackTrace();
//        }
//        // Send the JSON string to the client
//        try
//        {
//            response = httpClient.execute(httpPost);
//            final HttpEntity respEntity = response.getEntity();
//
//            if (respEntity != null)
//            {
//                // EntityUtils to get the response content
//                content = EntityUtils.toString(respEntity);
//            }
//        }
//        catch (final ClientProtocolException e)
//        {
//            // writing exception to log
//            e.printStackTrace();
//        }
//        catch (final IOException e)
//        {
//            // writing exception to log
//            e.printStackTrace();
//        }
//        finally
//        {
//            try
//            {
//                EntityUtils.consume(response.getEntity());
//            }
//            catch (final IOException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        System.out.println(jsonString);
//        System.out.println("JSON response: " + content);

    }

    private int skipBlankLines(int startIndex)
    {
        int lineIndex = startIndex;

        while (textLines.get(lineIndex).getText() == null)
        {
            lineIndex += 1;
        }

        return lineIndex;
    }

    private void sortLines()
    {
        Collections.sort(textLines);
    }

    private boolean wordIsCap(String word)
    {
        boolean wordIsCap = true;
        for (int i = 0; i < word.length(); i++)
        {
            if (!Character.isUpperCase(word.charAt(i)))
            {
                wordIsCap = false;
            }
        }

        return wordIsCap;
    }

    private boolean wordsAreCapitalized(VoterHeadTextLine ln)
    {
        boolean capitalized = true;

        final String[] words = ln.getText().split("\\s+");

        for (final String word : words)
        {
            if ((word.length() > 3) && !Character.isDigit(word.charAt(0)))
            {
                if (!Character.isUpperCase(word.charAt(0)))
                {//
                    capitalized = false;
                    break;
                }
            }
        }

        return capitalized;
    }

}