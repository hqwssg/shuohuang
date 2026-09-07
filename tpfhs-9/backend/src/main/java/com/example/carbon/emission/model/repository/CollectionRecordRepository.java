package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.CollectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 采集记录数据访问接口
 */
@Repository
public interface CollectionRecordRepository extends JpaRepository<CollectionRecord, Long> {

    /**
     * 根据采集点类型和采集点ID查询历史采集记录
     *
     * @param collectionPointType 采集点类型：1-电力表，2-化石燃料，3-外购热能
     * @param collectionPointId 采集点ID
     * @return 采集记录列表（按id升序）
     */
    List<CollectionRecord> findByCollectionPointTypeAndCollectionPointId(
            Integer collectionPointType, Long collectionPointId);

    /**
     * 根据采集点类型和采集点ID查询历史采集记录，按创建时间倒序（最新在前）
     *
     * @param collectionPointType 采集点类型
     * @param collectionPointId 采集点ID
     * @return 采集记录列表（最新记录在前）
     */
    List<CollectionRecord> findByCollectionPointTypeAndCollectionPointIdOrderByIdDesc(
            Integer collectionPointType, Long collectionPointId);

    /**
     * 根据采集时间范围查询采集记录
     *
     * @param startTime 起始时间
     * @param endTime 截止时间
     * @return 采集记录列表
     */
    List<CollectionRecord> findByCollectionTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据采集数据状态查询记录
     *
     * @param collectionStatus 采集数据状态：0-还未执行读取操作，1-读取成功，2-读取失败，3-多次读取失败后取消
     * @return 采集记录列表
     */
    List<CollectionRecord> findByCollectionStatus(Integer collectionStatus);

    /**
     * 根据采集数据状态集合查询记录
     * 用于遍历待处理记录（status=0 未读取）及失败需重读记录（status=2 读取失败）
     *
     * @param collectionStatuses 采集数据状态集合
     * @return 采集记录列表
     */
    List<CollectionRecord> findByCollectionStatusIn(Collection<Integer> collectionStatuses);

    /**
     * 根据采集点类型、采集点ID和采集数据状态查询记录
     * 用于扫描时判断某采集点是否已存在未处理的待采集记录（status=0 或 2），避免重复插入
     *
     * @param collectionPointType 采集点类型：1-电力表，2-化石燃料，3-外购热能
     * @param collectionPointId 采集点ID
     * @param collectionStatus 采集数据状态
     * @return 采集记录列表
     */
    List<CollectionRecord> findByCollectionPointTypeAndCollectionPointIdAndCollectionStatus(
            Integer collectionPointType, Long collectionPointId, Integer collectionStatus);
}
