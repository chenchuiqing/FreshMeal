package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.BannerSaveRequest;
import com.freshmeal.dto.admin.NoticeSaveRequest;
import com.freshmeal.entity.Banner;
import com.freshmeal.entity.Notice;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.BannerMapper;
import com.freshmeal.mapper.NoticeMapper;
import com.freshmeal.service.ContentService;
import com.freshmeal.vo.admin.AdminBannerVO;
import com.freshmeal.vo.admin.AdminNoticeVO;
import com.freshmeal.vo.user.BannerVO;
import com.freshmeal.vo.user.NoticeVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    private final BannerMapper bannerMapper;
    private final NoticeMapper noticeMapper;

    @Override
    public List<BannerVO> activeBanners() {
        return bannerMapper.selectList(Wrappers.<Banner>lambdaQuery()
                        .eq(Banner::getStatus, STATUS_ENABLED)
                        .orderByAsc(Banner::getSort)
                        .orderByDesc(Banner::getId))
                .stream()
                .map(banner -> new BannerVO(
                        banner.getId(), banner.getTitle(), banner.getImage(),
                        banner.getLink()))
                .toList();
    }

    @Override
    public List<NoticeVO> activeNotices() {
        return noticeMapper.selectList(Wrappers.<Notice>lambdaQuery()
                        .eq(Notice::getStatus, STATUS_ENABLED)
                        .orderByDesc(Notice::getCreateTime)
                        .orderByDesc(Notice::getId)
                        .last("LIMIT 10"))
                .stream()
                .map(notice -> new NoticeVO(
                        notice.getId(), notice.getTitle(), notice.getContent(),
                        notice.getType(), notice.getCreateTime()))
                .toList();
    }

    @Override
    public PageResult<AdminBannerVO> pageBanners() {
        List<AdminBannerVO> records = bannerMapper.selectList(
                        Wrappers.<Banner>lambdaQuery()
                                .orderByAsc(Banner::getSort)
                                .orderByDesc(Banner::getId))
                .stream()
                .map(this::toAdminBanner)
                .toList();
        return PageResult.of(records, records.size(), 1,
                Math.max(records.size(), 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBanner(BannerSaveRequest request) {
        validateBanner(request);
        Banner banner = new Banner();
        applyBanner(banner, request);
        banner.setStatus(normalizeStatus(request.status(), STATUS_ENABLED));
        bannerMapper.insert(banner);
        return banner.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBanner(Long id, BannerSaveRequest request) {
        validateBanner(request);
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(),
                    ErrorCode.NOT_FOUND.getMessage());
        }
        applyBanner(banner, request);
        banner.setStatus(normalizeStatus(request.status(), banner.getStatus()));
        bannerMapper.updateById(banner);
    }

    @Override
    public void deleteBanner(Long id) {
        if (bannerMapper.deleteById(id) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(),
                    ErrorCode.NOT_FOUND.getMessage());
        }
    }

    @Override
    public PageResult<AdminNoticeVO> pageNotices() {
        List<AdminNoticeVO> records = noticeMapper.selectList(
                        Wrappers.<Notice>lambdaQuery()
                                .orderByDesc(Notice::getCreateTime)
                                .orderByDesc(Notice::getId))
                .stream()
                .map(this::toAdminNotice)
                .toList();
        return PageResult.of(records, records.size(), 1,
                Math.max(records.size(), 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNotice(NoticeSaveRequest request) {
        validateNotice(request);
        Notice notice = new Notice();
        applyNotice(notice, request);
        notice.setStatus(normalizeStatus(request.status(), STATUS_ENABLED));
        noticeMapper.insert(notice);
        return notice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(Long id, NoticeSaveRequest request) {
        validateNotice(request);
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(),
                    ErrorCode.NOT_FOUND.getMessage());
        }
        applyNotice(notice, request);
        notice.setStatus(normalizeStatus(request.status(), notice.getStatus()));
        noticeMapper.updateById(notice);
    }

    @Override
    public void deleteNotice(Long id) {
        if (noticeMapper.deleteById(id) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(),
                    ErrorCode.NOT_FOUND.getMessage());
        }
    }

    private void validateBanner(BannerSaveRequest request) {
        if (request.sort() != null && request.sort() < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "轮播排序不能为负数");
        }
    }

    private void validateNotice(NoticeSaveRequest request) {
        if (request.type() != null && (request.type() < 1 || request.type() > 3)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "公告类型不合法");
        }
    }

    private void applyBanner(Banner banner, BannerSaveRequest request) {
        banner.setTitle(request.title().trim());
        banner.setImage(request.image().trim());
        banner.setLink(StringUtils.hasText(request.link()) ? request.link().trim() : null);
        banner.setSort(request.sort() == null ? 0 : request.sort());
    }

    private void applyNotice(Notice notice, NoticeSaveRequest request) {
        notice.setTitle(request.title().trim());
        notice.setContent(StringUtils.hasText(request.content())
                ? request.content().trim() : null);
        notice.setType(request.type() == null ? 1 : request.type());
    }

    private int normalizeStatus(Integer status, int fallback) {
        if (status == null) {
            return fallback;
        }
        if (status != STATUS_ENABLED && status != STATUS_DISABLED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "状态只能为0或1");
        }
        return status;
    }

    private AdminBannerVO toAdminBanner(Banner banner) {
        return new AdminBannerVO(
                banner.getId(), banner.getTitle(), banner.getImage(),
                banner.getLink(), banner.getSort(), banner.getStatus(),
                banner.getCreateTime());
    }

    private AdminNoticeVO toAdminNotice(Notice notice) {
        return new AdminNoticeVO(
                notice.getId(), notice.getTitle(), notice.getContent(),
                notice.getType(), notice.getStatus(), notice.getCreateTime());
    }
}
