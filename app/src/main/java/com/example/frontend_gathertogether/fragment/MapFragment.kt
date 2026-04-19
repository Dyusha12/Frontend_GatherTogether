package com.example.frontend_gathertogether.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.frontend_gathertogether.R
import com.example.frontend_gathertogether.adapter.EventLocationAdapter
import com.example.frontend_gathertogether.adapter.EventSessionAdapter
import com.example.frontend_gathertogether.adapter.ReviewAdapter
import com.example.frontend_gathertogether.models.Event
import com.example.frontend_gathertogether.models.EventLocation
import com.example.frontend_gathertogether.models.EventLocationReviewStats
import com.example.frontend_gathertogether.models.EventSession
import com.example.frontend_gathertogether.models.EventReviewStats
import com.example.frontend_gathertogether.models.ReviewEvent
import com.example.frontend_gathertogether.models.User
import com.example.frontend_gathertogether.services.ClientProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


class MapFragment : Fragment(R.layout.activity_map) {

    // Основные элементы интерфейса
    private lateinit var mapView: MapView
    private lateinit var mapController: IMapController
    private lateinit var etSearch: TextInputEditText
    private lateinit var bottomSheetEvents: BottomSheetBehavior<View>
    private lateinit var tvLocationTitle: TextView
    private lateinit var tvLocationAddress: TextView
    private lateinit var rvEvents: RecyclerView
    private lateinit var btnCloseBottom: ImageView
    private lateinit var eventsAdapter: EventLocationAdapter
    private lateinit var rvSessions: RecyclerView
    private lateinit var sessionAdapter: EventSessionAdapter
    private lateinit var btnDescription: MaterialButton
    private lateinit var btnReviews: MaterialButton
    private lateinit var layoutDescription: View
    private lateinit var btnBackFromDescription: ImageView
    private lateinit var tvDescriptionText: TextView
    private lateinit var layoutReviews: View
    private lateinit var rvReviews: RecyclerView
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var dropdownFilter: AutoCompleteTextView
    private lateinit var btnBackFromReviews: ImageView
    private lateinit var layoutSessions: View
    private lateinit var btnBackFromSessions: ImageView
    private lateinit var rbAverageRating: RatingBar
    private lateinit var tvAverageRatingText: TextView
    private lateinit var tvMapRating: TextView
    private lateinit var layoutMapRating: View
    private lateinit var btnAddReview: ImageView
    private lateinit var addReviewContainer: LinearLayout
    private lateinit var bottomSheetView: View
    private lateinit var tvFiveCount: TextView
    private lateinit var tvFourCount: TextView
    private lateinit var tvThreeCount: TextView
    private lateinit var tvTwoCount: TextView
    private lateinit var tvOneCount: TextView
    private lateinit var filterAddContainer: LinearLayout
    private lateinit var ratingDistribution: LinearLayout
    private lateinit var distributionMessage: TextView


    // Переменные для работы логики
    private var currentSelectedMarker: Marker? = null
    private var currentLoadEventsJob: Job? = null
    private var currentLocation: EventLocation? = null
    private var currentSelectedEvent: Event? = null
    private var userId: String? = null
    private var allReviews: List<ReviewEvent> = emptyList()
    private var editingReviewId: String? = null

