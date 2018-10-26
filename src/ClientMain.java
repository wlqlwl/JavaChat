import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;


public class ClientMain extends Application {

    Socket socket;
    TextArea textArea;

    // 클라이언트 프로그램 동작 메소드
    public void startClient(String IP, int port) {
        Thread thread = new Thread() {      // 쓰레드 객체를 만들어 준다.
            public void run() {
                try {
                    socket = new Socket(IP, port); // 소켓 초기화
                    receive();            // 서버로부터 메세지를 전달받을 수 있도록
                } catch (Exception e) {      // 오류가 발생했다면
                    if(!socket.isClosed()) { // 소켓이 열려있다면
                        stopClient();       // 클라이언트를 종료시킨다.
                        System.out.println("[서버 접속 실패]");
                        Platform.exit();    // 프로그램 자체를 종료
                    }
                }
            }
        };
        thread.start(); // 쓰레드를 시작할 수 있도록
    }

    // 클라이언트 프로그램 종료 메소드
    public void stopClient() {
        try {
            if(socket != null && !socket.isClosed()) { // 소켓이 열려있는 상태라면
                socket.close();   // 소켓 객체(자원)을 헤제
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 서버로부터 메세지를 전달받는 메소드
    public void receive() {
        while(true) {        // 계속 서버로부터 메세지를 받을 수 있도록 무한루프
            try {
                InputStream in = socket.getInputStream();   // 서버로부터 전닯 받을 수 있게
                byte[] buffer = new byte[512];            // 512byte 만큼
                int length = in.read(buffer);            // 실제로 입력을 받음
                if(length == -1) throw new IOException();   // 오류가 발생한다면~?
                String message = new String(buffer, 0, length, "UTF-8");
                // 버퍼에 있는 정보를 length만큼 메세지에 담고 출력
                Platform.runLater(()->{
                    textArea.appendText(message); // GUI 요소 중 하나
                });
            } catch (Exception e) {
                stopClient();
                break;      // 오류가 발생했을 때는 무한루프를 탈출할 수 있또록
            }
        }
    }

    // 서버로부터 메세지를 전송하는 메소드
    public void send(String message) {
        Thread thread = new Thread() {    // 전송할 때도 쓰레드 이용
            public void run(){         // 쓰레드가 어떠한 내용으로 동작을 할지 명시
                try {
                    OutputStream out = socket.getOutputStream();
                    byte[] buffer = message.getBytes("UTF-8");
                    // 서버에서 전달을 받을때도 UTF-8 으로 인코딩된 정보를 전달 받도록 코딩 했기 때문
                    out.write(buffer);
                    out.flush();    // 메세지 전송의 끝을 알림
                } catch (Exception e) {   // 오류가 생기면
                    stopClient();      // 멈춤
                }
            }
        };
        thread.start();
    }

    public boolean validate(String name, String password) {
        if(name.equals("") || password.equals("")) {
            return false;
        } else {
            return true;
        }
    }

    public boolean signup(String userId, String name, String password, String address, String postNo) {
        // DB connection
        Connection connection = ConnectionUtil.connect();
        return ConnectionUtil.insertUser(connection, userId, name, password, address, postNo);
    }

    public User signin(String name, String password) {
        // DB connection
        Connection connection = ConnectionUtil.connect();
        User user = ConnectionUtil.getUserList(connection, name, password);
        return user;
    }

    public void initLoginScene(Stage primaryStage) {
        StackPane pane = new StackPane();

        Label IName = new Label("Username: "); // 아이디를 위한 레이블
        Label IPassword = new Label("Password: "); // 비밀번호를 위한 레이블
        Label lMessage = new Label(); // 로그인 시 메세지를 표시하기 위함

        TextField tfName = new TextField(); // 유저가 입력한 값을 얻기 위함
        PasswordField tfPassword = new PasswordField();
        // 비밀번호의 특성을 표현하기 위함. ****로 표현

        Button button = new Button("Login");   // Login 버튼을 만듬
        button.prefHeightProperty().bind(tfName.heightProperty().add(tfPassword.heightProperty()));
        // 버튼의 크기를 조절한다. ID,PASSWORD 칸과 같은 크기로 하고 싶기때문에 bind로 묶고, 속에 add로 이어준다.

        button.setOnAction(e->{                     // 로그인 버튼을 눌렀을때 이벤트 발생
            lMessage.setStyle("-fx-text-till: red;");   // 메세지 색깔을 red로 설정

            String name = tfName.getText();            // 유저가 입력한 아이디
            String password = tfPassword.getText();      // 유저가 입력한 패스워드

            if (validate(name, password)) {

                // DB connection
                User user = signin(name, password);

                if(user != null) {
                    // 성공적으로 로그인이 되었다면
                    initChatScene(primaryStage, user);
                } else {
                    // 아이디 혹은 비밀번호가 틀렸다면
                    lMessage.setText("Wrong name or password. Please enter your name and password");
                }
            } else {
                lMessage.setText("Invalid name or password. Please enter your name and password");
            }
        });

        GridPane grid = new GridPane();
        grid.addRow(0, IName, tfName); // 가장 첫번째 줄에는 아이디를 표현하기 위함
        grid.addRow(1, IPassword, tfPassword); // 비밀번호를 표현하기 위함
        grid.add(button,2,0,1,2);      // 버튼을 넣고, 세로길이 2, 가로길이 0, 여분은 1,2
        grid.add(lMessage, 0, 2,3,1);
        grid.setAlignment(Pos.CENTER); // 중앙에 위치

        pane.getChildren().add(grid); // 앞서 정의한 pane에 gird를 넣어줌
        Scene scene = new Scene(pane,800,400);

        primaryStage.setTitle("[ 클라이언트 로그인 ]");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void initChatScene(Stage primaryStage, User user) {
        BorderPane root = new BorderPane();  // 기본적인 레이아웃
        root.setPadding((new Insets(5)));    // 디자인

        HBox hbox = new HBox();              // BorderPane 위에 새로운 레이아웃
        hbox.setSpacing(5);                // 여백

        TextField userName = new TextField();   // 이름이 들어갈 텍스트 공간 생성
        userName.setPrefWidth(150);            // 너비는 150
        userName.setPromptText(user.getName() + "님, 닉네임을 입력하세요");
        HBox.setHgrow(userName, Priority.ALWAYS);
        // HBox 내부에서 해당 텍스트필드가 출력이 될 수 있도록
        TextField IPText = new TextField("127.0.01"); // 기본적인 자신의 컴퓨터 주소
        TextField portText = new TextField("9876");
        portText.setPrefWidth(80);           // 너비 80

        hbox.getChildren().addAll(userName, IPText, portText); // 3개의 텍스트필드
        root.setTop(hbox);                  // HBox 위쪽에다가가

        textArea = new TextArea();         // 객체 초기화
        textArea.setEditable(false);      // 내용 수정 불가능하게 만듬
        root.setCenter(textArea);         // 레이아웃 중간에 위치하게

        TextField input = new TextField();
        input.setPrefWidth(Double.MAX_VALUE);
        input.setDisable(true);
        // 접속하기 이전에 어떠한 메세지를 전송할 수 없도록
        input.setOnAction(event->{   // 이벤트  발생시
            send(userName.getText() + ":"+input.getText()+"\n"); // 사용자 이름과 메세지내용
            input.setText("");
            input.requestFocus();         // 다시 보낼 수 있도록 설정
        });

        Button sendButton = new Button("보내기");
        sendButton.setDisable(true);      // 접속하기 이전에는 이용할 수 없도록

        sendButton.setOnAction(event->{
            send(userName.getText() + ":"+input.getText()+"\n");
            input.setText("");
            input.requestFocus();
        });
        Button connectionButton = new Button("접속하기");
        connectionButton.setOnAction(event ->{      // 버튼을 눌렀을 때 이벤트 발생
            if(connectionButton.getText().equals("접속하기")) { // 접속하기로 되어있다면
                int port = 9876;
                try {
                    port = Integer.parseInt(portText.getText());
                    // 사용자가 입력한 포트번호를 정수 형태로 변환해서 다시 담을 수 있또록
                } catch (Exception e) {
                    e.printStackTrace();
                }
                startClient(IPText.getText(), port);   // 특정한 IP주소, 포트번호로 접속할 수 있게
                Platform.runLater(()->{         // 화면에 관련된 내용이 출력될 수 있게
                    textArea.appendText("[ 채팅방 접속 ]\n");
                });
                connectionButton.setText("종료하기");
                input.setDisable(false);   // 사용자가 내용을 입력해서 버튼을 누른 후 보낼 수 있또록
                sendButton.setDisable(false);
                input.requestFocus();      // 바로 다른 메세지를 입력할 수 있또록
            } else {
                stopClient();
                Platform.runLater(()->{
                    textArea.appendText("[ 채팅방 퇴장 ]\n");
                });
                connectionButton.setText("접속하기");
                input.setDisable(true);
                sendButton.setDisable(true);
            }
        });

        BorderPane pane = new BorderPane();

        pane.setLeft(connectionButton);      //왼쪽에 접속하기 버튼
        pane.setCenter(input);            //중간에 인풋 버튼
        pane.setRight(sendButton);         //오른쪽에는 보내기 버튼

        root.setBottom(pane);
        Scene scene = new Scene(root, 800, 400);   // 해상도

        primaryStage.setTitle("[ 클라이언트 대기화면 ]");   // 정보
        primaryStage.setScene(scene);            // 등록
        primaryStage.setOnCloseRequest(event->stopClient());
        primaryStage.show();

        //기본적으로 프로그램이 시작되면 접속하기 버튼이 포커싱 되게 설정
        connectionButton.requestFocus();
    }

    //실제로 프로그램을 동작시키는 메소드
    @Override
    public void start(Stage primaryStage) {
        try {
            initLoginScene(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 프로그램 진입점
    public static void main(String[] args) {
        launch(args);
    }
}