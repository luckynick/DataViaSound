package com.luckynick.shared;

import com.luckynick.custom.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SharedUtils {

    public static final boolean DEBUG_MODE = true;

    public static final String SSID = "Heh_mobile", PASSWORD = "123456798";
    public static final int TCP_COMMUNICATION_PORT = 4445;
    public static final int UDP_COMMUNICATION_PORT = 4444;
    public static final String WIFI_SUBNET = "192.168";
    public static final String CONNECTION_CLOSED = "CONNECTION_CLOSED"; //10

    public static final String JSON_EXTENSION = ".json";
    public static final String PATH_SEPARATOR = File.separator;

    public static final int WAIT_TIME_AFTER_FAIL = 2000;
    public static final int COMMAND_PERSISTENCE_ATTEMPTS = 20; //10

    public static final long MAX_AUDIO_RECORD_SIZE = 2000000;//2mb //10000000 //10mb


    public enum DataStorage {
        ROOT(formPathString("data")),

        NONE(formPathString(DataStorage.ROOT.toString(), "devnull")),
        CONFIG(formPathString(DataStorage.ROOT.toString(), "config")),
        MODELS(formPathString(DataStorage.ROOT.toString(), "models")),

        DEVICES(formPathString(DataStorage.MODELS.toString(), "devices")),

        PROFILES(formPathString(DataStorage.MODELS.toString(), "profiles")),
        SINGLE(formPathString(DataStorage.PROFILES.toString(), "single")),
        SEQUENTIAL(formPathString(DataStorage.PROFILES.toString(), "sequential")),
        SCENARIO(formPathString(DataStorage.PROFILES.toString(), "scenario")),
        DICTIONARY(formPathString(DataStorage.MODELS.toString(), "dictionary")),

        RESULTS(formPathString(DataStorage.MODELS.toString(), "results")),
        SINGULAR_RESULT(formPathString(DataStorage.RESULTS.toString(), "singular")),
        ;

        private String path;

        DataStorage(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return path;
        }

        public String getFullPath(String fileName) {
            return formPathString(this.toString(), fileName);
        }

        public String getDirPath() {
            return toString();
        }
    }

    public static String formPathString(String ... elems) {
        if(elems.length == 0) return PATH_SEPARATOR;
        String result = elems[0] + (!elems[0].endsWith(PATH_SEPARATOR) ? PATH_SEPARATOR : "");
        if(elems.length == 1) return result;
        for(int i = 1; i < elems.length; i++) {
            result += elems[i] + PATH_SEPARATOR;
        }
        return result;
    }

    public static String getDateStringForFileName() {
        return new SimpleDateFormat("ddMMyy_HHmmss").format(new Date());
    }

    public static String getDateStringForFileName(long timeMillis) {
        return new SimpleDateFormat("ddMMyy_HHmmss").format(new Date(timeMillis));
    }




    private final static Set<Class<?>> NUMBER_REFLECTED_PRIMITIVES;
    static {
        Set<Class<?>> s = new HashSet<>();
        s.add(byte.class);
        s.add(short.class);
        s.add(int.class);
        s.add(long.class);
        s.add(float.class);
        s.add(double.class);
        NUMBER_REFLECTED_PRIMITIVES = s;
    }

    public static boolean isReflectedAsNumber(Class<?> type) {
        return Number.class.isAssignableFrom(type) || NUMBER_REFLECTED_PRIMITIVES.contains(type);
    }

    protected static void Log(String tag, String consoleLog) {
        if(SharedUtils.DEBUG_MODE) System.out.println("["+tag + "] " + consoleLog);
    }

    public static <T> T[] toArray(List<T> list) {
        if(list.size() < 1) return (T[]) java.lang.reflect.Array.newInstance(Object.class, 0);
        T[] toR = (T[]) java.lang.reflect.Array.newInstance(list.get(0)
                .getClass(), list.size());
        for (int i = 0; i < list.size(); i++) {
            toR[i] = list.get(i);
        }
        return toR;
    }
}