    // Константы
    companion object {
        private val BASE_URL_LOCATIONS = "http://10.0.2.2:8080/gather-together/locations"
        private val BASE_URL_EVENTS = "http://10.0.2.2:8080/gather-together/events"
        private val BASE_URL_SESSIONS = "http://10.0.2.2:8080/gather-together/sessions"
        private val BASE_URL_PARTICIPANT = "http://10.0.2.2:8080/gather-together/participants"
        private val BASE_URL_EVENT_REVIEWS = "http://10.0.2.2:8080/gather-together/event-reviews"

        // Ограничение карты
        private val SPB_CENTER = GeoPoint(59.9386, 30.3141)
        private val SPB_BOUNDS = BoundingBox(60.05, 30.60, 59.70, 29.85)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация User-Agent для работы карты
        Configuration.getInstance().userAgentValue = "Frontend_test_application"

        // Инициализация UI элементов из разметки
        mapView = view.findViewById(R.id.mapView)
        bottomSheetView = view.findViewById(R.id.bottomSheet)
        bottomSheetEvents = BottomSheetBehavior.from(bottomSheetView)
        tvLocationTitle = view.findViewById(R.id.tvLocationTitle)
        tvLocationAddress = view.findViewById(R.id.tvLocationAddress)
        rvEvents = view.findViewById(R.id.rvEventsAtLocation)
        btnCloseBottom = view.findViewById(R.id.btnCloseBottom)
        rvSessions = view.findViewById(R.id.rvSessions)
        btnDescription = view.findViewById(R.id.btnDescription)
        btnReviews = view.findViewById(R.id.btnReviews)
        layoutDescription = view.findViewById(R.id.layoutDescription)
        btnBackFromDescription = view.findViewById(R.id.btnBackFromDescription)
        tvDescriptionText = view.findViewById(R.id.tvDescriptionText)
        layoutReviews = view.findViewById(R.id.layoutReviews)
        rvReviews = view.findViewById(R.id.rvReviews)
        dropdownFilter = view.findViewById(R.id.dropdownFilter)
        btnBackFromReviews = view.findViewById(R.id.btnBackFromReviews)
        layoutSessions = view.findViewById(R.id.layoutSessions)
        btnBackFromSessions = view.findViewById(R.id.btnBackFromSessions)
        rbAverageRating = view.findViewById(R.id.rbAverageRating)
        tvAverageRatingText = view.findViewById(R.id.tvAverageRatingText)
        btnAddReview = view.findViewById(R.id.btnAddReview)
        tvMapRating = view.findViewById(R.id.tvMapRating)
        layoutMapRating = view.findViewById(R.id.layoutMapRating)
        addReviewContainer = view.findViewById(R.id.addReviewContainer)
        tvFiveCount = view.findViewById(R.id.tvFiveCount)
        tvFourCount = view.findViewById(R.id.tvFourCount)
        tvThreeCount = view.findViewById(R.id.tvThreeCount)
        tvTwoCount = view.findViewById(R.id.tvTwoCount)
        tvOneCount = view.findViewById(R.id.tvOneCount)
        filterAddContainer = view.findViewById(R.id.filterAddContainer)
        ratingDistribution = view.findViewById(R.id.layoutRatingDistribution)
        distributionMessage = view.findViewById(R.id.distribution_review_message)

        // Получение идентификатора пользователя из настроек
        userId = getUserId()

        // Стартовое состояние
        bottomSheetEvents.state = BottomSheetBehavior.STATE_HIDDEN

        // Обработка закрытие нижнего листа
        btnCloseBottom.setOnClickListener {
            hideBottomSheet()
        }

        // Обработка кнопок масштабирования карты
        view.findViewById<View>(R.id.btnZoomIn).setOnClickListener { mapController.zoomIn() }
        view.findViewById<View>(R.id.btnZoomOut).setOnClickListener { mapController.zoomOut() }

        // Инициализация адаптера мероприятий
        eventsAdapter = EventLocationAdapter(
            onEventClick = { event ->
                showSessionsForEvent(event)
                currentSelectedEvent = event
            },
            onRatingClick = { event ->
                showEventReviews(event)
            }
        )

        // Инициализация адаптера сеансов
        sessionAdapter = EventSessionAdapter(
            currentUserCode = userId!!,
            onParticipateClick = { session ->
                registerSession(session)
            },
            onCancelClick = { session ->
                cancelParticipation(session)
            }
        )

        // Обработка открытие описания
        btnDescription.setOnClickListener {
            showDescription()
        }

        // Обработка закрытия описания
        btnBackFromDescription.setOnClickListener {
            hideDescription()
        }

        // Обработка открытие отзывов
        btnReviews.setOnClickListener {
            showLocationStats(currentLocation!!.locationCode)
        }

        // Обработка закрытия отзывов
        btnBackFromReviews.setOnClickListener {
            hideLocationStats()
        }

        // Обработка выбора фильтра для отзывов
        dropdownFilter.setOnItemClickListener { _, _, _, _ ->
            applyFilters()
        }

        // Обработка открытия сессий мероприятия
        btnBackFromSessions.setOnClickListener {
            showEventsBack()
        }

        // Обработка открытия диалога добавления отзыва
        addReviewContainer.setOnClickListener{
            showReviewDialog(null)
        }

        // Настройка recyclerView для мероприятий
        rvEvents.layoutManager = LinearLayoutManager(requireContext())
        rvEvents.adapter = eventsAdapter

        // Настройка recyclerView для сеансов
        rvSessions.layoutManager = LinearLayoutManager(requireContext())
        rvSessions.adapter = sessionAdapter

        // Установка значений для фильтра
        val options = listOf(
            "Все отзывы",
            "Сначала новые",
            "Сначала старые",
            "Сначала положительные",
            "Сначала отрицательные"
        )

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        dropdownFilter.setAdapter(adapter)
        dropdownFilter.setText("Все отзывы", false)

        // Настройка recyclerView для отзывов
        reviewAdapter = ReviewAdapter(userId) { review ->
            showReviewDialog(review)
        }
        rvReviews.layoutManager = LinearLayoutManager(requireContext())
        rvReviews.adapter = reviewAdapter


        // Настройка карты и загрузка данных
        setupMap()
        loadLocations()

        // Увеличение размера карты
        mapView.setTilesScaledToDpi(true)

        bottomSheetEvents.peekHeight = 400
        // Перемещение камеры карты в центр
        mapController.animateTo(SPB_CENTER, 15.0, 1000L)
    }

