package com.example.secloud.users;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.example.secloud.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_SCREEN_TIMEOUT = 1500; // Duración de la pantalla de carga en milisegundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ViewGroup rootView = findViewById(android.R.id.content);
        Transition fade = new Fade();
        fade.setDuration(1000); // Duración de la transición en milisegundos
        TransitionManager.beginDelayedTransition(rootView, fade);
        rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.white)); // Establecer el color de fondo
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Este método se ejecutará después del tiempo de espera y abrirá la actividad de inicio de sesión
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_SCREEN_TIMEOUT);
    }
}