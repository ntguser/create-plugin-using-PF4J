package org.pf4j.demo.welcome;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Operations {

    /****************************************CREATE FOLDERS PART******************************************/

    public HashMap<String, Object> createFolders(HashMap<String, Object> inp) {

        HashMap<String, Object> out = new HashMap<String, Object>();

        try {
            Properties properties = new Properties();
            Application.properties();
            properties = readPropertiesFile("applicationPlugin.properties", properties);

            List<String> foldersList = new ArrayList<>();

            //generateFolderName
            String mainFolder = generateMainFolderName(inp);

            //Generate folders list
            generateFoldersList(foldersList, mainFolder, properties);

            create(foldersList);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;
    }

    private void generateFoldersList(List<String> foldersList, String mainFolder, Properties properties) {

        addGeneralPackageFolders(foldersList, mainFolder, properties);

        addSitePackageFolders(foldersList, mainFolder, properties);

        addLinkFolders(foldersList, mainFolder, properties);

        addSiteAttachmentsFolders(foldersList, mainFolder, properties);
    }

    private void addGeneralPackageFolders(List<String> foldersList, String mainFolder, Properties properties) {
        // General package before folders
        foldersList.add(mainFolder + properties.get("G-1").toString());
        foldersList.add(mainFolder + properties.get("G-2").toString());
        foldersList.add(mainFolder + properties.get("G-3").toString());
        foldersList.add(mainFolder + properties.get("G-4").toString());
        foldersList.add(mainFolder + properties.get("G-5").toString());
        foldersList.add(mainFolder + properties.get("G-6").toString());
        foldersList.add(mainFolder + properties.get("G-7").toString());
        foldersList.add(mainFolder + properties.get("G-8").toString());
        foldersList.add(mainFolder + properties.get("G-9").toString());
    }

    private void addSitePackageFolders(List<String> foldersList, String mainFolder, Properties properties) {
        //Site package After folders
        foldersList.add(mainFolder + properties.get("S-1").toString());
        foldersList.add(mainFolder + properties.get("S-2").toString());
        foldersList.add(mainFolder + properties.get("S-3").toString());
        foldersList.add(mainFolder + properties.get("S-4").toString());
        foldersList.add(mainFolder + properties.get("S-5").toString());
        foldersList.add(mainFolder + properties.get("S-6").toString());
        foldersList.add(mainFolder + properties.get("S-7").toString());
        foldersList.add(mainFolder + properties.get("S-8").toString());
        foldersList.add(mainFolder + properties.get("S-9").toString());
        foldersList.add(mainFolder + properties.get("S-10").toString());
    }

    private void addLinkFolders(List<String> foldersList, String mainFolder, Properties properties) {
        //Link folders
        foldersList.add(mainFolder + properties.get("L-1").toString());
        foldersList.add(mainFolder + properties.get("L-2").toString());
        foldersList.add(mainFolder + properties.get("L-3").toString());
    }

    private void addSiteAttachmentsFolders(List<String> foldersList, String mainFolder, Properties properties) {
        // Site attachement folders
        foldersList.add(mainFolder + properties.get("A-1").toString());
    }

    private void create(List<String> foldersList) {
        for(String folderPath: foldersList ) {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
    }

    /****************************************UPLOAD ATTACHMENTS PART**************************************/

    public HashMap<String, Object> uploadAttachments(HashMap<String, Object> inp) {

        HashMap<String, Object> out = new HashMap<String, Object>();
        Properties properties = new Properties();

        try {
            Application.properties();
            properties = readPropertiesFile("applicationPlugin.properties", properties);

            //get attachment folder path
            String folderId = inp.get("folderId").toString();
            String mainFolder = generateMainFolderName(inp);
            String path = mainFolder + properties.get(folderId).toString();

            //get attachments
            Object attachment = inp.get("attachmentUda");
            List<Object> attchmentList = Collections.singletonList(attachment);
            List<HashMap<String, Object>> attachments = (List<HashMap<String, Object>>) Collections.singletonList(attchmentList.get(0)).get(0);

            for (HashMap<String, Object> attach : attachments) {
                String key = attach.get("attachmentName").toString();
                String value = attach.get("attachmentValue").toString();
                upload(key, value, path);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;
    }

    private void upload(String key, String value, String path) throws IOException {

        path = path + File.separator + key;

        byte[] decodedfile = Base64.getDecoder().decode(value.getBytes(StandardCharsets.UTF_8));
        System.out.println("file:" + path);

        File file = new File(path);

        if (!file.exists()) {
            file.createNewFile();
        }

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(decodedfile);
        fos.close();
    }

    /****************************************************************************************************/

    private String generateMainFolderName(HashMap<String, Object> inp) {

        String mainPath = inp.get("mainPath").toString();
        String activityName = inp.get("activityName").toString();
        String siteId = inp.get("siteId").toString();

        String mainFolder = mainPath + siteId + "(" + activityName+ ")";

        return mainFolder;
    }

    public Properties readPropertiesFile(String propertyFileName, Properties properties) throws IOException {
        String path = System.getProperty("user.home") + "/.Smart2GoConfig/." + propertyFileName;
        File file = new File(path);
        FileInputStream fileInput = new FileInputStream(file);
        properties.load(fileInput);
        fileInput.close();
        return properties;
    }

}
