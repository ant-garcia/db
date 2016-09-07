import java.io.IOException;

public class Formats
{
    protected static String padVal(String s)
    {
        for(int i = s.length(); i < DB.lengthOfValue;i++)
            s += " ";
        return s;
    }

    protected static String padKey(String s)
    {
        for(int i = s.length();i < DB.lengthOfKey;i++)
            s += " ";
        return s;
    }

    public String formatTopBar(int valPad, int keyPad)
    {
        String top = "|VALUES";

        for(int i = 0; i <= (valPad - 6); i++)
            top += " ";
        top += "|KEYS";
        for(int i = 0; i <= (keyPad - 4); i++)
            top += " ";
        return top + "|";
    }

    public String formatDividerBar(int valPad, int keyPad)
    {
        String bot = "+";

        for(int i = 0; i <= valPad; i++)
            bot += "-";
        bot += "+";
        for(int i = 0; i <= keyPad; i++)
            bot += "-";
        return bot + "+";
    }

    public String formatKeyBar(String key, int keyPad)
    {
        String bar = "|" + key;
        int barLength = bar.length();
        for(int i = barLength; i <= keyPad; i++)
            bar += " ";
        return bar + "|";
    }

    public String formatKeyDividerBar(int keyPad)
    {
        String bar = "+";
        for(int i = 0; i < keyPad; i++)
            bar += "-";
        return bar + "+";
    }

    public String formatRecordBar(int pad)
    {
        String s = "+";
        for(int i = 2; i < pad; i++)
            s += "-";
        return s + "+";       
    }

    public String formatRecordStatusBar(Record r)
    {
        String s = "|" + r.name;
        for(int i = 0; i <= (r.namePad - r.name.length()); i++)
            s += " ";
        s += "|TOTAL KEYS: " + r.getTotalKeys() +
                " |TOTAL VALUES: " + r.getTotalValues() + " |";
        return s;
    }

    public void packFullTraverse(BTree t)throws IOException
    {
        System.out.println(formatDividerBar(DB.lengthOfValue, DB.lengthOfKey));
        System.out.println(formatTopBar(DB.lengthOfValue, DB.lengthOfKey));
        System.out.println(formatDividerBar(DB.lengthOfValue, DB.lengthOfKey));
        t.traverse();
        System.out.println(formatDividerBar(DB.lengthOfValue, DB.lengthOfKey));
    }

    public void packKeyTraverse(String k, BTree t)throws IOException
    {
        System.out.println(formatKeyDividerBar(DB.lengthOfValue));
        System.out.println(formatKeyBar(k, DB.lengthOfValue));
        System.out.println(formatKeyDividerBar(DB.lengthOfValue));
        t.displayAllDataFromKey(k);
        System.out.println(formatKeyDividerBar(DB.lengthOfValue));
    }

    public void packShowRecords(Record r)
    {
        formatRecordBar(r.namePad);
        formatRecordStatusBar(r);
        formatRecordBar(r.namePad);
    }
}