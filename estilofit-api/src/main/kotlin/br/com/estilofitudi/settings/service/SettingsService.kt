package br.com.estilofitudi.settings.service

import br.com.estilofitudi.settings.domain.SettingKey
import br.com.estilofitudi.settings.domain.SettingType
import br.com.estilofitudi.settings.domain.SystemSetting
import br.com.estilofitudi.settings.dto.SettingResponse
import br.com.estilofitudi.settings.repository.SystemSettingRepository
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import br.com.estilofitudi.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class SettingsService(
    private val repository: SystemSettingRepository,
    private val userRepository: UserRepository,
) {

    /** Lista as configurações conhecidas com o valor atual (ou o fallback quando ausente). */
    fun list(): List<SettingResponse> {
        val stored = repository.findAllByOrderByKeyAsc().associateBy { it.key }
        return SettingKey.entries.map { def ->
            val entity = stored[def.key]
            SettingResponse(
                key = def.key,
                label = def.label,
                value = entity?.value ?: def.fallback,
                type = def.type,
                min = def.min,
                max = def.max,
                description = entity?.description,
                updatedAt = entity?.updatedAt,
                updatedByName = entity?.updatedBy?.name,
            )
        }
    }

    /** Lê o valor bruto de uma chave conhecida, com fallback quando ausente. Usado pelo SettingsReader. */
    fun rawValue(def: SettingKey): String =
        repository.findByKey(def.key)?.value ?: def.fallback

    @Transactional
    fun update(key: String, rawValue: String, userEmail: String): SettingResponse {
        val def = SettingKey.fromKey(key)
            ?: throw EntityNotFoundException("Configuração", key)

        val value = validateAndNormalize(def, rawValue.trim())

        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { EntityNotFoundException("Usuário", userEmail) }

        val entity = repository.findByKey(def.key)?.apply {
            this.value = value
            this.updatedBy = user
        } ?: SystemSetting(key = def.key, value = value, description = def.label, updatedBy = user)

        val saved = repository.save(entity)
        return SettingResponse(
            key = def.key,
            label = def.label,
            value = saved.value,
            type = def.type,
            min = def.min,
            max = def.max,
            description = saved.description,
            updatedAt = saved.updatedAt,
            updatedByName = user.name,
        )
    }

    /** Valida faixa e tipo; devolve o valor normalizado (inteiros sem casas decimais). */
    private fun validateAndNormalize(def: SettingKey, raw: String): String {
        val number = raw.toBigDecimalOrNull()
            ?: throw BusinessException("O valor de '${def.label}' deve ser numérico.")

        if (def.type == SettingType.INTEGER && number.stripTrailingZeros().scale() > 0) {
            throw BusinessException("O valor de '${def.label}' deve ser um número inteiro.")
        }
        if (number < def.min) {
            throw BusinessException("O valor de '${def.label}' deve ser no mínimo ${def.min.toPlainString()}.")
        }
        def.max?.let { max ->
            if (number > max) {
                throw BusinessException("O valor de '${def.label}' deve ser no máximo ${max.toPlainString()}.")
            }
        }

        return if (def.type == SettingType.INTEGER) {
            number.toBigInteger().toString()
        } else {
            number.stripTrailingZeros().toPlainString()
        }
    }
}
