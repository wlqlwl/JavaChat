import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConnectionUtil {

    public static Connection connect() {
        try {
            Class.forName("com.mysql.jdbc.Driver"); // Reflection
            Connection connection = DriverManager.getConnection("jdbc:mysql://db-vbrb.pub-cdb.ntruss.com:3306/javachat", "javachat", "q1w2e3r4!");
            return connection;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
            return null;
        }
    }

    public static void shutdown(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean insertUser(Connection connection, String userId, String name, String password, String address, String postNo) {

        String sql = "INSERT INTO user (user_id, name, password, address, post_no) VALUES (?, ?, ?, ?, ?)";

        try {

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, password);
            preparedStatement.setString(4, address);
            preparedStatement.setString(5, postNo);

            preparedStatement.executeQuery();
            return true;

        } catch (SQLException e) {
            return false;
        } finally {
            shutdown(connection);
        }
    }

    public static User getUserList(Connection connection, String userId, String password) {

        String sql = "SELECT * FROM user WHERE user_id = ? and password = ?";

        try {

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, password);

            // 쿼리문이 select * 이기 때문에 여러개의 결과가 나올 수 있음
            ResultSet result = preparedStatement.executeQuery();

            List<User> userList = new ArrayList<>();
            while (result.next()) {
                String idValue = result.getString("id");
                String userIdValue = result.getString("user_id");
                String nameValue = result.getString("name");
                String passwordValue = result.getString("password");
                String addressValue = result.getString("address");
                String postNoValue = result.getString("post_no");
                String updatedAtValue = result.getString("updated_at");
                String createdAtValue = result.getString("created_at");

                User user = new User(idValue, userIdValue, nameValue, passwordValue, addressValue, postNoValue, updatedAtValue, createdAtValue);
                userList.add(user);
            }
            return userList.get(0);
        } catch (SQLException e) {
            return null;
        } finally {
            shutdown(connection);
        }
    }
}