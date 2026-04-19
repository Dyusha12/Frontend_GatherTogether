package com.example.frontend_gathertogether.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.frontend_gathertogether.R
import com.example.frontend_gathertogether.models.ReviewEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReviewAdapter(private val currentUserId: String?, private val onEditClick: (ReviewEvent) -> Unit) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    // Хранение списка отзывов
    private var items: List<ReviewEvent> = emptyList()

    // Обновление списка отзывов
    fun submitList(data: List<ReviewEvent>) {
        items = data
        notifyDataSetChanged()
    }

    // ViewHolder для отображения данных отзыва
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
        val rating: RatingBar = view.findViewById(R.id.ratingBar)
        val comment: TextView = view.findViewById(R.id.tvComment)
        val date: TextView = view.findViewById(R.id.tvDate)
        val btnEdit: ImageView = view.findViewById(R.id.btnEditReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Создание ViewHolder из layout элемента карточки отзыва
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //Получение текущего отзыва по позиции
        val review = items[position]

        // Установка имени и фамилии владельца отзыва
        holder.name.text = "${review.user.name} ${review.user.surname}"

        // Установка значения оценки
        holder.rating.rating = review.rating.toFloat()

        // Загрузка комментария
        holder.comment.text = review.comment ?: ""

        // Форматирование и установка даты создания отзыва
        holder.date.text = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.parse(review.dateCreation))

        // Управление отображением кнопки редактирования отзыва
        if (review.user.userCode == currentUserId) {
            holder.btnEdit.visibility = View.VISIBLE
        } else {
            holder.btnEdit.visibility = View.GONE
        }

        // Обработка нажатия на кнопку редактирования
        holder.btnEdit.setOnClickListener {
            onEditClick(review)
        }

    }

    override fun getItemCount() = items.size
}