package com.school.classboard.repository;

import com.school.classboard.domain.Announcement;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** 某班级可见的公告：未过期、全校公告（classId 为空）+ 本班公告，置顶优先、时间倒序 */
    @Query("""
            select a from Announcement a
            where a.active = true
              and (a.expireAt is null or a.expireAt > :now)
              and (a.classId is null or a.classId = :classId)
            order by a.pinned desc, a.publishedAt desc
            """)
    List<Announcement> findVisibleToClass(@Param("classId") Long classId, @Param("now") LocalDateTime now);

    /** 把已经超过有效期的公告自动下线，返回处理数量 */
    @Modifying
    @Query("""
            update Announcement a
            set a.active = false
            where a.active = true
              and a.expireAt is not null
              and a.expireAt <= :now
            """)
    int archiveExpired(@Param("now") LocalDateTime now);

    /** 删除班级时一并清理该班的定向公告 */
    @Modifying
    @Query("delete from Announcement a where a.classId = :classId")
    int deleteByClassId(@Param("classId") Long classId);
}
