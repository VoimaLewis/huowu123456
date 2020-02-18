package servlet;

import common.OrderStatus;
import entity.Account;
import entity.Goods;
import entity.Order;
import entity.OrderItem;
import util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/pay")
public class ReadyBuyServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html; charset=UTF-8");

        String goodsIdAndNum = req.getParameter("goodsIdAndNum");
        System.out.println(goodsIdAndNum);

        String[] strings = goodsIdAndNum.split(",");
        List<Goods> goodsList = new ArrayList<>();
        for (String s : strings) {
            String[] strings1 = s.split("-");
            Goods goods = this.getGoods(Integer.valueOf(strings1[0]));

            if (goods != null){
                goodsList.add(goods);
                goods.setBuyGoodsNum(Integer.valueOf(strings[1]));
            }else {
                Writer writer = resp.getWriter();
                writer.write("<h2>没有此货物：</h2>");
                throw new RuntimeException("没有此货物");
            }
        }
        //打印goodsList里面的内容
        System.out.println("当前购买的商品"+goodsList);
        System.out.println("当前数量");

        HttpSession session = req.getSession();
        Account account = (Account)session.getAttribute("user");

        Order order = new Order();
        order.setId(String.valueOf(System.currentTimeMillis()));
        order.setAccount_id(account.getId());

        //无法获取getName方法？？？？
        order.getAccount_name();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        order.setCreate_time(LocalDateTime.now().format(formatter));
        int totalMoney = 0;
        int actualMoney = 0;
        for (Goods goods : goodsList){

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setGoodsId(goods.getId());
            orderItem.setGoodsName(goods.getName());
            orderItem.setGoodsIntroduce(goods.getIntroduce());
            orderItem.setGoodsNum(goods.getBuyGoodsNum());
            orderItem.setGoodsUnit(goods.getUnit());
            orderItem.setGoodsPrice(goods.getPriceInt());
            orderItem.setGoodsDiscount(goods.getDiscount());
            order.orderItemList.add(orderItem);

            int currentMoney = goods.getBuyGoodsNum()*goods.getPriceInt();
            totalMoney += currentMoney;
            actualMoney += currentMoney*goods.getDiscount()/100;
        }
        order.setActual_amount(actualMoney);
        order.setTotal_money(totalMoney);
        order.setOrder_status(OrderStatus.PLAYING);

        //记录下当前的session；
        HttpSession session1 = req.getSession();
        session.setAttribute("order",order);
        session.setAttribute("goodsList",goodsList);

        resp.getWriter().println("<html>");
        resp.getWriter().println("<p>"+"【用户名称】"+order.getAccount_name()+"</p>");
        resp.getWriter().println("<p>"+"【订单编号】"+order.getId()+"</p>");
        //?????
        resp.getWriter().println("<p>"+"【订单状态】"+order.getOrder_status().getDesc()+"</p>");
        resp.getWriter().println("<p>"+"【创建时间】"+order.getCreate_time()+"</p>");
        resp.getWriter().println("<p>"+"编号  "+"名称  "+"数量  "+"单位（元）  "+"</p>");
        resp.getWriter().println("<ol>");

        for (OrderItem orderItem  : order.orderItemList){
            resp.getWriter().println("<li"+orderItem.getGoodsName()+" "+orderItem.getGoodsNum()
                    +" "+orderItem.getGoodsUnit()+" "+orderItem.getGoodsPrice()+"</li>");
        }

        resp.getWriter().println("</ol>");
        resp.getWriter().println("<p>"+"【总金额】"+order.getTotal_money()+"</p>");

        resp.getWriter().println("<p>"+"【优惠金额】"+order.getDiscount()+"</p>");
        resp.getWriter().println("<p>"+"【实付金额】"+order.getActual_amount()+"</p>");
        resp.getWriter().println("<a href=\"buyGoodsServlet\">确认</a>");
        resp.getWriter().println("<a href=\"index.html\">取消</a>");
        resp.getWriter().println("</html>");

    }

    private Goods getGoods(int goodsId){
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Goods goods = null;
        try{
            String sql = "delete from goods where id=?";
            connection = DBUtil.getConnection(true);
            preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setInt(1,goodsId);

            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()){
                goods = this.extractGoods(resultSet);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            DBUtil.close(connection,preparedStatement,resultSet);
        }
        return goods;
    }
    public Goods extractGoods(ResultSet resultSet) throws SQLException {
        Goods goods = new Goods();
        goods.setId(resultSet.getInt("id"));
        goods.setName(resultSet.getString("name"));
        goods.setIntroduce(resultSet.getString("introduce"));
        goods.setStock(resultSet.getInt("stock"));
        goods.setUnit(resultSet.getString("unit"));
        goods.setPrice(resultSet.getInt("price"));
        goods.setDiscount(resultSet.getInt("discount"));
        return goods;
    }
}
