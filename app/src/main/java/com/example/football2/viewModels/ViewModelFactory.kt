package com.example.football2.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.football2.repository.GameControlRepository
import com.example.football2.repository.HintRepository
import com.example.football2.repository.LevelRepository
import com.example.football2.repository.LogoHintRepository
import com.example.football2.repository.LogoRepository

class ViewModelFactory(
    private val levelRepository: LevelRepository? = null,
    private val logoRepository: LogoRepository? = null,
    private val hintRepository: HintRepository? = null,
    private val logoHintRepository: LogoHintRepository? = null,
    private val gameControlRepository: GameControlRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LevelViewModel::class.java) -> {
                requireNotNull(levelRepository) { "LevelRepository must not be null for LevelViewModel" }
                LevelViewModel(levelRepository) as T
            }
            modelClass.isAssignableFrom(LogoViewModel::class.java) -> {
                requireNotNull(logoRepository) { "LogoRepository must not be null for LogoViewModel" }
                requireNotNull(hintRepository) { "HintRepository must not be null for LogoViewModel" }
                requireNotNull(gameControlRepository) { "GameControlRepository must not be null for LogoViewModel" }
                LogoViewModel(logoRepository, hintRepository, gameControlRepository) as T
            }
            modelClass.isAssignableFrom(HintViewModel::class.java) -> {
                requireNotNull(hintRepository) { "HintRepository must not be null for HintViewModel" }
                HintViewModel(hintRepository) as T
            }
            modelClass.isAssignableFrom(LogoHintViewModel::class.java) -> {
                requireNotNull(logoHintRepository) { "LogoHintRepository must not be null for LogoHintViewModel" }
                requireNotNull(gameControlRepository) { "GameControlRepository must not be null for LogoHintViewModel" }
                LogoHintViewModel(logoHintRepository, gameControlRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}