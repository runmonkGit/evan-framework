package org.evanframework.datadict.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataDictionaryList extends ArrayList<DataDictionary> {
    private static final long serialVersionUID = 1755675279049794414L;

    public DataDictionaryList() {
        super();
    }

    public DataDictionaryList(int size) {
        super(size);
    }

    public DataDictionaryList(List<DataDictionary> list) {
        Collections.addAll(this, list.toArray(new DataDictionary[]{}));
    }
}
