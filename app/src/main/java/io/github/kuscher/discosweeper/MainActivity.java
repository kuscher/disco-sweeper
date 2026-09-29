package io.github.kuscher.discosweeper;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.PathInterpolator;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import io.github.kuscher.discosweeper.audio.Sfx;
import io.github.kuscher.discosweeper.game.Board;
import io.github.kuscher.discosweeper.game.Fit;
import io.github.kuscher.discosweeper.game.Level;
import io.github.kuscher.discosweeper.game.Settings;
import io.github.kuscher.discosweeper.game.Solver;
import io.github.kuscher.discosweeper.ui.NavItem;
import io.github.kuscher.discosweeper.ui.PillButton;
import io.github.kuscher.discosweeper.ui.StepSlider;
import io.github.kuscher.discosweeper.ui.Toggle;
import io.github.kuscher.discosweeper.view.BoardView;
import io.github.kuscher.discosweeper.view.DiscoOverlay;
import io.github.kuscher.discosweeper.view.LevelThumb;
import io.github.kuscher.discosweeper.view.StatusStrip;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
public final class MainActivity extends Activity implements BoardView.Listener {
    private static final float DRAWER_W = 300.0f;
    private static final float HEADER_H = 62.0f;
    private static final Level[] ITEMS = Level.values();
    /** Long enough to sit out a window drag, short enough to feel like the board is following. */
    private static final long REFLOW_SETTLE_MS = 150;
    /** Below this the window is close enough to the difficulty's own size to leave alone. */
    private static final float RESIZE_SLOP_DP = 8.0f;
    /** Long enough for the old task to be gone before the new one claims the bounds. */
    private static final long RELAUNCH_DELAY_MS = 220;
    private static final String STATE_BOARD = "board";
    private static final String STATE_MARK_MODE = "mark_mode";
    /** Custom's ball slider is a share of the tiles, in whole percent. */
    private static final int CUSTOM_MIN_PERCENT = 8;
    private static final int CUSTOM_MAX_PERCENT = 35;
    private TextView aboutBlurb;
    private PillButton aboutBtn;
    private LinearLayout aboutCard;
    private boolean aboutOpen;
    private FrameLayout aboutWrap;
    private TextView[] bestRows;
    private BoardView boardView;
    private PillButton closeBtn;
    private ValueAnimator drawerAnim;
    private boolean drawerOpen;
    private float drawerT;
    private TextView drawerTitle;
    private FrameLayout drawerWrap;
    private LinearLayout header;
    private FrameLayout headerWrap;
    private PillButton hintBtn;
    private PillButton markBtn;
    private PillButton menuBtn;
    private NavItem[] navItems;
    private TextView navLabel;
    private LinearLayout navPanel;
    private PillButton newBtn;
    private DiscoOverlay overlay;
    private LinearLayout rightRow;
    private final Runnable reflow = new Runnable() {
        @Override
        public final void run() {
            MainActivity.this.reflowIfUncommitted();
        }
    };
    private RootView root;
    private View scrim;
    private int seedColor;
    private int selected;
    private final Sfx sfx = new Sfx();
    private Toggle soundToggle;
    private Toggle noGuessToggle;
    private Toggle unsureToggle;
    private StepSlider colsSlider;
    private StepSlider rowsSlider;
    private StepSlider ballsSlider;
    private TextView colsValue;
    private TextView rowsValue;
    private TextView ballsValue;
    /** The first board is built before the window has a size; it is replaced at the first layout. */
    private boolean placeholder;
    private boolean backRegistered;
    private OnBackInvokedCallback closeOverlay;
    private StatusStrip status;
    private ValueAnimator themeAnim;
    private LevelThumb[] thumbs;
    private boolean tight;
    private Tokens tokens;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Ui.init(this);
        this.selected = Settings.level(this).ordinal();
        this.seedColor = ITEMS[this.selected].seedColor;
        this.tokens = Tokens.of(this, this.seedColor);
        buildUi();
        select(this.selected, false);
        if (bundle != null) {
            Board saved = Board.restore(bundle.getByteArray(STATE_BOARD), System.nanoTime());
            if (saved != null) {
                showBoard(saved);
            }
            this.boardView.setMarkMode(bundle.getBoolean(STATE_MARK_MODE));
        }
        restyle(this.tokens);
        this.sfx.setEnabled(Settings.sound(this));
        final Sfx sfx = this.sfx;
        Objects.requireNonNull(sfx);
        new Thread(new Runnable() { // from class: io.github.kuscher.discosweeper.MainActivity.1
            @Override // java.lang.Runnable
            public final void run() {
                sfx.prepare();
            }
        }, "sfx-prepare").start();
    }

    /** A recreated activity (font or display size, or the process reclaimed) picks the game up again. */
    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        Board board = this.boardView == null ? null : this.boardView.board();
        if (board != null) {
            bundle.putByteArray(STATE_BOARD, board.save());
        }
        bundle.putBoolean(STATE_MARK_MODE, this.boardView != null && this.boardView.markMode());
    }

    /** The clock runs only while the window can be seen. */
    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        Board board = this.boardView == null ? null : this.boardView.board();
        if (board != null) {
            board.resume();
            this.status.invalidate();
        }
    }

    @Override // android.app.Activity
    protected void onStop() {
        super.onStop();
        Board board = this.boardView == null ? null : this.boardView.board();
        if (board != null) {
            board.pause();
        }
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.tokens = Tokens.of(this, this.seedColor);
        restyle(this.tokens);
        redraw();
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        redraw();
    }

    private void redraw() {
        if (this.root == null) {
            return;
        }
        this.root.post(new Runnable() { // from class: io.github.kuscher.discosweeper.MainActivity.8
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$redraw$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$redraw$0() {
        this.root.requestLayout();
        invalidateAll(this.root);
    }

    private static void invalidateAll(View view) {
        view.invalidate();
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                invalidateAll(viewGroup.getChildAt(i));
            }
        }
    }

    /* JADX WARN: Type inference fix 'apply assigned field type' failed
    java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$UnknownArg
    	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:596)
    	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
    	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
     */
    private void buildUi() {
        this.root = new RootView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.addView(buildHeader(), new LinearLayout.LayoutParams(-1, -2));
        this.boardView = new BoardView(this);
        this.boardView.setListener(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        int iDp = Ui.dp(10.0f);
        layoutParams.bottomMargin = iDp;
        layoutParams.rightMargin = iDp;
        layoutParams.leftMargin = iDp;
        layoutParams.topMargin = Ui.dp(10.0f);
        linearLayout.addView(this.boardView, layoutParams);
        this.root.addView(linearLayout, new FrameLayout.LayoutParams(-1, -1));
        this.scrim = new View(this);
        this.scrim.setVisibility(8);
        this.scrim.setOnClickListener(new View.OnClickListener() { // from class: io.github.kuscher.discosweeper.MainActivity.13
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$buildUi$1(view);
            }
        });
        this.root.addView(this.scrim, new FrameLayout.LayoutParams(-1, -1));
        this.drawerWrap = buildDrawer();
        this.root.addView(this.drawerWrap, new FrameLayout.LayoutParams(Ui.dp(310.0f), -1, 8388611));
        this.overlay = new DiscoOverlay(this);
        this.root.addView(this.overlay, new FrameLayout.LayoutParams(-1, -1));
        setContentView(this.root);
        this.root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() { // from class: io.github.kuscher.discosweeper.MainActivity.14
            @Override // android.view.View.OnApplyWindowInsetsListener
            public final WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                return MainActivity.lambda$buildUi$2(view, windowInsets);
            }
        });
        this.root.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: io.github.kuscher.discosweeper.MainActivity.15
            @Override // android.view.View.OnLayoutChangeListener
            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                MainActivity.this.lambda$buildUi$3(view, i, i2, i3, i4, i5, i6, i7, i8);
            }
        });
        applyDrawer(0.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$1(View view) {
        setDrawer(false);
    }

    static /* synthetic */ WindowInsets lambda$buildUi$2(View view, WindowInsets windowInsets) {
        Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
        return windowInsets;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$3(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        applyTightIfNeeded();
        syncStatusInsets();
        if (this.placeholder) {
            // Before the first frame, so the board you first see (and click) is the real one.
            reflowIfUncommitted();
        } else if (i3 - i != i7 - i5 || i4 - i2 != i8 - i6) {
            scheduleReflow();
        }
    }

    private View buildHeader() {
        this.headerWrap = new FrameLayout(this);
        this.headerWrap.setPadding(Ui.dp(10.0f), Ui.dp(10.0f), Ui.dp(10.0f), 0);
        this.status = new StatusStrip(this);
        this.status.setOnReset(new StatusStrip.OnReset() { // from class: io.github.kuscher.discosweeper.MainActivity.17
            @Override // io.github.kuscher.discosweeper.view.StatusStrip.OnReset
            public final void onReset() {
                MainActivity.this.newGame();
            }
        });
        this.headerWrap.addView(this.status, new FrameLayout.LayoutParams(-1, Ui.dp(62.0f)));
        this.menuBtn = new PillButton(this);
        this.menuBtn.style(2).icon(6).sizing(46.0f, 23.0f, 14.0f, 14.0f);
        this.menuBtn.setOnClickListener(new View.OnClickListener() { // from class: io.github.kuscher.discosweeper.MainActivity.18
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$buildHeader$4(view);
            }
        });
        this.menuBtn.setTooltipText("Choose a difficulty");
        this.menuBtn.setContentDescription("Choose a difficulty");
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 8388627);
        layoutParams.leftMargin = Ui.dp(8.0f);
        this.headerWrap.addView(this.menuBtn, layoutParams);
        this.rightRow = new LinearLayout(this);
        this.rightRow.setOrientation(0);
        this.rightRow.setGravity(16);
        this.markBtn = new PillButton(this);
        this.markBtn.style(1).icon(8).label("Mark").sizing(46.0f, 23.0f, 16.0f, 14.0f);
        this.markBtn.setTooltipText("Mark mode — click a tile to put its shades on");
        this.markBtn.setOnClickListener(new View.OnClickListener() { // from class: io.github.kuscher.discosweeper.MainActivity.19
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$buildHeader$5(view);
            }
        });
        this.rightRow.addView(this.markBtn);
        this.hintBtn = new PillButton(this);
        this.hintBtn.style(1).icon(9).label("Hint").sizing(46.0f, 23.0f, 16.0f, 14.0f);
        this.hintBtn.setOnClickListener(new View.OnClickListener() { // from class: io.github.kuscher.discosweeper.MainActivity.20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$buildHeader$6(view);
            }
        });
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams2.leftMargin = Ui.dp(8.0f);
        this.rightRow.addView(this.hintBtn, layoutParams2);
        this.newBtn = new PillButton(this);
        this.newBtn.icon(1).label("New game").sizing(46.0f, 23.0f, 16.0f, 14.0f);
        this.newBtn.setOnClickListener(new View.OnClickListener() { // from class: io.github.kuscher.discosweeper.MainActivity.21
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$buildHeader$7(view);
            }
        });
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams3.leftMargin = Ui.dp(8.0f);
        this.rightRow.addView(this.newBtn, layoutParams3);
        FrameLayout.LayoutParams layoutParams4 = new FrameLayout.LayoutParams(-2, -2, 8388629);
        layoutParams4.rightMargin = Ui.dp(8.0f);
        this.headerWrap.addView(this.rightRow, layoutParams4);
        View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener() { // from class: io.github.kuscher.discosweeper.MainActivity.22
            @Override // android.view.View.OnLayoutChangeListener
            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                MainActivity.this.lambda$buildHeader$9(view, i, i2, i3, i4, i5, i6, i7, i8);
            }
        };
        this.menuBtn.addOnLayoutChangeListener(onLayoutChangeListener);
        this.rightRow.addOnLayoutChangeListener(onLayoutChangeListener);
        return this.headerWrap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildHeader$4(View view) {
        setDrawer(!this.drawerOpen);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildHeader$5(View view) {
        this.boardView.setMarkMode(!this.boardView.markMode());
        restyle(this.tokens);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildHeader$6(View view) {
        hint();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildHeader$7(View view) {
        newGame();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildHeader$9(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        if (i3 - i == i7 - i5) {
            return;
        }
        this.headerWrap.post(new Runnable() { // from class: io.github.kuscher.discosweeper.MainActivity.16
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$buildHeader$8();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildHeader$8() {
        syncStatusInsets();
        applyTightIfNeeded();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void syncStatusInsets() {
        if (this.status == null) {
            return;
        }
        this.status.setInsets((desiredWidth(this.menuBtn) + Ui.dp(16.0f)) / Ui.dpf(1.0f), (desiredWidth(this.rightRow) + Ui.dp(16.0f)) / Ui.dpf(1.0f));
    }

    private FrameLayout buildDrawer() {
        this.navPanel = new LinearLayout(this);
        this.navPanel.setOrientation(1);
        this.navPanel.setPadding(Ui.dp(10.0f), Ui.dp(16.0f), Ui.dp(10.0f), Ui.dp(14.0f));
        this.drawerTitle = label("Disco Sweeper", 17.5f, 700);
        this.drawerTitle.setSingleLine(true);
        this.drawerTitle.setPadding(Ui.dp(16.0f), 0, 0, Ui.dp(14.0f));
        this.navPanel.addView(this.drawerTitle);
        this.navLabel = label("Difficulty", 11.5f, 600);
        this.navLabel.setSingleLine(true);
        this.navLabel.setLetterSpacing(0.06f);
        this.navLabel.setPadding(Ui.dp(16.0f), 0, 0, Ui.dp(8.0f));
        this.navPanel.addView(this.navLabel);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        this.navItems = new NavItem[ITEMS.length];
        this.thumbs = new LevelThumb[ITEMS.length];
        for (int i = 0; i < ITEMS.length; i++) {
            final int index = i;   // captured by the listener below
            Level level = ITEMS[i];
            this.thumbs[i] = new LevelThumb(this, level);
            if (level.isCustom()) {
                this.thumbs[i].setShape(Settings.customCols(this), Settings.customRows(this), Settings.customMines(this));
            }
            NavItem navItem = new NavItem(this, this.thumbs[i]);
            navItem.text(level.title, shapeOf(level));
            navItem.setOnClickListener(new View.OnClickListener() { // from class: io.github.kuscher.discosweeper.MainActivity.6
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.this.lambda$buildDrawer$10(index, view);
                }
            });
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            layoutParams.bottomMargin = Ui.dp(4.0f);
            linearLayout.addView(navItem, layoutParams);
            this.navItems[i] = navItem;
        }
        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.addView(linearLayout, new FrameLayout.LayoutParams(-1, -2));
        this.navPanel.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.aboutBtn = new PillButton(this);
        this.aboutBtn.style(2).icon(5).label(getString(R.string.about)).sizing(42.0f, 21.0f, 12.0f, 13.0f);
        this.aboutBtn.setOnClickListener(new View.OnClickListener() { // from class: io.github.kuscher.discosweeper.MainActivity.7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$buildDrawer$11(view);
            }
        });
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.addView(this.aboutBtn, new LinearLayout.LayoutParams(-2, -2));
        this.navPanel.addView(linearLayout2);
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setPadding(Ui.dp(10.0f), Ui.dp(10.0f), 0, Ui.dp(10.0f));
        frameLayout.addView(this.navPanel, new FrameLayout.LayoutParams(-1, -1));
        return frameLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildDrawer$10(int i, View view) {
        select(i, true);
        setDrawer(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildDrawer$11(View view) {
        setDrawer(false);
        showAbout(true);
    }

    /** What this difficulty comes to in the window as it stands, not what it used to be fixed at. */
    private String shapeOf(Level level) {
        if (level.isCustom()) {
            return level.subtitle(Settings.customCols(this), Settings.customRows(this), Settings.customMines(this));
        }
        if (this.boardView != null && this.boardView.getWidth() > 0) {
            Fit fit = fitFor(level);
            return level.subtitle(fit.cols, fit.rows, fit.mines);
        }
        return level.subtitle(level.cols, level.rows, level.mines);
    }

    /** The drawer advertises live sizes, so the window is visibly what picks the board. */
    private void refreshShape() {
        if (this.navItems == null) {
            return;
        }
        for (int i = 0; i < this.navItems.length && i < ITEMS.length; i++) {
            this.navItems[i].text(ITEMS[i].title, shapeOf(ITEMS[i]));
        }
    }

    private TextView label(String str, float f, int i) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(f);
        textView.setTypeface(Ui.weight(i));
        textView.setIncludeFontPadding(false);
        return textView;
    }

    private void setDrawer(boolean z) {
        if (this.drawerOpen == z) {
            return;
        }
        this.drawerOpen = z;
        updateBackCallback();
        if (this.drawerAnim != null) {
            this.drawerAnim.cancel();
        }
        this.drawerAnim = ValueAnimator.ofFloat(this.drawerT, z ? 1.0f : 0.0f);
        this.drawerAnim.setDuration(z ? 340L : 260L);
        this.drawerAnim.setInterpolator(new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f));
        this.drawerAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: io.github.kuscher.discosweeper.MainActivity.4
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                MainActivity.this.lambda$setDrawer$12(valueAnimator);
            }
        });
        this.drawerAnim.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setDrawer$12(ValueAnimator valueAnimator) {
        applyDrawer(((Float) valueAnimator.getAnimatedValue()).floatValue());
    }

    private void applyDrawer(float f) {
        this.drawerT = f;
        this.drawerWrap.setTranslationX(Ui.dpf(310.0f) * (f - 1.0f));
        this.scrim.setAlpha(f);
        this.scrim.setVisibility(f < 0.01f ? 8 : 0);
        this.drawerWrap.setVisibility(f >= 0.01f ? 0 : 8);
    }

    /**
     * Back closes the About sheet or the drawer. From Android 16, apps targeting API 36 no longer
     * get onBackPressed, so while either is open a callback claims Back; with both closed, Back is
     * the system's again.
     */
    private void updateBackCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        boolean want = this.aboutOpen || this.drawerOpen;
        if (want == this.backRegistered) {
            return;
        }
        if (this.closeOverlay == null) {
            this.closeOverlay = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    MainActivity.this.closeTopOverlay();
                }
            };
        }
        OnBackInvokedDispatcher dispatcher = getOnBackInvokedDispatcher();
        if (want) {
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, this.closeOverlay);
        } else {
            dispatcher.unregisterOnBackInvokedCallback(this.closeOverlay);
        }
        this.backRegistered = want;
    }

    private boolean closeTopOverlay() {
        if (this.aboutOpen) {
            showAbout(false);
            return true;
        }
        if (this.drawerOpen) {
            setDrawer(false);
            return true;
        }
        return false;
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.aboutOpen) {
            showAbout(false);
        } else if (this.drawerOpen) {
            setDrawer(false);
        } else {
            super.onBackPressed();
        }
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 111 && this.aboutOpen) {
            showAbout(false);
            return true;
        }
        if (i == 111 && this.drawerOpen) {
            setDrawer(false);
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    private void applyTightIfNeeded() {
        if (this.headerWrap == null || this.headerWrap.getWidth() == 0 || this.status == null) {
            return;
        }
        boolean z = (this.headerWrap.getWidth() - this.headerWrap.getPaddingLeft()) - this.headerWrap.getPaddingRight() < ((((this.menuBtn.widthWithLabel() + this.markBtn.widthWithLabel()) + this.hintBtn.widthWithLabel()) + this.newBtn.widthWithLabel()) + Ui.dp(16.0f)) + Ui.dp((this.status.countersNeedDp() * 2.0f) + 48.0f);
        if (z != this.tight) {
            this.tight = z;
            applyTight();
        }
    }

    private static int desiredWidth(View view) {
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        view.measure(iMakeMeasureSpec, iMakeMeasureSpec);
        return view.getMeasuredWidth();
    }

    private void setLabelAlphas(float f) {
        this.menuBtn.setLabelAlpha(f);
        this.newBtn.setLabelAlpha(f);
        this.hintBtn.setLabelAlpha(f);
        this.markBtn.setLabelAlpha(f);
    }

    private void applyTight() {
        setLabelAlphas(this.tight ? 0.0f : 1.0f);
        this.headerWrap.post(new Runnable() { // from class: io.github.kuscher.discosweeper.MainActivity.0
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.syncStatusInsets();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void newGame() {
        Level level = ITEMS[this.selected];
        Board board;
        if (level.isCustom()) {
            board = new Board(Settings.customCols(this), Settings.customRows(this), Settings.customMines(this), System.nanoTime(), Settings.allowUnsure(this));
        } else {
            Fit fit = fitFor(level);
            board = new Board(fit.cols, fit.rows, fit.mines, System.nanoTime(), Settings.allowUnsure(this));
        }
        board.setNoGuess(Settings.noGuess(this));
        showBoard(board);
        this.placeholder = level.reflows() && this.boardView.getWidth() == 0;
    }

    private void showBoard(Board board) {
        this.placeholder = false;
        this.boardView.setCellCeiling(Ui.dpf(ITEMS[this.selected].cellMaxDp));
        this.boardView.setBoard(board);
        this.status.setBoard(board);
        this.status.setSpinning(false);
        refreshShape();
    }

    /**
     * Picking a difficulty by hand is the one moment the app may size its own window.
     *
     * <p>There is no API to resize a running window, but {@code setLaunchBounds} still applies to
     * a launch, so the activity relaunches itself into bounds cut for the level. The target is
     * chosen to be a fixed point of {@link Fit}: land in that window and the solver hands back
     * exactly the classic board, so the resize and the reflow agree instead of fighting.
     *
     * <p>Returns true when a relaunch is under way and the caller should stop.
     */
    private boolean resizeWindowTo(Level level) {
        if (!level.reflows() || this.boardView == null || this.boardView.getWidth() == 0) {
            return false;
        }
        Rect window = getWindowManager().getCurrentWindowMetrics().getBounds();
        Rect display = getWindowManager().getMaximumWindowMetrics().getBounds();
        // Fullscreen and maximised are the user having already said what size they want. Honour
        // it and let the board reflow into it instead.
        if (!isInMultiWindowMode() || window.width() >= display.width()
                || window.height() >= display.height()) {
            return false;
        }
        float cell = Ui.dpf(level.idealCellDp);
        float inset = BoardView.gridInset();
        // The window frame is measured, not assumed: in desktop windowing the caption bar sits
        // inside these bounds, and its height is not ours to guess.
        int frameW = window.width() - this.boardView.getWidth();
        int frameH = window.height() - this.boardView.getHeight();
        int width = Math.round((level.cols * cell) + inset) + frameW;
        int height = Math.round((level.rows * cell) + inset) + frameH;
        int slop = Ui.dp(RESIZE_SLOP_DP);
        if (Math.abs(width - window.width()) < slop && Math.abs(height - window.height()) < slop) {
            return false;
        }
        Rect bounds = new Rect(window.left, window.top, window.left + width, window.top + height);
        keepOnScreen(bounds, display);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchBounds(bounds);
        // Relaunching into the existing task lays out at the new bounds but keeps the old
        // surface, so the board comes up with its last rows uncomposited until the window
        // manager touches the window again. Tearing the task down first makes this the same
        // path as a cold launch, which the platform does size correctly.
        final Intent intent = new Intent(getApplicationContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        final Bundle launch = options.toBundle();
        final Context context = getApplicationContext();
        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public final void run() {
                context.startActivity(intent, launch);
            }
        }, RELAUNCH_DELAY_MS);
        finishAndRemoveTask();
        return true;
    }

    private static void keepOnScreen(Rect bounds, Rect display) {
        bounds.offset(Math.min(0, display.right - bounds.right), Math.min(0, display.bottom - bounds.bottom));
        bounds.offset(Math.max(0, display.left - bounds.left), Math.max(0, display.top - bounds.top));
    }

    /** The board the window can hold at this difficulty. */
    private Fit fitFor(Level level) {
        return Fit.of(this.boardView.usableWidth(), this.boardView.usableHeight(), level,
                Ui.dpf(level.cellMinDp), Ui.dpf(level.cellMaxDp));
    }

    /**
     * Reshape the board to the window, but only while nothing has been committed to it. Once a
     * cell is open or a ball is marked, the shape belongs to that game and resizing only re-fits
     * the cell — dragging a window edge never throws progress away.
     */
    private void reflowIfUncommitted() {
        Level level = ITEMS[this.selected];
        if (!level.reflows() || this.boardView == null || this.boardView.getWidth() == 0) {
            return;
        }
        this.placeholder = false;
        Board board = this.boardView.board();
        if (board != null && !uncommitted(board)) {
            return;
        }
        if (fitFor(level).matches(board)) {
            return;
        }
        newGame();
    }

    private static boolean uncommitted(Board board) {
        return !board.started() && !board.over() && board.ballsLeft() == board.mines;
    }

    /**
     * A drag resizes the window continuously, so settle before rebuilding rather than generating
     * a board per frame.
     */
    private void scheduleReflow() {
        if (this.root == null) {
            return;
        }
        this.root.removeCallbacks(this.reflow);
        this.root.postDelayed(this.reflow, REFLOW_SETTLE_MS);
    }

    private void hint() {
        Board board = this.boardView.board();
        if (board == null || board.over()) {
            return;
        }
        if (!board.started()) {
            this.boardView.flashHint(board.cols / 2, board.rows / 2, true);
            return;
        }
        Solver.Move moveHint = Solver.hint(board);
        if (moveHint == null) {
            Toast.makeText(this, "No certain move — this one needs a guess.", 0).show();
        } else {
            this.boardView.flashHint(moveHint.x, moveHint.y, moveHint.kind == 1);
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        if (this.root != null) {
            this.root.removeCallbacks(this.reflow);
        }
        this.sfx.release();
    }

    @Override // io.github.kuscher.discosweeper.view.BoardView.Listener
    public void onBoardChanged() {
        this.status.invalidate();
    }

    @Override // io.github.kuscher.discosweeper.view.BoardView.Listener
    public void onAction(int i) {
        switch (i) {
            case 1:
                this.sfx.play(1);
                break;
            case 2:
                this.sfx.play(2);
                break;
            default:
                this.sfx.play(0);
                break;
        }
    }

    @Override // io.github.kuscher.discosweeper.view.BoardView.Listener
    public void onFirstMove() {
        this.status.invalidate();
        Board board = this.boardView.board();
        if (board != null && board.noGuessMissed()) {
            Toast.makeText(this, "Too many balls for a no-guess board — this one may need a guess.", Toast.LENGTH_LONG).show();
        }
    }

    @Override // io.github.kuscher.discosweeper.view.BoardView.Listener
    public void onWin() {
        String str;
        Board board = this.boardView.board();
        boolean zRecordWin = Settings.recordWin(this, ITEMS[this.selected], board.cols, board.rows, board.mines, board.elapsedSeconds());
        this.status.invalidate();
        this.sfx.play(3);
        if (zRecordWin) {
            str = "Best time. " + board.elapsedSeconds() + "s.";
        } else {
            str = "Floor cleared.";
        }
        playDisco(true, str);
    }

    @Override // io.github.kuscher.discosweeper.view.BoardView.Listener
    public void onLoss() {
        this.status.invalidate();
        this.sfx.play(5);
        this.sfx.play(4);
        playDisco(false, "You found the party.");
    }

    private void playDisco(boolean z, String str) {
        float height;
        float f;
        Board board = this.boardView.board();
        int iHitIndex = board.hitIndex();
        int[] iArr = new int[2];
        int[] iArr2 = new int[2];
        this.boardView.getLocationOnScreen(iArr);
        this.root.getLocationOnScreen(iArr2);
        float f2 = iArr[0] - iArr2[0];
        float f3 = iArr[1] - iArr2[1];
        BoardView boardView = this.boardView;
        if (iHitIndex >= 0) {
            float fCellCentreX = f2 + boardView.cellCentreX(iHitIndex % board.cols);
            height = f3 + this.boardView.cellCentreY(iHitIndex / board.cols);
            f = fCellCentreX;
        } else {
            float width = f2 + (boardView.getWidth() / 2.0f);
            height = f3 + (this.boardView.getHeight() / 2.0f);
            f = width;
        }
        this.overlay.play(f, height, Math.max(Ui.dpf(8.0f), this.boardView.cellSize() * 0.34f), this.tokens, z, str, new DiscoOverlay.OnDone() { // from class: io.github.kuscher.discosweeper.MainActivity.12
            @Override // io.github.kuscher.discosweeper.view.DiscoOverlay.OnDone
            public final void onDone(boolean z2) {
                MainActivity.this.lambda$playDisco$13(z2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$playDisco$13(boolean z) {
        if (z) {
            this.sfx.stop(4);
        }
    }

    private void showAbout(boolean z) {
        if (z && this.aboutWrap == null) {
            buildAbout();
        }
        if (this.aboutWrap == null) {
            return;
        }
        this.aboutOpen = z;
        updateBackCallback();
        this.aboutWrap.setVisibility(0);
        this.aboutWrap.animate().alpha(z ? 1.0f : 0.0f).setDuration(z ? 220L : 160L).withEndAction(new Runnable() { // from class: io.github.kuscher.discosweeper.MainActivity.5
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$showAbout$14();
            }
        }).start();
        this.aboutCard.setScaleX(z ? 0.94f : 1.0f);
        this.aboutCard.setScaleY(z ? 0.94f : 1.0f);
        this.aboutCard.animate().scaleX(z ? 1.0f : 0.96f).scaleY(z ? 1.0f : 0.96f).setDuration(z ? 320L : 160L).setInterpolator(new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)).start();
        if (z) {
            refreshBestTimes();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showAbout$14() {
        if (!this.aboutOpen) {
            this.aboutWrap.setVisibility(8);
        }
    }

    private void buildAbout() {
        this.aboutWrap = new FrameLayout(this);
        this.aboutWrap.setAlpha(0.0f);
        this.aboutWrap.setOnClickListener(new View.OnClickListener() { // from class: io.github.kuscher.discosweeper.MainActivity.9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$buildAbout$15(view);
            }
        });
        this.aboutCard = new LinearLayout(this);
        this.aboutCard.setOrientation(1);
        this.aboutCard.setPadding(Ui.dp(26.0f), Ui.dp(24.0f), Ui.dp(26.0f), Ui.dp(20.0f));
        this.aboutCard.setClickable(true);
        TextView textViewLabel = label("Disco Sweeper", 24.0f, 700);
        textViewLabel.setLetterSpacing(-0.02f);
        this.aboutCard.addView(textViewLabel);
        this.aboutBlurb = label(getString(R.string.about_blurb), 14.0f, 400);
        this.aboutBlurb.setLineSpacing(Ui.dpf(4.0f), 1.0f);
        this.aboutBlurb.setPadding(0, Ui.dp(8.0f), 0, Ui.dp(4.0f));
        this.aboutCard.addView(this.aboutBlurb);
        TextView version = label(getString(R.string.about_version, versionName()), 12.0f, 400);
        version.setPadding(0, Ui.dp(2.0f), 0, Ui.dp(4.0f));
        this.aboutCard.addView(version);
        this.aboutCard.addView(sectionLabel("How to play"));
        this.aboutCard.addView(bullet("Click a tile to clear it. Numbers count the disco balls in the eight tiles around them."));
        this.aboutCard.addView(bullet("Right-click to put a tile's sunglasses on — that is how you mark a ball — and again to take them off. On a touchscreen, press and hold, or turn on Mark."));
        this.aboutCard.addView(bullet("Click a number whose balls are all marked to clear everything around it at once. Middle-click and shift-click do it too."));
        this.aboutCard.addView(bullet("Clear every tile that is not a ball and the floor is yours. Open one and the party finds you."));
        this.aboutCard.addView(sectionLabel("Sound"));
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.addView(label("Ticks, chimes and a beat under the disco ball", 13.5f, 400), new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.soundToggle = new Toggle(this);
        this.soundToggle.setOn(Settings.sound(this), false);
        this.soundToggle.setContentDescription("Sound");
        this.soundToggle.setOnChange(new Toggle.OnChange() { // from class: io.github.kuscher.discosweeper.MainActivity.10
            @Override // io.github.kuscher.discosweeper.ui.Toggle.OnChange
            public final void onChange(boolean z) {
                MainActivity.this.lambda$buildAbout$16(z);
            }
        });
        linearLayout.addView(this.soundToggle, new LinearLayout.LayoutParams(-2, -2));
        linearLayout.setPadding(0, Ui.dp(4.0f), 0, Ui.dp(4.0f));
        this.aboutCard.addView(linearLayout);
        buildGameSettings();
        this.aboutCard.addView(sectionLabel("Best times"));
        this.bestRows = new TextView[ITEMS.length];
        for (int i = 0; i < ITEMS.length; i++) {
            this.bestRows[i] = label("", 13.5f, 500);
            this.bestRows[i].setPadding(0, Ui.dp(3.0f), 0, Ui.dp(3.0f));
            this.aboutCard.addView(this.bestRows[i]);
        }
        this.closeBtn = new PillButton(this);
        this.closeBtn.style(1).label("Close").sizing(44.0f, 22.0f, 20.0f, 14.0f);
        this.closeBtn.setOnClickListener(new View.OnClickListener() { // from class: io.github.kuscher.discosweeper.MainActivity.11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$buildAbout$17(view);
            }
        });
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(8388613);
        linearLayout2.setPadding(0, Ui.dp(14.0f), 0, 0);
        linearLayout2.addView(this.closeBtn, new LinearLayout.LayoutParams(-2, -2));
        this.aboutCard.addView(linearLayout2);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.addView(this.aboutCard, new FrameLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(Ui.dp(460.0f), -2, 17);
        int iDp = Ui.dp(24.0f);
        layoutParams.bottomMargin = iDp;
        layoutParams.topMargin = iDp;
        this.aboutWrap.addView(scrollView, layoutParams);
        this.root.addView(this.aboutWrap, new FrameLayout.LayoutParams(-1, -1));
        styleAbout(this.tokens);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildAbout$15(View view) {
        showAbout(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildAbout$16(boolean z) {
        Settings.setSound(this, z);
        this.sfx.setEnabled(z);
        if (z) {
            this.sfx.play(2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildAbout$17(View view) {
        showAbout(false);
    }

    private void colourText(ViewGroup viewGroup, Tokens tokens) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childAt = viewGroup.getChildAt(i);
            if (childAt instanceof TextView) {
                TextView textView = (TextView) childAt;
                textView.setTextColor(((textView.getTextSize() / Ui.spf(1.0f)) > 20.0f ? 1 : ((textView.getTextSize() / Ui.spf(1.0f)) == 20.0f ? 0 : -1)) > 0 ? tokens.onSurface : tokens.onSurfaceVariant);
            } else if (childAt instanceof ViewGroup) {
                colourText((ViewGroup) childAt, tokens);
            }
        }
    }

    /** The options a game is dealt with, and Custom's own shape. They apply to the next board. */
    private void buildGameSettings() {
        this.aboutCard.addView(sectionLabel("Game"));
        this.noGuessToggle = toggleRow("No-guess boards", "Every board can be cleared by logic alone, never by luck",
                Settings.noGuess(this), new Toggle.OnChange() {
                    @Override
                    public void onChange(boolean on) {
                        Settings.setNoGuess(MainActivity.this, on);
                        Board board = MainActivity.this.boardView.board();
                        if (board != null && !board.started()) {
                            board.setNoGuess(on);   // its balls aren't placed until the first click
                        }
                    }
                });
        this.unsureToggle = toggleRow("“?” marks", "Right-click a marked tile once more for a “?”",
                Settings.allowUnsure(this), new Toggle.OnChange() {
                    @Override
                    public void onChange(boolean on) {
                        Settings.setAllowUnsure(MainActivity.this, on);
                        Board board = MainActivity.this.boardView.board();
                        if (board != null) {
                            board.setAllowUnsure(on);
                        }
                    }
                });

        this.aboutCard.addView(sectionLabel("Custom board"));
        StepSlider.OnChange custom = new StepSlider.OnChange() {
            @Override
            public void onChange(int step) {
                MainActivity.this.customChanged();
            }
        };
        TextView[] value = new TextView[1];
        this.colsSlider = sliderRow("Columns", value, Level.CUSTOM_MAX_COLS - Level.MIN_SIDE + 1,
                String.valueOf(Level.MIN_SIDE), String.valueOf(Level.CUSTOM_MAX_COLS),
                Settings.customCols(this) - Level.MIN_SIDE, custom);
        this.colsValue = value[0];
        this.rowsSlider = sliderRow("Rows", value, Level.CUSTOM_MAX_ROWS - Level.MIN_SIDE + 1,
                String.valueOf(Level.MIN_SIDE), String.valueOf(Level.CUSTOM_MAX_ROWS),
                Settings.customRows(this) - Level.MIN_SIDE, custom);
        this.rowsValue = value[0];
        int cells = Settings.customCols(this) * Settings.customRows(this);
        this.ballsSlider = sliderRow("Disco balls", value, CUSTOM_MAX_PERCENT - CUSTOM_MIN_PERCENT + 1,
                CUSTOM_MIN_PERCENT + "%", CUSTOM_MAX_PERCENT + "%",
                Math.round((100.0f * Settings.customMines(this)) / cells) - CUSTOM_MIN_PERCENT, custom);
        this.ballsValue = value[0];
        syncCustomValues();
    }

    private Toggle toggleRow(String title, String detail, boolean on, Toggle.OnChange change) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(0);
        row.setGravity(16);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(1);
        words.addView(label(title, 13.5f, 500));
        TextView detailView = label(detail, 12.0f, 400);
        detailView.setPadding(0, Ui.dp(3.0f), Ui.dp(12.0f), 0);
        words.addView(detailView);
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Toggle toggle = new Toggle(this);
        toggle.setOn(on, false);
        toggle.setOnChange(change);
        toggle.setContentDescription(title);
        row.addView(toggle, new LinearLayout.LayoutParams(-2, -2));
        row.setPadding(0, Ui.dp(5.0f), 0, Ui.dp(5.0f));
        this.aboutCard.addView(row);
        return toggle;
    }

    private StepSlider sliderRow(String title, TextView[] valueOut, int steps, String low, String high,
                                 int step, StepSlider.OnChange change) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(0);
        head.addView(label(title, 13.5f, 500), new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView value = label("", 13.5f, 600);
        head.addView(value, new LinearLayout.LayoutParams(-2, -2));
        head.setPadding(0, Ui.dp(8.0f), Ui.dp(4.0f), Ui.dp(4.0f));
        this.aboutCard.addView(head);
        StepSlider slider = new StepSlider(this);
        slider.config(steps, low, high);
        slider.setValue(step, false);
        slider.setOnChange(change);
        slider.setContentDescription(title);
        this.aboutCard.addView(slider, new LinearLayout.LayoutParams(-1, -2));
        valueOut[0] = value;
        return slider;
    }

    /** Custom's sliders moved: store the shape, and deal it now if Custom is up and untouched. */
    private void customChanged() {
        int cols = this.colsSlider.value() + Level.MIN_SIDE;
        int rows = this.rowsSlider.value() + Level.MIN_SIDE;
        int percent = this.ballsSlider.value() + CUSTOM_MIN_PERCENT;
        Settings.setCustom(this, cols, rows, Math.round((percent / 100.0f) * cols * rows));
        syncCustomValues();
        this.thumbs[Level.CUSTOM.ordinal()].setShape(Settings.customCols(this), Settings.customRows(this), Settings.customMines(this));
        refreshShape();
        refreshBestTimes();
        Board board = this.boardView.board();
        if (ITEMS[this.selected].isCustom() && (board == null || uncommitted(board))) {
            newGame();
        }
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private void syncCustomValues() {
        if (this.colsValue == null) {
            return;
        }
        this.colsValue.setText(String.valueOf(Settings.customCols(this)));
        this.rowsValue.setText(String.valueOf(Settings.customRows(this)));
        int mines = Settings.customMines(this);
        this.ballsValue.setText(mines + (mines == 1 ? " ball" : " balls"));
    }

    private TextView sectionLabel(String str) {
        TextView textViewLabel = label(str, 11.5f, 600);
        textViewLabel.setLetterSpacing(0.06f);
        textViewLabel.setPadding(0, Ui.dp(18.0f), 0, Ui.dp(6.0f));
        return textViewLabel;
    }

    private TextView bullet(String str) {
        TextView textViewLabel = label("·  " + str, 13.5f, 400);
        textViewLabel.setLineSpacing(Ui.dpf(3.0f), 1.0f);
        textViewLabel.setPadding(0, Ui.dp(3.0f), 0, Ui.dp(3.0f));
        return textViewLabel;
    }

    private void refreshBestTimes() {
        if (this.bestRows == null) {
            return;
        }
        // A best time belongs to a board shape, so show the one for the board this window
        // would give you now — the same board the drawer is advertising.
        for (int i = 0; i < ITEMS.length; i++) {
            Level level = ITEMS[i];
            int cols;
            int rows;
            int mines;
            if (level.isCustom()) {
                cols = Settings.customCols(this);
                rows = Settings.customRows(this);
                mines = Settings.customMines(this);
            } else {
                Fit fit = fitFor(level);
                cols = fit.cols;
                rows = fit.rows;
                mines = fit.mines;
            }
            int best = Settings.best(this, level, cols, rows, mines);
            this.bestRows[i].setText(level.title + "   " + (best == 0 ? "—" : best + "s")
                    + "   " + cols + "×" + rows);
        }
    }

    private void styleAbout(Tokens tokens) {
        if (this.aboutWrap == null) {
            return;
        }
        this.aboutWrap.setBackgroundColor(tokens.scrim);
        this.aboutCard.setBackground(Ui.round(tokens.surfaceContainerLow, 28.0f));
        Ui.clipRound(this.aboutCard, 28.0f);
        colourText(this.aboutCard, tokens);
        this.closeBtn.colors(tokens.secondaryContainer, tokens.onSecondaryContainer, 0);
        if (this.bestRows != null) {
            for (TextView textView : this.bestRows) {
                textView.setTextColor(tokens.onSurface);
            }
        }
        for (Toggle toggle : new Toggle[] {this.soundToggle, this.noGuessToggle, this.unsureToggle}) {
            if (toggle != null) {
                toggle.setTokens(tokens);
            }
        }
        for (StepSlider slider : new StepSlider[] {this.colsSlider, this.rowsSlider, this.ballsSlider}) {
            if (slider != null) {
                slider.setTokens(tokens);
            }
        }
        for (TextView value : new TextView[] {this.colsValue, this.rowsValue, this.ballsValue}) {
            if (value != null) {
                value.setTextColor(tokens.onSurface);
            }
        }
    }

    private void select(int i, boolean z) {
        this.selected = i;
        Level level = ITEMS[i];
        Settings.setLevel(this, level);
        if (z && resizeWindowTo(level)) {
            return;
        }
        this.menuBtn.label(level.title);
        this.menuBtn.setContentDescription(level.title + ". Choose a difficulty");
        int i2 = 0;
        while (i2 < this.navItems.length) {
            this.navItems[i2].setSelected(i2 == i, z);
            i2++;
        }
        newGame();
        post(new Runnable() { // from class: io.github.kuscher.discosweeper.MainActivity.2
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.lambda$select$18();
            }
        });
        final int i3 = this.seedColor;
        this.seedColor = level.seedColor;
        if (this.themeAnim != null) {
            this.themeAnim.cancel();
        }
        final int i4 = this.seedColor;
        if (!z) {
            this.tokens = Tokens.of(this, i4);
            restyle(this.tokens);
            return;
        }
        this.themeAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.themeAnim.setDuration(420L);
        this.themeAnim.setInterpolator(new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f));
        this.themeAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: io.github.kuscher.discosweeper.MainActivity.3
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                MainActivity.this.lambda$select$19(i3, i4, valueAnimator);
            }
        });
        this.themeAnim.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$select$18() {
        syncStatusInsets();
        applyTightIfNeeded();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$select$19(int i, int i2, ValueAnimator valueAnimator) {
        this.tokens = Tokens.of(this, Ui.mix(i, i2, ((Float) valueAnimator.getAnimatedValue()).floatValue()));
        restyle(this.tokens);
    }

    private void post(Runnable runnable) {
        this.root.post(runnable);
    }

    private void restyle(Tokens tokens) {
        this.root.setBackgroundColor(tokens.surface);
        this.navPanel.setBackground(Ui.round(tokens.surfaceContainer, 26.0f));
        this.scrim.setBackgroundColor(tokens.scrim);
        this.drawerTitle.setTextColor(tokens.onSurface);
        this.navLabel.setTextColor(tokens.onSurfaceVariant);
        for (NavItem navItem : this.navItems) {
            navItem.setTokens(tokens);
        }
        for (LevelThumb levelThumb : this.thumbs) {
            levelThumb.setTokens(tokens);
        }
        this.boardView.setTokens(tokens);
        this.status.setTokens(tokens);
        this.menuBtn.colors(0, tokens.onSurfaceVariant, 0);
        this.newBtn.colors(tokens.primary, tokens.onPrimary, 0);
        this.hintBtn.colors(tokens.secondaryContainer, tokens.onSecondaryContainer, 0);
        this.markBtn.colors(this.boardView.markMode() ? tokens.primary : tokens.secondaryContainer, this.boardView.markMode() ? tokens.onPrimary : tokens.onSecondaryContainer, 0);
        this.aboutBtn.colors(0, tokens.onSurfaceVariant, 0);
        styleAbout(tokens);
        getWindow().setStatusBarColor(0);
        getWindow().setNavigationBarColor(0);
        WindowInsetsController insetsController = getWindow().getInsetsController();
        if (insetsController != null) {
            insetsController.setSystemBarsAppearance(tokens.dark ? 0 : 24, 24);
        }
    }

    private final class RootView extends FrameLayout {
        private final int[] loc;

        RootView(Context context) {
            super(context);
            this.loc = new int[2];
        }

        @Override // android.view.ViewGroup, android.view.View
        public boolean dispatchTouchEvent(MotionEvent motionEvent) {
            if (MainActivity.this.status != null && MainActivity.this.boardView != null && !MainActivity.this.drawerOpen) {
                int actionMasked = motionEvent.getActionMasked();
                if (actionMasked == 0 || actionMasked == 1 || actionMasked == 3) {
                    MainActivity.this.status.setSpinning(actionMasked == 0 && hits(MainActivity.this.boardView, motionEvent.getRawX(), motionEvent.getRawY()));
                }
            }
            return super.dispatchTouchEvent(motionEvent);
        }

        private boolean hits(View view, float f, float f2) {
            view.getLocationOnScreen(this.loc);
            return f >= ((float) this.loc[0]) && f <= ((float) (this.loc[0] + view.getWidth())) && f2 >= ((float) this.loc[1]) && f2 <= ((float) (this.loc[1] + view.getHeight()));
        }
    }
}
