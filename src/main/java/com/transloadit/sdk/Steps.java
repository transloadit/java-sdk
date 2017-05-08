package com.transloadit.sdk;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds all the steps that would be added to a given assembly.
 */
public class Steps {
    Map<String, Step> all;

    public Steps(){
        all = new HashMap<String, Step>();
    }

    /**
     * This class represents a single step that may be added to an assembly.
     */
    public class Step {
        String name;
        String robot;
        Map<String, Object> options;

        /**
         *
         * @param name Name of the step.
         * @param robot The name of the robot ot use with the step.
         * @param options extra options required for the step.
         */
        public Step(String name, String robot, Map<String, Object> options){
            this.name = name;
            this.robot = robot;
            this.options = options;
        }

        /**
         *
         * @return Map representation of the Step.
         */
        public Map<String, Object> toMap() {
            Map<String, Object> options = new HashMap<String, Object>(this.options);
            options.put("robot", this.robot);
            return options;
        }
    }

    /**
     * Adds a new step to the list of steps.
     *
     * @param name Name of the step to add.
     * @param robot The name of the robot ot use with the step.
     * @param options extra options required for the step.
     */
    public void addStep(String name, String robot, Map<String, Object> options) {
        all.put(name, new Step(name, robot, options));
    }

    /**
     * Removes step with the given name from the list of steps.
     *
     * @param name Name of the step to remove.
     */
    public void removeStep(String name) {
        all.remove(name);
    }

    /**
     *
     * @param name The name of the step to return.
     * @return an instance of ({@link} Step) with the specified name.
     */
    public Step getStep(String name) {
        return all.get(name);
    }

    /**
     *
     * @return Steps as a HashMap ready to be used by an assembly.
     */
    public Map<String, Map> toMap(){
        Map<String, Map> hash = new HashMap<String, Map>();
        for (Map.Entry<String, Step> entry :
                all.entrySet()) {
            hash.put(entry.getKey(), entry.getValue().toMap());
        }
        return  hash;
    }
}
