package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.BannerSaveRequest;
import com.freshmeal.dto.admin.NoticeSaveRequest;
import com.freshmeal.vo.admin.AdminBannerVO;
import com.freshmeal.vo.admin.AdminNoticeVO;
import com.freshmeal.vo.user.BannerVO;
import com.freshmeal.vo.user.NoticeVO;
import java.util.List;

public interface ContentService {

    List<BannerVO> activeBanners();

    List<NoticeVO> activeNotices();

    PageResult<AdminBannerVO> pageBanners();

    Long createBanner(BannerSaveRequest request);

    void updateBanner(Long id, BannerSaveRequest request);

    void deleteBanner(Long id);

    PageResult<AdminNoticeVO> pageNotices();

    Long createNotice(NoticeSaveRequest request);

    void updateNotice(Long id, NoticeSaveRequest request);

    void deleteNotice(Long id);
}
