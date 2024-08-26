package com.currenthealth.json.junit.data;

import com.currenthealth.json.JSONPropertyIgnore;
import com.currenthealth.json.JSONPropertyName;

public interface MyBeanCustomNameInterface {
    @JSONPropertyName("InterfaceField")
    float getSomeFloat();
    @JSONPropertyIgnore
    int getIgnoredInt();
}