package br.com.estilofitudi.settings.repository

import br.com.estilofitudi.settings.domain.SystemSetting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SystemSettingRepository : JpaRepository<SystemSetting, UUID> {
    fun findByKey(key: String): SystemSetting?
    fun findAllByOrderByKeyAsc(): List<SystemSetting>
}
