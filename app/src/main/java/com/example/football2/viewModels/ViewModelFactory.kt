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
                LevelViewModel(levelRepository!!) as T
            }
            modelClass.isAssignableFrom(LogoViewModel::class.java) -> {
                LogoViewModel(logoRepository!!) as T
            }
            modelClass.isAssignableFrom(HintViewModel::class.java) -> {
                HintViewModel(hintRepository!!) as T
            }
            modelClass.isAssignableFrom(LogoHintViewModel::class.java) -> {
                LogoHintViewModel(logoHintRepository!!, gameControlRepository!!) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}