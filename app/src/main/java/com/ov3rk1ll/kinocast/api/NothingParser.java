package com.ov3rk1ll.kinocast.api;

public abstract class NothingParser extends Parser {

    public static final int PARSER_ID = 99999;
    @Override
    public int getParserId() {
        return PARSER_ID;
    }

}
