package at.htl.client;

import at.htl.common.MyUtils;
import at.htl.common.actions.RobotAction;
import at.htl.common.actions.RobotActionQueue;
import at.htl.common.fx.FxUtils;
import at.htl.common.io.FileUtils;
import at.htl.common.transfer.DocumentsTransfer;
import at.htl.common.transfer.Packet;
import javafx.application.Platform;
import org.apache.logging.log4j.Level;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.time.LocalTime;

import static at.htl.common.transfer.Packet.Action;
import static at.htl.common.transfer.Packet.Resource;

/**
 * @timeline Client
 * 24.10.2015: PON 010  students can log out - thereby no longer be sent screenshots
 * 26.10.2015: MET 002  renamed this class from "Student" to "Client"
 * 31.10.2015: MET 075  function "download specification" implemented
 * 01.11.2015: MET 015  hand in a watched folder automatically zipped
 * 01.11.2015: MET 005  Bug found: hand in only immediately after login
 * 08.02.2016: GNA 005  Added Errors to LogFilesdfdf
 * 11.06.2016: PHI 020  Implemented the new Object-Stream.
 * 11.06.2016: PHI 030  implemented the endTime.
 */
public class Client {

    private ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Robot robot;
    private RobotActionQueue jobs;
    private Socket socket;
    private final ProcessorThread processor;
    private final ReaderThread reader;
    private LocalTime endTime = null;
    private boolean signedIn = false;
    private boolean isRunning = false;

    public Client(Packet packet) throws IOException, AWTException {
        try {
            socket = new Socket(Exam.getInstance().getServerIP(), Exam.getInstance().getPort());
        } catch (Exception e) {
            FxUtils.showPopUp("ERROR: Connection to server failed!", false);
        }
        if (socket != null) {
            FileUtils.createDirectory(Exam.getInstance().getPupil().getPathOfProject());
            robot = new Robot();
            jobs = new RobotActionQueue();
            in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            setOut(new ObjectOutputStream(socket.getOutputStream()));
            DocumentsTransfer.sendObject(out, packet);
            processor = new ProcessorThread();
            reader = new ReaderThread();
        } else {
            robot = null;
            in = null;
            processor = null;
            reader = null;
        }
    }

    public ObjectOutputStream getOut() {
        return out;
    }

    public void setOut(ObjectOutputStream out) {
        this.out = out;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public boolean isSignedIn() {
        return signedIn;
    }

    /**
     * gets the file from the teacher for the test and saves it
     * extracts the information from the received HandOutPackage.
     * Saves the file from the package.
     */
    public void loadFiles() {
        try {
            Packet packet = (Packet) in.readObject();
            if (packet.getAction() == Action.HAND_OUT) {
                byte[] handout = (byte[]) packet.get(Resource.FILE);
                if (handout.length != 0) {
                    File handoutFile = new File(Exam.getInstance().getPupil().getPathOfProject()
                            + "/angabe." + packet.get(Resource.FILE_EXTENSION));

                    handoutFile.createNewFile();
                    //TODO test if this is working
                    Files.write(handoutFile.toPath(), handout);
//                    try (OutputStream out = new BufferedOutputStream(
//                            Files.newOutputStream(handoutFile.toPath(),CREATE_NEW,WRITE))) {
//                        out.write(handout, 0, handout.length);
//                    }

                }
                endTime = (LocalTime) packet.get(Resource.TIME);
                signedIn = true;
            }
        } catch (IOException | ClassNotFoundException e) {
            signedIn = false;
            FileUtils.log(this, Level.ERROR, "Failed to receive: " + MyUtils.exceptionToString(e));
        }
        processor.start();
        reader.start();
    }

    /**
     * reads jobs from the stream and adds them as a new job
     */
    private class ReaderThread extends Thread {
        @Override
        public void run() {
            try {
                Object obj = in.readObject();

                //TODO this while loop seems "unused"
                while (obj.getClass().toString().contains(Packet.class.toString())) {
                    obj = in.readObject();
                }

                RobotAction action = (RobotAction) obj;
                do {
                    jobs.add(action);
                    //TODO check if duplicate requests should be discarded
//                    if (!action.equals(jobs.peekLast())) {
//                    } else {
//                        FileUtils.log(this, Level.INFO, "Discarding duplicate request");
//                    }
                } while ((action = (RobotAction) in.readObject()) != null);
            } catch (EOFException|SocketException exception) {
                FileUtils.log(this, Level.ERROR, "Connection closed!");
            }
            catch (Exception ex) {
                FileUtils.log(this, Level.ERROR, "Send Boolean " + MyUtils.exceptionToString(ex));
            } finally {
                if (isRunning)
                    Platform.runLater(() -> FxUtils.showPopUp("LOST CONNECTION", false));
            }
        }
    }


    /**
     * takes all jobs from the stream and executes them.
     * Afterwords he sends the result from the job back with the stream.
     */
    private class ProcessorThread extends Thread {
        public ProcessorThread() {
            super("ProcessorThread");
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted() && socket.isConnected()) {
                    RobotAction action = jobs.take();
                    Object document = action.execute(robot);

                    DocumentsTransfer.sendObject(getOut(), document);
                }
            } catch (InterruptedException e) {
                interrupt();
            } catch (IOException e) {
                FileUtils.log(this, Level.ERROR, "Connection closed " + MyUtils.exceptionToString(e));
                closeOut();
            }
        }
    }

    /**
     * closes the stream
     */
    public void closeOut() {
        try {
            getOut().close();
        } catch (IOException e) {
            FileUtils.log(this, Level.ERROR, "Error by closing of ObjectOutStream!" + MyUtils.exceptionToString(e));
        }
    }

    /**
     * start-point for the thread
     */
    public boolean start() {
        if (processor != null && reader != null) {
            loadFiles();
            isRunning = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * called when the client logs out.
     * stop all streams from this client.
     */
    public void stop() {
        if (processor != null && reader != null) {
            isRunning = false;
            processor.interrupt();
            reader.interrupt();
            closeOut();
        }
    }
}