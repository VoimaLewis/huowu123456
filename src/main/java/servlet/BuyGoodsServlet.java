package servlet;

import common.OrderStatus;
import entity.Goods;
import entity.Order;
import entity.OrderItem;
import util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BuyGoodsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        HttpSession session = req.getSession();
        Order order = (Order) session.getAttribute("order");
        List<Goods> goodsList = (List<Goods>)session.getAttribute("goodsList");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        order.setCreate_time(LocalDateTime.now().format(formatter));

        order.setOrder_status(OrderStatus.OK);
        boolean effect = this.commitOrder(order);
        //遍历所有的货物，把这些货物库存进行修改。
        if (effect){
            for (Goods goods : goodsList){
                boolean isUpdate = updateAfterBuy(goods,goods.getBuyGoodsNum());
                if (isUpdate){
                    System.out.println("更新库存成功");
                }else {
                    System.out.println("更新库存失败");
                }
            }
            resp.sendRedirect("buyGoodsSuccess.html");
        }

    }

    private boolean updateAfterBuy(Goods goods, Integer buyGoodsNum) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        boolean effect = false;
        try{
            String sql = "update goods set stock=? where id=?";
            connection = DBUtil.getConnection(true);
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1,goods.getStock()-goods.getBuyGoodsNum());
            preparedStatement.setInt(2,goods.getId());
            if (preparedStatement.executeUpdate()==1){
                effect = true;
            }
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            DBUtil.close(connection,preparedStatement,null);
        }
        return effect;
    }

    private boolean commitOrder(Order order) {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try{
            String insertOrder = "insert into `order`(id, account_id, account_name, create_time, finish_time, actual_amount, total_money, order_status) values (?,?,?,now(),now(),?,?,?)";
            String insertOrderItem = "insert into order_item(order_id, goods_id, goods_name, goods_introduce, goods_num, goods_unit, goods_price, goods_discount) values (?,?,?,?,?,?,?,?)";
            //此处涉及事务的回滚，需手动提交。
            connection = DBUtil.getConnection(false);
            preparedStatement = connection.prepareStatement(insertOrder);
            preparedStatement.setString(1,order.getId());
            preparedStatement.setInt(2,order.getAccount_id());
            preparedStatement.setString(3,order.getAccount_name());
            preparedStatement.setInt(4,order.getActualAmountInt());
            preparedStatement.setInt(5,order.getTotalMoneyInt());
            preparedStatement.setInt(6,order.getOrder_status().getFlag());

            if (preparedStatement.executeUpdate() == 0){
                throw new RuntimeException("插入订单失败");
            }

            preparedStatement = connection.prepareStatement(insertOrderItem);
            for (OrderItem orderItem : order.orderItemList){
                preparedStatement.setString(1,order.getId());
                preparedStatement.setInt(2,orderItem.getGoodsId());
                preparedStatement.setString(3,orderItem.getGoodsName());
                preparedStatement.setString(4,orderItem.getGoodsIntroduce());
                preparedStatement.setInt(5,orderItem.getGoodsNum());
                preparedStatement.setString(6,orderItem.getGoodsUnit());
                preparedStatement.setInt(7,orderItem.getGoodsPriceInt());
                preparedStatement.setInt(8,orderItem.getGoodsDiscount());
            }
            int[] effects = preparedStatement.executeBatch();
            for (int i : effects){
                if (i == 0){
                    throw new RuntimeException("插入订单项失败");
                }
            }
            //手动提交
            connection.commit();

        }catch (Exception e){
            e.printStackTrace();
            if (connection != null){
                try {
                    connection.rollback();
                }catch (SQLException e1){
                    e.printStackTrace();
                }
            }
            return false;
        }finally {
            DBUtil.close(connection,preparedStatement,null);
        }
        return true;
    }
}
