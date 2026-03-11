package com.example.music.service.impl;

import com.example.music.model.entity.Genre;
import com.example.music.mapper.GenreMapper;
import com.example.music.service.IGenreService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
  
 */
@Service
public class GenreServiceImpl extends ServiceImpl<GenreMapper, Genre> implements IGenreService {

}
