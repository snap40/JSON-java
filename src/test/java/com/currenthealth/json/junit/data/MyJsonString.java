package com.currenthealth.json.junit.data;

import com.currenthealth.json.JSONString;
import org.json.*;

/**
 * Used in testing when a JSONString is needed
 */
public class MyJsonString implements JSONString {

    @Override
    public String toJSONString() {
        return "my string";
    }
}