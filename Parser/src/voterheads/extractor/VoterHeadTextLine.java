package voterheads.extractor;

import org.apache.commons.lang3.builder.CompareToBuilder;

public class VoterHeadTextLine implements Comparable<VoterHeadTextLine>
{

    private int          lineNumber;
    private TextLocation textStartX;
    private float        textEndX;
    private int          pageNumber;
    private float        y;
    private boolean      isBold;
    private boolean      isCentered;
    private String       firstCharFontType;
    private int          wordCount;
    private boolean      hasPeriod;
    private boolean      hasPrecedingBlankLine;
    private boolean      isHeader;
    private boolean      isFooter;
    private boolean      isTableOfContents;
    private String       text;

    public VoterHeadTextLine(float y)
    {
        this.y = y;
        isBold = false;
        isCentered = false;
        isHeader = false;
        isFooter = false;
    }

    @Override
    public int compareTo(VoterHeadTextLine other)
    {
        return new CompareToBuilder()
                .append(getPageNumber(), other.getPageNumber())
                .append(getY(), other.getY()).toComparison();

    }

    public String getFirstCharFontType()
    {
        return firstCharFontType;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public int getPageNumber()
    {
        return pageNumber;
    }

    public String getText()
    {
        return text;
    }

    public float getTextEndX()
    {
        return textEndX;
    }

    public TextLocation getTextStartX()
    {
        return textStartX;
    }

    public int getWordCount()
    {
        return wordCount;
    }

    public float getY()
    {
        return y;
    }

    public boolean isBold()
    {
        return isBold;
    }

    public boolean isCentered()
    {
        return isCentered;
    }

    public boolean isFooter()
    {
        return isFooter;
    }

    public boolean isHasPeriod()
    {
        return hasPeriod;
    }

    public boolean isHasPrecedingBlankLine()
    {
        return hasPrecedingBlankLine;
    }

    public boolean isHeader()
    {
        return isHeader;
    }

    public boolean isTableOfContents()
    {
        return isTableOfContents;
    }

    public void setBold(boolean isBold)
    {
        this.isBold = isBold;
    }

    public void setCentered(boolean isCentered)
    {
        this.isCentered = isCentered;
    }

    public void setFirstCharFontType(String firstCharFontType)
    {
        this.firstCharFontType = firstCharFontType;
    }

    public void setFooter(boolean isFooter)
    {
        this.isFooter = isFooter;
    }

    public void setHasPeriod(boolean hasPeriod)
    {
        this.hasPeriod = hasPeriod;
    }

    public void setHasPrecedingBlankLine(boolean hasPrecedingBlankLine)
    {
        this.hasPrecedingBlankLine = hasPrecedingBlankLine;
    }

    public void setHeader(boolean isHeader)
    {
        this.isHeader = isHeader;
    }

    public void setLineNumber(int lineNumber)
    {
        this.lineNumber = lineNumber;
    }

    public void setPageNumber(int pageNumber)
    {
        this.pageNumber = pageNumber;
    }

    public void setTableOfContents(boolean isTableOfContents)
    {
        this.isTableOfContents = isTableOfContents;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setTextEndX(float textEndX)
    {
        this.textEndX = textEndX;
    }

    public void setTextStartX(TextLocation textStartX)
    {
        this.textStartX = textStartX;
    }

    public void setWordCount(int wordCount)
    {
        this.wordCount = wordCount;
    }

    public void setY(float y)
    {
        this.y = y;
    }

    @Override
    public String toString()
    {
        final StringBuffer str = new StringBuffer(100);

        str.append("lineNumber=" + lineNumber);
        if (textStartX == null)
        {
            str.append("textStartX=null");
        }
        else
        {
            str.append(" textStartX=" + textStartX.x);
        }
        str.append(" textEndX=" + textEndX);
        str.append(" pageNumber=" + pageNumber);
        str.append(" y=" + y);
        str.append(" isBold=" + isBold);
        str.append(" isCentered=" + isCentered);
        str.append(" isHeader=" + isHeader);
        str.append(" isFooter=" + isFooter);
        str.append(" firstCharFontType=" + firstCharFontType);
        str.append(" text=" + text);

        return str.toString();
    }

}
