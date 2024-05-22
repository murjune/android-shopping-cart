package woowacourse.shopping.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "RecentProduct")
data class RecentProductEntity(
    @PrimaryKey val id: Long,
    val createdTime: LocalDateTime,
)