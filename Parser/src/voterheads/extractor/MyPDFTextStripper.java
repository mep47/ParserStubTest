package voterheads.extractor;

import java.io.IOException;

import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;

public class MyPDFTextStripper extends PDFTextStripper
{

    public MyPDFTextStripper() throws IOException
    {

    }

    @Override
    protected void processTextPosition(TextPosition position)
    {
        System.out.println("processTextPosition");
        final String c = position.getCharacter();
        final String postscriptName = position.getFont().getBaseFont();
        System.out.println("Character=" + c + " font=" + postscriptName + " x="
                + position.getX() + " y=" + position.getY());

    }

}
