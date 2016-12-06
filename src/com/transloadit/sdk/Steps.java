package com.transloadit.sdk;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds all the steps that would be added to a given assembly
 */
public class Steps {
    private Map<String, Step> all;

    public Steps(){
        all = new HashMap<>();
    }

    /**
     * This class represents a single step that may be added to an assembly.
     */
    public class Step {
        public String name;
        public String robot;
        public Map<String, Object> options;

        /**
         *
         * @param name Name of the step.
         * @param robot The name of the robot ot use with the step
         * @param options
         */
        public Step(String name, String robot, Map<String, Object> options){
            this.name = name;
            this.robot = robot;
            this.options = options;
        }

        /**
         *
         * @return Map representation of the Step
         */
        public Map<String, Object> asHash() {
            Map<String, Object> options = new HashMap<>(this.options);
            options.put("robot", this.robot);
            return options;
        }
    }

    /**
     * Adds a new step to the list of steps.
     *
     * @param name Name of the step to add
     * @param robot The name of the robot ot use with the step
     * @param options
     */
    public void addStep(String name, String robot, Map<String, Object> options) {
        all.put(name, new Step(name, robot, options));
    }

    /**
     *
     * @param name The name of the step to return.
     * @return an instance of ({@link Step ) with the specified name.
     */
    public Step getStep(String name) {
        return all.get(name);
    }

    /**
     *
     * @return Steps as a HashMap ready to be used by an assembly.
     */
    public Map<String, Map> asHash(){
        Map<String, Map> hash = new HashMap<>();
        all.forEach((name, step)-> hash.put(name, step.asHash()));
        return  hash;
    }
}
