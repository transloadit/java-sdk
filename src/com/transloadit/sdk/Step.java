package com.transloadit.sdk;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ifedapo on 17/11/2016.
 */
public class Step {
    public String name;
    public String robot;
    public Map options;

    public Step(String name, String robot, Map options){
        this.name = name;
        this.robot = robot;
        this.options = options;
    }

    public void use(String input) {
        this.options.put("use", input);
    }

    public Map asHash() {
        Map map = new HashMap();
        Map options = new HashMap();
        options.putAll(this.options);
        options.put("robot", this.robot);
        map.put(name, options);
        return map;
    }
}
