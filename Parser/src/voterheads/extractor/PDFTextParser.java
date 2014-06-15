package voterheads.extractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;

public class PDFTextParser
{

    public static void main(String args[])
    {
        System.out.println(pdftoText("CharlestonCountyAgenda.pdf"));
    }

    // Extract text from PDF Document
    static String pdftoText(String fileName)
    {
        PDFParser parser;
        String parsedText = null;
        ;
        MyPDFTextStripper pdfStripper = null;
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;
        final File file = new File(fileName);
        if (!file.isFile())
        {
            System.err.println("File " + fileName + " does not exist.");
            return null;
        }
        try
        {
            parser = new PDFParser(new FileInputStream(file));
        }
        catch (final IOException e)
        {
            System.err.println("Unable to open PDF Parser. " + e.getMessage());
            return null;
        }
        try
        {
            parser.parse();
            cosDoc = parser.getDocument();
            pdfStripper = new MyPDFTextStripper();
            pdDoc = new PDDocument(cosDoc);
            pdfStripper.setStartPage(1);
            pdfStripper.setEndPage(5);
            parsedText = pdfStripper.getText(pdDoc);
        }
        catch (final Exception e)
        {
            System.err
                    .println("An exception occured in parsing the PDF Document."
                            + e.getMessage());
        }
        finally
        {
            try
            {
                if (cosDoc != null)
                {
                    cosDoc.close();
                }
                if (pdDoc != null)
                {
                    pdDoc.close();
                }
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        }
        return parsedText;
    }

}