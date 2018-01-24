/**
 * 
 */
package com.alldream.manage.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alldream.manage.bean.AomengAppointment;
import com.alldream.manage.bean.vo.AomengAppointmentVo;
import com.alldream.manage.mapper.AomengAppointmentMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

/**
 * @author Administrator
 *
 */
@Service
public class AomengAppointmentService extends BaseService<AomengAppointment> {
	@Autowired
	private AomengAppointmentMapper aomengAppointmentMapper;

	public PageInfo<AomengAppointmentVo> queryByPage(Integer page, int rows,
			Map<String, Object> param) {
		PageHelper.startPage(page, rows);
		List<AomengAppointmentVo> list = aomengAppointmentMapper
				.queryByPage(param);
		return new PageInfo<AomengAppointmentVo>(list);
	}

	public AomengAppointmentVo queryVoById(String id) {
		return aomengAppointmentMapper.queryVoById(id);
	}

}
