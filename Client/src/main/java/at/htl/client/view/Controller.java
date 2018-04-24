package at.htl.client.view;

import at.htl.client.Client;
import at.htl.client.Exam;
import at.htl.common.Countdown;
import at.htl.common.MyUtils;
import at.htl.common.Pupil;
import at.htl.common.actions.IpConnection;
import at.htl.common.actions.LineCounter;
import at.htl.common.fx.FxUtils;
import at.htl.common.io.FileUtils;
import at.htl.common.transfer.Address;
import at.htl.common.transfer.Packet;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.net.URL;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import static at.htl.common.transfer.Packet.Action;
import static at.htl.common.transfer.Packet.Resource;

/**
 * @timeline StudentController
 * 15.10.2015: PON 001  created class
 * 18.10.2015: PHI 030  created login with text boxes
 * 14.10.2015: MET 030  new GUI design and created function setControls(value)
 * 24.10.2015: PON 005  added logout method
 * 30.10.2015: MET 010  expanded to login and logout
 * 31.10.2015: MET 020  creating Client packages and submitting them to client
 * 06.11.2015: PON 010  expansion to the password field
 * 19.11.2015: PON 015  GUI extended by the TextField "Port" and implements corresponding functions
 * 29.11.2015: PHI 040  display of messages in the GUI with setMsg()
 * 03.01.2016: MET 005  setMsg() improved by using FxUtils
 * 12.02.2016: MET 005  activation and deactivation of controls depending on login and logout
 * 12.02.2016: MET 030  display of messages in the GUI with setMsg() enormously improved
 * 25.02.2016: MET 005  default settings for testing
 * 20.03.2016: PHI 001  fixed bug (check ip-address on correctness)
 * 21.04.2016: PHI 060  added the finished-Mode
 * 12.05.2016: MET 010  fixed FileUtils-Error
 * 20.05.2016: PHI 035  improved the connection-testing (+ testing connection on serverStart)
 * 09.06.2016: MET 100  Show quick info (time, status, transparent backgroundColor, positioning, ...)
 * 11.06.2016: MET 040  moving the QuickInfo-Window
 * 11.06.2016: MET 020  show and hide the QuickInfo-Window
 * 11.06.2016: MET 055  display of a monitoring symbol when connected
 * 12.06.2016: MET 005  fixed login failures, show Slider after login
 * 12.06.2016: MET 010  order of the methods and reduction on a countdown
 * 12.06.2016: MET 005  connection messages
 * 12.06.2016: MET 010  implementation of the correct remaining time
 * 14.06.2016: PHI 002  added new validation: pattern A-Z
 * 12.06.2016: MET 010  provided controls in the Student-GUI with tooltips
 * 12.06.2016: MET 030  auto size of the QuickInfo-Window (Bug: the size does not change correctly)
 * 16.06.2016: MET 040  login status "Signed in!" when student really logged in  (
 * 16.06.2016: PON 040  login status "Signed in!" when student really logged in
 * 13.02.2017: MET 030  Fehlermeldung, wenn der Server noch nicht gestartet ist
 * 13.02.2017: MET 020  farbliche Kennzeichnung von neutralen Mitteilungen (gelb)
 * 13.02.2017: MET 010  Fehlermeldung, wenn keine IP-Addresse angegeben wird
 */
public class Controller implements Initializable {


    //region Controls
    @FXML
    private TextField tfServerIP;
    @FXML
    private TextField tfPort;
    @FXML
    private TextField tfEnrolmentID;
    @FXML
    private TextField tfCatalogNumber;
    @FXML
    private TextField tfFirstName;
    @FXML
    private TextField tfLastName;
    @FXML
    private TextField tfPathOfProject;
    @FXML
    private CheckBox cbNoLogin;
    @FXML
    private Button btnTestMode;
    @FXML
    private Button btnChooseDirectory;
    @FXML
    private Button btnLogin;
    @FXML
    private CheckBox cbFinished;
    @FXML
    private Button btnLogout;
    @FXML
    private Label lbAlert;
    @FXML
    private Label lbTimeLeft;
    @FXML
    private Text txTimeLeft;
    @FXML
    private Slider sliderPos;
    //endregion

    private Client client;
    private Countdown countdown;
    private Stage quickInfo;

