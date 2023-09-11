package com.hero.retrywhendo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonUtils {
    public static String javabeanToJson(Object obj) {
        try {
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            String json = gson.toJson(obj);
            return json != null ? json : "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
