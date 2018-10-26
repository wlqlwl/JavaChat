import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Iterator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Server extends Application {

    public static ExecutorService threadPooL;
    // 쓰레드풀을 이용해서 다양한 클라이언트가 접속했을때 쓰레드가 효과적으로 관리하기 위해서 사용
    public static Vector<Client> clients = new Vector<Client>();
    // 접속한 클라이언트들을 관리할 수 있도록 만듬

    ServerSocket serverSocket;

    // 서버를 구동시켜서 클라이언트의 연결을 기다리는 메소드
    public void startServer(String IP,int port) {
        try {                     // 서버가 실행되면~?
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(IP,port));
            // 소켓의 대한 객체를 활성화해주고 바인드를 해서 서버 컴퓨터 역할을 수행하는 컴퓨터가
            // 자신의 IP주소, 포트번호로 특정한 클라이언트의 접속을 기다리게 만듬

        } catch(Exception e) {
            e.printStackTrace();
            if(!serverSocket.isClosed()) { // 서버소켓이 닫혀있는 상황이 아니라면
                stopServer();             // 서버를 종료할 수 있다.
            }
            return;
        }
        // 클라이언트가 접속할 때까지 계속 기다리는 쓰레드
        Runnable thread = new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Socket socket = serverSocket.accept();  // 클라이언트가 접속을 했다면
                        clients.add(new Client(socket));       // 클라이언트 배열에 새롭게 접속한 클라이언트를 추가해주는 것
                        System.out.println("[클라이언트 접속]"
                                + socket.getRemoteSocketAddress()
                                + ": " + Thread.currentThread().getName());
                    } catch(Exception e) {
                        if(!serverSocket.isClosed()) // 서버소켓에 문제가 발생했다면
                            stopServer();            // 서버 작동 중단
                        break;                   // break로 빠져나옴
                    }
                }
            }
        };
        threadPooL = Executors.newCachedThreadPool(); // 쓰레드풀 초기화
        threadPooL.submit(thread); // 쓰레드풀에 현재 클라이언트를 기다리는 쓰레드를 담음
    }


    //서버의 작동을 중지시키는 메소드
    public void stopServer() {
        try {
            // 현재 작동 중인 모든 소켓 닫기
            Iterator<Client> iterator = clients.iterator();  // 모든 클라이언트에 개별적으로 접근할 수 있게 함
            while(iterator.hasNext()) {                      // 하나씩 접근할 수 있도록 만듬
                Client client = iterator.next();           // 특정한 클라이언트에 접근해서
                client.socket.close();                   // 그 클라이언트 소켓을 닫음
                iterator.remove();                      // 연결이 끊긴 클라이언트를 제거
            }
            // 서버 소켓 객체 닫기
            if(serverSocket != null && !serverSocket.isClosed()) // 서버소켓이 널값이 아니고, 현재 소켓이 열려있다면
                serverSocket.close();                      // 해당 서버 소켓 닫음
            // 쓰레드풀 종료하기
            if(threadPooL != null && !threadPooL.isShutdown())
                threadPooL.shutdown();                   // 쓰레드풀을 셧다운해서 자원을 할당할 수 있게 함
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //UI를 생성하고, 실질적으로 프로그램을 동작시키는 메소드
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();         // 전체 디자인 틀을 담는 레이아웃
        root.setPadding(new Insets(5));            // 내부에 5만큼 패딩을 줌

        TextArea textArea = new TextArea();         // 긴 문장의 텍스트가 담길 수 있는 공간
        textArea.setEditable(false);            // 문장을 출력만 하고 수정이 불가하게 만듬
        textArea.setFont(new Font("나눔 고딕",15));   // 나눔고딕체
        root.setCenter(textArea);               // 중간에 담을 수 있는 공간

        Button toggleButton = new Button("시작하기");   // 버튼을 생성
        toggleButton.setMaxWidth(Double.MAX_VALUE);   // toggleButton = 스위치 역할
        BorderPane.setMargin(toggleButton, new Insets(1,0,0,0)); // 디자인을 이쁘게
        root.setBottom(toggleButton);             // 버튼을 담을 수 있도록

        String IP = "127.0.0.1";            // 자기 자신의 컴퓨터 주소(루프백 주소)
        int port = 9876;

        toggleButton.setOnAction(event -> {    // 토글 버튼을 눌렀을 경우 액션 처리(이벤트)
            if(toggleButton.getText().equals("시작하기")) { // 토글버튼이 시작버튼 이라면
                startServer(IP,port);                 // 서버 시작
                Platform.runLater(() -> {              // GUI 요소를 출력할 수 있게 함
                    String message = String.format("[서버 시작]\n",IP,port);
                    textArea.appendText(message);        // 메세지 출력
                    toggleButton.setText("종료하기");        // 시작하기 -> 종료하기
                });
            }else {                     // 종료 버튼을 눌렀다면
                stopServer();            // 서버 종료
                Platform.runLater(() -> {   // GUI 요소~
                    String message = String.format("[서버 종료]\n",IP,port);
                    textArea.appendText(message);
                    toggleButton.setText("시작하기"); // 다시 시작할 수 있도록
                });
            }
        });
        Scene scene = new Scene(root, 400, 400); // 화면 크기 구상
        primaryStage.setTitle("[ 채팅 서버 ]");       // 서버 정보 출력
        primaryStage.setOnCloseRequest(event -> stopServer()); // 종료했다면~
        primaryStage.setScene(scene);          // 씬 정보를 출력할 수 있도록
        primaryStage.show();                // 화면에 출력할 수 있도록
    }

    // 프로그램의 진입점
    public static void main(String[] args) {
        launch(args);
    }
}