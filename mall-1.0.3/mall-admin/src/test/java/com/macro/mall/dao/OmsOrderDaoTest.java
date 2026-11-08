package com.macro.mall.dao;

import com.macro.mall.dto.OmsOrderDeliveryParam;
import com.macro.mall.dto.OmsOrderDetail;
import com.macro.mall.dto.OmsOrderQueryParam;
import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.OmsOrderOperateHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class OmsOrderDaoTest {
    @Autowired
    OmsOrderDao omsOrderDao;

    @Test
    @DisplayName("测试是否能够正确获取所有订单详情，且顺序正确")
    void getDetailTest(){
        OmsOrderDetail detail_12L = omsOrderDao.getDetail(12L);
        assertThat(detail_12L.getId()).isEqualTo(12L);
        assertThat(detail_12L.getHistoryList()).hasSize(3);
        assertThat(detail_12L.getOrderItemList()).hasSize(5);
        assertThat(detail_12L.getHistoryList()).extracting(OmsOrderOperateHistory::getId).containsExactly(23L,7L,5L);

        OmsOrderDetail detail_15L = omsOrderDao.getDetail(15L);
        assertThat(detail_15L.getId()).isEqualTo(15L);
        assertThat(detail_15L.getHistoryList()).hasSize(0);
        assertThat(detail_15L.getOrderItemList()).hasSize(5);

        OmsOrderDetail detail_17L = omsOrderDao.getDetail(17L);
        assertThat(detail_17L.getId()).isEqualTo(17L);
        assertThat(detail_17L.getHistoryList()).hasSize(1);
        assertThat(detail_17L.getOrderItemList()).hasSize(0);
    }

    @Test
    @DisplayName("应该返回所有记录，当对象特定属性都为空时")
    void shouldReturnAllWhenThePropertyOfOmsOrderQueryParamIsEmpty(){
        //=========当传进一个空的对象时，理应返回所有48条记录================
        List<OmsOrder> list_Empty = omsOrderDao.getList(new OmsOrderQueryParam());
        assertThat(list_Empty).hasSize(48);
        assertThat(list_Empty).extracting(OmsOrder::getId).doesNotContain(15L,16L,17L,18L,19L,20L,21L,22L,23L,24L,25L,26L,27L,28L,33L,34L,39L);
    }

    @Test
    @DisplayName("应该返回对应记录，当实现某一单一条件时")
    void shouldReturnCorrespondRecordWhenRealizeASingleCondition(){
        //===============满足orderSn不为空的条件====================
        OmsOrderQueryParam qp_orderSn = new OmsOrderQueryParam();
        qp_orderSn.setOrderSn("202002250100000004");
        List<OmsOrder> list_orderSn = omsOrderDao.getList(qp_orderSn);
        assertThat(list_orderSn).hasSize(1);
        assertThat(list_orderSn).extracting(OmsOrder::getId).containsExactly(30L);

        //====================满足status不为空的条件========================
        OmsOrderQueryParam qp_status = new OmsOrderQueryParam();
        qp_status.setStatus(1);
        List<OmsOrder> list_status1 = omsOrderDao.getList(qp_status);
        assertThat(list_status1).extracting(OmsOrder::getId).containsExactlyInAnyOrder(13L,32L,40L,42L,45L,46L,49L,50L,51L,53L,54L,56L,57L,58L,67L,68L);

        qp_status.setStatus(2);
        List<OmsOrder> list_status2 = omsOrderDao.getList(qp_status);
        assertThat(list_status2).extracting(OmsOrder::getId).containsExactlyInAnyOrder(63L,65L,72L,74L);

        qp_status.setStatus(3);
        List<OmsOrder> list_status3 = omsOrderDao.getList(qp_status);
        assertThat(list_status3).extracting(OmsOrder::getId).containsExactlyInAnyOrder(14L,30L,35L,37L,41L,47L,48L,52L,60L,62L,69L,70L,75L,76L);

        qp_status.setStatus(4);
        List<OmsOrder> list_status4 = omsOrderDao.getList(qp_status);
        assertThat(list_status4).extracting(OmsOrder::getId).containsExactlyInAnyOrder(12L,29L,31L,36L,38L,43L,44L,55L,59L,61L,64L,66L,71L);

        //=====================满足sourceType的条件不为空=========================
        OmsOrderQueryParam qp_sourceType = new OmsOrderQueryParam();
        qp_sourceType.setSourceType(1);
        List<OmsOrder> list_sourceType = omsOrderDao.getList(qp_sourceType);
        assertThat(list_sourceType).hasSize(48);

        qp_sourceType.setSourceType(0);
        list_sourceType = omsOrderDao.getList(qp_sourceType);
        assertThat(list_sourceType).isEmpty();

        //=====================满足orderType不为空的条件==========================
        OmsOrderQueryParam qp_orderType = new OmsOrderQueryParam();
        qp_orderType.setOrderType(0);
        List<OmsOrder> list_orderType = omsOrderDao.getList(qp_orderType);
        assertThat(list_orderType).hasSize(48);

        qp_orderType.setOrderType(1);
        list_orderType = omsOrderDao.getList(qp_orderType);
        assertThat(list_orderType).isEmpty();

        //=====================满足createTime不为空的条件==========================
        OmsOrderQueryParam qp_createTime = new OmsOrderQueryParam();
        qp_createTime.setCreateTime("2020-05-17");
        List<OmsOrder> list_createTime = omsOrderDao.getList(qp_createTime);
        assertThat(list_createTime).extracting(OmsOrder::getId).containsExactlyInAnyOrder(32L,35L,36L,37L,38L);

        //=====================满足receiverKeyword不为空的条件========================
        OmsOrderQueryParam qp_receiverKeyword = new OmsOrderQueryParam();
        qp_receiverKeyword.setReceiverKeyword("大梨");
        List<OmsOrder> list_receiverKeyword = omsOrderDao.getList(qp_receiverKeyword);
        assertThat(list_receiverKeyword).hasSize(40);

        qp_receiverKeyword.setReceiverKeyword("18961511111");
        list_receiverKeyword = omsOrderDao.getList(qp_receiverKeyword);
        assertThat(list_receiverKeyword).hasSize(8);
    }

    @Test
    @DisplayName("应该返回对应记录，当满足多个条件时")
    void shouldReturnCorrespondRecordWhenMultipleConditionsAreMet(){
        //====================同时满足OrderSn不为空和Status不为空的条件=========================
        OmsOrderQueryParam qp_OrderSn_status = new OmsOrderQueryParam();
        qp_OrderSn_status.setOrderSn("202002250100000004");
        qp_OrderSn_status.setStatus(3);
        List<OmsOrder> list_OrderSn_status = omsOrderDao.getList(qp_OrderSn_status);
        assertThat(list_OrderSn_status).extracting(OmsOrder::getId).containsExactly(30L);

        qp_OrderSn_status.setStatus(4);
        list_OrderSn_status = omsOrderDao.getList(qp_OrderSn_status);
        assertThat(list_OrderSn_status).isEmpty();

        //================同时满足createTime、sourceType、orderType、receiverKeyword不为空的条件======================
        OmsOrderQueryParam qp_CSOR = new OmsOrderQueryParam();
        qp_CSOR.setSourceType(1);
        qp_CSOR.setOrderType(0);
        qp_CSOR.setCreateTime("2020-05-17");
        qp_CSOR.setReceiverKeyword("18033441849");
        List<OmsOrder> list_CSOR = omsOrderDao.getList(qp_CSOR);
        assertThat(list_CSOR).extracting(OmsOrder::getId).containsExactlyInAnyOrder(32L,35L,36L,37L,38L);
    }

    public OmsOrderDeliveryParam createDeliveryParam(Long orderId,String deliveryCompany,String deliverySn){
        OmsOrderDeliveryParam deliveryParam = new OmsOrderDeliveryParam();
        deliveryParam.setOrderId(orderId);
        deliveryParam.setDeliveryCompany(deliveryCompany);
        deliveryParam.setDeliverySn(deliverySn);
        return deliveryParam;
    }

    @Test
    @DisplayName("测试批量发货功能是否能够正确修改库存信息")
    void deliveryTest(){
        OmsOrderDeliveryParam deliveryParam_32L = createDeliveryParam(32L,"顺丰快递","111");
        OmsOrderDeliveryParam deliveryParam_40L = createDeliveryParam(40L,"中通快递","222");
        OmsOrderDeliveryParam deliveryParam_63L = createDeliveryParam(63L,"韵达快递","333");
        List<OmsOrderDeliveryParam> list = new ArrayList<>();
        list.add(deliveryParam_32L);
        list.add(deliveryParam_40L);
        list.add(deliveryParam_63L);

        int result = omsOrderDao.delivery(list);

        assertThat(result).isEqualTo(2);

        OmsOrderDetail omsOrderDetail_32L = omsOrderDao.getDetail(32L);
        assertThat(omsOrderDetail_32L.getId()).isEqualTo(32L);
        assertThat(omsOrderDetail_32L.getDeliverySn()).isEqualTo("111");
        assertThat(omsOrderDetail_32L.getDeliveryCompany()).isEqualTo("顺丰快递");
        assertThat(omsOrderDetail_32L.getStatus()).isEqualTo(2);

        OmsOrderDetail omsOrderDetail_40L = omsOrderDao.getDetail(40L);
        assertThat(omsOrderDetail_40L.getId()).isEqualTo(40L);
        assertThat(omsOrderDetail_40L.getDeliverySn()).isEqualTo("222");
        assertThat(omsOrderDetail_40L.getDeliveryCompany()).isEqualTo("中通快递");
        assertThat(omsOrderDetail_40L.getStatus()).isEqualTo(2);

        OmsOrderDetail omsOrderDetail_63L = omsOrderDao.getDetail(63L);
        assertThat(omsOrderDetail_63L.getId()).isEqualTo(63L);
        assertThat(omsOrderDetail_63L.getDeliverySn()).isEqualTo("112");
        assertThat(omsOrderDetail_63L.getDeliveryCompany()).isEqualTo("顺丰快递");
        assertThat(omsOrderDetail_63L.getStatus()).isEqualTo(2);
    }
}
