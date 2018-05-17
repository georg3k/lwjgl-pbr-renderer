package org.orchid;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Configuration
{

    private static Map<String, String> properties = new HashMap<>();
    private static Map<String, Method> callbacks = new HashMap<>();

    /**
     * Property getter
     *
     * @param name name of the property
     * @return property value
     */
    public static String getProperty(String name)
    {
        return properties.get(name);
    }

    /**
     * Property setter - sets or updates existent property and invokes callback if it is defined
     *
     * @param name  name of the property
     * @param value value of the property
     */
    public static void setProperty(String name, String value)
    {
        properties.put(name, value);
        if (callbacks.containsKey(name)) {
            try {
                callbacks.get(name).invoke(null);
            } catch (Exception e) {
                System.err.println("Callback invoke failed");
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads configuration file
     *
     * @param path path to configuration file
     */
    static void loadConfiguration(String path)
    {
        properties.clear();
        callbacks.clear();

        SAXParser parser = null;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(path, new DefaultHandler()
            {
                private String element, name, value, callback;

                @Override
                public void startElement(String namespace, String lName, String gName, Attributes attr)
                {
                    element = gName;

                    if (element.equals("property"))
                        name = value = callback = "";
                }

                @Override
                public void characters(char[] characters, int start, int length)
                {
                    // Ignoring whitespace padding
                    if (new String(characters, start, length).trim().length() == 0)
                        return;

                    switch (element) {
                        case "name":
                            name = new String(characters, start, length);
                            break;
                        case "value":
                            value = new String(characters, start, length);
                            break;
                        case "callback":
                            callback = new String(characters, start, length);
                            break;
                    }
                }

                @Override
                public void endElement(String namespace, String lName, String gName)
                {
                    if (gName.equals("property") || name.isEmpty() || value.isEmpty())
                        return;

                    properties.put(name, value);

                    if (!callback.isEmpty()) {
                        try {
                            Method method = Orchid.class.getMethod(callback);
                            callbacks.put(name, method);
                        } catch (NoSuchMethodException e) {
                            System.err.println("Callback \"" + callback + "\"is not found");
                        }
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Configuration loading failed");
            e.printStackTrace();
        }
    }
}
