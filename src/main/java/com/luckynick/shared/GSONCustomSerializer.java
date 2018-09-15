package com.luckynick.shared;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

public class GSONCustomSerializer<T> {


    protected Class<T> classOfModel;

    public GSONCustomSerializer (Class<T> classOfModel) {
        this.classOfModel = classOfModel;
    }

    ExclusionStrategy ioExclusionStrategy = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            IOFieldHandling a = f.getAnnotation(IOFieldHandling.class);
            return a != null ? !a.serialize() : false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    };
    private Gson gsonIO = new GsonBuilder().setPrettyPrinting().serializeNulls()
            .setExclusionStrategies(ioExclusionStrategy).create();


    public void serialize(Writer writer, T object) {
        gsonIO.toJson(object, writer);
    }

    public String serializeStr(T object) {
        return gsonIO.toJson(object);
    }

    public T deserialize(Reader reader) {
        T result = gsonIO.fromJson(reader, classOfModel);
        return result;
    }

    public T deserialize(String json) {
        T result = gsonIO.fromJson(json, classOfModel);
        return result;
    }

    public Class<T> getClassOfModel() {
        return classOfModel;
    }
}
