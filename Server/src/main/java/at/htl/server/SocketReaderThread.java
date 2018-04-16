package at.htl.server;

import at.htl.common.MyUtils;
import at.htl.common.enums.StudentState;
import at.htl.common.transfer.Packet;
import at.htl.server.entity.Student;
import at.htl.common.io.FileUtils;
import at.htl.server.feature.ScreenShotController;
import at.htl.server.logic.SoundController;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.io.ObjectInputStream;

import static at.htl.common.transfer.Packet.*;

/**
 * Der SocketReaderThread liest Screenshots von unserem Studenten
 * und reicht sie dann an dem TeacherServer weiter, der sie auch
 * sofort anzeigt.
 *
 * @timeline SocketReaderThread
 * 31.10.2015: MET 010  Änderung
 * 21.04.2016: PHI 015  added the coloring from the student
 * 14.06.2016: PHI 002  fixed bug
 */
class SocketReaderThread extends Thread {

    private final Student student;
    private final ObjectInputStream in;
    private final Server server;

    public SocketReaderThread(Student student,
                              ObjectInputStream in,
                              Server server) {
        super("Reader from " + student.getPupil().getLastName());
        this.student = student;
        this.in = in;
        this.server = server;
    }

    /**
     * reads images from stream and gives them the teacher-server to save them.
     * counts the lines of code.
     */
    public void run() {
        boolean finished = false;
        while (!isInterrupted()) {
            try {

                Packet packet = (Packet) in.readObject();

                //Settings.getInstance().printErrorLine(Level.INFO, "received package from " + student.getName(), true, "OTHER");

                byte[] img = (byte[]) packet.get(Resource.SCREENSHOT);
                ScreenShotController.saveImage(img,student);

                //save and show Lines of Code
                Settings.getInstance().addValue((long[]) packet.get(Resource.LINES), student);

                finished = (boolean) packet.get(Resource.FINISHED);
                if (finished) {
                    student.setStudentState(StudentState.FINISHED);
                    StudentList.getStudentList().updateStudent(student);
                }
                else{
                    student.setStudentState(StudentState.NORMAL);
                    StudentList.getStudentList().updateStudent(student);
                }

            } catch (Exception ex) {
                FileUtils.log(this, Level.INFO, "canceled " + MyUtils.exceptionToString(ex));
                Settings.getInstance().printErrorLine(
                        Level.INFO, student.getPupil().getLastName() + " logged out!", true, "DISCONNECT");
                if (!finished) {
                    SoundController.startWarnSound();
                    student.setStudentState(StudentState.CONNECTION_LOST);
                    StudentList.getStudentList().updateStudent(student);
                } else {
                    student.setStudentState(StudentState.FINISHED);
                    StudentList.getStudentList().updateStudent(student);
                }
                //student.finishSeries();
                server.shutdown();
                return;
            }
        }
    }

    /**
     * closes the stream.
     */
    public void close() {
        try {
            in.close();
        } catch (IOException e) {
            FileUtils.log(this, Level.WARN, "Error by closing of ObjectInputStream!" + MyUtils.exceptionToString(e));
            Settings.getInstance().printError(Level.WARN, e.getStackTrace(), "WARNINGS", e.getMessage());
        }
    }
}