package com.example.frontend_gathertogether.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.frontend_gathertogether.R
import com.example.frontend_gathertogether.models.Event

class EventLocationAdapter(private val onEventClick: (Event) -> Unit, private val onRatingClick: (Event) -> Unit) : RecyclerView.Adapter<EventLocationAdapter.ViewHolder>() {

    // Хранение списка мероприятий
    private var eventsList: List<Event> = emptyList()

    // Обновление списка мероприятий
    fun submitList(newList: List<Event>) {
        eventsList = newList
        notifyDataSetChanged()
    }

    // ViewHolder для отображения данных мероприятия
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEventName: TextView = view.findViewById(R.id.tvEventName)
        val tvEventType: TextView = view.findViewById(R.id.tvEventType)
        val tvEventDescription: TextView = view.findViewById(R.id.tvEventDescription)
        val tvMinimumAge: TextView = view.findViewById(R.id.tvMinimumAge)
        val tvAgeInfo: TextView = view.findViewById(R.id.tvInfoAge)
        val tvEventRating: TextView = view.findViewById(R.id.tvEventRating)
        val layoutRating: View = view.findViewById(R.id.layoutEventRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Создание ViewHolder из layout элемента карточки мероприятия
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_location, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //Получение текущего мероприятия по позиции
        val event = eventsList[position]

        // Скрытие информационного сообщения
        holder.tvAgeInfo.visibility = View.GONE

        // Установка названия мероприятия
        holder.tvEventName.text = event.nameEvent

        // Установка типа мероприятия
        holder.tvEventType.text = event.type.name

        // Формирование описания мероприятия
        holder.tvEventDescription.text = if (event.description.isNullOrBlank()) {
            "Описание отсутствует"
        } else {
            event.description.take(120) + if (event.description.length > 120) "..." else ""
        }

        // Отображение минимального возраста
        holder.tvMinimumAge.text = if (event.minimumAge != null) {
            "${event.minimumAge}+"
        } else {
            "Без ограничений"
        }

        // Отображение информационного сообщения про возраст
        if (event.minimumAge != null){
            if (event.minimumAge >= 14){
                holder.tvAgeInfo.visibility = View.VISIBLE
            }
            else{
                holder.tvAgeInfo.visibility = View.GONE
            }

        }

        // Отображение оценки
        holder.tvEventRating.text = String.format("%.2f", event.rating ?: 0.0)

        // Обработка нажатия на отзывы у мероприятия
        holder.layoutRating.setOnClickListener {
            onRatingClick(event)
        }

        // Обработка нажатия на карточку
        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    override fun getItemCount() = eventsList.size
}