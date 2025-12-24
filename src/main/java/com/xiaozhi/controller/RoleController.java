package com.xiaozhi.controller;

import com.github.pagehelper.PageInfo;
import com.xiaozhi.common.web.ResultMessage;
import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.dialogue.tts.factory.TtsServiceFactory;
import com.xiaozhi.dto.param.RoleAddParam;
import com.xiaozhi.dto.param.RoleUpdateParam;
import com.xiaozhi.dto.param.TestVoiceParam;
import com.xiaozhi.dto.response.RoleDTO;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.entity.SysRole;
import com.xiaozhi.service.SysConfigService;
import com.xiaozhi.service.SysRoleService;
import com.xiaozhi.utils.CmsUtils;
import com.xiaozhi.utils.DtoConverter;

import cn.dev33.satoken.stp.StpUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理
 * 
 * @author Joey
 * 
 */

@RestController
@RequestMapping("/api/role")
@Tag(name = "角色管理", description = "角色相关操作")
public class RoleController extends BaseController {

    /**
     * 统一从 Sa-Token 获取当前登录用户ID（数据库自增 int）
     */
    private Integer currentUserIdOrNull() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            return StpUtil.getLoginIdAsInt();
        } catch (Exception e) {
            logger.error("[AUTH] getLoginIdAsInt failed, loginId={} token={} ", StpUtil.getLoginId(), StpUtil.getTokenValue(), e);
            return null;
        }
    }

    @Resource
    private SysRoleService roleService;

    @Resource
    private TtsServiceFactory ttsService;

    @Resource
    private SysConfigService configService;

    /**
     * 角色查询
     *
     * @param role
     * @return roleList
     */
    @GetMapping("")
    @ResponseBody
    @Operation(summary = "根据条件查询角色信息", description = "返回角色信息列表")
    public ResultMessage list(SysRole role, HttpServletRequest request) {
        try {
            PageFilter pageFilter = initPageFilter(request);
            Integer uid = currentUserIdOrNull();
            if (uid == null) {
                // 兼容旧逻辑：如果 CmsUtils 能取到就用（但优先 Sa-Token）
                uid = CmsUtils.getUserId();
            }
            if (uid == null) {
                return ResultMessage.error("未登录或登录态异常：无法获取 userId");
            }
            role.setUserId(uid);
            List<SysRole> roleList = roleService.query(role, pageFilter);

            // 转换为DTO
            List<RoleDTO> roleDTOList = DtoConverter.toRoleDTOList(roleList);

            ResultMessage result = ResultMessage.success();
            result.put("data", new PageInfo<>(roleDTOList));
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResultMessage.error();
        }
    }

    /**
     * 角色信息更新
     *
     * @param roleId 角色ID
     * @param param 更新参数
     * @return
     */
    @PutMapping("/{roleId}")
    @ResponseBody
    @Operation(summary = "更新角色信息", description = "更新语音助手角色配置")
    public ResultMessage update(@PathVariable Integer roleId, @Valid @RequestBody RoleUpdateParam param) {
        try {
            Integer uid = currentUserIdOrNull();
            if (uid == null) {
                uid = CmsUtils.getUserId();
            }
            if (uid == null) {
                return ResultMessage.error("未登录或登录态异常：无法获取 userId");
            }

            SysRole role = new SysRole();
            role.setRoleId(roleId);
            role.setRoleName(param.getRoleName());
            role.setRoleDesc(param.getRoleDesc());
            role.setAvatar(param.getAvatar());
            role.setRoleSound(param.getRoleSound());
            role.setVoiceName(param.getVoiceName());
            role.setTtsPitch(param.getTtsPitch());
            role.setTtsSpeed(param.getTtsSpeed());
            role.setState(param.getState());
            role.setTtsId(param.getTtsId());
            role.setModelId(param.getModelId());
            role.setModelName(param.getModelName());
            role.setSttId(param.getSttId());
            role.setTemperature(param.getTemperature());
            role.setTopP(param.getTopP());
            role.setVadEnergyTh(param.getVadEnergyTh());
            role.setVadSpeechTh(param.getVadSpeechTh());
            role.setVadSilenceTh(param.getVadSilenceTh());
            role.setVadSilenceMs(param.getVadSilenceMs());
            role.setModelProvider(param.getModelProvider());
            role.setTtsProvider(param.getTtsProvider());
            role.setIsDefault(param.getIsDefault());
            role.setDatasetId(param.getDatasetId());
            role.setUserId(uid);

            roleService.update(role);

            // 返回更新后的角色信息
            SysRole updatedRole = roleService.selectRoleById(roleId);
            return ResultMessage.success(DtoConverter.toRoleDTO(updatedRole));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResultMessage.error();
        }
    }

    /**
     * 添加角色
     *
     * @param param 添加参数
     */
    @PostMapping("")
    @ResponseBody
    @Operation(summary = "添加角色信息", description = "添加新的语音助手角色")
    public ResultMessage create(@Valid @RequestBody RoleAddParam param) {
        try {
            // 1) 强制要求已登录（否则别走到 SQL）
            if (!StpUtil.isLogin()) {
                logger.error("[CREATE ROLE] NOT LOGIN, token={}", StpUtil.getTokenValue());
                return ResultMessage.error("未登录：请先登录后再创建角色");
            }

            // 2) 强制拿到 userId(int)
            Integer uid;
            try {
                uid = StpUtil.getLoginIdAsInt();
            } catch (Exception ex) {
                logger.error("[CREATE ROLE] loginId is not int, loginId={}", StpUtil.getLoginId(), ex);
                return ResultMessage.error("登录态异常：loginId 不是 int，请检查登录接口 StpUtil.login 是否传入 userId(int)");
            }
            if (uid == null) {
                logger.error("[CREATE ROLE] uid is null, loginId={}, token={}", StpUtil.getLoginId(), StpUtil.getTokenValue());
                return ResultMessage.error("登录态异常：未获取到 userId");
            }

            SysRole role = new SysRole();
            role.setRoleName(param.getRoleName());
            role.setRoleDesc(param.getRoleDesc());
            role.setAvatar(param.getAvatar());
            role.setRoleSound(param.getRoleSound());
            role.setVoiceName(param.getVoiceName());
            role.setTtsPitch(param.getTtsPitch());
            role.setTtsSpeed(param.getTtsSpeed());
            role.setState(param.getState());
            role.setTtsId(param.getTtsId());
            role.setModelId(param.getModelId());
            role.setModelName(param.getModelName());
            role.setSttId(param.getSttId());
            role.setTemperature(param.getTemperature());
            role.setTopP(param.getTopP());
            role.setVadEnergyTh(param.getVadEnergyTh());
            role.setVadSpeechTh(param.getVadSpeechTh());
            role.setVadSilenceTh(param.getVadSilenceTh());
            role.setVadSilenceMs(param.getVadSilenceMs());
            role.setModelProvider(param.getModelProvider());
            role.setTtsProvider(param.getTtsProvider());
            role.setIsDefault(param.getIsDefault());
            role.setDatasetId(param.getDatasetId());

            // 3) 关键：绑定当前登录用户 userId
            role.setUserId(uid);
            logger.info("[CREATE ROLE] FINAL uid={} token={} loginId={}", uid, StpUtil.getTokenValue(), StpUtil.getLoginId());

            roleService.add(role);

            // 返回新增的角色信息
            return ResultMessage.success(DtoConverter.toRoleDTO(role));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResultMessage.error();
        }
    }

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     * @return
     */
    @DeleteMapping("/{roleId}")
    @ResponseBody
    @Operation(summary = "删除角色信息", description = "删除指定的语音助手角色")
    public ResultMessage delete(@PathVariable Integer roleId) {
        try {
            Integer uid = currentUserIdOrNull();
            if (uid == null) {
                uid = CmsUtils.getUserId();
            }
            if (uid == null) {
                return ResultMessage.error("未登录或登录态异常：无法获取 userId");
            }

            // 验证角色是否属于当前用户
            SysRole role = roleService.selectRoleById(roleId);
            if (role == null) {
                return ResultMessage.error("角色不存在");
            }
            logger.info("[DELETE ROLE] uid={}, roleUserId={}, roleId={}", uid, role.getUserId(), roleId);
            if (role.getUserId() == null || !role.getUserId().equals(uid)) {
                return ResultMessage.error("无权删除该角色");
            }

            // 业务规则：published=1（已发布/端侧引用）禁止删除
            if (role.getPublished() != null && role.getPublished() == 1) {
                return ResultMessage.error("该角色已发布到端侧，无法删除。请先取消发布后再删除。");
            }

            roleService.deleteById(roleId);
            return ResultMessage.success("删除成功");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResultMessage.error("删除失败");
        }
    }

    @GetMapping("/testVoice")
    @ResponseBody
    @Operation(summary = "测试语音合成", description = "测试指定配置的语音合成效果")
    public ResultMessage testAudio(@Valid TestVoiceParam param) {
        SysConfig config = null;
        try {
            if (!param.getProvider().equals("edge")) {
                config = configService.selectConfigById(param.getTtsId());
            }
            String audioFilePath = ttsService.getTtsService(config, param.getVoiceName(), param.getTtsPitch(), param.getTtsSpeed())
                    .textToSpeech(param.getMessage());

            ResultMessage result = ResultMessage.success();
            result.put("data", audioFilePath);
            return result;
        } catch (IndexOutOfBoundsException e) {
            logger.error(e.getMessage(), e);
            return ResultMessage.error("请先到语音合成配置页面配置对应Key");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResultMessage.error();
        }
    }
}