    public void initialize(URL location, ResourceBundle resources) {
        if (MyUtils.readProperty("settings.properties", "testmode")
                .equalsIgnoreCase("false")) {
            cbNoLogin.setVisible(false);
            btnTestMode.setVisible(false);
        }
        setControls(true);

        //only allow number to be entered
        tfPort.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d*")) {
                    tfPort.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });
    }

    /**
     * Called from main to set the Stage.
     * Sets event when the Window is closed to logout correctly
     *
     * @param stage
     */
    public void setStage(Stage stage) {
        stage.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Do you really want to exit?",
                    ButtonType.YES, ButtonType.NO);
            alert.setTitle("Exit?");
            if (alert.showAndWait().get() == ButtonType.YES)
                logout();
            else
                event.consume();
        });
    }

    /**
     * sets the default values into the GUI.
     */
    @FXML
    public void setDefaultSettings() {
        tfServerIP.setText("localhost");
        tfPort.setText("50555");
        tfEnrolmentID.setText("in120001");
        tfCatalogNumber.setText("01");
        tfFirstName.setText("Max");
        tfLastName.setText("Mustermann");
        tfPathOfProject.setText(System.getProperty("user.home"));
    }

    /**
     * checks the ip-connection and shows a pupUp-Window.
     */
    @FXML
    public void testConnection() {
        int port = 0;
        try {
            port = Integer.valueOf(tfPort.getText());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        IpConnection.isIpReachable(tfServerIP.getText(), port, true, true);
    }

    /**
     * Shows a dialog-screen to choose the working-directory where
     * the project will be and saves the path of it.
     */
    @FXML
    public void chooseProjectDirectory() {
        File file = FxUtils.chooseDirectory("Select Project Directory", null);
        //avoid NullPointerException when no file is selected
        if (file != null) {
            tfPathOfProject.setText(file.getPath());
        }
    }

    /**
     * Connects the client with the server.
     */
    @FXML
    public void login() {
        setMsg("Establish connection with server ...", 1);
        int port = 0;
        try {
            port = Integer.valueOf(tfPort.getText());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (IpConnection.isIpReachable(tfServerIP.getText(), port, true, false)) {
            if (setExam() && isLoggedOut()) {
                setMsg("Trying to login ...", 1);
                try {
                    LocalTime toTime;
                    boolean loggedIn;
                    if (cbNoLogin.isSelected()) {
                        toTime = LocalTime.now().plusMinutes(0).plusSeconds(30);
                        loggedIn = true;
                    } else {
                        Packet packet = new Packet(Action.LOGIN, "LoginPackage");
                        packet.put(Resource.PUPIL, new Pupil(
                                Exam.getInstance().getPupil().getCatalogNumber(),
                                Exam.getInstance().getPupil().getEnrolmentID(),
                                Exam.getInstance().getPupil().getFirstName(),
                                Exam.getInstance().getPupil().getLastName(),
                                Exam.getInstance().getPupil().getPathOfProject()
                        ));
                        packet.put(Resource.ADDRESS, new Address(
                                Exam.getInstance().getServerIP(),
                                Exam.getInstance().getPort()
                        ));
                        client = new Client(packet);
                        loggedIn = client.start();
                        toTime = client.getEndTime();
                    }
                    if (loggedIn) {
                        //SignedInThread t = new SignedInThread();
                        //t.start();
                        //t.setDaemon(true);
                        setMsg("Signed in!", 2);

                        setTimeLeft(toTime);
                        showQuickInfo();
                        setControls(false);
                    }
                } catch (Exception e) {
                    FileUtils.log(this, Level.ERROR, e.getMessage());
                    e.printStackTrace();
                    setMsg("Login failed!", 0);
                }
            }
        } else {
            setMsg("Connecting to server failed!", 0);
        }
    }

    private class SignedInThread extends Thread {
        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                if (client.isSignedIn()) {
                    setMsg("Signed in!", 2);
                    return;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            setMsg("Time überschritten", 0);
        }
    }

    private void setTimeLeft(LocalTime toTime) {
        countdown = new Countdown(toTime, txTimeLeft);
        countdown.setDaemon(false);
        countdown.start();
    }

    public void showQuickInfo() {
        quickInfo = new Stage();
        quickInfo.setTitle("QuickInfo");
        quickInfo.initStyle(StageStyle.TRANSPARENT);
        quickInfo.setAlwaysOnTop(true);

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        quickInfo.setX(primaryScreenBounds.getMinX());
        quickInfo.setY(primaryScreenBounds.getMinY());

        sliderPos.valueProperty().addListener((ov, old_val, new_val) -> {
            quickInfo.setX((Screen.getPrimary().getVisualBounds().getWidth() - quickInfo.getWidth())
                    * new_val.doubleValue() / sliderPos.getMax());
        });
        sliderPos.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                if (mouseEvent.getClickCount() == 2) {
                    if (quickInfo.isShowing()) {
                        quickInfo.hide();
                    } else {
                        quickInfo.show();
                    }
                }
            }
        });

        HBox hBox = new HBox(5);
        hBox.setBackground(Background.EMPTY);
        Text text = new Text(0, 18, "00:00:00");
        text.setStrokeType(StrokeType.OUTSIDE);
        text.setFont(Font.font("System", FontWeight.BOLD, 18));
        ImageView iv = new ImageView(new Image("/images/eye-green.png"));
        iv.setFitHeight(22);
        iv.setFitWidth(35);

        countdown.addText(text);
        hBox.getChildren().addAll(text, iv);

        Scene scene = new Scene(hBox);
        scene.setFill(null);
        quickInfo.initStyle(StageStyle.TRANSPARENT);
        quickInfo.setScene(scene);

        quickInfo.hide();
    }

    /**
     * Checks whether the user has entered valid values
     *
     * @return is form valid?
     */
    public boolean validForm() {
        String serverIP = tfServerIP.getText();
        int port = MyUtils.stringToInt(tfPort.getText());
        String enrolmentID = tfEnrolmentID.getText();
        int catalogNumber = MyUtils.stringToInt(tfCatalogNumber.getText());
        String firstName = tfFirstName.getText();
        String lastName = tfLastName.getText();
        String pathOfProject = tfPathOfProject.getText();

        boolean validity = false;
        if (serverIP.isEmpty()) {
            setMsg("Specify the IP-Address of the server!", 0);
        } else if ((serverIP.split("\\.").length != 4 && !serverIP.equals("localhost"))
                || serverIP.length() > 15) {
            setMsg("Invalid IP-Address!", 0);
        } else if (port < 1) {
            setMsg("Invalid Port!", 0);
        } else if (enrolmentID.isEmpty()) {
            setMsg("Enter your enrolment id", 0);
        } else if (enrolmentID.length() >= 10) {
            setMsg("The enrolment id is too long!", 0);
        } else if (catalogNumber < 1) {
            setMsg("Invalid catalog number!", 0);
        } else if (firstName.isEmpty()) {
            setMsg("Your first name should not be empty", 0);
        } else if (firstName.length() > 20) {
            setMsg("Your first name should not be longer than 20 characters", 0);
        } else if (lastName.isEmpty()) {
            setMsg("Your last name should not be empty", 0);
        } else if (lastName.length() > 20) {
            setMsg("Your last name should not be longer than 20 characters", 0);
        } else if (!Pattern.compile("[a-zA-ZäöüÄÖÜ]").matcher(lastName).find()) {
            setMsg("Unknown letter in your lastname. Allowed: A-Z", 0);
        } else if (!Pattern.compile("[a-zA-ZäöüÄÖÜ]").matcher(lastName).find()) {
            setMsg("Unknown letter in your firstname. Allowed: A-Z", 0);
        } else if (firstName.length() < 3) {
            setMsg("Your firstname must be at least 3 characters long!", 0);
        } else if (pathOfProject.isEmpty()) {
            setMsg("Specify the path of project!", 0);
        } else {
            validity = true;
        }
        return validity;
    }

    /**
     * Enables and disables controls depending on login and logout
     *
     * @param value enable controls?
     */
    public void setControls(boolean value) {
        tfServerIP.setEditable(value);
        tfPort.setEditable(value);
        tfEnrolmentID.setEditable(value);
        tfCatalogNumber.setEditable(value);
        tfFirstName.setEditable(value);
        tfLastName.setEditable(value);
        tfPathOfProject.setEditable(value);
        btnChooseDirectory.setDisable(!value);
        btnLogin.setDisable(!value);
        cbFinished.setVisible(!value);
        btnLogout.setDisable(value);
        lbTimeLeft.setVisible(!value);
        txTimeLeft.setVisible(!value);
        sliderPos.setVisible(!value);
    }

    /**
     * Saves the valid values in the repository "Exam"
     *
     * @return true, if a successful storage, else false
     */
    public boolean setExam() {
        if (validForm()) {
            Exam.getInstance().setServerIP(tfServerIP.getText());
            Exam.getInstance().setPort(Integer.valueOf(tfPort.getText()));
            Exam.getInstance().setPupil(new Pupil(
                    Integer.valueOf(tfCatalogNumber.getText()),
                    tfEnrolmentID.getText(),
                    tfFirstName.getText(),
                    tfLastName.getText(),
                    tfPathOfProject.getText())
            );
            return true;
        }
        return false;
    }


    public boolean isLoggedIn() {
        return btnLogin.isDisable();
    }

    @FXML
    public void setMode() {
        LineCounter.getInstance().setFinished(cbFinished.isSelected());
    }

    /**
     * Disconnects from the server.
     */
    @FXML
    public void logout() {
        if (countdown != null) {
            countdown.interrupt();
            countdown.reset();
        }
        if (quickInfo != null)
            quickInfo.close();
        setControls(true);
        setMsg("Test successfully submitted", 2);
        if (client != null)
            client.stop();
    }

    public boolean isLoggedOut() {
        return btnLogout.isDisable();
    }

    /**
     * Sets an message on the screen of the student.
     *
     * @param text   specifies the message to show
     * @param status
     */
    private void setMsg(String text, int status) {
        FxUtils.setMsg(lbAlert, text, status);
    }


}
