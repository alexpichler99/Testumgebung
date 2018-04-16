package at.htl.client.view;

import at.htl.client.business.TestController;
import at.htl.common.MyUtils;
import at.htl.common.io.FileUtils;
import org.apache.logging.log4j.Level;

import java.io.*;
import java.nio.file.Files;
import java.util.Properties;
import java.util.Scanner;

public class StudentConsole {
    public static void main(String[] args) throws IOException {
        TestController controller = new TestController();

        File lastUserSettings = new File(System.getProperty("user.dir") + "/user.properties");

        Properties properties = getPropertyFile(lastUserSettings.getAbsolutePath());

        if (properties != null) {
            System.out.println("Found User settings");
            printProperties(properties);

            System.out.println("Enter [u] to use this settings");
            System.out.println("Enter [e] to enter new settings");
            System.out.print("-> ");
            int input = System.in.read();

            if (input == (int) 'e') {
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


        if(!controller.login()){
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

        Properties prop = new Properties();
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(filename))) {
            prop.load(inputStream);
        } catch (IOException e) {
            FileUtils.log(Level.ERROR, e.getMessage());
            return null;
        }
        return prop;
    }

    private static void editProperties(Properties properties) {
        System.out.print(String.format("%30s: ","Server IP"));
        String ip = readLine();
        if(validIP(ip)){
            properties.setProperty("ip",ip );;

            try {

                System.out.print(String.format("%30s: ","PORT"));
                String port = readLine();
                if(Integer.parseInt(port)<=0){
                    printError("Port is negative");
                }

                properties.setProperty("port", port);

            } catch (NumberFormatException e) {
                printError("Port invalid");
            }

            System.out.print(String.format("%30s: ","EnrolementId (like it170001)"));
            String enrolementId = readLine();
            if(!enrolementId.isEmpty()){
                properties.setProperty("enrolementId", enrolementId);


                System.out.print(String.format("%30s: ","CatalogNr"));
                String catalogNr = readLine();
                if(!catalogNr.isEmpty()){
                    properties.setProperty("catalogNr", catalogNr);

                    System.out.print(String.format("%30s: ","Firstname"));
                    String firstName = readLine();
                    if(!firstName.isEmpty()){
                        properties.setProperty("firstName", firstName);

                        System.out.print(String.format("%30s: ", "Lastname"));
                        String lastName = readLine();
                        if(!lastName.isEmpty()){
                            properties.setProperty("lastName", lastName);

                            System.out.print(String.format("%30s: ","Projectpath"));
                            String projectPath = readLine();
                            if(new File(projectPath).exists()){
                                properties.setProperty("pathOfProject", projectPath);
                            }
                            else {
                                printError("Project Path invalid");
                            }
                        }
                        else {
                            printError("Lastname empty");
                        }
                    } else{
                        printError("Firstname empty");
                    }
                } else{
                    printError("CatalogNr empty");
                }
            } else{
                printError("EnrolementId empty");
            }
        }
        else {
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
        try (OutputStream stream = new FileOutputStream(filename)) {
            properties.store(stream, "Last User settings of 'Testumgebung'");
        } catch (IOException ex) {
            FileUtils.log(Level.ERROR, ex.getMessage());
        }
    }

    private static String readLine() {
        Scanner in = new Scanner(System.in);
        return in.next();
    }

    public static void printError(String msg){
        System.err.println("---------------------------------------------------");
        System.err.println("ERROR: "+msg);
        System.err.println("---------------------------------------------------");
        System.exit(1);
    }

    public static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
