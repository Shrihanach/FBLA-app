package com.example.fblaapp;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;

import com.google.android.material.card.MaterialCardView;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fblaapp.data.AnnouncementEntity;
import com.example.fblaapp.data.AnnouncementRepository;
import com.example.fblaapp.data.AuthRepository;
import com.example.fblaapp.data.EventEntity;
import com.example.fblaapp.data.EventRepository;
import com.example.fblaapp.data.UserEntity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocialActivity extends AppCompatActivity {

    private static final String TAG = "SocialActivity";

    // SharedPreferences keys for chapter social links
    private static final String PREFS_SOCIAL = "FBLASocialPrefs";
    private static final String KEY_INSTAGRAM_HANDLE = "instagram_handle";
    private static final String KEY_INSTAGRAM_URL = "instagram_url";
    // Cache keys for offline feed
    private static final String KEY_CACHED_IG_CAPTION = "cached_ig_caption";
    private static final String KEY_CACHED_IG_TIMESTAMP = "cached_ig_timestamp";
    private static final String KEY_CACHED_IG_POST_URL = "cached_ig_post_url";

    // Default values
    private static final String DEFAULT_INSTAGRAM_HANDLE = "@indyfbla";
    private static final String DEFAULT_INSTAGRAM_URL = "https://www.instagram.com/indyfbla/";

    // National FBLA social media URLs
    private static final String URL_FACEBOOK = "https://www.facebook.com/FutureBusinessLeaders";
    private static final String URL_INSTAGRAM_NATIONAL = "https://www.instagram.com/fbla_pbl/";
    private static final String URL_WEBSITE = "https://www.fbla.org";

    private BottomNavigationView bottomNavigation;
    private MaterialButton btnEditLinks;
    private TextView textInstagramHandle;

    // Feed preview views
    private TextView textInstagramPostTitle;
    private TextView textInstagramPostSnippet;
    private TextView textInstagramPostTime;

    // Embedded Instagram feed views
    private WebView webViewInstagram;
    private ProgressBar progressInstagramFeed;
    private LinearLayout layoutFeedError;
    private MaterialButton btnFeedOpenInstagram;

    // Morph animation views
    private View cardInstagramHeader;
    private MaterialCardView cardInstagramFeed;
    private boolean feedMorphPlayed = false;

    private SharedPreferences socialPrefs;
    private AuthRepository authRepository;
    private AnnouncementRepository announcementRepository;
    private EventRepository eventRepository;
    private boolean isOfficer = false;

    // Current chapter links (loaded from prefs)
    private String instagramHandle;
    private String instagramUrl;

    // Firestore for real-time feed sync
    private FirebaseFirestore db;
    private ListenerRegistration igFeedListener;
    private ExecutorService executor;
    private Handler uiHandler;
    private boolean feedLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);

        socialPrefs = getSharedPreferences(PREFS_SOCIAL, MODE_PRIVATE);
        authRepository = AuthRepository.getInstance(this);
        announcementRepository = AnnouncementRepository.getInstance(this);
        eventRepository = EventRepository.getInstance(this);
        db = FirebaseFirestore.getInstance();
        executor = Executors.newSingleThreadExecutor();
        uiHandler = new Handler(Looper.getMainLooper());

        initViews();
        loadChapterLinks();
        setupUserRole();
        setupChapterChannelButtons();
        setupInstagramWebView();
        setupShareActions();
        setupNationalLinks();
        setupBottomNavigation();
        loadFeedPreviews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_social);
        loadChapterLinks();
        updateHandleViews();
        loadFeedPreviews();
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnEditLinks = findViewById(R.id.btnEditLinks);
        textInstagramHandle = findViewById(R.id.textInstagramHandle);

        // Feed preview views
        textInstagramPostTitle = findViewById(R.id.textInstagramPostTitle);
        textInstagramPostSnippet = findViewById(R.id.textInstagramPostSnippet);
        textInstagramPostTime = findViewById(R.id.textInstagramPostTime);

        // Embedded Instagram feed views
        webViewInstagram = findViewById(R.id.webViewInstagram);
        progressInstagramFeed = findViewById(R.id.progressInstagramFeed);
        layoutFeedError = findViewById(R.id.layoutFeedError);
        btnFeedOpenInstagram = findViewById(R.id.btnFeedOpenInstagram);

        // Morph animation views
        cardInstagramHeader = findViewById(R.id.cardInstagramHeader);
        cardInstagramFeed = findViewById(R.id.cardInstagramFeed);
    }

    private void loadChapterLinks() {
        instagramHandle = socialPrefs.getString(KEY_INSTAGRAM_HANDLE, DEFAULT_INSTAGRAM_HANDLE);
        instagramUrl = socialPrefs.getString(KEY_INSTAGRAM_URL, DEFAULT_INSTAGRAM_URL);
    }

    private void updateHandleViews() {
        textInstagramHandle.setText(instagramHandle);
    }

    private void setupUserRole() {
        UserEntity currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            isOfficer = currentUser.isOfficer();
        }

        // Show/hide officer edit button
        if (isOfficer) {
            btnEditLinks.setVisibility(View.VISIBLE);
            btnEditLinks.setOnClickListener(v -> showEditLinksDialog());
        } else {
            btnEditLinks.setVisibility(View.GONE);
        }

        updateHandleViews();
    }

    // ==================== Chapter Channel Buttons ====================

    private void setupChapterChannelButtons() {
        // Instagram - Open
        findViewById(R.id.btnOpenInstagram).setOnClickListener(v -> {
            openDeepLink(instagramUrl, "com.instagram.android");
        });

        // Instagram - Copy Handle
        findViewById(R.id.btnCopyInstagram).setOnClickListener(v -> {
            copyToClipboard("Instagram Handle", instagramHandle);
        });

        // Instagram - Share
        findViewById(R.id.btnShareInstagram).setOnClickListener(v -> {
            shareText("Follow our FBLA chapter on Instagram!\n" + instagramHandle + "\n" + instagramUrl);
        });

        // Instagram - View More (opens the latest post if available, else the profile)
        findViewById(R.id.btnInstagramViewMore).setOnClickListener(v -> {
            String cachedPostUrl = socialPrefs.getString(KEY_CACHED_IG_POST_URL, "");
            if (!cachedPostUrl.isEmpty()) {
                openDeepLink(cachedPostUrl, "com.instagram.android");
            } else {
                openDeepLink(instagramUrl, "com.instagram.android");
            }
        });
    }

    // ==================== Embedded Instagram Feed ====================

    @SuppressLint("SetJavaScriptEnabled")
    private void setupInstagramWebView() {
        // Enable third-party cookies (required for Instagram)
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webViewInstagram, true);

        // Configure WebView settings for Instagram
        WebSettings settings = webViewInstagram.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

        // Handle page loading states
        webViewInstagram.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                // Don't show spinner — the morph animation masks loading
                layoutFeedError.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                feedLoaded = true;
                progressInstagramFeed.setVisibility(View.GONE);

                // Morph the feed card open, then fade in the WebView
                morphFeedCardOpen(() -> {
                    webViewInstagram.setAlpha(0f);
                    webViewInstagram.setVisibility(View.VISIBLE);
                    webViewInstagram.animate()
                            .alpha(1f)
                            .setDuration(250)
                            .start();
                });

                // Inject CSS to hide Instagram chrome + JS to intercept post clicks
                String injectJs =
                        "javascript:(function() {" +
                        // --- CSS: hide nav, login walls, banners, footer ---
                        "  var style = document.createElement('style');" +
                        "  style.innerHTML = '" +
                        "    nav, header { display: none !important; } " +
                        "    div[role=\"presentation\"] { display: none !important; } " +
                        "    div[role=\"dialog\"] { display: none !important; } " +
                        "    div[class*=\"cookie\"] { display: none !important; } " +
                        "    footer { display: none !important; } " +
                        "    a[href*=\"get-app\"] { display: none !important; } " +
                        "    div[class*=\"Banner\"] { display: none !important; } " +
                        "    body { padding-top: 0 !important; margin-top: 0 !important; } " +
                        "  ';" +
                        "  document.head.appendChild(style);" +
                        // --- Remove login overlays after delay ---
                        "  setTimeout(function() {" +
                        "    var overlays = document.querySelectorAll('div[role=\"presentation\"], div[role=\"dialog\"]');" +
                        "    overlays.forEach(function(el) { el.remove(); });" +
                        "    document.body.style.overflow = 'auto';" +
                        "  }, 1500);" +
                        // --- Intercept clicks on post/reel links ---
                        "  document.addEventListener('click', function(e) {" +
                        "    var target = e.target;" +
                        "    var link = null;" +
                        // Walk up the DOM to find the nearest <a> tag
                        "    while (target && target !== document) {" +
                        "      if (target.tagName === 'A' && target.href) {" +
                        "        link = target;" +
                        "        break;" +
                        "      }" +
                        "      target = target.parentElement;" +
                        "    }" +
                        "    if (link) {" +
                        "      var href = link.href;" +
                        // Check if it's a post, reel, or story link
                        "      if (href.match(/instagram\\.com\\/(p|reel|reels|stories)\\/[^/]+/)) {" +
                        "        e.preventDefault();" +
                        "        e.stopPropagation();" +
                        // Navigate using a custom scheme so shouldOverrideUrlLoading catches it
                        "        window.location.href = 'openpost://' + href;" +
                        "        return false;" +
                        "      }" +
                        "    }" +
                        "  }, true);" +  // 'true' = capture phase, runs before Instagram's handlers
                        "})()";
                view.evaluateJavascript(injectJs, null);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.w(TAG, "Feed load error: " + errorCode + " - " + description + " url=" + failingUrl);
                // Only show error state if the main page failed (not sub-resources)
                if (failingUrl != null && failingUrl.contains("instagram.com") && failingUrl.startsWith("https://")) {
                    progressInstagramFeed.setVisibility(View.GONE);
                    webViewInstagram.setVisibility(View.GONE);
                    morphFeedCardOpen(() -> {
                        layoutFeedError.setVisibility(View.VISIBLE);
                    });
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "WebView URL: " + url);

                // Handle our custom openpost:// scheme — user tapped a post
                if (url.startsWith("openpost://")) {
                    // Strip the custom scheme to get the real URL
                    String postUrl = url.substring("openpost://".length());
                    if (!postUrl.startsWith("http")) {
                        postUrl = "https://" + postUrl;
                    }
                    Log.d(TAG, "Opening post externally: " + postUrl);
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(postUrl));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Log.w(TAG, "Could not open post URL: " + e.getMessage());
                    }
                    return true;
                }

                // Handle intent:// deep links — Instagram uses these when you tap posts
                if (url.startsWith("intent://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            // Check for a browser fallback URL first
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            if (fallbackUrl != null && !fallbackUrl.isEmpty()
                                    && fallbackUrl.contains("instagram.com")) {
                                // Open in browser/IG app instead of loading in WebView
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
                                } catch (Exception e) {
                                    view.loadUrl(fallbackUrl);
                                }
                                return true;
                            }
                            // Convert intent data to an https URL if it's Instagram
                            Uri data = intent.getData();
                            if (data != null) {
                                String host = data.getHost();
                                String path = data.getPath();
                                if (host != null && host.contains("instagram.com")
                                        && path != null && !path.isEmpty()) {
                                    String igUrl = "https://www.instagram.com" + path;
                                    try {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(igUrl)));
                                    } catch (Exception e) {
                                        view.loadUrl(igUrl);
                                    }
                                    return true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to parse intent URL: " + e.getMessage());
                    }
                    return true;
                }

                // Block other non-http schemes (instagram://, market://, etc.)
                if (!url.startsWith("https://") && !url.startsWith("http://")) {
                    Log.d(TAG, "Blocked non-http scheme: " + url);
                    return true;
                }

                // Keep Instagram pages inside the WebView
                if (url.contains("instagram.com") || url.contains("cdninstagram.com")) {
                    return false;
                }

                // Block everything else (ads, tracking redirects, etc.)
                Log.d(TAG, "Blocked external URL: " + url);
                return true;
            }
        });

        webViewInstagram.setWebChromeClient(new WebChromeClient());

        // Error state — open Instagram externally
        btnFeedOpenInstagram.setOnClickListener(v -> {
            openDeepLink(instagramUrl, "com.instagram.android");
        });

        // Load the Instagram profile feed
        loadInstagramFeed();
    }

    private void loadInstagramFeed() {
        String username = instagramHandle.replace("@", "");
        String profileUrl = "https://www.instagram.com/" + username + "/";

        // Hide spinner — we use the morph animation instead
        progressInstagramFeed.setVisibility(View.GONE);
        layoutFeedError.setVisibility(View.GONE);
        webViewInstagram.setVisibility(View.GONE);
        feedLoaded = false;

        // Collapse the feed card initially (scale to 0 height from top)
        if (!feedMorphPlayed) {
            cardInstagramFeed.setPivotX(cardInstagramFeed.getWidth() / 2f);
            cardInstagramFeed.setPivotY(0f);
            cardInstagramFeed.setScaleY(0f);
            cardInstagramFeed.setAlpha(0f);
        }

        Log.d(TAG, "Loading Instagram feed: " + profileUrl);
        webViewInstagram.loadUrl(profileUrl);

        // Safety timeout — show error state after 15 seconds if feed doesn't load
        uiHandler.removeCallbacksAndMessages(null);
        uiHandler.postDelayed(() -> {
            if (!feedLoaded) {
                Log.w(TAG, "Feed load timeout — showing error state");
                // Expand the feed card if it hasn't been yet, then show error
                if (!feedMorphPlayed) {
                    morphFeedCardOpen(() -> {
                        layoutFeedError.setVisibility(View.VISIBLE);
                    });
                } else {
                    layoutFeedError.setVisibility(View.VISIBLE);
                }
            }
        }, 15_000);
    }

    /**
     * Morphs the feed card open from the Instagram header card position.
     * The card expands from scaleY=0 (collapsed) to scaleY=1 (full height),
     * giving the appearance of "growing" out of the header card above.
     */
    private void morphFeedCardOpen(Runnable onComplete) {
        if (feedMorphPlayed) {
            if (onComplete != null) onComplete.run();
            return;
        }
        feedMorphPlayed = true;

        // Ensure the feed card is visible but collapsed
        cardInstagramFeed.setVisibility(View.VISIBLE);

        // Wait for layout pass to get correct dimensions
        cardInstagramFeed.post(() -> {
            cardInstagramFeed.setPivotX(cardInstagramFeed.getWidth() / 2f);
            cardInstagramFeed.setPivotY(0f);

            ObjectAnimator scaleY = ObjectAnimator.ofFloat(cardInstagramFeed, "scaleY", 0f, 1f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(cardInstagramFeed, "alpha", 0f, 1f);

            // Subtle slide up from below the header
            ObjectAnimator transY = ObjectAnimator.ofFloat(cardInstagramFeed, "translationY", 60f, 0f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(scaleY, alpha, transY);
            set.setDuration(400);
            set.setInterpolator(new DecelerateInterpolator(2f));
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (onComplete != null) onComplete.run();
                }
            });
            set.start();
        });
    }

    // ==================== Share & Promote Actions ====================

    private void setupShareActions() {
        // Share our Instagram
        findViewById(R.id.btnShareOurInstagram).setOnClickListener(v -> {
            shareText("Check out our FBLA chapter's Instagram!\n\n" +
                    instagramHandle + "\n" + instagramUrl +
                    "\n\n#FBLA #FutureBusinessLeaders");
        });

        // Share an Event
        findViewById(R.id.btnShareEvent).setOnClickListener(v -> {
            showShareEventPicker();
        });
    }

    // ==================== National FBLA Links ====================

    private void setupNationalLinks() {
        findViewById(R.id.btnFacebook).setOnClickListener(v -> openUrl(URL_FACEBOOK));
        findViewById(R.id.btnInstagram).setOnClickListener(v -> openUrl(URL_INSTAGRAM_NATIONAL));
        findViewById(R.id.btnWebsite).setOnClickListener(v -> openUrl(URL_WEBSITE));
    }

    // ==================== Live Instagram Feed ====================

    /**
     * Loads the latest Instagram post with 3 layers:
     *   1. Firestore real-time listener — auto-updates across all devices
     *   2. Background HTTP fetch — tries multiple strategies to scrape Instagram
     *   3. Fallback — shows latest announcement if nothing else works
     *
     * Officers can also manually enter the latest post via the edit dialog,
     * which writes directly to Firestore and syncs to everyone.
     */
    private void loadFeedPreviews() {
        // --- Layer 1: Firestore real-time listener (auto-updates) ---
        igFeedListener = db.collection("social_feed").document("instagram")
                .addSnapshotListener(this, (snapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Firestore listen failed, using fallback", error);
                        showInstagramFallback();
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        String caption = snapshot.getString("caption");
                        Long timestamp = snapshot.getLong("timestamp");
                        String postUrl = snapshot.getString("postUrl");

                        if (caption != null && !caption.isEmpty()) {
                            textInstagramPostTitle.setText("Latest Post");
                            textInstagramPostSnippet.setText(caption);
                            if (timestamp != null && timestamp > 0) {
                                textInstagramPostTime.setVisibility(View.VISIBLE);
                                textInstagramPostTime.setText(formatTimeAgo(timestamp));
                            } else {
                                textInstagramPostTime.setVisibility(View.GONE);
                            }
                            // Cache locally for offline
                            socialPrefs.edit()
                                    .putString(KEY_CACHED_IG_CAPTION, caption)
                                    .putLong(KEY_CACHED_IG_TIMESTAMP, timestamp != null ? timestamp : 0)
                                    .putString(KEY_CACHED_IG_POST_URL, postUrl != null ? postUrl : "")
                                    .apply();
                            return; // Firestore data is good, stop here
                        }
                    }
                    // Document doesn't exist or is empty — try cache, then announcements
                    showInstagramFallback();
                });

        // --- Layer 2: Background fetch from Instagram (writes to Firestore) ---
        fetchInstagramLatestPost();
    }

    /**
     * Tries multiple strategies to fetch the latest Instagram post.
     * On success, writes to Firestore which triggers the listener on all devices.
     *
     * Strategy 1: Instagram's internal web profile API
     * Strategy 2: Scrape the Instagram profile HTML page for embedded JSON
     * Strategy 3: Parse Open Graph meta tags from the profile page
     */
    private void fetchInstagramLatestPost() {
        executor.execute(() -> {
            String username = instagramHandle.replace("@", "");

            // --- Strategy 1: Instagram web profile API ---
            if (tryInstagramWebApi(username)) return;

            // --- Strategy 2: Scrape profile page HTML for embedded JSON ---
            if (tryInstagramHtmlScrape(username)) return;

            Log.w(TAG, "All Instagram fetch strategies failed");
        });
    }

    /** Strategy 1: Instagram's internal web profile API */
    private boolean tryInstagramWebApi(String username) {
        try {
            URL url = new URL("https://i.instagram.com/api/v1/users/web_profile_info/?username=" + username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            conn.setRequestProperty("x-ig-app-id", "936619743392459");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setRequestProperty("Referer", "https://www.instagram.com/" + username + "/");
            conn.setRequestProperty("x-requested-with", "XMLHttpRequest");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                String body = readStream(conn);
                conn.disconnect();

                JSONObject json = new JSONObject(body);
                JSONObject userData = json.getJSONObject("data").getJSONObject("user");
                JSONObject edgeMedia = userData.getJSONObject("edge_owner_to_timeline_media");
                JSONArray edges = edgeMedia.getJSONArray("edges");

                if (edges.length() > 0) {
                    JSONObject node = edges.getJSONObject(0).getJSONObject("node");
                    String caption = extractCaption(node);
                    long timestamp = node.optLong("taken_at_timestamp", 0) * 1000;
                    String shortcode = node.optString("shortcode", "");
                    String postUrl = shortcode.isEmpty() ? "" : "https://www.instagram.com/p/" + shortcode + "/";

                    writeToFirestore(caption, timestamp, postUrl, username);
                    Log.d(TAG, "Strategy 1 (web API) succeeded: " + shortcode);
                    return true;
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.d(TAG, "Strategy 1 (web API) failed: " + e.getMessage());
        }
        return false;
    }

    /** Strategy 2: Scrape the profile HTML page for embedded JSON data */
    private boolean tryInstagramHtmlScrape(String username) {
        try {
            URL url = new URL("https://www.instagram.com/" + username + "/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                String html = readStream(conn);
                conn.disconnect();

                // Look for window._sharedData or window.__additionalDataLoaded JSON
                String caption = null;
                long timestamp = 0;
                String postUrl = "";

                // Try to find _sharedData JSON block
                int sharedDataIdx = html.indexOf("window._sharedData");
                if (sharedDataIdx != -1) {
                    int jsonStart = html.indexOf("=", sharedDataIdx) + 1;
                    int jsonEnd = html.indexOf(";</script>", jsonStart);
                    if (jsonStart > 0 && jsonEnd > jsonStart) {
                        String jsonStr = html.substring(jsonStart, jsonEnd).trim();
                        JSONObject shared = new JSONObject(jsonStr);
                        JSONObject entryData = shared.optJSONObject("entry_data");
                        if (entryData != null) {
                            JSONArray profilePage = entryData.optJSONArray("ProfilePage");
                            if (profilePage != null && profilePage.length() > 0) {
                                JSONObject user = profilePage.getJSONObject(0)
                                        .getJSONObject("graphql").getJSONObject("user");
                                JSONArray edges = user.getJSONObject("edge_owner_to_timeline_media")
                                        .getJSONArray("edges");
                                if (edges.length() > 0) {
                                    JSONObject node = edges.getJSONObject(0).getJSONObject("node");
                                    caption = extractCaption(node);
                                    timestamp = node.optLong("taken_at_timestamp", 0) * 1000;
                                    String sc = node.optString("shortcode", "");
                                    postUrl = sc.isEmpty() ? "" : "https://www.instagram.com/p/" + sc + "/";
                                }
                            }
                        }
                    }
                }

                // Try to find JSON-LD structured data as fallback
                if (caption == null) {
                    int ldIdx = html.indexOf("\"application/ld+json\"");
                    if (ldIdx != -1) {
                        int ldStart = html.indexOf(">", ldIdx) + 1;
                        int ldEnd = html.indexOf("</script>", ldStart);
                        if (ldStart > 0 && ldEnd > ldStart) {
                            String ldJson = html.substring(ldStart, ldEnd).trim();
                            JSONObject ld = new JSONObject(ldJson);
                            String desc = ld.optString("description", "");
                            String name = ld.optString("name", "");
                            if (!desc.isEmpty()) {
                                caption = desc;
                            } else if (!name.isEmpty()) {
                                caption = name;
                            }
                        }
                    }
                }

                // Try parsing og:description meta tag as last resort
                if (caption == null) {
                    String ogDesc = extractMetaTag(html, "og:description");
                    if (ogDesc != null && !ogDesc.isEmpty()) {
                        caption = ogDesc;
                    }
                }

                if (caption != null && !caption.isEmpty()) {
                    writeToFirestore(caption, timestamp, postUrl, username);
                    Log.d(TAG, "Strategy 2 (HTML scrape) succeeded");
                    return true;
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.d(TAG, "Strategy 2 (HTML scrape) failed: " + e.getMessage());
        }
        return false;
    }

    /** Extracts caption text from an Instagram media node JSON */
    private String extractCaption(JSONObject node) {
        try {
            JSONObject captionObj = node.optJSONObject("edge_media_to_caption");
            if (captionObj != null) {
                JSONArray edges = captionObj.getJSONArray("edges");
                if (edges.length() > 0) {
                    String text = edges.getJSONObject(0).getJSONObject("node").getString("text");
                    return text;
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    /** Extracts an Open Graph meta tag value from HTML */
    private String extractMetaTag(String html, String property) {
        String marker = "property=\"" + property + "\"";
        int idx = html.indexOf(marker);
        if (idx == -1) {
            marker = "name=\"" + property + "\"";
            idx = html.indexOf(marker);
        }
        if (idx != -1) {
            int contentIdx = html.indexOf("content=\"", idx);
            if (contentIdx != -1 && contentIdx - idx < 100) {
                int start = contentIdx + 9;
                int end = html.indexOf("\"", start);
                if (end > start) {
                    return html.substring(start, end);
                }
            }
        }
        return null;
    }

    /** Reads an HttpURLConnection's input stream to a String */
    private String readStream(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    /** Writes Instagram post data to Firestore (triggers all listeners) */
    private void writeToFirestore(String caption, long timestamp, String postUrl, String username) {
        Map<String, Object> feedData = new HashMap<>();
        feedData.put("caption", caption);
        feedData.put("timestamp", timestamp);
        feedData.put("postUrl", postUrl);
        feedData.put("lastFetched", System.currentTimeMillis());
        feedData.put("username", username);

        db.collection("social_feed").document("instagram")
                .set(feedData)
                .addOnSuccessListener(v -> Log.d(TAG, "Instagram feed synced to Firestore"))
                .addOnFailureListener(e -> Log.w(TAG, "Firestore write failed", e));

        // Update UI immediately without waiting for Firestore round-trip
        runOnUiThread(() -> {
            textInstagramPostTitle.setText("Latest Post");
            textInstagramPostSnippet.setText(caption);
            if (timestamp > 0) {
                textInstagramPostTime.setVisibility(View.VISIBLE);
                textInstagramPostTime.setText(formatTimeAgo(timestamp));
            }

            // Cache locally
            socialPrefs.edit()
                    .putString(KEY_CACHED_IG_CAPTION, caption)
                    .putLong(KEY_CACHED_IG_TIMESTAMP, timestamp)
                    .putString(KEY_CACHED_IG_POST_URL, postUrl)
                    .apply();
        });
    }

    /**
     * Fallback chain when Firestore has no Instagram data:
     *  1. Check SharedPreferences cache
     *  2. Use the latest announcement as a stand-in
     */
    private void showInstagramFallback() {
        // Try cache first
        String cached = socialPrefs.getString(KEY_CACHED_IG_CAPTION, "");
        long cachedTs = socialPrefs.getLong(KEY_CACHED_IG_TIMESTAMP, 0);

        if (!cached.isEmpty()) {
            textInstagramPostTitle.setText("Latest Post");
            textInstagramPostSnippet.setText(cached);
            if (cachedTs > 0) {
                textInstagramPostTime.setVisibility(View.VISIBLE);
                textInstagramPostTime.setText(formatTimeAgo(cachedTs));
            } else {
                textInstagramPostTime.setVisibility(View.GONE);
            }
            return;
        }

        // No cache — mirror the latest announcement
        announcementRepository.getAllAnnouncementsLive().observe(this, announcements -> {
            if (announcements != null && !announcements.isEmpty()) {
                AnnouncementEntity latest = announcements.get(0);
                textInstagramPostTitle.setText(latest.getTitle());
                textInstagramPostSnippet.setText(latest.getContent());
                textInstagramPostTime.setVisibility(View.VISIBLE);
                textInstagramPostTime.setText(formatTimeAgo(latest.getCreatedAtMillis()));
            } else {
                textInstagramPostTitle.setText("No recent posts");
                textInstagramPostSnippet.setText("Check back soon for updates from our chapter.");
                textInstagramPostTime.setVisibility(View.GONE);
            }
        });
    }

    // ==================== Officer Edit Links Dialog ====================

    private void showEditLinksDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Scrollable root so content fits on smaller screens
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);

        // Build dialog layout programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(24);
        layout.setPadding(pad, pad, pad, pad);

        // Title
        TextView title = new TextView(this);
        title.setText("Edit Chapter Social Links");
        title.setTextSize(20);
        title.setTextColor(getResources().getColor(R.color.navy, null));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        // Subtitle
        TextView subtitle = new TextView(this);
        subtitle.setText("Update your chapter's social media handles, URLs, and latest post");
        subtitle.setTextSize(13);
        subtitle.setTextColor(getResources().getColor(R.color.text_secondary, null));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dpToPx(4);
        subtitleParams.bottomMargin = dpToPx(20);
        subtitle.setLayoutParams(subtitleParams);
        layout.addView(subtitle);

        // Instagram Handle
        TextInputLayout tilIgHandle = createTextInput("Instagram Handle", instagramHandle);
        layout.addView(tilIgHandle);

        // Instagram URL
        TextInputLayout tilIgUrl = createTextInput("Instagram URL", instagramUrl);
        layout.addView(tilIgUrl);

        // --- Latest Instagram Post section ---
        TextView postSectionLabel = new TextView(this);
        postSectionLabel.setText("LATEST INSTAGRAM POST");
        postSectionLabel.setTextSize(12);
        postSectionLabel.setTextColor(getResources().getColor(R.color.cobalt, null));
        postSectionLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        postSectionLabel.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sectionParams.topMargin = dpToPx(8);
        sectionParams.bottomMargin = dpToPx(8);
        postSectionLabel.setLayoutParams(sectionParams);
        layout.addView(postSectionLabel);

        String currentCaption = socialPrefs.getString(KEY_CACHED_IG_CAPTION, "");
        String currentPostUrl = socialPrefs.getString(KEY_CACHED_IG_POST_URL, "");

        TextInputLayout tilPostCaption = createTextInput(
                "Latest Post Caption (paste from Instagram)", currentCaption);
        layout.addView(tilPostCaption);

        TextInputLayout tilPostUrl = createTextInput(
                "Latest Post URL (optional)", currentPostUrl);
        layout.addView(tilPostUrl);

        // Button row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnRowParams.topMargin = dpToPx(16);
        btnRow.setLayoutParams(btnRowParams);

        // Cancel button
        MaterialButton btnCancel = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(getResources().getColor(R.color.text_secondary, null));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Save button
        MaterialButton btnSave = new MaterialButton(this);
        btnSave.setText("Save");
        btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(R.color.cobalt, null)));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        saveParams.setMarginStart(dpToPx(8));
        btnSave.setLayoutParams(saveParams);

        btnSave.setOnClickListener(v -> {
            TextInputEditText editIgHandle = (TextInputEditText) tilIgHandle.getEditText();
            TextInputEditText editIgUrl = (TextInputEditText) tilIgUrl.getEditText();
            TextInputEditText editPostCaption = (TextInputEditText) tilPostCaption.getEditText();
            TextInputEditText editPostUrl = (TextInputEditText) tilPostUrl.getEditText();

            String newIgHandle = editIgHandle != null ? editIgHandle.getText().toString().trim() : "";
            String newIgUrl = editIgUrl != null ? editIgUrl.getText().toString().trim() : "";
            String newPostCaption = editPostCaption != null ? editPostCaption.getText().toString().trim() : "";
            String newPostUrl = editPostUrl != null ? editPostUrl.getText().toString().trim() : "";

            // Validate handles/urls
            if (newIgHandle.isEmpty() || newIgUrl.isEmpty()) {
                Toast.makeText(this, "Handle and URL fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save handles to SharedPreferences
            socialPrefs.edit()
                    .putString(KEY_INSTAGRAM_HANDLE, newIgHandle)
                    .putString(KEY_INSTAGRAM_URL, newIgUrl)
                    .apply();

            // If officer entered a latest post caption, write to Firestore
            if (!newPostCaption.isEmpty()) {
                Map<String, Object> feedData = new HashMap<>();
                feedData.put("caption", newPostCaption);
                feedData.put("timestamp", System.currentTimeMillis());
                feedData.put("postUrl", newPostUrl);
                feedData.put("lastFetched", System.currentTimeMillis());
                feedData.put("username", newIgHandle.replace("@", ""));
                feedData.put("manualEntry", true);

                db.collection("social_feed").document("instagram")
                        .set(feedData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Officer updated Instagram feed in Firestore");
                            Toast.makeText(this, "Latest post updated for all users!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Failed to update Firestore feed", e);
                            Toast.makeText(this, "Feed update failed — saved locally", Toast.LENGTH_SHORT).show();
                        });

                // Also cache locally
                socialPrefs.edit()
                        .putString(KEY_CACHED_IG_CAPTION, newPostCaption)
                        .putLong(KEY_CACHED_IG_TIMESTAMP, System.currentTimeMillis())
                        .putString(KEY_CACHED_IG_POST_URL, newPostUrl)
                        .apply();
            }

            // Reload
            loadChapterLinks();
            updateHandleViews();
            loadInstagramFeed(); // Reload WebView with new handle

            Toast.makeText(this, "Social links updated!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnSave);
        layout.addView(btnRow);

        scrollView.addView(layout);
        dialog.setContentView(scrollView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private TextInputLayout createTextInput(String hint, String value) {
        TextInputLayout til = new TextInputLayout(this,
                null, com.google.android.material.R.attr.textInputOutlinedStyle);
        til.setHint(hint);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dpToPx(12);
        til.setLayoutParams(params);

        TextInputEditText edit = new TextInputEditText(til.getContext());
        edit.setText(value);
        edit.setTextSize(14);
        til.addView(edit);

        return til;
    }

    // ==================== Share Event Picker ====================

    private void showShareEventPicker() {
        List<EventEntity> events = eventRepository.getAllEventsSync();

        if (events == null || events.isEmpty()) {
            Toast.makeText(this, "No events available to share", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build list of event titles for the picker
        String[] eventTitles = new String[events.size()];
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

        for (int i = 0; i < events.size(); i++) {
            EventEntity event = events.get(i);
            String date = dateFormat.format(new Date(event.getStartTimeMillis()));
            eventTitles[i] = event.getTitle() + " — " + date;
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose an event to share")
                .setItems(eventTitles, (dialog, which) -> {
                    EventEntity selectedEvent = events.get(which);
                    shareEvent(selectedEvent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareEvent(EventEntity event) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault());
        String date = dateFormat.format(new Date(event.getStartTimeMillis()));

        StringBuilder shareText = new StringBuilder();
        shareText.append("FBLA Event: ").append(event.getTitle()).append("\n\n");
        shareText.append("Date: ").append(date).append("\n");

        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            shareText.append("Location: ").append(event.getLocation()).append("\n");
        }

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            shareText.append("\n").append(event.getDescription()).append("\n");
        }

        shareText.append("\nFollow us on Instagram: ").append(instagramHandle);
        shareText.append("\n#FBLA #FutureBusinessLeaders");

        shareText(shareText.toString());
    }

    // ==================== Utility Methods ====================

    private void openDeepLink(String url, String packageName) {
        // Try to open in the native app first
        try {
            Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            appIntent.setPackage(packageName);
            startActivity(appIntent);
        } catch (Exception e) {
            // Fall back to browser
            openUrl(url);
        }
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareText(String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private String formatTimeAgo(long timeMillis) {
        long now = System.currentTimeMillis();
        long diff = now - timeMillis;

        if (diff < 60_000) {
            return "Just now";
        } else if (diff < 3_600_000) {
            long minutes = diff / 60_000;
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (diff < 86_400_000) {
            long hours = diff / 3_600_000;
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (diff < 604_800_000) {
            long days = diff / 86_400_000;
            return days + (days == 1 ? " day ago" : " days ago");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            return sdf.format(new Date(timeMillis));
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ==================== Lifecycle ====================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firestore listener to prevent memory leaks
        if (igFeedListener != null) igFeedListener.remove();
        // Shut down background executor
        if (executor != null && !executor.isShutdown()) executor.shutdown();
        // Cancel any pending timeout callbacks
        if (uiHandler != null) uiHandler.removeCallbacksAndMessages(null);
        // Clean up WebView to prevent memory leaks
        if (webViewInstagram != null) {
            webViewInstagram.stopLoading();
            webViewInstagram.destroy();
        }
    }

    // ==================== Bottom Navigation ====================

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_social);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent i = new Intent(this, HomeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (itemId == R.id.nav_calendar) {
                Intent i = new Intent(this, CalendarActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (itemId == R.id.nav_resources) {
                Intent i = new Intent(this, ResourcesActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (itemId == R.id.nav_social) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });
    }
}
