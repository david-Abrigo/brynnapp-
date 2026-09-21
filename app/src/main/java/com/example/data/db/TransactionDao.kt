package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.YapeTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("""
        SELECT * FROM yape_transactions 
        WHERE (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
        ORDER BY timestamp DESC
    """)
    fun getAllTransactions(storeCode: String = ""): Flow<List<YapeTransaction>>

    @Query("""
        SELECT * FROM yape_transactions 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
          AND (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
        ORDER BY timestamp DESC
    """)
    fun getTransactionsBetween(startTime: Long, endTime: Long, storeCode: String = ""): Flow<List<YapeTransaction>>

    @Query("""
        SELECT * FROM yape_transactions 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
          AND (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
        ORDER BY timestamp DESC
    """)
    fun getTransactionsBetweenSync(startTime: Long, endTime: Long, storeCode: String = ""): List<YapeTransaction>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM yape_transactions 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
          AND isStoreTransaction = 1 
          AND (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
    """)
    fun getTotalAmountBetween(startTime: Long, endTime: Long, storeCode: String = ""): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM yape_transactions 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
          AND isStoreTransaction = 1 
          AND (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
    """)
    fun getTotalAmountBetweenSync(startTime: Long, endTime: Long, storeCode: String = ""): Double

    @Query("""
        SELECT COUNT(*) FROM yape_transactions 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
          AND isStoreTransaction = 1 
          AND (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
    """)
    fun getCountBetween(startTime: Long, endTime: Long, storeCode: String = ""): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM yape_transactions 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
          AND isStoreTransaction = 1 
          AND (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
    """)
    fun getCountBetweenSync(startTime: Long, endTime: Long, storeCode: String = ""): Int

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM yape_transactions 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
          AND isStoreTransaction = 0 
          AND (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
    """)
    fun getExcludedTotalAmountBetween(startTime: Long, endTime: Long, storeCode: String = ""): Flow<Double>

    @Query("""
        SELECT COUNT(*) FROM yape_transactions 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
          AND isStoreTransaction = 0 
          AND (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
    """)
    fun getExcludedCountBetween(startTime: Long, endTime: Long, storeCode: String = ""): Flow<Int>

    @Query("UPDATE yape_transactions SET isStoreTransaction = :isStore, exclusionReason = :reason WHERE id = :id")
    suspend fun updateStoreTransactionStatus(id: Long, isStore: Boolean, reason: String)

    @Query("""
        SELECT * FROM yape_transactions 
        WHERE (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
        ORDER BY timestamp DESC LIMIT 1
    """)
    fun getLatestTransaction(storeCode: String = ""): Flow<YapeTransaction?>

    @Query("""
        SELECT * FROM yape_transactions 
        WHERE (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
        ORDER BY timestamp DESC LIMIT 1
    """)
    fun getLatestTransactionSync(storeCode: String = ""): YapeTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: YapeTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<YapeTransaction>): List<Long>

    @Query("SELECT * FROM yape_transactions WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedTransactions(): List<YapeTransaction>

    @Query("UPDATE yape_transactions SET isSynced = 1, remoteId = :remoteId, syncTimestamp = :syncTimestamp WHERE id = :id")
    suspend fun markAsSynced(id: Long, remoteId: String, syncTimestamp: Long)

    @Query("UPDATE yape_transactions SET remoteId = :remoteId, isSynced = 1, syncTimestamp = :syncTimestamp WHERE id = :id AND (remoteId IS NULL OR remoteId = '')")
    suspend fun linkRemoteId(id: Long, remoteId: String, syncTimestamp: Long)

    @Query("SELECT * FROM yape_transactions WHERE remoteId = :remoteId AND remoteId != '' LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): YapeTransaction?

    @Query("""
        SELECT * FROM yape_transactions 
        WHERE (:remoteId != '' AND remoteId = :remoteId)
           OR (
               ABS(amount - :amount) < 0.01 
               AND ABS(timestamp - :timestamp) <= 300000
               AND (
                   LOWER(TRIM(senderName)) = LOWER(TRIM(:senderName))
                   OR :senderName = '' 
                   OR senderName = ''
               )
           )
        ORDER BY CASE WHEN remoteId = :remoteId AND :remoteId != '' THEN 0 ELSE 1 END, timestamp DESC
        LIMIT 1
    """)
    suspend fun findDuplicate(timestamp: Long, amount: Double, senderName: String, remoteId: String = ""): YapeTransaction?

    @Query("""
        SELECT * FROM yape_transactions 
        WHERE (:storeCode = '' OR LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode)))
          AND ABS(amount - :amount) < 0.01
          AND timestamp >= :minTimestamp AND timestamp <= :maxTimestamp
        ORDER BY timestamp DESC
    """)
    suspend fun findCandidateDuplicates(
        amount: Double,
        minTimestamp: Long,
        maxTimestamp: Long,
        storeCode: String = ""
    ): List<YapeTransaction>

    @Query("SELECT * FROM yape_transactions ORDER BY timestamp ASC")
    suspend fun getAllTransactionsSync(): List<YapeTransaction>

    @Query("DELETE FROM yape_transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Delete
    suspend fun delete(transaction: YapeTransaction)

    @Query("DELETE FROM yape_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM yape_transactions WHERE LOWER(TRIM(storeCode)) = LOWER(TRIM(:storeCode))")
    suspend fun getTransactionsCountForStore(storeCode: String): Int

    // Obsoleto: Neutralizado para preservar la caché local multi-tienda y permitir carga instantánea y offline
    @Query("DELETE FROM yape_transactions WHERE 0 = 1 AND storeCode = :storeCode")
    suspend fun deleteOtherStores(storeCode: String)

    @Query("""
        UPDATE yape_transactions 
        SET branchName = :branchName, claimedBy = :claimedBy, claimedByName = :claimedByName, claimedAt = :claimedAt,
            note = CASE WHEN :note IS NOT NULL THEN :note ELSE note END
        WHERE (remoteId = :remoteId AND :remoteId != '') OR (id = :localId AND :localId > 0)
    """)
    suspend fun updateTransactionBranch(
        localId: Long,
        remoteId: String,
        branchName: String,
        claimedBy: String,
        claimedByName: String,
        claimedAt: Long,
        note: String? = null
    )

    @Query("""
        UPDATE yape_transactions 
        SET note = :note
        WHERE (remoteId = :remoteId AND :remoteId != '') OR (id = :localId AND :localId > 0)
    """)
    suspend fun updateTransactionNote(
        localId: Long,
        remoteId: String,
        note: String
    )

    @Query("DELETE FROM yape_transactions WHERE remoteId = :remoteId AND remoteId != ''")
    suspend fun deleteByRemoteId(remoteId: String)

    @Query("DELETE FROM yape_transactions")
    suspend fun clearAll()
}
