package com.ingcorp.webhard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Insets;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import com.ingcorp.webhard.model.VersionResponse;
import com.ingcorp.webhard.network.NetworkClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends Activity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 3000; // 3초
    private boolean isVersionCheckCompleted = false;
    private boolean isSplashTimeCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // EdgeToEdge 스타일 적용
        setupEdgeToEdge();

        // 스플래시 레이아웃 생성
        LinearLayout splashLayout = createSplashLayout();
        setContentView(splashLayout);

        // WindowInsets 처리
        setupWindowInsets(splashLayout);

        // 스플래시 애니메이션 시작
        startSplashAnimations();

        // 버전 체크 시작
        checkVersion();

        // 스플래시 타이머 시작
        startSplashTimer();
    }

    /**
     * 버전 체크 수행
     */
    private void checkVersion() {
        NetworkClient.getApiService().getVersionInfo().enqueue(new Callback<VersionResponse>() {
            @Override
            public void onResponse(Call<VersionResponse> call, Response<VersionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    handleVersionResponse(response.body());
                } else {
                    Log.e(TAG, "Version check failed: " + response.code());
                    onVersionCheckCompleted();
                }
            }

            @Override
            public void onFailure(Call<VersionResponse> call, Throwable t) {
                Log.e(TAG, "Version check error", t);
                onVersionCheckCompleted();
            }
        });
    }

    /**
     * 버전 응답 처리
     */
    private void handleVersionResponse(VersionResponse versionResponse) {
        VersionResponse.Root root = versionResponse.getRoot();

        if (root.isCheck()) {
            String currentPackageName = getPackageName();
            String serverPackageName = root.getPackageName();

            // 패키지명 비교
            if (!currentPackageName.equals(serverPackageName)) {
                showPackageUpdateDialog(serverPackageName);
                return;
            }

            // 버전 코드 비교
            int currentVersionCode = getCurrentVersionCode();
            int serverVersionCode = root.getNowVersionCode();

            if (currentVersionCode < serverVersionCode) {
                showVersionUpdateDialog();
                return;
            }
        }

        // 업데이트가 필요하지 않은 경우
        onVersionCheckCompleted();
    }

    /**
     * 현재 앱의 버전 코드 가져오기
     */
    private int getCurrentVersionCode() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) packageInfo.getLongVersionCode();
            } else {
                return packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get version code", e);
            return 0;
        }
    }

    /**
     * 패키지 업데이트 다이얼로그 표시
     */
    private void showPackageUpdateDialog(String packageName) {
        new AlertDialog.Builder(this)
                .setTitle("앱 업데이트 필요")
                .setMessage("새로운 버전의 앱이 있습니다. 플레이스토어에서 업데이트해주세요.")
                .setPositiveButton("업데이트", (dialog, which) -> {
                    openPlayStore(packageName);
                })
                .setNegativeButton("나중에", (dialog, which) -> {
                    // 앱 종료
                    finish();
                    System.exit(0);
                })
                .setCancelable(false)
                .show();
    }

    /**
     * 버전 업데이트 다이얼로그 표시
     */
    private void showVersionUpdateDialog() {
        new AlertDialog.Builder(this)
                .setTitle("앱 업데이트")
                .setMessage("새로운 버전이 출시되었습니다. 업데이트하시겠습니까?")
                .setPositiveButton("업데이트", (dialog, which) -> {
                    openPlayStore(getPackageName());
                })
                .setNegativeButton("나중에", (dialog, which) -> {
                    // 앱 종료
                    finish();
                    System.exit(0);
                })
                .setCancelable(false)
                .show();
    }

    /**
     * 플레이스토어 열기
     */
    private void openPlayStore(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName));
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            startActivity(intent);
        }
        finish();
    }

    /**
     * 버전 체크 완료 처리
     */
    private void onVersionCheckCompleted() {
        isVersionCheckCompleted = true;
        checkAndProceedToMain();
    }

    /**
     * 스플래시 타이머 시작
     */
    private void startSplashTimer() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                isSplashTimeCompleted = true;
                checkAndProceedToMain();
            }
        }, SPLASH_DURATION);
    }

    /**
     * 메인 액티비티로 진행 체크
     */
    private void checkAndProceedToMain() {
        if (isVersionCheckCompleted && isSplashTimeCompleted) {
            startMainActivity();
        }
    }

    /**
     * 스플래시 레이아웃 생성
     */
    private LinearLayout createSplashLayout() {
        int colorPrimary = getResources().getColor(R.color.colorPrimary);
        int colorAccent = getResources().getColor(R.color.colorAccent);

        // 루트 레이아웃
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(colorPrimary);
        rootLayout.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        rootLayout.setLayoutParams(rootParams);

        // 상단 여백
        View topSpacer = new View(this);
        LinearLayout.LayoutParams topSpacerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                2.0f // weight
        );
        topSpacer.setLayoutParams(topSpacerParams);
        rootLayout.addView(topSpacer);

        // 앱 아이콘
        ImageView iconView = createAppIcon();
        rootLayout.addView(iconView);

        // 아이콘 애니메이션 시작
        startIconAnimation(iconView);

        // 중간 여백
        View middleSpacer = new View(this);
        LinearLayout.LayoutParams middleSpacerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                3.0f // weight
        );
        middleSpacer.setLayoutParams(middleSpacerParams);
        rootLayout.addView(middleSpacer);

        // 하단 컨테이너 (프로그레스바, 앱이름, 저작권)
        LinearLayout bottomContainer = createBottomContainer(colorAccent);
        rootLayout.addView(bottomContainer);

        // 하단 여백
        View bottomSpacer = new View(this);
        LinearLayout.LayoutParams bottomSpacerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f // weight
        );
        bottomSpacer.setLayoutParams(bottomSpacerParams);
        rootLayout.addView(bottomSpacer);

        return rootLayout;
    }

    /**
     * 앱 아이콘 생성
     */
    private ImageView createAppIcon() {
        ImageView iconView = new ImageView(this);
        iconView.setImageResource(R.mipmap.ic_launcher);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        // 아이콘 크기 설정 (120dp)
        int iconSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 120,
                getResources().getDisplayMetrics()
        );

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                iconSize, iconSize
        );
        iconParams.gravity = Gravity.CENTER;
        iconView.setLayoutParams(iconParams);

        // 아이콘에 그림자 효과 추가
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            iconView.setElevation(8.0f);
        }

        return iconView;
    }

    /**
     * 하단 컨테이너 생성 (프로그레스바, 앱이름, 저작권)
     */
    private LinearLayout createBottomContainer(int colorAccent) {
        LinearLayout bottomContainer = new LinearLayout(this);
        bottomContainer.setOrientation(LinearLayout.VERTICAL);
        bottomContainer.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(60, 40, 60, 40);
        bottomContainer.setLayoutParams(containerParams);

        // 프로그레스바 (커스텀 스피너)
        View progressView = createProgressBar(colorAccent);
        bottomContainer.addView(progressView);

        // 앱 이름
        TextView appNameView = createAppNameView();
        bottomContainer.addView(appNameView);

        // 저작권 정보
        TextView copyrightView = createCopyrightView();
        bottomContainer.addView(copyrightView);

        return bottomContainer;
    }

    /**
     * 프로그레스바 생성 (도트 회전 스피너)
     */
    private View createProgressBar(int colorAccent) {
        // 컨테이너 프레임 레이아웃
        android.widget.FrameLayout spinnerContainer = new android.widget.FrameLayout(this);

        int progressSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 40,
                getResources().getDisplayMetrics()
        );

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                progressSize, progressSize
        );
        containerParams.gravity = Gravity.CENTER;
        containerParams.setMargins(0, 0, 0, 30);
        spinnerContainer.setLayoutParams(containerParams);

        // 3개의 도트 생성
        for (int i = 0; i < 3; i++) {
            View dot = createDot(colorAccent, i);
            spinnerContainer.addView(dot);
        }

        return spinnerContainer;
    }

    /**
     * 개별 도트 생성 및 애니메이션
     */
    private View createDot(int color, int index) {
        View dot = new View(this);

        // 도트 크기 (6dp)
        int dotSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 6,
                getResources().getDisplayMetrics()
        );

        android.widget.FrameLayout.LayoutParams dotParams =
                new android.widget.FrameLayout.LayoutParams(dotSize, dotSize);
        dotParams.gravity = Gravity.CENTER;
        dot.setLayoutParams(dotParams);

        // 도트 스타일
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            android.graphics.drawable.GradientDrawable shape =
                    new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            shape.setColor(color);
            dot.setBackground(shape);
        } else {
            dot.setBackgroundColor(color);
        }

        // 회전 애니메이션 (각 도트마다 120도씩 차이)
        startDotAnimation(dot, index * 120);

        return dot;
    }

    /**
     * 도트 회전 애니메이션
     */
    private void startDotAnimation(View dot, int startAngle) {
        // 회전 반지름 (15dp)
        final int radius = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 15,
                getResources().getDisplayMetrics()
        );

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0, 360);
        animator.setDuration(1200); // 1.2초
        animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.setStartDelay(0);

        animator.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                float angle = (Float) animation.getAnimatedValue() + startAngle;
                double radians = Math.toRadians(angle);

                float x = (float) (radius * Math.cos(radians));
                float y = (float) (radius * Math.sin(radians));

                dot.setTranslationX(x);
                dot.setTranslationY(y);
            }
        });

        animator.start();
    }

    /**
     * 앱 이름 텍스트뷰 생성
     */
    private TextView createAppNameView() {
        TextView appNameView = new TextView(this);

        // 앱 이름 가져오기 (strings.xml에서)
        String appName = getString(R.string.app_name);
        appNameView.setText(appName);

        appNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        appNameView.setTextColor(getResources().getColor(R.color.white));
        appNameView.setGravity(Gravity.CENTER);
        appNameView.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        nameParams.gravity = Gravity.CENTER;
        nameParams.setMargins(0, 0, 0, 20);
        appNameView.setLayoutParams(nameParams);

        return appNameView;
    }

    /**
     * 저작권 정보 텍스트뷰 생성
     */
    private TextView createCopyrightView() {
        TextView copyrightView = new TextView(this);

        // 현재 연도 가져오기
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        String copyrightText = "© " + currentYear + " INGCORP. All rights reserved.";

        copyrightView.setText(copyrightText);
        copyrightView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        copyrightView.setTextColor(getResources().getColor(R.color.white));
        copyrightView.setGravity(Gravity.CENTER);
        copyrightView.setAlpha(0.8f); // 약간 투명하게

        LinearLayout.LayoutParams copyrightParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        copyrightParams.gravity = Gravity.CENTER;
        copyrightView.setLayoutParams(copyrightParams);

        return copyrightView;
    }

    /**
     * 아이콘 애니메이션 시작
     */
    private void startIconAnimation(ImageView iconView) {
        // 페이드인 애니메이션
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1000);

        // 스케일 애니메이션
        ScaleAnimation scaleIn = new ScaleAnimation(
                0.5f, 1.0f, 0.5f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleIn.setDuration(1000);

        // 애니메이션 세트
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(fadeIn);
        animationSet.addAnimation(scaleIn);

        iconView.startAnimation(animationSet);
    }

    /**
     * 스플래시 애니메이션 시작
     */
    private void startSplashAnimations() {
        // 전체 화면에 페이드인 효과
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(500);
            rootView.startAnimation(fadeIn);
        }
    }

    /**
     * MainActivity로 이동
     */
    private void startMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

        // 전환 애니메이션 (페이드 효과)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * EdgeToEdge 설정
     */
    private void setupEdgeToEdge() {
        Window window = getWindow();
        View decorView = window.getDecorView();

        int colorPrimaryDark = getResources().getColor(R.color.colorPrimaryDark);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setupEdgeToEdgeApi29(window, decorView, colorPrimaryDark);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupEdgeToEdgeApi26(window, decorView, colorPrimaryDark);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setupEdgeToEdgeApi23(window, decorView, colorPrimaryDark);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setupEdgeToEdgeApi21(window, decorView, colorPrimaryDark);
        }
    }

    private void setupEdgeToEdgeApi29(Window window, View decorView, int colorPrimaryDark) {
        setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);
        window.setStatusBarContrastEnforced(false);
        window.setNavigationBarContrastEnforced(false);

        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    private void setupEdgeToEdgeApi26(Window window, View decorView, int colorPrimaryDark) {
        setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);

        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    private void setupEdgeToEdgeApi23(Window window, View decorView, int colorPrimaryDark) {
        setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);

        int flags = decorView.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        decorView.setSystemUiVisibility(flags);
    }

    private void setupEdgeToEdgeApi21(Window window, View decorView, int colorPrimaryDark) {
        setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(colorPrimaryDark);
        window.setNavigationBarColor(colorPrimaryDark);
    }

    private void setDecorFitsSystemWindows(Window window, boolean decorFitsSystemWindows) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(decorFitsSystemWindows);
        } else {
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

    private void setupWindowInsets(LinearLayout rootLayout) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            rootLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top,
                            systemBars.right, systemBars.bottom);
                    return WindowInsets.CONSUMED;
                }
            });
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    v.setPadding(insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom());
                    return insets;
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        // 스플래시 화면에서 뒤로가기 버튼 비활성화
        // 아무것도 하지 않음
    }
}