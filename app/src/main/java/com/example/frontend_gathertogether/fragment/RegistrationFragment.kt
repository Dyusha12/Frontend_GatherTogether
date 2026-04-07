package com.example.frontend_gathertogether.fragment

import androidx.fragment.app.Fragment
import com.example.frontend_gathertogether.R
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.frontend_gathertogether.provider.ClientProvider
import com.example.frontend_gathertogether.models.UserResponse
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class RegistrationFragment : Fragment(R.layout.activity_registration) {

    // Основные элементы интерфейса
    private lateinit var birthDateText: TextView
    private lateinit var mailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var firstNameContainer: LinearLayout
    private lateinit var lastNameContainer: LinearLayout
    private lateinit var mailContainer: LinearLayout
    private lateinit var numberPhoneContainer: LinearLayout
    private lateinit var passwordContainer: LinearLayout
    private lateinit var birthDateContainer: LinearLayout
    private lateinit var authorizationLink: TextView

    // Константы
    companion object {
        private const val TAG = "RegistrationActivity"
        private const val REGISTER_URL = "http://10.0.2.2:8080/gather-together/users/register"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация UI элементов из разметки
        birthDateText = view.findViewById(R.id.birthDateText)
        mailEditText = view.findViewById(R.id.mailEditText)
        phoneEditText = view.findViewById(R.id.phoneEditText)
        firstNameEditText = view.findViewById(R.id.firstNameEditText)
        lastNameEditText = view.findViewById(R.id.lastNameEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        registerButton = view.findViewById(R.id.registerButton)
        firstNameContainer = view.findViewById(R.id.firstNameContainer)
        lastNameContainer = view.findViewById(R.id.lastNameContainer)
        mailContainer = view.findViewById(R.id.mailContainer)
        numberPhoneContainer = view.findViewById(R.id.numberPhoneContainer)
        passwordContainer = view.findViewById(R.id.passwordContainer)
        birthDateContainer = view.findViewById(R.id.birthDateContainer)
        authorizationLink = view.findViewById(R.id.authorizationLink)

        // Настройка обработчиков событий
        birthDateText.setOnClickListener { showDatePicker() }
        registerButton.setOnClickListener { validateRegister() }
        authorizationLink.setOnClickListener {
            openAuthorizationWindow()
        }

        // Очистка ошибок при вводе текста
        firstNameEditText.addTextChangedListener {
            clearError(firstNameContainer)
        }
        lastNameEditText.addTextChangedListener {
            clearError(lastNameContainer)
        }
        mailEditText.addTextChangedListener {
            clearError(mailContainer)
        }
        phoneEditText.addTextChangedListener {
            clearError(numberPhoneContainer)
        }
        passwordEditText.addTextChangedListener {
            clearError(passwordContainer)
        }

        // Настройка маски для телефона
        setupPhoneMask()
    }

    // Показывание диалога выбора даты рождения
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formatted = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                birthDateText.text = formatted
            },
            year, month, day
        )
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    // Проверка всех полей перед регистрацией
    private fun validateRegister() {
        val email = mailEditText.text.toString()
        val phoneFormat = phoneEditText.text.toString()
        val firstName = firstNameEditText.text.toString()
        val lastName = lastNameEditText.text.toString()
        val password = passwordEditText.text.toString()
        val birthDate = birthDateText.text.toString()
        var isValid = true
        val phone = phoneFormat.replace(" ", "")

        // Проверка имени и фамилии
        val nameRegex = "^[a-zA-Zа-яА-Я]{1,25}$"
        if(firstName.isBlank()){
            setError(firstNameContainer, "Заполните поле", firstNameEditText)
            isValid = false
        }
        else if (firstName != firstName.trim()) {
            setError(firstNameContainer, "Имя не должно содержать пробелы в начале или конце", firstNameEditText)
            isValid = false
        }
        else if (!firstName.matches(nameRegex.toRegex())) {
            setError(firstNameContainer, "Имя должно содержать только буквы", firstNameEditText)
            isValid = false
        }
        else{
            clearError(firstNameContainer)
        }
        if(lastName.isBlank()){
            setError(lastNameContainer, "Заполните поле", lastNameEditText)
            isValid = false
        }
        else if (lastName != lastName.trim()) {
            setError(lastNameContainer, "Фамилия не должна содержать пробелы в начале или конце", lastNameEditText)
            isValid = false
        }
        else if (!lastName.matches(nameRegex.toRegex())) {
            setError(lastNameContainer, "Фамилия должна содержать только буквы", lastNameEditText)
            isValid = false
        }
        else{
            clearError(lastNameContainer)
        }

        // Проверка почты
        if (!isValidEmail(email)) {
            setError(mailContainer, "Неверный формат почты", mailEditText)
            isValid = false
        }
        else{
            clearError(mailContainer)
        }

        // Проверка пароля
        if (!isValidPassword(password)) {
            setError(passwordContainer, "Длина пароля минимум 6 символов, содержать хотя бы одну букву и один спецсимвол", passwordEditText)
            isValid = false
        }
        else{
            clearError(passwordContainer)
        }

        // Проверка даты рождения
        if (!isValidBirthDate(birthDate)) {
            Toast.makeText(requireContext(), "Возраст должен быть не меньше 14 лет", Toast.LENGTH_LONG).show()
            isValid = false
            birthDateContainer.setBackgroundResource(R.drawable.bg_input_fields_error)
        }
        else{
            clearError(birthDateContainer)
        }

        if (!isValid) return

        // Отправка данных на сервер
        lifecycleScope.launch {
            registerUser(email, phone, password, lastName, firstName, birthDate)
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

    // Проверка пароля
    private fun isValidPassword(password: String): Boolean {
        if (password.length <= 6) return false
        val hasLetter = password.any { it.isLetter() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasLetter && hasSpecial
    }

    // Проверка возраста
    private fun isValidBirthDate(dateStr: String): Boolean {
        if (dateStr.isEmpty() || dateStr == getString(R.string.date_selection)) return false
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val birthDate = sdf.parse(dateStr) ?: return false
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -14)
        val minDate = calendar.time
        return birthDate <= minDate
    }

    // Отправка запроса на регистрацию на сервер
    private suspend fun registerUser(email: String, phone: String?, password: String, surname: String, name: String, birthDate: String) {
        try {
            val client = ClientProvider().instance()

            // Запрос с параметрами
            val response: HttpResponse = client.submitForm(
                url = REGISTER_URL,
                formParameters = Parameters.build {
                    append("email", email)
                    append("phone", phone ?: "")
                    append("password", password)
                    append("surname", surname)
                    append("name", name)
                    append("birthDate", birthDate)
                }
            )

            // Обработка успешного ответа
            if (response.status == HttpStatusCode.OK) {
                val user: UserResponse = response.body()
                Log.d(TAG, "Пользователь зарегистрирован: $user")
                openAuthorizationWindow()
            } else {
                // Ошибка регистрации
                Log.e(TAG, "Ошибка регистрации: ${response.status}")
                Toast.makeText(requireContext(), "Ошибка регистрации: ${response.status}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            // Ошибка запроса
            Log.e(TAG, "Ошибка при запросе регистрации", e)
            Toast.makeText(requireContext(), "Ошибка регистрации", Toast.LENGTH_LONG).show()
        }
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

    // Переход на экран авторизации
    private fun openAuthorizationWindow(){
        findNavController().navigate(R.id.action_to_authorization)
    }
}