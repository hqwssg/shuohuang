package cn.com.v2.service.impl;

import cn.com.v2.model.GoviewProjectData;
import cn.com.v2.mapper.GoviewProjectDataMapper;
import cn.com.v2.service.IGoviewProjectDataService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author fc
 * @since 2023-04-30
 */
@Service
public class GoviewProjectDataServiceImpl extends ServiceImpl<GoviewProjectDataMapper, GoviewProjectData> implements IGoviewProjectDataService {
	@Autowired
	GoviewProjectDataMapper dataMapper;
	@Override
	public GoviewProjectData getProjectid(String projectId) {
		return dataMapper.selectOne(new QueryWrapper<GoviewProjectData>().eq("project_id", projectId));
		
	}

}
