package com.quip.backend.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.file.model.File;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper extends BaseMapper<File> {
}
