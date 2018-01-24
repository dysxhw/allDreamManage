/**
 * 
 */
package com.alldream.manage.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.alldream.manage.annotation.AomengHandleLogAnnotation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import com.alldream.manage.bean.AomengAdmin;
import com.alldream.manage.bean.AomengAppointment;
import com.alldream.manage.bean.AomengStudent;
import com.alldream.manage.bean.vo.AomengAppointmentVo;
import com.alldream.manage.common.utils.Constant;
import com.alldream.manage.common.utils.OSSUtil;
import com.alldream.manage.exception.BusinessException;
import com.alldream.manage.service.AomengAppointmentService;
import com.alldream.manage.service.AomengStudentService;
import com.github.pagehelper.PageInfo;

/**
 * @author Administrator
 *
 */
@Controller
@RequestMapping("appointment")
public class AomengAppointmentController extends BaseController {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(AomengAppointmentController.class);
	@Autowired
	private AomengAppointmentService aomengAppointmentService;
	@Autowired
	private AomengStudentService aomengStudentService;

	private String validate(AomengAppointment appointment,
			CommonsMultipartFile picture, String oper) throws BusinessException {
		if (null == appointment) {
			return "获取预约信息失败";
		}
		if (StringUtils.isBlank(appointment.getAppointmentName())) {
			return "预约名称不能为空";
		}
		if ("add".equals(oper)) {
			appointment.setId(createUUID());
		}
		if (picture != null && !picture.isEmpty()) {
			// 保存
			try {
				String fileName = Constant.OSS_PICTURE
						+ "/"
						+ appointment.getId()
						+ picture.getOriginalFilename().substring(
								picture.getOriginalFilename().lastIndexOf("."));
				appointment.setPictureUrl(fileName);
				// 上传到oss
				OSSUtil.uploadFile(fileName, picture.getInputStream(),
						picture.getSize());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@RequestMapping(value = "edit", produces = "application/json; charset=utf-8")
	@ResponseBody
	@AomengHandleLogAnnotation(description = "编辑预约信息")
	public Object editCourseCatagory(
			@RequestParam(value = "oper", required = true) String oper,
			@RequestParam(required = false) CommonsMultipartFile picture,
			AomengAppointment appointment, HttpServletRequest request) {
		if (StringUtils.isNotBlank(oper)) {
			try {
				if ("del".equals(oper)) {
					// 删除操作
					List<Object> idlist = getDeleteIds(appointment.getId());
					for (Object id : idlist) {
						Map<String, Object> param = new HashMap<String, Object>();
						param.put("appointmentId", id);
						PageInfo<AomengStudent> pageInfo = aomengStudentService
								.queryStudentAppointmentByPage(1, 1, param);
						if (pageInfo != null && pageInfo.getTotal() > 0) {
							return error("已有学生预约，无法删除");
						} else {
							aomengAppointmentService.deleteById(id);
						}
					}
					return success("删除成功");
				} else {
					String msg = validate(appointment, picture, oper);
					if (StringUtils.isNotBlank(msg)) {
						return error(msg);
					}
					AomengAdmin user = (AomengAdmin) request.getSession()
							.getAttribute("user");
					if ("add".equals(oper)) {
						// 添加操作
						appointment.setCreateTime(new Date());
						appointment.setCreateName(user.getName());
						appointment.setUpdateTime(new Date());
						appointment.setUpdateName(user.getName());
						aomengAppointmentService.insert(appointment);
						return success(msg);
					} else if ("edit".equals(oper)) {
						// 编辑操作
						appointment.setUpdateTime(new Date());
						appointment.setUpdateName(user.getName());
						aomengAppointmentService.updateSelective(appointment);
						return success(msg);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(oper + "[预约]" + e.getMessage());
				return error(e.getMessage());
			}
		}
		return error("系统内部错误");
	}

	@RequestMapping(value = "list", method = RequestMethod.GET)
	public ResponseEntity<PageInfo<AomengAppointmentVo>> listCourseCatagory(
			String name, String student, String teacher, String studentId,
			@RequestParam(value = "page", defaultValue = "1") Integer page,
			@RequestParam(value = "rows", defaultValue = "10") int rows) {
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			if (StringUtils.isNotBlank(name)) {
				param.put("name", "%" + name + "%");
			}
			if (StringUtils.isNotBlank(student)) {
				param.put("student", "%" + student + "%");
			}
			if (StringUtils.isNotBlank(studentId)) {
				param.put("studentId", studentId);
			}
			if (StringUtils.isNotBlank(teacher)) {
				param.put("teacher", teacher);
			}
			PageInfo<AomengAppointmentVo> pageInfo = aomengAppointmentService
					.queryByPage(page, rows, param);
			return ResponseEntity.ok(pageInfo);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("查询预约列表失败！");
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
				null);
	}

	@RequestMapping(value = "get", method = RequestMethod.GET)
	@ResponseBody
	public Object getById(@RequestParam(value = "id", required = true) String id) {
		AomengAppointmentVo app = null;
		try {
			app = aomengAppointmentService.queryVoById(id);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("查询预约列表失败！");
			return error(e.getMessage());
		}
		return success(app);
	}

	@RequestMapping(value = "studentapp", method = RequestMethod.GET)
	@ResponseBody
	public Object getStudentAppointment(HttpServletRequest request,
			String appName, String teacheName, String teacherId,
			String courseType, String studentName, String studentId,
			@RequestParam(value = "page", defaultValue = "1") Integer page,
			@RequestParam(value = "rows", defaultValue = "10") int rows) {
		PageInfo<AomengAppointmentVo> pageInfo = null;
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			if (StringUtils.isNotBlank(courseType)) {
				param.put("courseType", courseType);
			}
			if (StringUtils.isNotBlank(studentId)) {
				param.put("studentId", studentId);
			}
			if (StringUtils.isNotBlank(teacherId)) {
				param.put("teacher", teacherId);
			}
			if (StringUtils.isNotBlank(appName)) {
				param.put("name", "%" + appName + "%");
			}
			if (StringUtils.isNotBlank(studentName)) {
				param.put("student", "%" + studentName + "%");
			}
			if (StringUtils.isNotBlank(teacheName)) {
				param.put("teacheName", "%" + teacheName + "%");
			}
			pageInfo = aomengAppointmentService.queryByPage(page, rows, param);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("查询预约列表失败！");
			return error(e.getMessage());
		}
		return success(pageInfo);
	}
}
