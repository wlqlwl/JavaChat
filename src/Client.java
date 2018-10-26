import java.net.Socket; // 소켓을 사용하기 위해 라이브러리를 정의
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

// 챗 서버가 한명의 클라이언트와 통신을 하기 위해서 필요한 기능들을 정의할 것임

public class Client {

    Socket socket; // 컴퓨터와 네트워크 상에서 통신하기 위하여 socket을 만듬

    public Client(Socket socket){
        this.socket=socket;
        receive();
    } // 어떠한 변수의 초기화를 위하여 생성자를 만들어줌


    // ★클라이언트로부터 메세지를 전달 받는 메소드
    public void receive() {
        Runnable thread = new Runnable() {
            // 하나의 쓰레드를 만들기 위하여 Runnable 객체를 만든다.

            public void run() {                                             //Runnable 라이브러리는 내부적으로 반드시 run()를 가져야 함.
                //즉 하나의 쓰레드가 어떠한 모듈로써 동작을 할건지 run() 안에서 정의
                try {                                               //예외발생을 위한 try-catch문
                    while(true) {                                     // 반복적으로 클라이언트로부터 어떠한 정보를 받을수 있도록 설정
                        InputStream in = socket.getInputStream();
                        // 어떠한정보를 전달받을 수 있도록 InputStream 객체를 만듬
                        byte[] buffer = new byte[512];
                        // buffer을 이용해서 한 번에 512byte만큼 전달 받을 수 있도록 만듬
                        int length = in.read(buffer);
                        // 실제로 클라이언트로부터 어떠한 내용을 전달받아서 buffer에 담아주도록 만듬
                        // 여기서 length는 담긴 메세지의 크기를 의미한다.
                        while(length == -1) throw new IOException();        // 오류가 발생했다면~
                        System.out.println("[메세지 수신 성공]"                  // 메세지를 잘받았다면~
                                +socket.getRemoteSocketAddress()
                                // 현재 접속한 클라이언트의 IP주소와 같은 주소 정보를 출력
                                +":"+Thread.currentThread().getName());
                        // 쓰레드의 고유한 정보를 출력
                        String message = new String(buffer, 0, length, "UTF-8");
                        // 한글도 포함할 수 있도록 UTF-8로 인코딩 처리함
                        for(Client client : Server.clients) {
                            client.send(message);                        // 전달받은 메세지를 다른 클라이언트한테도 보낼 수 있도록 만듬

                        }
                    }
                }catch(Exception e) {
                    try {
                        System.out.println("[메세지 수신 오류]"
                                +socket.getRemoteSocketAddress()
                                +" : " + Thread.currentThread().getName());;
                    }catch(Exception e2) {
                        e2.printStackTrace();
                    }
                }
            }
        };
        Server.threadPooL.submit(thread); // threadPooL에 만들어진 쓰레드를 등록해주는 것
    }                                  // -> 이는 안정적으로 관리해주기 위해서 threadPooL 사용


    // ★클라이언트에게 메세지를 전송하는 메소드
    public void send(String message) {
        Runnable thread = new Runnable() {
            public void run() {
                try {
                    OutputStream out = socket.getOutputStream();
                    // 보낼때는 OutputStream 을 이용
                    byte[] buffer = message.getBytes("UTF-8");
                    out.write(buffer);         // buffer에 있는 정보를 서버에서 클라이언트로 전송을 해주는 것
                    out.flush();               // 성공적으로 여기까지 전송을 했다는 것을 알려주는 역할
                }catch(Exception e) {
                    try {
                        System.out.println("[메세지 송신 오류]"
                                +socket.getRemoteSocketAddress()
                                +": " + Thread.currentThread().getName());
                        Server.clients.remove(Client.this);
                        // 오류가 발생했다면 모든 클라이언트의 대한 정보를 담는 배열에서
                        // 현재 존재하는 클라이언트를 지워주는 것
                        // 즉, 클라이언트 배열에서 오류가 생긴 클라이언트를 제거해주는 것
                        socket.close();
                    } catch(Exception e2) {
                        e2.printStackTrace();
                    }
                }
            }
        };
        Server.threadPooL.submit(thread);
    }
}
