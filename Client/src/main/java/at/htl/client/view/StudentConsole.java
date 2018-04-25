package at.htl.client.view;

import at.htl.client.business.TestController;
import at.htl.common.MyUtils;
import at.htl.common.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Properties;
import java.util.Scanner;

public class StudentConsole {
    public static void main(String[] args) throws IOException {
        printHeader();

        TestController controller = new TestController();

        File workingDirectory = new File(System.getProperty("user.dir"));
        File lastUserSettings = new File(System.getProperty("user.dir") + "/user.properties");

        if (!workingDirectory.exists()) {
            printError(String.format("\"%s\" doesnt exist", System.getProperty("user.dir")));
            System.exit(-1);
        } else if (!workingDirectory.canWrite() && !workingDirectory.canRead()) {
            printError(String.format("\"%s\"-> Permissions insufficient", System.getProperty("user.dir")));
            System.exit(-1);
        }

        Properties properties = getPropertyFile(lastUserSettings.getAbsolutePath());

        if (properties != null) {
            System.out.println("Found User settings");
            printProperties(properties);

            System.out.println("Enter [u] to use this settings");
            System.out.println("Enter [e] to enter new settings");
            System.out.print("-> ");
            String input = readLine();

            if (input != null && input.equalsIgnoreCase("e")) {
                editProperties(properties);
                setPropertyFile(lastUserSettings.getAbsolutePath(), properties);
            }

        } else {
            properties = new Properties();
            editProperties(properties);
            setPropertyFile(lastUserSettings.getAbsolutePath(), properties);
        }
        controller.setExam(
                properties.getProperty("ip"),
                properties.getProperty("port"),
                properties.getProperty("enrolementId"),
                properties.getProperty("catalogNr"),
                properties.getProperty("firstName"),
                properties.getProperty("lastName"),
                properties.getProperty("pathOfProject"));


        if (!controller.login()) {
            return;
        }

        int input = 0;
        System.out.println("[x] Exit | [p] Properties | [f] Toggle Test finished to " + !controller.isTestFinished());
        while ((input = System.in.read()) != (int) 'x') {
            if (input != 10) { //ENTER key
                if (input == (int) 'p') {
                    printProperties(properties);
                } else if (input == (int) 'f') {
                    controller.toggleFinishedTest();
                }
                System.out.println("[x] Exit | [p] Properties | [f] Toggle Test finished to " + !controller.isTestFinished());
            }
        }
        controller.logout();
    }

    private static Properties getPropertyFile(String filename) {
        if (!new File(filename).exists())
            return null;

        Properties prop = new Properties();
        try (InputStreamReader inputStream = new InputStreamReader(
                new BufferedInputStream(new FileInputStream(filename)),
                Charset.forName("UTF8"))) {
            prop.load(inputStream);
        } catch (IOException e) {
            FileUtils.log(Level.ERROR, e.getMessage());
            return null;
        }
        return prop;
    }

    private static void editProperties(Properties properties) {
        System.out.println("--- PROPERTIES ---");
        System.out.println("Some Information is provided by the teacher");
        System.out.println("Press [Enter] if you want to use the default value!");

        String ip = nicePropertyInput("Server IP",
                "for example: 172.18.25.250",
                null);
        if (validIP(ip)) {
            properties.setProperty("ip", ip);

            try {
                String port = nicePropertyInput("Port", "Application", "50555");
                if (Integer.parseInt(port) <= 0) {
                    printError("Port is negative");
                }

                properties.setProperty("port", port);

            } catch (NumberFormatException e) {
                printError("Port invalid");
            }

            String enrolementId = nicePropertyInput("EnrolementId", "like it170001", null);
            if (!enrolementId.isEmpty()) {
                properties.setProperty("enrolementId", enrolementId);


                String catalogNr = nicePropertyInput("CatalogNr",
                        "like \"12\"",
                        null);
                if (!catalogNr.isEmpty()) {
                    properties.setProperty("catalogNr", catalogNr);

                    String firstName = nicePropertyInput("Firstname",
                            null,
                            null);
                    if (!firstName.isEmpty()) {
                        properties.setProperty("firstName", firstName);

                        String lastName = nicePropertyInput("Lastname",null,null);
                        if (!lastName.isEmpty()) {
                            properties.setProperty("lastName", lastName);

                            String projectPath = nicePropertyInput("Projectpath","Directory for all test resources",System.getProperty("user.dir"));
                            if (new File(projectPath).exists()) {
                                properties.setProperty("pathOfProject", projectPath);
                            } else {
                                printError("Project Path invalid");
                            }
                        } else {
                            printError("Lastname empty");
                        }
                    } else {
                        printError("Firstname empty");
                    }
                } else {
                    printError("CatalogNr empty");
                }
            } else {
                printError("EnrolementId empty");
            }
        } else {
            printError("IP invalid");
        }
    }

    public static void printProperties(Properties properties) {
        System.out.println("-----------------------------------");
        System.out.println(properties
                .toString()
                .replace("{", " ")
                .replace("}", "")
                .replaceAll(",", ",\n"));
        System.out.println("-----------------------------------");
    }

    private static void setPropertyFile(String filename, Properties properties) {
        try (OutputStreamWriter stream = new OutputStreamWriter(new FileOutputStream(filename),
                Charset.forName("UTF8"))) {
            properties.store(stream, "Last User settings of 'Testumgebung'");
        } catch (IOException ex) {
            FileUtils.log(Level.ERROR, ex.getMessage());
        }
    }

    private static String readLine() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            return br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void printError(String msg) {
        System.err.println("---------------------------------------------------");
        System.err.println("ERROR: " + msg);
        System.err.println("---------------------------------------------------");
        System.exit(1);
    }

    public static String nicePropertyInput(String propertyName, String description, String defaultValue) {
        System.out.format(">--------------------\n");
        if (defaultValue == null || defaultValue.isEmpty()) {
            System.out.format("%s: (no default value)\n", propertyName);
        } else {
            System.out.format("%s: (default: %s)\n", propertyName, defaultValue);
        }

        if (description != null && !description.isEmpty())
            System.out.format("%s\n", description);

        System.out.format("> ");

        //READ
        String res = readLine();
        if ((res == null || res.isEmpty()) && defaultValue != null && !defaultValue.isEmpty()) {
            return defaultValue;
        } else if (res == null || res.isEmpty()) {
            printError("No Input");
        }
        return res;
    }

    public static void printHeader() {
        System.out.println("------------------------------------------------------------");
        System.out.println("-----                                                  -----");
        System.out.println("-----            TESTUMGEBUNG CLIENT CONSOLE           -----");
        System.out.println("-----                                                  -----");
        System.out.println("------------------------------------------------------------");
        System.out.println();
    }

    public static boolean validIP(String ip) {
        try {
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            for (String s : parts) {
                int i = Integer.parseInt(s);
                if ((i < 0) || (i > 255)) {
                    return false;
                }
            }
            if (ip.endsWith(".")) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
