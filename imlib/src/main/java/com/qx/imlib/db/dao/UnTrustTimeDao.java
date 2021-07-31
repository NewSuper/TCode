package com.qx.imlib.db.dao;

import com.qx.imlib.db.entity.TBUnTrustTime;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

//不信任时间表
@Dao
public interface UnTrustTimeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Long insert(TBUnTrustTime time);

    @Query("SELECT * FROM un_trust_time WHERE owner_id = :ownerId AND conversation_type = " +
            ":conversationType AND target_id = :targetId AND (:min < start_time  AND :max> start_time AND :max < end_time) LIMIT 1")
    TBUnTrustTime startsWith(String conversationType, String targetId, String ownerId, long min, long max);

    @Query("SELECT * FROM un_trust_time  WHERE owner_id = :ownerId AND conversation_type = " +
            ":conversationType AND target_id = :targetId AND (start_time < :min AND :min< end_time AND end_time < :max) LIMIT 1")
    TBUnTrustTime endsWith(String conversationType, String targetId, String ownerId, long min, long max);

    @Query("SELECT * FROM un_trust_time  WHERE owner_id = :ownerId AND conversation_type = " +
            ":conversationType AND target_id = :targetId AND (start_time < :min AND :min< end_time AND :max < end_time) LIMIT 1")
    TBUnTrustTime centresWith(String conversationType, String targetId, String ownerId, long min, long max);

    @Query("SELECT * FROM un_trust_time  WHERE owner_id = :ownerId AND conversation_type = " +
            ":conversationType AND target_id = :targetId AND (:min < start_time AND :max > end_time) LIMIT 1")
    TBUnTrustTime outSidesWith(String conversationType, String targetId, String ownerId, long min, long max);

    @Query("UPDATE un_trust_time SET start_time = :max WHERE owner_id = :ownerId AND conversation_type = " +
            ":conversationType AND target_id = :targetId AND (:min < start_time  AND :max> start_time AND :max < end_time)")
    long startsWithAndUpdate(String conversationType, String targetId, String ownerId, long min, long max);

    @Query("UPDATE un_trust_time SET end_time = :min WHERE owner_id = :ownerId AND conversation_type = " +
            ":conversationType AND target_id = :targetId AND (start_time < :min AND :min< end_time AND end_time < :max)")
    long endsWithAndUpdate(String conversationType, String targetId, String ownerId, long min, long max);

    @Query("SELECT * FROM un_trust_time WHERE owner_id = :ownerId AND conversation_type = " +
            ":conversationType AND target_id = :targetId AND (start_time <= :min AND start_time <= :max AND end_time >= :max AND end_time >= :min)")
    List<TBUnTrustTime> containsWithAndUpdate(String conversationType, String targetId, String ownerId, long min, long max);

    @Query("DELETE FROM un_trust_time WHERE owner_id = :ownerId AND conversation_type = " +
            ":conversationType AND target_id = :targetId AND (start_time >= :min AND end_time <= :max)")
    int beContain(String conversationType, String targetId, String ownerId, long min, long max);

    @Query("DELETE FROM un_trust_time WHERE id = :id")
    int delete(long id);

    @Query("DELETE FROM un_trust_time WHERE target_id = :targetId")
    int deleteByTargetId(String targetId);

    @Query("SELECT * FROM un_trust_time WHERE owner_id = :ownerId AND conversation_type = " +
            ":conversationType AND target_id = :targetId AND ((start_time <= :min AND end_time >= :min) OR (start_time <= :max AND end_time >= :max) OR (start_time >= :min AND end_time <= :max))")
    List<TBUnTrustTime> containsWithInUnTrustTimes(String conversationType, String targetId, String ownerId, long min, long max);

    @Query("SELECT * FROM un_trust_time WHERE owner_id = :ownerId AND conversation_type = :conversationType AND target_id = :targetId ORDER BY start_time ASC")
    List<TBUnTrustTime> getConversationAllUnTrustTimes(String conversationType, String targetId, String ownerId);
}
