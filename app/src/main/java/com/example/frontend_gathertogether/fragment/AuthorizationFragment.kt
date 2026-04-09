package com.example.frontend_gathertogether.fragment

import androidx.fragment.app.Fragment
import com.example.frontend_gathertogether.R
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.frontend_gathertogether.services.ClientProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import org.json.JSONObject

class AuthorizationFragment : Fragment(R.layout.activity_authorization) {

    // Основные элементы интерфейса
    private lateinit var mailEditText: EditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var registerLink: TextView
    private lateinit var mailContainer: LinearLayout
    private lateinit var passwordContainer: LinearLayout
    private lateinit var iconPassword: ImageView

    // Константы
    companion object {
        private const val TAG = "AuthorizationActivity"
        private const val LOGIN_URL = "http://10.0.2.2:8080/gather-together/users/login"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация UI элементов из разметки
        mailEditText = view.findViewById(R.id.mailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)
        registerLink = view.findViewById(R.id.registerLink)
        mailContainer = view.findViewById(R.id.mailContainer)
        passwordContainer = view.findViewById(R.id.passwordContainer)
        iconPassword = view.findViewById(R.id.passwordToggle)

        // Очистка ошибок при вводе текста
        mailEditText.addTextChangedListener {
            clearError(mailContainer)
        }

        passwordEditText.addTextChangedListener {
            clearError(passwordContainer)
        }

        // Обработка нажатия кнопки входа
        loginButton.setOnClickListener {
            validateAuthorization()
        }

        // Переход на экран регистрации
        registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_to_registration)
        }

        var passwordVisible = false

        // Обработка нажатия на показ/скрытие пароля
        iconPassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                passwordEditText.transformationMethod = null // Показывает пароль
                iconPassword.setImageResource(R.mipmap.ic_password_open)
            } else {
                passwordEditText.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance() // Скрывает пароль
                iconPassword.setImageResource(R.mipmap.ic_password_close)
            }
            passwordEditText.setSelection(passwordEditText.text?.length ?: 0)
        }
    }

    // Проверка введенных данных перед отправкой запроса
    private fun validateAuthorization(){
        val mail = mailEditText.text.toString()
        val password = passwordEditText.text.toString()
        var isValid = true

        // Проверка почты
        if(mail.isBlank()){
            setError(mailContainer, "Заполните поле", mailEditText)
            isValid = false
        }
        else{
            clearError(mailContainer)
        }

        // Проверка пароля
        if(password.isBlank()){
            setError(passwordContainer, "Заполните поле", passwordEditText)
            isValid = false
        }
        else{
            clearError(passwordContainer)
        }

        if (!isValid) return

        // Запуск запроса авторизации в корутине
        lifecycleScope.launch {
            val token = loginUser(mail, password)

            if (token != null) {
                Log.d(TAG, "Успешная авторизация! Token: $token")
                Toast.makeText(requireContext(), "Вход выполнен успешно!", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Отправка запроса на сервер для авторизации
    private suspend fun loginUser(email: String, password: String): String? {
        return try {
            val clientProvider = ClientProvider(requireContext())
            val client = clientProvider.instance()

            // POST-запрос с параметрами почты и пароля
            val response: HttpResponse = client.post(LOGIN_URL) {
                url {
                    parameters.append("email", email)
                    parameters.append("password", password)
                }
            }

            // Обработка успешного ответа
            if (response.status == HttpStatusCode.OK) {

                val token: String = response.body()

                val payload = decodeJwt(token)
                val userId = payload?.getString("sub")

                Log.d(TAG, payload.toString())
                if (userId != null) {
                    clientProvider.saveToken(token)
                    saveUserId(userId)
                    Log.d(TAG, "User ID: $userId")
                }
                findNavController().navigate(R.id.action_to_profile)
                return token
            }
            else {
                // Ошибка авторизации
                Log.e(TAG, "Ошибка авторизации: ${response.status}")
                setError(mailContainer, "Неверный логин или пароль", mailEditText)
                return null
            }
        } catch (e: Exception) {
            // Ошибка запроса
            Log.e(TAG, "Ошибка при запросе", e)
            null
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

    // Сохранение идентификатора пользователя в настройки
    private fun saveUserId(userId: String) {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        prefs.edit().putString("USER_ID", userId).apply()
        prefs.edit().putBoolean("ACCOUNT_ACTIVE", true).apply()
    }

    // Расшифровка JWT-токена
    fun decodeJwt(token: String): JSONObject? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            val payload = parts[1]

            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
            val decodedString = String(decodedBytes)

            JSONObject(decodedString)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}