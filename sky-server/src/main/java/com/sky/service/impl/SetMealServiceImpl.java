package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.mapper.SetMealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetMealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SetMealServiceImpl implements SetMealService {

    @Autowired
    private SetMealMapper setMealMapper;

    @Autowired
    private SetMealDishMapper setMealDishMapper;

    @Autowired
    private DishMapper dishMapper;


    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Override
    public void save(SetmealDTO setmealDTO) {
        log.info("新增套餐:{}",setmealDTO);

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //默认套餐为停用状态
        setmeal.setStatus(StatusConstant.DISABLE);

        //crateTime、updateTime、createUser、updateUser
        //已经通过AutoFill注解，无需手动设置

        //向套餐表插入1条数据
        setMealMapper.insert(setmeal);

        //获取生成的套餐id
        Long setmealId = setmeal.getId();

        //获取套餐菜品列表
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();

        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });

        //保存套餐与菜品之间的关联关系
        setMealDishMapper.insertBatch(setmealDishes);


    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());

        Page<SetmealVO> page = setMealMapper.pageQuery(setmealPageQueryDTO);
        long total = page.getTotal();
        List<SetmealVO> setmealVOS = page.getResult();
        return new PageResult(total, setmealVOS);
    }

    /**
     * 删除套餐
     * @param ids
     */
    @Override
    public void delete(List<Long> ids) {
        ids.forEach(id -> {
            Setmeal setmeal = setMealMapper.getById(id);

            if(setmeal.getStatus() == StatusConstant.ENABLE){
                //起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        ids.forEach(id -> {
            //删除套餐
            setMealMapper.delete(id);
            //删除套餐菜品关系
            setMealDishMapper.deleteBySetmealId(id);
        });

    }


    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {

        Setmeal setmeal = setMealMapper.getById(id);
        List<SetmealDish> setmealDishes = setMealDishMapper.getBySetmealId(id);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //1.修改套餐表
        setMealMapper.update(setmeal);

        //2.删除套餐与菜品的关联关系
        setMealDishMapper.deleteBySetmealId(setmealDTO.getId());

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealDTO.getId());
        });

        //3.重新插入套餐与菜品的关联关系
        setMealDishMapper.insertBatch(setmealDishes);

    }

    @Override
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if(status == StatusConstant.ENABLE){
            //select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = ?
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if(dishList != null && dishList.size() > 0){
                dishList.forEach(dish -> {
                    if(StatusConstant.DISABLE == dish.getStatus()){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setMealMapper.update(setmeal);
    }


}
