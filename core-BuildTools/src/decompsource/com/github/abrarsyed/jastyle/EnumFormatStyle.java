package decompsource.com.github.abrarsyed.jastyle;


public enum EnumFormatStyle
{
    NONE,
    ALLMAN,
    JAVA,
    KR,
    STROUSTRUP,
    WHITESMITH,
    BANNER,
    GNU,
    LINUX;

    @SuppressWarnings("incomplete-switch")
    public void apply(ASFormatter formatter)
    {
        switch (this)
        {
            case ALLMAN:
                formatter.setBracketFormatMode(EnumBracketMode.BREAK);
                formatter.setBlockIndent(false);
                formatter.setBracketIndent(false);
                break;

            case JAVA:
                formatter.setBracketFormatMode(EnumBracketMode.ATTACH);
                formatter.setBlockIndent(false);
                formatter.setBracketIndent(false);
                break;

            case KR:
                formatter.setBracketFormatMode(EnumBracketMode.LINUX);
                formatter.setBlockIndent(false);
                formatter.setBracketIndent(false);
                break;

            case STROUSTRUP:
                formatter.setBracketFormatMode(EnumBracketMode.STROUSTRUP);
                formatter.setBlockIndent(false);
                formatter.setBracketIndent(false);
                break;

            case WHITESMITH:
                formatter.setBracketFormatMode(EnumBracketMode.BREAK);
                formatter.setBlockIndent(false);
                formatter.setBracketIndent(true);
                formatter.setClassIndent(true);
                formatter.setSwitchIndent(true);
                break;

            case BANNER:
                formatter.setBracketFormatMode(EnumBracketMode.ATTACH);
                formatter.setBlockIndent(false);
                formatter.setBracketIndent(true);
                formatter.setClassIndent(true);
                formatter.setSwitchIndent(true);
                break;

            case GNU:
                formatter.setBracketFormatMode(EnumBracketMode.BREAK);
                formatter.setBlockIndent(true);
                formatter.setBracketIndent(false);
                formatter.setSpaceIndentation(2);
                break;

            case LINUX:
                formatter.setBracketFormatMode(EnumBracketMode.LINUX);
                formatter.setBlockIndent(false);
                formatter.setBracketIndent(false);
                formatter.setSpaceIndentation(8);
                break;
        }
    }
}
