package servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import entity.Account;
import entity.Goods;
import entity.Order;
import util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BrowseOrderServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html; charset=UTF-8");

        HttpSession session = req.getSession();
        Account account = (Account)session.getAttribute("user");
        List<Order> orderList = this.queryOrder(account.getId());

        if (orderList == null){
            System.out.println("订单链表为空！");
        }else {
            ObjectMapper objectMapper = new ObjectMapper();
            PrintWriter printWriter = resp.getWriter();
            objectMapper.writeValue(printWriter,orderList);
            Writer writer = resp.getWriter();
            writer.write(printWriter.toString());
        }

    }

    private List<Order> queryOrder(int accountId) {
        List<Order> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        //46

    }

//    private List<Order> queryOrder(Account account) {
//        Connection connection = null;
//        PreparedStatement preparedStatement = null;
//        ResultSet resultSet = null;
//        List<Order> orderList = this.queryOrder(account);
//        try {
//            String sql = "SELECT id,account_id,account_name,create_time,finish_time," +
//                    "actual_amount,total_money,order_status from order where id=?";
//            connection = DBUtil.getConnection(true);
//            preparedStatement = connection.prepareStatement(sql);
//
//            resultSet = preparedStatement.executeQuery();
//
//
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }finally {
//            DBUtil.close(connection,preparedStatement,resultSet);
//        }
//
//    }
}
