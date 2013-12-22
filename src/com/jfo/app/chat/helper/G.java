package com.jfo.app.chat.helper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class G {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();//new Gson();

    public static <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return gson.fromJson(json, classOfT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static String toJson(Object src) {
        return gson.toJson(src);
    }

}