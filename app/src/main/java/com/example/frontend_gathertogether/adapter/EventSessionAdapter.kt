package com.example.frontend_gathertogether.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.frontend_gathertogether.R
import com.example.frontend_gathertogether.models.EventSession
import com.example.frontend_gathertogether.models.User
import com.google.android.material.button.MaterialButton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class EventSessionAdapter(private val currentUserCode: String, private val onParticipateClick: (EventSession) -> Unit, private val onCancelClick: (EventSession) -> Unit) : RecyclerView.Adapter<EventSessionAdapter.ViewHolder>() {

    // Хранение списка сессий и участников
    private var items: List<Pair<EventSession, List<User>>> = emptyList()

    // Обновление списка сессий
    fun submitList(data: List<Pair<EventSession, List<User>>>) {
        items = data
        notifyDataSetChanged()
    }

    // ViewHolder для отображения информации о сессии
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSessionDate: TextView = view.findViewById(R.id.tvSessionDate)
        val tvSessionTime: TextView = view.findViewById(R.id.tvSessionTime)
        val tvNumberPeople: TextView = view.findViewById(R.id.tvNumberPeople)
        val btnParticipate: MaterialButton = view.findViewById(R.id.btnParticipate)
        val tvInfoCancel: TextView = view.findViewById(R.id.tvInfoCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Создание элемента списка сессий
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // Получение текущей сессии и списка пользователей
        val (session, users) = items[position]

        val count = users.size
        val max = session.numberPeople ?: 0

        // Проверка регистрации текущего пользователя на сессию
        val isRegistered = users.any { it.userCode == currentUserCode }

        // Проверка возможности отмены участия
        val canCancel = canCancel(session.dateEvent)

        // Формирование даты и времени сессии
        try {
            val dateTime = LocalDateTime.parse(session.dateEvent)
            holder.tvSessionDate.text =
                dateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru")))
            holder.tvSessionTime.text =
                dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale("ru")))
        } catch (e: Exception) {
            holder.tvSessionDate.text = session.dateEvent ?: "Дата не указана"
            holder.tvSessionTime.text = ""
        }

        // Отображение количества участников
        holder.tvNumberPeople.text = "$count из $max участников"

        // Проверка заполненности сессии
        if (max != 0 && count >= max) {
            holder.btnParticipate.text = "Мест нет"
            holder.btnParticipate.isEnabled = false
            return
        }

        // Логика отображения кнопки для зарегистрированного пользователя
        if (isRegistered) {
            holder.btnParticipate.text = "Отменить участие"

            // Ограничение отмены за 1 день до события
            if (!canCancel) {
                holder.btnParticipate.text = "Отмена недоступна"
                holder.btnParticipate.isEnabled = false
            } else {
                holder.btnParticipate.isEnabled = true
            }

        } else {
            holder.btnParticipate.text = "Участвовать"
            holder.btnParticipate.isEnabled = true
        }

        // Обработка нажатия на кнопку участия или отмены
        holder.btnParticipate.setOnClickListener {
            if (isRegistered && canCancel) {
                onCancelClick(session)
            } else if (!isRegistered) {
                onParticipateClick(session)
            }
        }

        // Отображение информационного сообщения об ограничении отмены
        holder.tvInfoCancel.visibility =
            if (isRegistered) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = items.size

    // Проверка возможности отмены участия за 3 дня до события
    private fun canCancel(sessionDate: String): Boolean {
        return try {
            val sessionTime = LocalDateTime.parse(sessionDate)
            val now = LocalDateTime.now()

            ChronoUnit.DAYS.between(now, sessionTime) >= 1
        } catch (e: Exception) {
            false
        }
    }
}