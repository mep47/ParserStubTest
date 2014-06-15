package voterheads.extractor;

public class TextLocation
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // TODO Auto-generated method stub

    }

    public float x;

    public TextLocation(float x)
    {
        this.x = x;
    }

    public boolean equals(TextLocation inX)
    {
        boolean same = false;

        final float diff = x - inX.x;
        if (Math.abs(diff) < .1)
        {
            same = true;
        }

        return same;
    }

    public boolean greater(TextLocation inX)
    {
        boolean greater = false;

        final float diff = x - inX.x;
        if ((Math.abs(diff) > .1) && (x > inX.x))
        {
            greater = true;
        }

        return greater;
    }

    public boolean less(TextLocation inX)
    {
        boolean less = false;

        final float diff = x - inX.x;
        if ((Math.abs(diff) > .1) && (x < inX.x))
        {
            less = true;
        }

        return less;
    }

}
