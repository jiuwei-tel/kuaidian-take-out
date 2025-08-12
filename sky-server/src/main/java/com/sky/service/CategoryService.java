package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

import java.util.List;

public interface CategoryService {


    Category save(CategoryDTO categoryDTO);


    PageResult page(CategoryPageQueryDTO categoryPageQueryDTO);

    void deleteById(long id);

    Category update(CategoryDTO categoryDTO);

    void startOrStop(Integer status, Long id);

    List<Category> list(Integer type);
}