    // Настройка параметров карты
    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(false)

        mapView.setScrollableAreaLimitDouble(SPB_BOUNDS)
        mapView.setMinZoomLevel(13.0)
        mapView.setMaxZoomLevel(23.0)

        mapController = mapView.controller
        mapController.setZoom(15.8)
        mapController.setCenter(SPB_CENTER)
    }

    // Загрузка всех локаций и отображение их на карте
    private fun loadLocations() {
        lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                val locations: List<EventLocation> = client.get(BASE_URL_LOCATIONS).body() // Получение локаций

                // Очистка старых маркеров
                mapView.overlays.clear()

                // Добавление новых маркеров
                locations.forEach { location ->
                    if (location.latitude != null && location.longitude != null) {
                        val marker = createMarker(location)
                        mapView.overlays.add(marker)
                    }
                }
                mapView.invalidate()
            } catch (e: Exception) {
                Log.e("MapFragment", "Ошибка загрузки локаций", e)
            }
        }
    }

    // Загрузка мероприятий для выбранной локации
    private fun loadEventsForLocation(location: EventLocation): Job {
        return lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                val allEvents: List<Event> = client.get(BASE_URL_EVENTS).body() // Получение мероприятий

                // Отбор мероприятий для конкретной локации
                val eventsLocation = allEvents.filter {
                    it.location.locationCode?.trim() == location.locationCode?.trim()
                }
                Log.d("MapFragment", "Для локации '${location.nameLocation}' найдено ${eventsLocation.size} мероприятий")
                eventsAdapter.submitList(eventsLocation)

                if (eventsLocation.isEmpty()) {
                    tvLocationTitle.text = "${location.nameLocation} (мероприятий пока нет)"
                }
            } catch (e: Exception) {
                Log.e("MapFragment", "Ошибка загрузки мероприятий", e)
                eventsAdapter.submitList(emptyList())
            }
        }
    }

    // Загрузка сеансов выбранного мероприятия с его участниками
    private fun loadSessionsForEvent(eventCode: String) : Job? {
        return lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                val sessions: List<EventSession> = client.get("$BASE_URL_SESSIONS/event/$eventCode").body() // Получение сеансов мероприятия

                // Получение участников мероприятия
                val data = sessions.map { session ->
                    val users: List<User> = client.get("$BASE_URL_PARTICIPANT/event-session/${session.sessionCode}").body()
                    Pair(session, users)
                }
                sessionAdapter.submitList(data)
            } catch (e: Exception) {
                Log.e("MapFragment", "Ошибка загрузки сеансов", e)
            }
        }
    }

    // Регистрация пользователя на выбранный сеанс
    private fun registerSession(session: EventSession) {
        if (userId != null) {
            lifecycleScope.launch {
                try {

                    // Запрос на запись участника на сеанс мероприятия
                    val client = ClientProvider(requireContext()).instance()
                    client.post("$BASE_URL_PARTICIPANT/register") {
                        parameter("sessionCode", session.sessionCode)
                        parameter("userCode", userId)
                    }
                    Toast.makeText(requireContext(), "Вы записались!", Toast.LENGTH_SHORT).show()

                    // Обновление данных
                    loadSessionsForEvent(session.eventCode)
                } catch (e: Exception) {
                    Log.e("MapFragment", "Ошибка регистрации", e)
                    Toast.makeText(
                        requireContext(),
                        e.message ?: "Ошибка регистрации",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Отмена участия пользователя в сеансе
    private fun cancelParticipation(session: EventSession) {
        lifecycleScope.launch {
            try {

                // Запрос на удаление записи об участии пользователя в сеансе мероприятия
                val client = ClientProvider(requireContext()).instance()
                client.delete("$BASE_URL_PARTICIPANT/cancel") {
                    parameter("sessionCode", session.sessionCode)
                    parameter("userCode", userId)
                }
                Toast.makeText(requireContext(), "Участие отменено", Toast.LENGTH_SHORT).show()
                loadSessionsForEvent(session.eventCode)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка отмены", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Обновление отзыва
    private fun updateReview(reviewId: String, rating: Int, comment: String?, eventCode: String) {
        lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                client.put("$BASE_URL_EVENT_REVIEWS/$reviewId") {
                    parameter("rating", rating)
                    parameter("comment", comment)
                }
                Toast.makeText(requireContext(), "Отзыв обновлён", Toast.LENGTH_SHORT).show()
                loadEventReviews(eventCode)
                loadEventRating(eventCode)
                refreshEvents()
                loadLocationRatingStats(currentLocation!!.locationCode)
            } catch (e: Exception) {
                Log.e("MapFragment", "Ошибка обновления отзыва", e)
                Toast.makeText(requireContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Проверка на существование отзыва пользователя на конкретном мероприятии
    private fun checkUserReview() {
        lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                val reviews: List<ReviewEvent> = client.get("$BASE_URL_EVENT_REVIEWS/user/$userId").body()
                val eventCode = currentSelectedEvent?.eventCode ?: return@launch
                val alreadyReviewed = reviews.any {
                    it.eventCode == eventCode
                }
                addReviewContainer.isEnabled = !alreadyReviewed // Управление отображением кнопки редактирования комментария
            } catch (e: Exception) {
                Log.e("MapFragment", "Ошибка проверки отзыва", e)
            }
        }
    }

    // Загрузка списка отзывов мероприятия
    private fun loadEventReviews(eventCode: String) {
        lifecycleScope.launch {
            val client = ClientProvider(requireContext()).instance()
            allReviews = client.get("$BASE_URL_EVENT_REVIEWS/event/$eventCode").body()
            applyFilters()
        }
    }

    // Загрузка рейтинга мероприятия
    private fun loadEventRating(eventCode: String) {
        lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                val stats: EventReviewStats = client.get("$BASE_URL_EVENT_REVIEWS/event/$eventCode/stats").body()

                // Обновление UI рейтинга мероприятия
                updateRatingBar(stats)
            } catch (e: Exception) {
                Log.e("MapFragment", "Ошибка рейтинга события", e)
            }
        }
    }

    // Удаление отзыва мероприятия
    private fun deleteReview(reviewId: String, eventCode: String) {
        lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                client.delete("$BASE_URL_EVENT_REVIEWS/$reviewId")
                Toast.makeText(requireContext(), "Отзыв удалён", Toast.LENGTH_SHORT).show()

                // Обновление данных после удаления
                loadEventReviews(eventCode)
                loadEventRating(eventCode)
                refreshEvents()
                loadLocationRatingStats(currentLocation!!.locationCode)
            } catch (e: Exception) {
                Log.e("MapFragment", "Ошибка удаления отзыва", e)
                Toast.makeText(requireContext(), "Ошибка удаления", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Получение идентификатора пользователя из настроек
    private fun getUserId(): String? {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        return prefs.getString("USER_ID", null)
    }

    // Создание маркера на карте для конкретной локации
    private fun createMarker(location: EventLocation): Marker {
        return Marker(mapView).apply {
            position = GeoPoint(location.latitude!!.toDouble(), location.longitude!!.toDouble())
            title = location.nameLocation
            snippet = location.address
            icon = ContextCompat.getDrawable(requireContext(), R.mipmap.ic_map_marker_orange)

            setOnMarkerClickListener { _, _ ->
                onMarkerClicked(location, this)
                true
            }
        }
    }

    // Обработка клика по маркеру
    private fun onMarkerClicked(location: EventLocation, newMarker: Marker) {
        currentSelectedMarker?.icon = ContextCompat.getDrawable(requireContext(), R.mipmap.ic_map_marker_orange)
        newMarker.icon = ContextCompat.getDrawable(requireContext(), R.mipmap.ic_map_marker_selected)
        currentSelectedMarker = newMarker
        mapView.invalidate()
        showLocationBottomSheet(location)
    }

    // Отображение диалога добавления или редактирования отзыва
    private fun showReviewDialog(review: ReviewEvent? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_review, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create() // Инициализация диалога
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // Инициализация UI элементов из разметки
        val rb = dialogView.findViewById<RatingBar>(R.id.rbDialogRating)
        val etComment = dialogView.findViewById<EditText>(R.id.etDialogComment)
        val btnBack = dialogView.findViewById<ImageView>(R.id.btnBackDialog)
        val btnSubmit = dialogView.findViewById<MaterialButton>(R.id.btnSubmitReview)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btnDeleteReview)

        // Определение режима редактирования
        val isEdit = review != null
        editingReviewId = review?.reviewCodeEvent

        if (isEdit) {
            rb.rating = review!!.rating.toFloat()
            etComment.setText(review.comment ?: "")
            btnSubmit.text = "Изменить отзыв"
            btnDelete.isVisible = true
        } else {
            btnSubmit.text = "Добавить"
            btnDelete.isVisible = false
        }

        // Обработка закрытие диалога
        btnBack.setOnClickListener {
            dialog.dismiss()
        }

        // Обработка нажатия создания или редактирования отзыва
        btnSubmit.setOnClickListener {
            val rating = rb.rating.toInt()
            val comment = etComment.text?.toString()?.trim()
            val event = currentSelectedEvent ?: return@setOnClickListener
            val error = validateComment(comment)

            // Валидация комментария
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEdit) {
                updateReview(review!!.reviewCodeEvent, rating, comment, event.eventCode!!)
            } else {
                createReview(event.eventCode, userId!!, rating, comment)
            }
            dialog.dismiss() // Закрытие диалога
        }

        // Обработка нажатия удаления отзыва
        btnDelete.setOnClickListener {
            val reviewId = review?.reviewCodeEvent ?: return@setOnClickListener
            val event = currentSelectedEvent ?: return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setTitle("Удалить отзыв?")
                .setMessage("Это действие нельзя отменить")
                .setPositiveButton("Удалить") { _, _ ->
                    deleteReview(reviewId, event.eventCode) // Удаление отзыва
                }
                .setNegativeButton("Отмена", null)
                .show()
            dialog.dismiss() // Закрытие диалога
        }
        dialog.show()
    }

    // Валидация текста комментария
    private fun validateComment(comment: String?): String? {
        if (comment.isNullOrBlank()) return null
        val trimmed = comment.trim()

        // Проверка минимальной длины
        if (trimmed.length < 5) {
            return "Комментарий должен быть минимум 5 символов"
        }

        // Проверка содержимого
        if (trimmed.all { it.isDigit() || it.isWhitespace() || !it.isLetter() }) {
            return "Комментарий не может состоять только из цифр или символов"
        }
        return null
    }

    // Создание нового отзыва и обновление данных
    private fun createReview(eventCode: String, userCode: String, rating: Int, comment: String?) {
        lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                // Отправка запроса на создание отзыва
                client.post(BASE_URL_EVENT_REVIEWS) {
                    parameter("typeCode", "RVWTP1")
                    parameter("userCode", userCode)
                    parameter("eventCode", eventCode)
                    parameter("rating", rating)
                    parameter("comment", comment)
                }
                Toast.makeText(requireContext(), "Отзыв добавлен", Toast.LENGTH_SHORT).show()

                // Обновление данных после добавления
                loadEventReviews(eventCode)
                loadEventRating(eventCode)
                refreshEvents()
                loadLocationRatingStats(currentLocation!!.locationCode)
            } catch (e: Exception) {
                Log.e("MapFragment", "Ошибка создания отзыва", e)
                Toast.makeText(requireContext(), "Ошибка добавления отзыва", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Отображение отзывов мероприятия
    private fun showEventReviews(event: Event) {
        currentSelectedEvent = event
        tvLocationTitle.text = event.nameEvent
        tvLocationAddress.text = "Отзывы мероприятия"

        // Загрузка данных мероприятия
        loadEventReviews(event.eventCode)
        loadEventRating(event.eventCode)
        checkUserReview()

        // Настройка UI под отзывы мероприятия
        filterAddContainer.isVisible = true
        rvEvents.isVisible = false
        rvReviews.isVisible = true
        layoutReviews.isVisible = true
        addReviewContainer.isVisible = true
        ratingDistribution.isVisible = false
        btnDescription.isVisible = false
        btnReviews.isVisible = false
        bottomSheetEvents.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // Отображение нижнего листа с мероприятиями выбранной локации
    private fun showLocationBottomSheet(location: EventLocation) {
        currentLocation = location
        tvLocationTitle.text = location.nameLocation
        tvLocationAddress.text = location.address

        // Отмена загрузки
        currentLoadEventsJob?.cancel()

        // Настройка UI под список мероприятий
        bottomSheetEvents.state = BottomSheetBehavior.STATE_HALF_EXPANDED // Установка состояния нижненого листа
        layoutDescription.isVisible = false
        layoutSessions.isVisible = false
        rvEvents.isVisible = true
        btnDescription.isVisible = true
        btnReviews.isVisible = true
        rvReviews.isVisible = false
        layoutReviews.isVisible = false
        layoutDescription.isVisible = false
        layoutMapRating.isVisible = true
        sessionAdapter.submitList(emptyList())
        eventsAdapter.submitList(emptyList())

        // Загрузка данных
        currentLoadEventsJob = loadEventsForLocation(location)
        loadLocationRatingStats(currentLocation!!.locationCode)
    }

    // Отображение нижнего листа с сеансами выбранного мероприятия
    private fun showSessionsForEvent(event: Event) {
        tvLocationTitle.text = event.nameEvent
        tvLocationAddress.text = "Сеансы мероприятия"

        // Настройка UI под сеансы
        rvEvents.isVisible = false
        layoutSessions.isVisible = true
        btnDescription.isVisible = false
        btnReviews.isVisible = false
        bottomSheetEvents.state = BottomSheetBehavior.STATE_EXPANDED // Разворачивание нижнего листа
        sessionAdapter.submitList(emptyList())
        currentLoadEventsJob?.cancel() // Отмена загрузки
        currentLoadEventsJob = loadSessionsForEvent(event.eventCode) // Загрузка сессий мероприятий
    }

    // Закрытие нижнего листа и сброс состояния карты
    private fun hideBottomSheet() {
        bottomSheetEvents.state = BottomSheetBehavior.STATE_HIDDEN
        currentSelectedMarker?.icon = ContextCompat.getDrawable(requireContext(), R.mipmap.ic_map_marker_orange)
        currentSelectedMarker = null // Сброс выбранного маркера
        currentLoadEventsJob?.cancel() // Отмена текущей загрузки мероприятий
        mapView.invalidate() // Перерисовка карты
    }

    // Возврат к списку мероприятий из списка сеансов
    private fun showEventsBack() {
        val location = currentLocation ?: return
        tvLocationTitle.text = location.nameLocation
        tvLocationAddress.text = location.address

        // Настройка UI
        layoutSessions.isVisible = false
        rvEvents.isVisible = true
        btnDescription.isVisible = true
        btnReviews.isVisible = true
        bottomSheetEvents.state = BottomSheetBehavior.STATE_EXPANDED // Разворачивание нижнего листа
    }

    // Отображение описания локации
    private fun showDescription() {
        val location = currentLocation ?: return
        tvDescriptionText.text = location.description ?: "Описание отсутствует"

        // Настройка UI
        rvEvents.isVisible = false
        btnDescription.isVisible = false
        btnReviews.isVisible = false
        layoutDescription.isVisible = true
        bottomSheetEvents.state = BottomSheetBehavior.STATE_EXPANDED // Разворачивание нижнего листа
    }

    // Скрытие описания и возврат к списку мероприятий
    private fun hideDescription() {
        layoutDescription.isVisible = false
        rvEvents.isVisible = true
        btnDescription.isVisible = true
        btnReviews.isVisible = true
        bottomSheetEvents.state = BottomSheetBehavior.STATE_EXPANDED // Разворачивание нижнего листа
    }

    // Скрытие статистики локации
    private fun hideLocationStats(){
        val location = currentLocation ?: return
        tvLocationTitle.text = location.nameLocation
        tvLocationAddress.text = location.address

        // Настройка UI
        filterAddContainer.isVisible = false
        layoutReviews.isVisible = false
        rvEvents.isVisible = true
        btnDescription.isVisible = true
        btnReviews.isVisible = true
        ratingDistribution.isVisible = false
        distributionMessage.isVisible = false
        refreshEvents() // Обновление списка мероприятий
        bottomSheetEvents.state = BottomSheetBehavior.STATE_EXPANDED // Разворачивание нижнего листа
    }

    // Отображение статистики локации
    private fun showLocationStats(locationCode: String){
        // Настройка UI
        rvEvents.isVisible = false
        layoutSessions.isVisible = false
        rvReviews.isVisible = true
        layoutReviews.isVisible = true
        btnDescription.isVisible = false
        btnReviews.isVisible = false
        filterAddContainer.isVisible = false
        layoutMapRating.isVisible = true
        ratingDistribution.isVisible = true
        distributionMessage.isVisible = true
        rvReviews.isVisible = false
        loadLocationRatingStats(locationCode)
        bottomSheetEvents.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // Применение выбранного фильтра к списку отзывов
    private fun applyFilters() {
        val selected = dropdownFilter.text.toString()
        val filtered = when (selected) {
            "Сначала положительные" -> allReviews.sortedByDescending { it.rating }
            "Сначала отрицательные" -> allReviews.sortedBy { it.rating }
            "Сначала новые" -> allReviews.sortedByDescending { it.dateCreation }
            "Сначала старые" -> allReviews.sortedBy { it.dateCreation }
            "Все отзывы" -> allReviews
            else -> allReviews
        }
        reviewAdapter.submitList(filtered) // Обновление списка в адаптере
    }

    // Обновление UI рейтинга мероприятия
    private fun updateRatingBar(stats: EventReviewStats) {
        val rating = stats.rating.toFloat()
        val count = stats.reviewCount
        rbAverageRating.rating = rating
        tvAverageRatingText.text = String.format("%.2f (%d)", rating, count)
    }

    // Перезагрузка списка мероприятий текущей локации
    private fun refreshEvents() {
        val location = currentLocation ?: return
        currentLoadEventsJob?.cancel()
        currentLoadEventsJob = loadEventsForLocation(location)
    }

    // Возобновление работы MapView
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    // Приостановка работы MapView
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    // Загрузка статистики рейтинга локации
    private fun loadLocationRatingStats(locationCode: String) {
        lifecycleScope.launch {
            try {
                val client = ClientProvider(requireContext()).instance()
                val stats: EventLocationReviewStats = client.get("$BASE_URL_LOCATIONS/$locationCode/rating-stats").body()

                // Обновление UI локации
                updateLocationRating(stats)
            } catch (e: Exception) {
                Log.e("MapFragment", "Ошибка загрузки статистики локации", e)
            }
        }
    }

    // Обновление UI рейтинга локации
    private fun updateLocationRating(stats: EventLocationReviewStats) {
        val rating = stats.averageRating.toFloat()
        val count = stats.totalReviews
        if (count == 0) {
            rbAverageRating.rating = 0f
            tvAverageRatingText.text = "Нет отзывов"
            tvFiveCount.text = "0"
            tvFourCount.text = "0"
            tvThreeCount.text = "0"
            tvTwoCount.text = "0"
            tvOneCount.text = "0"
            return
        }

        layoutMapRating.isVisible = true
        tvMapRating.text = String.format("%.2f", stats.averageRating)
        rbAverageRating.rating = rating
        tvAverageRatingText.text = String.format("%.2f (%d)", stats.averageRating, count)
        tvFiveCount.text = stats.fiveStars.toString()
        tvFourCount.text = stats.fourStars.toString()
        tvThreeCount.text = stats.threeStars.toString()
        tvTwoCount.text = stats.twoStars.toString()
        tvOneCount.text = stats.oneStars.toString()
    }
}