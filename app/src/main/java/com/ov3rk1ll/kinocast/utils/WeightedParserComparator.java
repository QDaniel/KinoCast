package com.ov3rk1ll.kinocast.utils;

import android.util.SparseIntArray;
import com.ov3rk1ll.kinocast.api.Parser;
import java.util.Comparator;


public class WeightedParserComparator implements Comparator<Parser> {
    private SparseIntArray weightedList;

    public WeightedParserComparator(SparseIntArray weightedList){
        this.weightedList = weightedList;
    }

    @Override
    public int compare(Parser o1, Parser o2) {
        if(weightedList == null) return compate(o1.getParserId(), o2.getParserId());
        int w1 = weightedList.get(o1.getParserId(), o1.getParserId());
        int w2 = weightedList.get(o2.getParserId(), o2.getParserId());
        if(w1 == w2){ // Same host, sort by mirror
            compate(o1.getParserId(), o2.getParserId());
        }
        return compate(w1, w2);
    }

    private int compate(int x, int y){
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
}
