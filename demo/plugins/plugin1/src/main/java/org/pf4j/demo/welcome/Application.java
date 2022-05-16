package org.pf4j.demo.welcome;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableCaching
@EnableAspectJAutoProxy
@CrossOrigin
@EnableAsync

public class Application {

    static String Version = "";

    public static PropertyPlaceholderConfigurer properties() throws Exception {

        final PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        System.err.println("creating External plugin Config");
        CreateExternalPropertyFile();
        System.out.println("plugin Config file is created");
        Properties prop = intializeSettings("applicationPlugin.properties", false, false);


        ppc.setIgnoreResourceNotFound(true);
        final List<Resource> resourceLst = new ArrayList<Resource>();
        String homePath = System.getProperty("user.home");
        ppc.setProperties(prop);
        // resourceLst.add(new ClassPathResource("applicationPlugin.properties"));
        ppc.setLocations(resourceLst.toArray(new Resource[]{}));
        return ppc;
    }


    public static void CreateExternalPropertyFile() {
        try {
            final String[] listOfPropertiesFiles = new String[]{"applicationPlugin.properties"};

            String path = System.getProperty("user.home") + "/.Smart2GoConfig";
            File file = new File(path);
            if (!file.exists()) {
                Path pathToCreate = Paths.get(path);
                Files.createDirectories(pathToCreate);
            }
            for (String f : listOfPropertiesFiles) {
                path = System.getProperty("user.home") + "/.Smart2GoConfig/." + f;
                file = new File(path);
                if (!file.exists()) {
                    file.createNewFile();
                }
            }


        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static Properties intializeSettings(String PropertyFileName, boolean EscapeCopyOut, boolean IgnoreHomeResourceFile) throws Exception {


        Properties props = new Properties();
        Properties propsToWrite = null;
        InputStream input = Application.class.getClassLoader().getResourceAsStream(PropertyFileName);
        if (input == null) {
            System.out.println("Resource File " + PropertyFileName + " can't be Loaded");
        }

        if (input != null) {
            props.load(input);
            propsToWrite = new Properties();
            Properties oldProps = new Properties();
            File f = null;
            if (IgnoreHomeResourceFile == false) {
                String path = System.getProperty("user.home") + "/.Smart2GoConfig/." + PropertyFileName;
                f = new File(path);
                if (f.exists()) {
                    oldProps.load(new FileInputStream(f));
                }
            }

            for (Object property : props.keySet()) {

                String properyName = property.toString().replaceAll("\\[", "").replaceAll("\\]", "");
                Object properyValue;
                if (System.getenv(properyName) != null) {
                    properyValue = System.getenv(properyName);

                } else {
                    if (oldProps.get(property) != null
                        && property.toString().startsWith("pom.") == false
                        && property.toString().startsWith("ImpExp.version") == false
                    ) {
                        properyValue = oldProps.get(property);
                    } else {
                        properyValue = props.get(property);
                        if (property.toString().equals("pom.version")) {
                            Version = (String) props.get(property);
                        }
                    }
                }
                propsToWrite.put(property, properyValue);

                if (properyName.equals("app.instance-id")) {
                    if (propsToWrite.get(properyName).equals("${random.uuid}")) {
                        propsToWrite.put(properyName, "BE-" + java.util.UUID.randomUUID());
                    }
                }

            }

            // write into it
            if (EscapeCopyOut == false) {
                StoreTheNewPropertiyFile(propsToWrite, PropertyFileName, f);
            }

            Object[] keys = propsToWrite.keySet().toArray();
            for (Object k : keys) {
                String v = (String) propsToWrite.get(k);
                System.setProperty((String) k, v);
            }

        }

        return propsToWrite;
    }


    public static void StoreTheNewPropertiyFile(Properties propsToWrite, String PropertyFileName, File f) throws Exception {

        InputStream in = Application.class.getClassLoader().getResourceAsStream(PropertyFileName);
        Scanner inp = new Scanner(in);
        FileWriter wr = new FileWriter(f);
        while (inp.hasNextLine()) {
            String line = inp.nextLine().trim();

            if (line.indexOf("=") > 0 && line.startsWith("#") == false) {
                String[] list = line.split("=");
                //find value from the propert
                String Key = list[0].trim();
                Object newValue = propsToWrite.get(Key);

                String ValueStr = (newValue == null || newValue.equals("null") ? "" : newValue.toString());

                line = Key + "=" + ValueStr;
            }
            wr.write(line);
            wr.write("\r\n");
        }
        wr.close();
        inp.close();
        in.close();
        System.out.println("ReWrite Property File --> " + PropertyFileName);
    }

}
