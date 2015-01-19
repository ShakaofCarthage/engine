package com.eaw1805.resources.bundles;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class MessageLoader {

    Map<String, ResourceBundle> localeToBundle = new HashMap<String, ResourceBundle>();

    private static MessageLoader instance;

    private MessageLoader() {
        //load locales
        final Locale greekLocale = new Locale("el", "GR");
        localeToBundle.put(Locale.US.getLanguage(), ResourceBundle.getBundle("empire/resources/bundles/engine", Locale.US, new UTF8Control()));
        localeToBundle.put(greekLocale.getLanguage(), ResourceBundle.getBundle("empire/resources/bundles/engine", greekLocale, new UTF8Control()));
    }

    /**
     * This is a singleton class.
     *
     * @return The single instance of this class.
     */
    public static MessageLoader getInstance() {
        if (instance == null) {
            instance = new MessageLoader();
        }
        return instance;
    }

    /**
     * Get a text by its key and locale.
     *
     * @param key The key to search the value for.
     * @param locale The locale to select the appropriate language.
     *
     * @return The value that corresponds to the key and locale.
     */
    public String getString(final String key, final Locale locale) {
        return localeToBundle.get(locale.getLanguage()).getString(key);
    }

}
