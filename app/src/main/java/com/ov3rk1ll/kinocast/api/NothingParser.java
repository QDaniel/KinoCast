package com.ov3rk1ll.kinocast.api;

import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class NothingParser extends Parser {

    public static final int PARSER_ID = 99999;
    @Override
    public int getParserId() {
        return PARSER_ID;
    }

}
