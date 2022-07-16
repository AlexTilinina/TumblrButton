package ru.ozon.tumblr_button

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.core.view.isVisible
import ru.ozon.tumblr_button.databinding.ActivityMainBinding
import ru.ozon.tumblr_button.tumblr_button.ButtonState

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //setContentView(R.layout.activity_main)

        initOnClickListeners()
        initTumblrButton()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    private fun initOnClickListeners() = with(binding) {
        btnTopStart.setOnClickListener {
            tumblrButton.setGravity(
                tumblrButton.gravity.and(Gravity.BOTTOM.inv()).or(Gravity.TOP)
                    .and(Gravity.END.inv()).or(Gravity.START)
            )
        }
        btnBottomStart.setOnClickListener {
            tumblrButton.setGravity(
                tumblrButton.gravity.and(Gravity.TOP.inv()).or(Gravity.BOTTOM)
                    .and(Gravity.END.inv()).or(Gravity.START)
            )
        }
        btnTopEnd.setOnClickListener {
            tumblrButton.setGravity(
                tumblrButton.gravity.and(Gravity.BOTTOM.inv()).or(Gravity.TOP)
                    .and(Gravity.START.inv()).or(Gravity.END)
            )
        }
        btnBottomEnd.setOnClickListener {
            tumblrButton.setGravity(
                tumblrButton.gravity.and(Gravity.TOP.inv()).or(Gravity.BOTTOM)
                    .and(Gravity.START.inv()).or(Gravity.END)
            )
        }
    }

    private fun initTumblrButton() {
        binding.tumblrButton.setOnClickListener {
            Toast.makeText(this, "Добавлен кот!", Toast.LENGTH_SHORT).show()
        }
        val btnList = listOf(
            ButtonState("Чатик", Color.parseColor("#354DA9"), R.drawable.ic_assistant),
            //blue
            ButtonState("Отправить", Color.parseColor("#43B581"), R.drawable.ic_backup), //green
            ButtonState("Показать код", Color.parseColor("#6E7882"), R.drawable.ic_code), //grey
            ButtonState(
                "Добавить координаты", Color.parseColor("#2B2F33"),
                R.drawable.ic_location
            ), //black
            ButtonState(
                "Прикрепить фото",
                Color.parseColor("#B9C7FF"),
                R.drawable.ic_photo
            ), //purple
        )
        btnList.forEachIndexed { index, buttonState ->
            // buttonState.label = "Кнопка ${index + 1}"
            buttonState.action = {
                Toast.makeText(this, "Нажали на ${buttonState.label}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        binding.tumblrButton.buttons = btnList

    }
}