package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.VoucherPageQueryDTO;
import com.sky.entity.Voucher;
import com.sky.vo.AdminVoucherVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface VoucherMapper {

    /**
     * 根据id查询优惠券
     * @param id
     * @return
     */
    @Select("select * from voucher where id = #{id}")
    Voucher getById(Long id);

    /**
     * 乐观锁扣减库存：只有库存大于0才扣减，返回影响行数
     * @param id
     * @return 影响行数：1=扣减成功，0=库存已不足（超卖拦截）
     */
    @Update("update voucher set stock = stock - 1 where id = #{id} and stock > 0")
    int deductStock(Long id);

    /**
     * 上架中、且当前处于活动期内的优惠券（用户端领券中心用）
     * @return 券列表
     */
    @Select("select * from voucher where status = 1 and begin_time <= now() and end_time >= now() order by id desc")
    List<Voucher> listAvailable();

    /* ---------------- 管理端 ---------------- */

    /**
     * 分页查询（PageHelper 会自动拼 limit），顺带把已领取张数带出来
     * @param queryDTO 名称 / 状态 / 页码
     * @return 券列表
     */
    @Select("<script>" +
            "select v.id, v.name, v.value, v.min_amount as minAmount, v.stock, v.status, " +
            "v.begin_time as beginTime, v.end_time as endTime, v.create_time as createTime, " +
            "(select count(*) from voucher_order o where o.voucher_id = v.id) as receivedCount " +
            "from voucher v " +
            "<where>" +
            "<if test='name != null'>and v.name like concat('%', #{name}, '%')</if>" +
            "<if test='status != null'>and v.status = #{status}</if>" +
            "</where> " +
            "order by v.id desc" +
            "</script>")
    Page<AdminVoucherVO> pageQuery(VoucherPageQueryDTO queryDTO);

    /**
     * 新增优惠券
     * @param voucher
     */
    @Insert("insert into voucher (name, value, min_amount, stock, status, begin_time, end_time, create_time, update_time) " +
            "values (#{name}, #{value}, #{minAmount}, #{stock}, #{status}, #{beginTime}, #{endTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Voucher voucher);

    /**
     * 修改优惠券（只更新传了值的字段）
     * @param voucher
     */
    @Update("<script>" +
            "update voucher " +
            "<set>" +
            "<if test='name != null'>name = #{name},</if>" +
            "<if test='value != null'>value = #{value},</if>" +
            "<if test='minAmount != null'>min_amount = #{minAmount},</if>" +
            "<if test='stock != null'>stock = #{stock},</if>" +
            "<if test='status != null'>status = #{status},</if>" +
            "<if test='beginTime != null'>begin_time = #{beginTime},</if>" +
            "<if test='endTime != null'>end_time = #{endTime},</if>" +
            "update_time = now()," +
            "</set>" +
            "where id = #{id}" +
            "</script>")
    void update(Voucher voucher);

    /**
     * 上架 / 下架
     * @param id
     * @param status 1上架 0下架
     */
    @Update("update voucher set status = #{status}, update_time = now() where id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 删除优惠券
     * @param id
     */
    @Delete("delete from voucher where id = #{id}")
    void deleteById(Long id);
}
