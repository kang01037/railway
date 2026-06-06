package com.railway.user.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.util.IdCardValidator;
import com.railway.common.util.SnowflakeIdWorker;
import com.railway.common.util.UserContext;
import com.railway.user.dto.query.PassengerQuery;
import com.railway.user.dto.request.PassengerReq;
import com.railway.user.entity.PassengerDO;
import com.railway.user.mapper.PassengerMapper;
import com.railway.user.service.PassengerService;
import com.railway.user.vo.PassengerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerMapper passengerMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PassengerReq req) {
        Long userId = UserContext.mustCurrentUserId();

        // 身份证校验
        if (!IdCardValidator.isValid(req.getIdCardNo())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "证件号不合法");
        }

        PassengerDO p = new PassengerDO();
        p.setId(snowflakeIdWorker.nextId());
        p.setUserId(userId);
        p.setName(req.getName());
        p.setIdCardType(1);  // 默认身份证
        p.setIdCardNo(req.getIdCardNo());
        p.setPhone(req.getPhone());
        p.setPassengerType(req.getPassengerType() == null ? 0 : req.getPassengerType());
        p.setIsDefault(req.getIsDefault() == null ? 0 : req.getIsDefault());
        p.setCreateTime(LocalDateTime.now());
        p.setUpdateTime(LocalDateTime.now());

        // 若是默认乘车人，先把同用户其他默认取消
        if (Integer.valueOf(1).equals(p.getIsDefault())) {
            passengerMapper.clearDefaultByUserId(userId);
        }

        passengerMapper.insert(p);
        log.info("新增乘车人 userId={} passengerId={} name={}", userId, p.getId(), p.getName());
        return p.getId();
    }

    @Override
    public PageInfo<PassengerVO> page(PassengerQuery query) {
        Long userId = UserContext.mustCurrentUserId();

        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<PassengerDO> list = passengerMapper.listByUserId(userId);
        PageInfo<PassengerDO> page = new PageInfo<>(list);

        // DO -> VO（身份证脱敏）
        List<PassengerVO> voList = list.stream().map(this::toVO).toList();
        PageInfo<PassengerVO> result = new PageInfo<>(voList);
        result.setTotal(page.getTotal());
        result.setPageNum(page.getPageNum());
        result.setPageSize(page.getPageSize());
        result.setPages(page.getPages());
        return result;
    }

    @Override
    public PassengerVO getById(Long id) {
        Long userId = UserContext.mustCurrentUserId();
        PassengerDO p = passengerMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.PASSENGER_NOT_FOUND);
        }
        if (!p.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该乘车人");
        }
        return toVO(p);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PassengerReq req) {
        Long userId = UserContext.mustCurrentUserId();
        PassengerDO exist = passengerMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ErrorCode.PASSENGER_NOT_FOUND);
        }
        if (!exist.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能修改自己的乘车人");
        }
        if (StringUtils.hasText(req.getIdCardNo()) && !IdCardValidator.isValid(req.getIdCardNo())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "证件号不合法");
        }

        PassengerDO update = new PassengerDO();
        update.setId(id);
        update.setName(req.getName());
        update.setIdCardNo(req.getIdCardNo());
        update.setPhone(req.getPhone());
        update.setPassengerType(req.getPassengerType());
        update.setIsDefault(req.getIsDefault());
        update.setUpdateTime(LocalDateTime.now());

        // 若改为默认，先把同用户其他默认取消
        if (Integer.valueOf(1).equals(update.getIsDefault())) {
            passengerMapper.clearDefaultByUserId(userId);
        }

        passengerMapper.updateById(update);
        log.info("修改乘车人 userId={} passengerId={}", userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = UserContext.mustCurrentUserId();
        PassengerDO exist = passengerMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ErrorCode.PASSENGER_NOT_FOUND);
        }
        if (!exist.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能删除自己的乘车人");
        }
        passengerMapper.deleteById(id);
        log.info("删除乘车人 userId={} passengerId={}", userId, id);
    }

    private PassengerVO toVO(PassengerDO p) {
        PassengerVO vo = new PassengerVO();
        vo.setId(p.getId());
        vo.setUserId(p.getUserId());
        vo.setName(p.getName());
        vo.setIdCardType(p.getIdCardType());
        vo.setIdCardNo(IdCardValidator.mask(p.getIdCardNo()));
        vo.setPhone(p.getPhone());
        vo.setPassengerType(p.getPassengerType());
        vo.setIsDefault(p.getIsDefault());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }
}
