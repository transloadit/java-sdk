package com.transloadit.sdk;

import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.HashMap;

public class Main {

    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("KEY", "SECRET");
        Assembly ass = transloadit.assembly();

        try {
            System.out.println(ass.list(new HashMap()).getBody());
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }
}
