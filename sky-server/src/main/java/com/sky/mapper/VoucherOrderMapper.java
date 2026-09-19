package com.sky.mapper;

import com.sky.entity.VoucherOrder;
import com.sky.vo.UserVoucherVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VoucherOrderMapper {

    /**
     * 查询用户是否已抢过该优惠券（一人一单防重）
     * @param voucherId
     * @param userId
     * @return 记录数：>0 表示已抢过
     */
    @Select("select count(*) from voucher_order where voucher_id = #{voucherId} and user_id = #{userId}")
    int countByVoucherIdAndUserId(@Param("voucherId") Long voucherId, @Param("userId") Long userId);

    /**
     * 插入秒杀订单（回填自增id，秒杀接口要把订单id返回给前端）
     * @param voucherOrder
     */
    @Insert("insert into voucher_order (voucher_id, user_id, status, create_time) " +
            "values (#{voucherId}, #{userId}, #{status}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(VoucherOrder voucherOrder);

    /**
     * 用户抢到的券（带券的信息），未使用的排前面
     * @param userId
     * @return 我的券列表
     */
    @Select("select o.id, o.voucher_id as voucherId, v.name, v.value, v.min_amount as minAmount, " +
            "o.status, o.create_time as createTime, o.used_time as usedTime, " +
            "v.begin_time as beginTime, v.end_time as endTime " +
            "from voucher_order o join voucher v on o.voucher_id = v.id " +
            "where o.user_id = #{userId} order by o.status asc, o.create_time desc")
    List<UserVoucherVO> listByUserId(Long userId);

    /**
     * 查一条领取记录（限定必须属于该用户），下单用券时校验
     * @param id 领取记录id
     * @param userId
     * @return 记录；不存在或不属于该用户时为 null
     */
    @Select("select o.id, o.voucher_id as voucherId, v.name, v.value, v.min_amount as minAmount, " +
            "o.status, o.create_time as createTime, o.used_time as usedTime, " +
            "v.begin_time as beginTime, v.end_time as endTime " +
            "from voucher_order o join voucher v on o.voucher_id = v.id " +
            "where o.id = #{id} and o.user_id = #{userId}")
    UserVoucherVO getByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 用户抢过的券id集合（领券中心标记「已抢过」用）
     * @param userId
     * @return 券id列表
     */
    @Select("select voucher_id from voucher_order where user_id = #{userId}")
    List<Long> listVoucherIdsByUserId(Long userId);

    /**
     * 核销优惠券：只把「未使用」改成「已使用」。
     * 条件更新 + 影响行数判断，同一张券并发下两单只会成功一次。
     * @param id 领取记录id
     * @param userId
     * @param orderId 使用该券的订单id
     * @param usedTime 使用时间
     * @return 影响行数：1=核销成功；0=已被用过或不属于该用户
     */
    @Update("update voucher_order set status = 1, used_time = #{usedTime}, order_id = #{orderId} " +
            "where id = #{id} and user_id = #{userId} and status = 0")
    int use(@Param("id") Long id, @Param("userId") Long userId,
            @Param("orderId") Long orderId, @Param("usedTime") LocalDateTime usedTime);

    /**
     * 该券已被领取的张数（管理端列表用）
     * @param voucherId
     * @return 张数
     */
    @Select("select count(*) from voucher_order where voucher_id = #{voucherId}")
    int countByVoucherId(Long voucherId);
}
