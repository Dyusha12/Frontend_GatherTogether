package com.example.frontend_gathertogether.activity

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.frontend_gathertogether.R
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    // Основные элементы интерфейса
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI элементов из разметки
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuButton = findViewById(R.id.menuButton)

        // Получение NavController для управления фрагментами
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Подключение бокового меню к системе навигации
        NavigationUI.setupWithNavController(navigationView, navController)

        // Обработчик открытия бокового меню по нажатию
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Обработка выбора пунктов меню
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {

                // Переход на экран авторизации
                R.id.nav_home -> {
                    navController.navigate(R.id.authorizationFragment)
                }
                R.id.nav_profile -> {
                    // TODO: экран профиля
                }
                R.id.nav_settings -> {
                    // TODO: экран настроек
                }
            }

            // Закрытие меню после выбора пункта
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onBackPressed() {
        // Закрытие меню при нажатии назад
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}