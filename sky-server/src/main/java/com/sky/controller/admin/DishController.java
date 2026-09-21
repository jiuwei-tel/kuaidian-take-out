package com.sky.controller.admin;


import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.EmployeeService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "菜品相关接口")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 新增菜品和口味
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品:{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);

        //将所有的菜品数据清理掉，所有以dish_开头的key
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);

        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询:{}",dishPageQueryDTO);
        PageResult pageResult = dishService.page(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 菜品批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result delete(@RequestParam List<Long> ids){
        log.info("菜品批量删除:{}",ids);
        dishService.deleteBatch(ids);

        //将所有的菜品数据清理掉，所有以dish_开头的key
        cleanCache("dish_*");

        return Result.success();
    }


    /**
     * 根据id查询菜品和对应的口味
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品:{}",id);
        DishVO dishVO = dishService.getById(id);
        return Result.success(dishVO);
    }


    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("更新菜品:{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);

        //将所有的菜品数据清理掉，所有以dish_开头的key
        cleanCache("dish_*");

        return Result.success();
    }


    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result StartOrStop(@PathVariable("status") Integer status, Long id){
        log.info("根据id启动或停止菜品：{}, {}", status, id);
        dishService.startOrStop(status,id);

        //将所有的菜品数据清理掉，所有以dish_开头的key
        cleanCache("dish_*");

        return Result.success();
    }


    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {

        //构造redis当中得key，规则：dish_分类id
        String key = "dish_" + categoryId;

        //查询redis中是否存在菜品数据(缓存是否存在)
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);

        if(list!=null && list.size() > 0){
            //如果存在，直接返回，无需查询数据库
            return Result.success(list);
        }


        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        //如果不存在，查询数据库，将查询结果放入redis中
        //增删改、起售停售时会清缓存，TTL 只是兜底，防漏清
        list = dishService.listWithFlavor(dish);
        redisTemplate.opsForValue().set(key, list, 30, TimeUnit.MINUTES);

        return Result.success(list);
    }


    /**
     * 清理缓存数据
     * @param pattern
     */
    public void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

}
