package org.pf4j.demo.welcome;

import org.apache.commons.lang.StringUtils;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.pf4j.RuntimeMode;
import org.pf4j.demo.api.GenericPluginInterface;
import java.lang.reflect.Method;
import java.util.HashMap;



public class VinMapping extends Plugin{

    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     *
     * @param wrapper
     */
    public VinMapping(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("VINPlugin.start()");
        // for testing the development mode
        if (RuntimeMode.DEPLOYMENT.equals(wrapper.getRuntimeMode())) {
            System.out.println(StringUtils.upperCase("VINTSSRPlugin"));
            System.out.println("*******************");
            System.out.println("\n" +
                " __      _______ _   _   _____  _     _    _  _____ _____ _   _  \n" +
                " \\ \\    / /_   _| \\ | | |  __ \\| |   | |  | |/ ____|_   _| \\ | | \n" +
                "  \\ \\  / /  | | |  \\| | | |__) | |   | |  | | |  __  | | |  \\| | \n" +
                "   \\ \\/ /   | | | . ` | |  ___/| |   | |  | | | |_ | | | | . ` | \n" +
                "    \\  /   _| |_| |\\  | | |    | |___| |__| | |__| |_| |_| |\\  | \n" +
                "     \\/   |_____|_| \\_| |_|    |______\\____/ \\_____|_____|_| \\_| \n" +
                "                                                                 \n" +
                "                                                                 \n");
            System.out.println(" VIN Plugin Started");
            System.out.println("*******************");
        }

    }

    @Override
    public void stop() {
        System.out.println("VINPlugin.stop()");
    }

    @Extension
    public static class VinMappingExtension implements GenericPluginInterface {

        @Override //main function
        public HashMap<String, Object> runPlugin(HashMap<String, Object> inp) throws Exception {

            HashMap<String, Object> out = new HashMap<String, Object>();

            String operationName = inp.get("operationName").toString();
            System.out.println("operationName:" + operationName);


            if(!operationName.isEmpty()) {
                //call methods here by reflection
                Class<?> mclass = Class.forName("org.pf4j.demo.welcome.Operations");
                Method method = mclass.getMethod(operationName, HashMap.class);
                Object obj = mclass.newInstance();
                out = (HashMap<String, Object>) method.invoke(obj, inp);
                System.out.println("Finished Plugin: creation");

            } else {
                throw new NoSuchMethodException("No such Operation specified");
            }

            return out;
        }
    }

}
