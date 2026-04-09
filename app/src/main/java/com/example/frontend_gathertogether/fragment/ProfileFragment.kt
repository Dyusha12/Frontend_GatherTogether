package com.example.frontend_gathertogether.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.frontend_gathertogether.R
import com.example.frontend_gathertogether.models.Interest
import com.example.frontend_gathertogether.models.User
import com.example.frontend_gathertogether.services.ClientProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ProfileFragment : Fragment(R.layout.activity_profile) {

    // Основные элементы интерфейса
    private lateinit var fullName: TextView
    private lateinit var rating: TextView
    private lateinit var mailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var updateButton: MaterialButton
    private lateinit var mailContainer: LinearLayout
    private lateinit var numberPhoneContainer: LinearLayout
    private lateinit var passwordContainer: LinearLayout
    private lateinit var mailActionIcon: ImageView
    private lateinit var phoneActionIcon: ImageView
    private lateinit var passwordActionIcon: ImageView
    private lateinit var interestsChipGroup: ChipGroup
    private lateinit var addInterestChip: Chip

    // Переменные для работы логики
    private lateinit var currentUser: User
    private var originalEmail = ""
    private var originalPhone = ""
    private var originalPassword = "******"
    private var allInterestsList: List<Interest> = emptyList()
    private val selectedInterests = mutableSetOf<String>()
    private var bottomSheetDialog: BottomSheetDialog? = null
    private val currentUserInterests = mutableSetOf<String>()

    // Константы
    companion object {
        private const val TAG = "ProfileFragment"
        private const val BASE_URL = "http://10.0.2.2:8080/gather-together/users/"
        private const val BASE_URL_INTERESTS = "http://10.0.2.2:8080/gather-together/interests"
        private const val BASE_URL_USER_INTERESTS = "http://10.0.2.2:8080/gather-together/user-interests"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация UI элементов из разметки
        fullName = view.findViewById(R.id.profileFullName)
        rating = view.findViewById(R.id.profileRating)
        mailEditText = view.findViewById(R.id.emailField)
        phoneEditText = view.findViewById(R.id.phoneField)
        passwordEditText = view.findViewById(R.id.passwordField)
        updateButton = view.findViewById(R.id.saveChangesButton)
        mailContainer = view.findViewById(R.id.mailContainer)
        numberPhoneContainer = view.findViewById(R.id.numberPhoneContainer)
        passwordContainer = view.findViewById(R.id.passwordContainer)
        mailActionIcon = view.findViewById(R.id.icon_mail_edit)
        phoneActionIcon = view.findViewById(R.id.icon_phone_edit)
        passwordActionIcon = view.findViewById(R.id.icon_password_edit)
        interestsChipGroup = view.findViewById(R.id.interestsChipGroup)
        addInterestChip = view.findViewById(R.id.addInterestChip)

        val userId = getUserId()

        // Загрузка данных пользователя
        if (userId != null) {
            loadUserData(userId)
            loadAllInterests()
            loadUserInterests(userId)
        } else {
            Log.e(TAG, "User ID not found")
        }

        // Обработка нажатия кнопки сохранения изменений
        updateButton.setOnClickListener {
            validateUpdateUser()
        }

        // Очистка пароля при фокусе
        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && passwordEditText.text.toString() == "******") {
                passwordEditText.setText("")
            }
        }

        // Очистка ошибок при вводе текста
        mailEditText.addTextChangedListener {
            clearError(mailContainer)
        }
        phoneEditText.addTextChangedListener {
            clearError(numberPhoneContainer)
        }
        passwordEditText.addTextChangedListener {
            clearError(passwordContainer)
        }

        setupPhoneMask()

        // Кнопка добавления интересов
        addInterestChip.apply {
            setChipBackgroundColorResource(R.color.button_bg)
            setTextColor(resources.getColor(R.color.white, null))
        }
        addInterestChip.setOnClickListener {
            showInterestsBottomSheet()
        }
    }

    // Получение идентификатора пользователя
    private fun getUserId(): String? {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        return prefs.getString("USER_ID", null)
    }

    // Загрузка данных пользователя
    private fun loadUserData(userId: String) {
        lifecycleScope.launch {
            try {
                val clientProvider = ClientProvider(requireContext())
                val client = clientProvider.instance()
                currentUser = client.get("$BASE_URL$userId").body()
                fillUserData(currentUser)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки пользователя", e)
            }
        }
    }

    // Загрузка всех доступных интересов
    private fun loadAllInterests() {
        lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                allInterestsList = client.get(BASE_URL_INTERESTS).body()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки всех интересов", e)
            }
        }
    }

    // Загрузка интересов пользователя
    private fun loadUserInterests(userCode: String) {
        currentUserInterests.clear()
        lifecycleScope.launch {
            try {
                val clientProvider = ClientProvider(requireContext())
                val client = clientProvider.instance()
                val response = client.get("$BASE_URL_USER_INTERESTS/user/$userCode")
                val userInterests: List<Interest> = response.body()

                interestsChipGroup.removeAllViews()

                // Создание чипов интересов
                userInterests.forEach { interest ->
                    currentUserInterests.add(interest.code)
                    val chip = createUserInterestChip(interest)
                    interestsChipGroup.addView(chip)
                }

                // Кнопка добавления отображается если интересов менее 5 штук
                if (userInterests.size < 5) {
                    interestsChipGroup.addView(addInterestChip)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки интересов пользователя", e)
            }
        }
    }

    // Заполнение UI данными пользователя
    private fun fillUserData(user: User) {
        fullName.text = "${user.surname} ${user.name}"
        rating.text = "${user.rating}"
        originalEmail = user.mail
        originalPhone = user.phoneNumber ?: "Не указан"
        originalPassword = "******"
        mailEditText.setText(originalEmail)
        phoneEditText.setText(originalPhone)
        passwordEditText.setText(originalPassword)

        // Подключение логики редактирования полей
        setupFieldEdit(mailEditText, mailActionIcon, originalEmail, mailContainer)
        setupFieldEdit(phoneEditText, phoneActionIcon, originalPhone, numberPhoneContainer)
        setupFieldEdit(passwordEditText, passwordActionIcon, originalPassword, passwordContainer)
    }

    // Отправка запроса на сервер для обновления данных пользователя
    private fun updateUserData(userId: String) {
        lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                val newEmail = mailEditText.text.toString()
                val phoneFormat = phoneEditText.text.toString()
                val newPassword = passwordEditText.text.toString()
                val newPhone = phoneFormat.replace(" ", "")

                // Отправка PUT с параметрами запроса
                val response = client.put("$BASE_URL$userId") {
                    // Отправка только тех параметров, которые заполнены
                    if (newEmail.isNotEmpty() && newEmail != currentUser.mail) parameter("email", newEmail)
                    if (newPhone.isNotEmpty() && newPhone != currentUser.phoneNumber) parameter("phone", newPhone)
                    if (newPassword.isNotEmpty() && newPassword != "******") parameter("password", newPassword)
                }
                if (response.status == HttpStatusCode.OK) {
                    currentUser = response.body()
                    Log.d(TAG, "Пользователь успешно обновлён: $currentUser")
                    fillUserData(currentUser)
                    resetField(mailEditText, mailActionIcon, currentUser.mail)
                    resetField(phoneEditText, phoneActionIcon, currentUser.phoneNumber ?: "Не указан")
                    resetField(passwordEditText, passwordActionIcon, "******")
                } else {
                    Log.e(TAG, "Ошибка обновления: ${response.status}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении данных пользователя", e)
            }
        }
    }

    // Создание чипа для уже добавленного интереса пользователя
    private fun createUserInterestChip(interest: Interest): Chip {
        return Chip(requireContext()).apply {
            text = interest.name
            isCloseIconVisible = true

            // Стиль выбранного интереса
            setChipBackgroundColorResource(R.color.red_gradient)
            setTextColor(resources.getColor(R.color.white, null))
            setChipStrokeColorResource(R.color.black)
            chipCornerRadius = 50f
            chipStrokeWidth = 1f

            // Удаление интереса по нажатию на крестик
            setOnCloseIconClickListener {
                removeInterestFromUser(interest.code)
            }
        }
    }

    // Открывает BottomSheet со списком всех интересов для выбора
    private fun showInterestsBottomSheet() {

        // Если интересы не загрузились то отображается сообщение
        if (allInterestsList.isEmpty()) {
            Toast.makeText(requireContext(), "Список интересов загружается...", Toast.LENGTH_SHORT).show()
            return
        }
        bottomSheetDialog = BottomSheetDialog(requireContext()).apply {
            setContentView(R.layout.bottom_sheet_interests)
            val chipGroup = findViewById<ChipGroup>(R.id.allInterestsChipGroup)!!
            val btnAdd = findViewById<MaterialButton>(R.id.btnAddSelectedInterests)!!
            chipGroup.removeAllViews()

            // Очистка выбранных интересов и добавление текущих интересов пользователя
            selectedInterests.clear()
            selectedInterests.addAll(currentUserInterests)

            // Создание чипа для каждого интереса
            allInterestsList.forEach { interest ->
                val chip = Chip(context).apply {
                    text = interest.name
                    isCheckable = true

                    // Проверка на существование интереса у пользователя
                    val isExisting = currentUserInterests.contains(interest.code)

                    // Установка выбора чипа
                    isChecked = selectedInterests.contains(interest.code)
                    chipCornerRadius = 50f
                    chipStrokeWidth = 1f

                    // Стилизация чипов
                    if (isExisting) {
                        setChipBackgroundColorResource(R.color.gray)
                        setTextColor(resources.getColor(R.color.white, null))
                        isEnabled = false
                    } else if (isChecked) {
                        setChipBackgroundColorResource(R.color.red_gradient)
                        setTextColor(resources.getColor(R.color.white, null))
                    } else {
                        setChipBackgroundColorResource(R.color.orange_gradient)
                        setTextColor(resources.getColor(R.color.black, null))
                    }

                    // Обработчик выбора
                    setOnCheckedChangeListener { chip, isChecked ->

                        // Запрет взаимодействовать с существующими интересами
                        if (isExisting) return@setOnCheckedChangeListener

                        if (isChecked) {

                            // Ограничение на количество выбранных интересов
                            if (selectedInterests.size >= 5) {
                                chip.isChecked = false
                                Toast.makeText(context, "Максимум 5 интересов", Toast.LENGTH_SHORT).show()
                                return@setOnCheckedChangeListener
                            }

                            // Добавление интереса
                            selectedInterests.add(interest.code)

                            // Изменение стиля
                            setChipBackgroundColorResource(R.color.red_gradient)
                            chip.setTextColor(resources.getColor(R.color.white, null))

                        } else {
                            // Удаление интереса
                            selectedInterests.remove(interest.code)

                            // Возврат обычного стиля
                            setChipBackgroundColorResource(R.color.orange_gradient)
                            chip.setTextColor(resources.getColor(R.color.black, null))
                        }
                    }
                }
                chipGroup.addView(chip)
            }

            // Обработка нажатия кнопки добавить
            btnAdd.setOnClickListener {
                if (selectedInterests.isNotEmpty()) {
                    addSelectedInterestsToUser()
                }
                dismiss()
            }
            show()
        }
    }

    // Отправка запроса на сервер для добавления интереса пользователю
    private fun addSelectedInterestsToUser() {
        val userCode = getUserId() ?: return
        lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                for (interestCode in selectedInterests) {
                    client.post(BASE_URL_USER_INTERESTS) {
                        parameter("userCode", userCode)
                        parameter("interestCode", interestCode)
                    }
                }
                Toast.makeText(requireContext(), "Интересы добавлены!", Toast.LENGTH_SHORT).show()
                loadUserInterests(userCode)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка добавления интересов", e)
                Toast.makeText(requireContext(), "Ошибка при добавлении", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Отправка запроса на сервер для удаления интереса пользователя
    private fun removeInterestFromUser(interestCode: String) {
        val userCode = getUserId() ?: return
        lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                client.delete(BASE_URL_USER_INTERESTS) {
                    parameter("userCode", userCode)
                    parameter("interestCode", interestCode)
                }
                loadUserInterests(userCode)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка удаления интереса", e)
            }
        }
    }

    // Установка маски для телефона
    private fun setupPhoneMask() {
        phoneEditText.addTextChangedListener(object : android.text.TextWatcher {
            private var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                if (isEditing) return
                isEditing = true

                // Удаляются все символы кроме цифр
                val digits = s.toString().replace("\\D".toRegex(), "")
                val formatted = StringBuilder()
                if (digits.isNotEmpty()) {
                    // Начало номера
                    formatted.append("+7 ")

                    // Добавление открывающейся скобки для кода региона
                    if (digits.length > 1) {
                        formatted.append("(")
                        formatted.append(digits.substring(1, minOf(4, digits.length)))
                        if (digits.length >= 4) formatted.append(") ") // Закрытие скобки после кода региона
                    }

                    // Добавление следующих трех цифр номера
                    if (digits.length >= 4) {
                        formatted.append(digits.substring(4, minOf(7, digits.length)))
                    }

                    // Добавление дефиса и следующих двух цифр
                    if (digits.length >= 7) {
                        formatted.append("-")
                        formatted.append(digits.substring(7, minOf(9, digits.length)))
                    }

                    // Добавление дефиса и последних двух цифр
                    if (digits.length >= 9) {
                        formatted.append("-")
                        formatted.append(digits.substring(9, minOf(11, digits.length)))
                    }
                }

                // Установка отформированного текста обратно в поле
                phoneEditText.setText(formatted.toString())

                // Перемещение курсора в конец текста
                phoneEditText.setSelection(formatted.length)

                isEditing = false
            }
        })
    }

    // Проверка всех полей перед обновлением данных
    private fun validateUpdateUser() {
        val email = mailEditText.text.toString()
        val phoneFormat = phoneEditText.text.toString()
        val password = passwordEditText.text.toString()
        var isValid = true

        // Проверка почты
        if (!isValidEmail(email)) {
            setError(mailContainer, "Неверный формат почты", mailEditText)
            isValid = false
        }
        else{
            clearError(mailContainer)
        }

        // Проверка телефона
        if (!isValidPhone(phoneFormat)) {
            setError(numberPhoneContainer, "Некорректный формат телефона", phoneEditText)
            isValid = false
        } else {
            clearError(numberPhoneContainer)
        }

        // Проверка пароля
        if (password != "******" && !isValidPassword(password)) {
            setError(passwordContainer, "Длина пароля минимум 6 символов, содержать хотя бы одну букву и один спецсимвол", passwordEditText)
            isValid = false
        }
        else{
            clearError(passwordContainer)
        }

        if (!isValid) return

        val userId = getUserId()
        if (userId != null) {
            updateUserData(userId)
        }
    }

    // Проверка почты по локальной и доменной части
    private fun isValidEmail(email: String): Boolean {
        val localPartPattern = "^[a-zA-Z0-9]+([._+-]?[a-zA-Z0-9]+)*$"
        val domainPattern = "^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$"
        val parts = email.split("@")
        if (parts.size != 2) return false
        val local = parts[0]
        val domain = parts[1]
        if (local.length < 3) return false

        return Pattern.matches(localPartPattern, local) && Pattern.matches(domainPattern, domain)
    }

    // Проверка номера телефона
    private fun isValidPhone(phone: String): Boolean {
        val digits = phone.replace("\\D".toRegex(), "")
        return digits.length == 11
    }

    // Проверка пароля
    private fun isValidPassword(password: String): Boolean {
        if (password.length <= 6) return false
        val hasLetter = password.any { it.isLetter() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasLetter && hasSpecial
    }

    // Установка состояния ошибки для поля
    private fun setError(container: LinearLayout, message: String, editText: EditText? = null) {
        container.setBackgroundResource(R.drawable.bg_input_fields_error)
        editText?.error = message
    }

    // Очистка состояния ошибки
    private fun clearError(container: LinearLayout) {
        container.setBackgroundResource(R.drawable.bg_input_fields)
    }

    // Логика редактирование поля
    private fun setupFieldEdit(editText: EditText, actionIcon: ImageView, originalValue: String, layout: LinearLayout) {
        var isEditing = false
        lockField(editText)
        actionIcon.setOnClickListener {
            if (!isEditing) {

                // Активация редактирования
                unlockField(editText)
                editText.requestFocus()
                editText.setSelection(editText.text.length)
                actionIcon.setImageResource(R.mipmap.ic_return)
                isEditing = true
            } else {

                // Возврат к исходным данным
                editText.setText(originalValue)
                lockField(editText)
                actionIcon.setImageResource(R.mipmap.ic_edit)
                isEditing = false
                clearError(layout)
                editText.error = null
            }
        }
    }

    // Блокировка поля для редактирования
    private fun lockField(editText: EditText) {
        editText.isFocusable = false
        editText.isFocusableInTouchMode = false
        editText.isCursorVisible = false
        editText.setTextIsSelectable(false)
    }

    // Разблокировка поля для редактирования
    private fun unlockField(editText: EditText) {
        editText.isFocusable = true
        editText.isFocusableInTouchMode = true
        editText.isCursorVisible = true
        editText.setTextIsSelectable(true)
    }

    // Сброс поля к исходному значению
    private fun resetField(editText: EditText, actionIcon: ImageView, value: String) {
        editText.setText(value)
        lockField(editText)
        actionIcon.setImageResource(R.mipmap.ic_edit)
    }
}