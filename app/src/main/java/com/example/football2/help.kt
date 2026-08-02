//package com.example.football2
//
//بجعل زر المساعده مفتوح ع طول
//        lifecycleScope.launch {
//    logoHintViewModel.currentLogoHintState.collect { hintEntity ->
//        if (hintEntity != null) {
//            if (hintEntity.hide == 1) {
//                updateHideButtonState(true)
//                applyHideHintIfUnlocked()
//            } else {
//                updateHideButtonState(false)
//            }
//
//            // 🟢 إبقاء زر الحروف مفعل دائماً ليتسنى الخصم مجدداً
//            updateLetterButtonState(false)
//
//            applyRevealedLettersIfUnlocked()
//        }
//    }
//}
//
//private fun revealLetterAtSlot(slotIndex: Int) {
//    isSelectingSlotForLetterHint = false
//
//    val correctAnswer = currentLogo?.lo_name ?: return
//    val selectedSlot = answerSlots.getOrNull(slotIndex) ?: return
//
//    clearQuestionMarks()
//
//    val isTargetSlot = selectedSlot.text.toString().trim() == "?" || selectedSlot.text.isEmpty()
//
//    if (isTargetSlot) {
//        val correctChar = correctAnswer[slotIndex].uppercaseChar()
//
//        val existingSourcePos = slotSourcePositions[slotIndex]
//        if (existingSourcePos != null) {
//            lettersAdapter.showLetter(existingSourcePos)
//            slotSourcePositions.remove(slotIndex)
//        }
//
//        selectedSlot.text = correctChar.toString()
//        selectedSlot.setTextColor(android.graphics.Color.YELLOW)
//
//        findAvailablePositionOfLetter(correctChar)?.let { gridPosition ->
//            slotSourcePositions[slotIndex] = gridPosition
//            lettersAdapter.hideLetter(gridPosition)
//        }
//
//        // 🔴 تم إزالة updateLetterButtonState(true) لإبقاء الزر مفتوحاً للاستخدام القادم
//
//        logoHintViewModel.unlockLetterHintAt(currentLogoId, slotIndex)
//        checkAnswerComplete()
//    }
//}
//
//private fun updateLetterButtonState(isUsed: Boolean) {
//    binding.letter.isEnabled = !isUsed
//    binding.letter.alpha = if (isUsed) 0.5f else 1.0f
//}