package at.htl.common.io;

import at.htl.common.MyUtils;
import org.apache.logging.log4j.Level;

import java.io.*;

/**
 * @timeline FileStream
 * 01.11.2015: MET 001  created class
 * 01.11.2015: MET 040  sending and receiving files
 * 08.02.2016: GNA 020  Errors und Infos in LogFile gespeichert
 */
public class FileStream {

    private static final int BUFFER_SIZE = 16384;

    /**
     * sends a file.
     *
     * @param out  Specifies the stream which is used for sending the file.
     * @param file Specifies the file to send.
     * @return the success of it.
     */
    public static boolean send(ObjectOutputStream out, File file) {
        boolean sent = false;
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            FileUtils.log(FileStream.class, Level.INFO, String.format("sending %s ... ", file.getName()));
            InputStream fis = new FileInputStream(file);
            int len;
            while ((len = fis.read(buffer)) > 0)
                out.write(buffer, 0, len);
            fis.close();
            FileUtils.log(FileStream.class, Level.INFO, "sending completed: " + file.getName());
            sent = true;
        } catch (IOException e) {
            FileUtils.log(FileStream.class, Level.ERROR, "cannot send screenshot to teacher" + MyUtils.exToStr(e));
        }
        return sent;
    }

    /**
     * gets a file and saves it.
     *
     * @param in   Specifies the stream which is used for receiving the file.
     * @param path Specifies the path where the file is saved.
     * @return the success of it.
     */
    public static boolean receive(ObjectInputStream in, String path) {
        boolean received = false;
        File file = new File(path);
        try {
            FileUtils.log(FileStream.class, Level.INFO, String.format("fetching file %s ... ", file.getName()));

            OutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) > 0)
                fos.write(buffer, 0, len);
            fos.close();
            FileUtils.log(FileStream.class, Level.INFO, "fetching completed: " + file.getName());
            received = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return received;
    }

}
