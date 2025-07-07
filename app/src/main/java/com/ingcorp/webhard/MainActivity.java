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

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 타이틀바만 숨기기 (상태바는 유지)
        if (getActionBar() != null) {
            getActionBar().hide();
        }

        // 루트 레이아웃 생성
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.WHITE); // 배경색 명시적 설정

        // 레이아웃 파라미터 설정
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        rootLayout.setLayoutParams(rootParams);

        // 간단한 텍스트 뷰 생성
        TextView textView = new TextView(this);
        textView.setText("Hello, Android!");
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24); // SP 단위로 명시
        textView.setTextColor(Color.BLACK); // 텍스트 색상 명시적 설정
        textView.setGravity(android.view.Gravity.CENTER);

        // 텍스트뷰 레이아웃 파라미터 설정
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        textView.setLayoutParams(textParams);

        // 최소 패딩 설정 (구형 기기 호환성)
        int padding = 50; // 픽셀 단위
        textView.setPadding(padding, padding, padding, padding);

        // 레이아웃에 텍스트뷰 추가
        rootLayout.addView(textView);

        // Android 버전별 윈도우 인셋 처리 (상태바 영역 고려)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) 이상
            rootLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
                    // 상태바 높이만큼 상단 패딩 추가
                    v.setPadding(systemBars.left, systemBars.top,
                            systemBars.right, 0); // 하단은 네비게이션바 영역 제외
                    return WindowInsets.CONSUMED;
                }
            });
            getWindow().setDecorFitsSystemWindows(false);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            // Android 5.0 (API 20) ~ Android 10 (API 29)
            rootLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    // 상태바 높이만큼 상단 패딩 추가
                    v.setPadding(insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(),
                            0); // 하단은 네비게이션바 영역 제외
                    return insets;
                }
            });
        }

        // 레이아웃을 액티비티의 콘텐츠 뷰로 설정
        setContentView(rootLayout);
    }
}