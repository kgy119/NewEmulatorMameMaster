package com.ingcorp.webhard;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Insets;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // EdgeToEdge 스타일 적용 (모든 API 버전에서 일관된 동작)
        setupEdgeToEdge();

        // colorPrimary 및 colorPrimaryDark 색상 가져오기
        int colorPrimary = getResources().getColor(R.color.colorPrimary);
        int colorPrimaryDark = getResources().getColor(R.color.colorPrimaryDark);

        // 루트 레이아웃 생성
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(colorPrimary);

        // 레이아웃 파라미터 설정
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        rootLayout.setLayoutParams(rootParams);

        // 간단한 텍스트 뷰 생성
        TextView textView = new TextView(this);
        textView.setText("Hello, Android!");
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);

        // 보라색 배경에 흰색 텍스트
        int textColor = getResources().getColor(R.color.white);
        textView.setTextColor(textColor);
        textView.setGravity(android.view.Gravity.CENTER);

        // 텍스트뷰 레이아웃 파라미터 설정
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        textView.setLayoutParams(textParams);

        // 최소 패딩 설정
        int padding = 50;
        textView.setPadding(padding, padding, padding, padding);

        // 레이아웃에 텍스트뷰 추가
        rootLayout.addView(textView);

        // WindowInsets 처리 (EdgeToEdge에 맞춘 패딩 적용)
        setupWindowInsets(rootLayout);

        // 레이아웃을 액티비티의 콘텐츠 뷰로 설정
        setContentView(rootLayout);
    }

    /**
     * enableEdgeToEdge의 내부 로직을 참고한 EdgeToEdge 설정
     * 모든 API 버전에서 일관된 동작을 보장
     */
    private void setupEdgeToEdge() {
        Window window = getWindow();
        View decorView = window.getDecorView();

        int colorPrimaryDark = getResources().getColor(R.color.colorPrimaryDark);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29+
            // API 29+ EdgeToEdge 설정
            setupEdgeToEdgeApi29(window, decorView, colorPrimaryDark);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26+
            // API 26+ EdgeToEdge 설정
            setupEdgeToEdgeApi26(window, decorView, colorPrimaryDark);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // API 23+
            // API 23+ EdgeToEdge 설정
            setupEdgeToEdgeApi23(window, decorView, colorPrimaryDark);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // API 21+
            // API 21+ EdgeToEdge 설정
            setupEdgeToEdgeApi21(window, decorView, colorPrimaryDark);
        } else {
            // API 21 미만 - 기본 설정
            setupEdgeToEdgeBase(window, decorView);
        }
    }

    /**
     * API 29+ (Android 10+) EdgeToEdge 설정
     */
    private void setupEdgeToEdgeApi29(Window window, View decorView, int colorPrimaryDark) {
        // DecorView가 시스템 윈도우에 맞지 않도록 설정
        setDecorFitsSystemWindows(window, false);

        // 상태바와 네비게이션바를 우리 색상으로 설정 (투명하지 않음)
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);

        // 시스템 대비 강제 비활성화 (우리가 색상을 제어)
        window.setStatusBarContrastEnforced(false);
        window.setNavigationBarContrastEnforced(false);

        // 상태바 아이콘을 밝은 색으로 (어두운 배경이므로)
        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    /**
     * API 26+ (Android 8.0+) EdgeToEdge 설정
     */
    private void setupEdgeToEdgeApi26(Window window, View decorView, int colorPrimaryDark) {
        setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);

        // 상태바와 네비게이션바 아이콘을 밝은 색으로
        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    /**
     * API 23+ (Android 6.0+) EdgeToEdge 설정
     */
    private void setupEdgeToEdgeApi23(Window window, View decorView, int colorPrimaryDark) {
        setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);

        // 상태바 아이콘을 밝은 색으로 (네비게이션바는 API 26부터 지원)
        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    /**
     * API 21+ (Android 5.0+) EdgeToEdge 설정
     */
    private void setupEdgeToEdgeApi21(Window window, View decorView, int colorPrimaryDark) {
        setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);

        // API 21-22에서는 상태바 아이콘 색상 제어 불가
    }

    /**
     * API 21 미만 EdgeToEdge 설정
     */
    private void setupEdgeToEdgeBase(Window window, View decorView) {
        // API 21 미만에서는 상태바 색상 설정 불가
        // 기본 시스템 동작 유지
    }

    /**
     * DecorView가 시스템 윈도우에 맞는지 설정 (API 호환성 고려)
     */
    private void setDecorFitsSystemWindows(Window window, boolean decorFitsSystemWindows) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+
            window.setDecorFitsSystemWindows(decorFitsSystemWindows);
        } else {
            // API 30 미만에서는 SYSTEM_UI_FLAG로 제어
            View decorView = window.getDecorView();
            if (!decorFitsSystemWindows) {
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                decorView.setSystemUiVisibility(flags);
            }
        }
    }

    /**
     * WindowInsets 처리 설정
     */
    private void setupWindowInsets(LinearLayout rootLayout) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ WindowInsets 처리
            rootLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
                    // 상태바와 네비게이션바 영역을 고려한 패딩 설정
                    v.setPadding(systemBars.left, systemBars.top,
                            systemBars.right, systemBars.bottom);
                    return WindowInsets.CONSUMED;
                }
            });
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            // API 20-29 WindowInsets 처리
            rootLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    // 시스템 윈도우 인셋을 고려한 패딩 설정
                    v.setPadding(insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom());
                    return insets;
                }
            });
        }
    }
}