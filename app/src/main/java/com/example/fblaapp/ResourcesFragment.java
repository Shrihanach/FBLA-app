package com.example.fblaapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fblaapp.data.AuthRepository;
import com.example.fblaapp.data.FirestoreResource;
import com.example.fblaapp.data.UserEntity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fragment for displaying and managing FBLA chapter resources.
 * Shows 3 category cards. Objective Testing shows a 2-column event grid.
 * Presentation Events and BAA show Firestore resource lists.
 */
public class ResourcesFragment extends Fragment {

    private static final String COLLECTION_RESOURCES = "resources";
    private static final String STORAGE_PATH = "resources/";

    // Views - Category cards
    private ScrollView layoutCategoryCards;
    private MaterialCardView cardObjectiveTesting;
    private MaterialCardView cardPresentationEvents;
    private MaterialCardView cardRoleplayEvents;
    private MaterialCardView cardBAA;

    // Views - BAA info
    private ScrollView layoutBAAInfo;
    private ImageButton btnBackBAA;
    private Button btnOpenFBLAConnect;
    private Button btnViewBAAResources;

    // Views - Objective testing grid
    private LinearLayout layoutObjectiveGrid;
    private ImageButton btnBackGrid;
    private RecyclerView recyclerEventTiles;

    // Views - Presentation events grid
    private LinearLayout layoutPresentationGrid;
    private ImageButton btnBackPresentationGrid;
    private RecyclerView recyclerPresentationTiles;

    // Views - Roleplay events grid
    private LinearLayout layoutRoleplayGrid;
    private ImageButton btnBackRoleplayGrid;
    private RecyclerView recyclerRoleplayTiles;

    // Views - Event detail (two-tab view inside each event)
    private LinearLayout layoutEventDetail;
    private ImageButton btnBackEventDetail;
    private TextView textEventDetailName;
    private com.google.android.material.button.MaterialButton tabEventOverview;
    private com.google.android.material.button.MaterialButton tabStudyMaterials;
    private ScrollView contentEventOverview;
    private ScrollView contentStudyMaterials;
    private com.google.android.material.button.MaterialButton btnOpenGuidelines;

    // Views - Resource list
    private LinearLayout layoutResourceList;
    private ImageButton btnBack;
    private TextView textCurrentCategory;
    private RecyclerView recyclerResources;
    private ProgressBar progressLoading;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutError;
    private TextView textEmptyHint;
    private TextView textErrorMessage;
    private Button btnRetry;
    private FloatingActionButton fabUpload;
    private TextView badgeOfficer;

    // Data
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private AuthRepository authRepository;
    private ResourcesAdapter adapter;
    private EventTileAdapter eventTileAdapter;
    private EventTileAdapter presentationTileAdapter;
    private EventTileAdapter roleplayTileAdapter;
    private final List<FirestoreResource> allResources = new ArrayList<>();
    private String currentCategory = null;
    private String currentEventName = null;
    private String currentEventGuidelineUrl = null;
    private String previousGrid = null; // "objective" or "presentation" — to go back to the right grid
    private boolean isOfficer = false;
    private String currentUserName = "";

    // File picker
    private Uri selectedFileUri = null;
    private String selectedFileName = null;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Dialog currentUploadDialog;

    // Event icon mapping (event name → drawable resource ID)
    private static final Map<String, Integer> EVENT_ICON_MAP = new HashMap<>();
    static {
        // Presentation / Role Play events
        EVENT_ICON_MAP.put("Banking & Financial Systems", R.drawable.ic_event_banking);
        EVENT_ICON_MAP.put("Business Management", R.drawable.ic_event_business_mgmt);
        EVENT_ICON_MAP.put("Entrepreneurship", R.drawable.ic_event_entrepreneurship);
        EVENT_ICON_MAP.put("Hospitality & Event Management", R.drawable.ic_event_hospitality);
        EVENT_ICON_MAP.put("International Business", R.drawable.ic_event_international);
        EVENT_ICON_MAP.put("Management Information Systems", R.drawable.ic_event_mis);
        EVENT_ICON_MAP.put("Marketing", R.drawable.ic_event_marketing);
        EVENT_ICON_MAP.put("Network Design", R.drawable.ic_event_network);
        EVENT_ICON_MAP.put("Sports & Entertainment Management", R.drawable.ic_event_sports);
        EVENT_ICON_MAP.put("Parliamentary Procedure", R.drawable.ic_event_parliamentary);
        EVENT_ICON_MAP.put("Customer Service", R.drawable.ic_event_customer_service);
        EVENT_ICON_MAP.put("Technology Support & Services", R.drawable.ic_event_tech_support);

        // Objective Test events
        EVENT_ICON_MAP.put("Accounting", R.drawable.ic_event_accounting);
        EVENT_ICON_MAP.put("Advanced Accounting", R.drawable.ic_event_adv_accounting);
        EVENT_ICON_MAP.put("Advertising", R.drawable.ic_event_advertising);
        EVENT_ICON_MAP.put("Agribusiness", R.drawable.ic_event_agribusiness);
        EVENT_ICON_MAP.put("Business Communication", R.drawable.ic_event_bus_communication);
        EVENT_ICON_MAP.put("Business Law", R.drawable.ic_event_business_law);
        EVENT_ICON_MAP.put("Computer Problem Solving", R.drawable.ic_event_comp_problem);
        EVENT_ICON_MAP.put("Cybersecurity", R.drawable.ic_event_cybersecurity);
        EVENT_ICON_MAP.put("Data Science & AI", R.drawable.ic_event_data_science);
        EVENT_ICON_MAP.put("Economics", R.drawable.ic_event_economics);
        EVENT_ICON_MAP.put("Healthcare Administration", R.drawable.ic_event_healthcare);
        EVENT_ICON_MAP.put("Human Resource Management", R.drawable.ic_event_hr);
        EVENT_ICON_MAP.put("Insurance & Risk Management", R.drawable.ic_event_insurance);
        EVENT_ICON_MAP.put("Introduction to Business Communication", R.drawable.ic_event_intro_bus_comm);
        EVENT_ICON_MAP.put("Introduction to Business Concepts", R.drawable.ic_event_intro_bus_concepts);
        EVENT_ICON_MAP.put("Introduction to Business Procedures", R.drawable.ic_event_intro_bus_proc);
        EVENT_ICON_MAP.put("Introduction to FBLA", R.drawable.ic_event_intro_fbla);
        EVENT_ICON_MAP.put("Introduction to Information Technology", R.drawable.ic_event_intro_it);
        EVENT_ICON_MAP.put("Introduction to Marketing Concepts", R.drawable.ic_event_intro_marketing);
        EVENT_ICON_MAP.put("Introduction to Parliamentary Procedure", R.drawable.ic_event_intro_parl_proc);
        EVENT_ICON_MAP.put("Introduction to Retail & Merchandising", R.drawable.ic_event_intro_retail);
        EVENT_ICON_MAP.put("Introduction to Supply Chain Management", R.drawable.ic_event_intro_supply_chain);
        EVENT_ICON_MAP.put("Journalism", R.drawable.ic_event_journalism);
        EVENT_ICON_MAP.put("Networking Infrastructures", R.drawable.ic_event_net_infra);
        EVENT_ICON_MAP.put("Organizational Leadership", R.drawable.ic_event_org_leadership);
        EVENT_ICON_MAP.put("Personal Finance", R.drawable.ic_event_personal_finance);
        EVENT_ICON_MAP.put("Project Management", R.drawable.ic_event_project_mgmt);
        EVENT_ICON_MAP.put("Public Administration & Management", R.drawable.ic_event_public_admin);
        EVENT_ICON_MAP.put("Real Estate", R.drawable.ic_event_real_estate);
        EVENT_ICON_MAP.put("Retail Management", R.drawable.ic_event_retail);
        EVENT_ICON_MAP.put("Securities & Investments", R.drawable.ic_event_securities);

        // Presentation / Performance events (additional)
        EVENT_ICON_MAP.put("Business Ethics", R.drawable.ic_event_business_ethics);
        EVENT_ICON_MAP.put("Future Business Educator", R.drawable.ic_event_future_educator);
        EVENT_ICON_MAP.put("Future Business Leader", R.drawable.ic_event_future_leader);
        EVENT_ICON_MAP.put("Job Interview", R.drawable.ic_event_job_interview);
        EVENT_ICON_MAP.put("Coding & Programming", R.drawable.ic_event_coding);
        EVENT_ICON_MAP.put("Computer Game & Simulation Programming", R.drawable.ic_event_game_sim);

        // Presentation Events (remaining)
        EVENT_ICON_MAP.put("Broadcast Journalism", R.drawable.ic_event_broadcast_journalism);
        EVENT_ICON_MAP.put("Digital Animation", R.drawable.ic_event_digital_animation);
        EVENT_ICON_MAP.put("Digital Video Production", R.drawable.ic_event_digital_video);
        EVENT_ICON_MAP.put("Event Planning", R.drawable.ic_event_event_planning);
        EVENT_ICON_MAP.put("Career Portfolio", R.drawable.ic_event_career_portfolio);
        EVENT_ICON_MAP.put("Sales Presentation", R.drawable.ic_event_sales_presentation);
        EVENT_ICON_MAP.put("Data Analysis", R.drawable.ic_event_data_analysis);
        EVENT_ICON_MAP.put("Financial Planning", R.drawable.ic_event_financial_planning);
        EVENT_ICON_MAP.put("Financial Statement Analysis", R.drawable.ic_event_financial_statement);
        EVENT_ICON_MAP.put("Graphic Design", R.drawable.ic_event_graphic_design);
        EVENT_ICON_MAP.put("Introduction to Business", R.drawable.ic_event_intro_business);
        EVENT_ICON_MAP.put("Introduction to Social Media Strategy", R.drawable.ic_event_intro_social_media);
        EVENT_ICON_MAP.put("Public Service Announcement", R.drawable.ic_event_psa);
        EVENT_ICON_MAP.put("Social Media Strategies", R.drawable.ic_event_social_media);
        EVENT_ICON_MAP.put("Supply Chain Management", R.drawable.ic_event_supply_chain);
        EVENT_ICON_MAP.put("Visual Design", R.drawable.ic_event_visual_design);
        EVENT_ICON_MAP.put("Introduction to Programming", R.drawable.ic_event_intro_programming);
        EVENT_ICON_MAP.put("Mobile Application Development", R.drawable.ic_event_mobile_app);
        EVENT_ICON_MAP.put("Local Chapter Annual Business Report", R.drawable.ic_event_annual_report);
        EVENT_ICON_MAP.put("Website Coding & Development", R.drawable.ic_event_website_coding);
        EVENT_ICON_MAP.put("Website Design", R.drawable.ic_event_website_design);
        EVENT_ICON_MAP.put("Business Plan", R.drawable.ic_event_business_plan);
        EVENT_ICON_MAP.put("Community Service Project", R.drawable.ic_event_community_service);
        EVENT_ICON_MAP.put("Computer Applications", R.drawable.ic_event_computer_apps);
        EVENT_ICON_MAP.put("Impromptu Speaking", R.drawable.ic_event_impromptu_speaking);
        EVENT_ICON_MAP.put("Introduction to Public Speaking", R.drawable.ic_event_intro_public_speaking);
        EVENT_ICON_MAP.put("Public Speaking", R.drawable.ic_event_public_speaking);
    }

    // FBLA 2026 Objective Test Events (31 events)
    private static final String[] OBJECTIVE_TEST_EVENTS = {
            "Accounting",
            "Advanced Accounting",
            "Advertising",
            "Agribusiness",
            "Business Communication",
            "Business Law",
            "Computer Problem Solving",
            "Cybersecurity",
            "Data Science & AI",
            "Economics",
            "Healthcare Administration",
            "Human Resource Management",
            "Insurance & Risk Management",
            "Introduction to Business Communication",
            "Introduction to Business Concepts",
            "Introduction to Business Procedures",
            "Introduction to FBLA",
            "Introduction to Information Technology",
            "Introduction to Marketing Concepts",
            "Introduction to Parliamentary Procedure",
            "Introduction to Retail & Merchandising",
            "Introduction to Supply Chain Management",
            "Journalism",
            "Networking Infrastructures",
            "Organizational Leadership",
            "Personal Finance",
            "Project Management",
            "Public Administration & Management",
            "Real Estate",
            "Retail Management",
            "Securities & Investments"
    };

    // FBLA 2026 Roleplay Events — Test + Case Study + Roleplay (12 events)
    private static final String[] ROLEPLAY_EVENTS = {
            "Banking & Financial Systems",
            "Business Management",
            "Customer Service",
            "Entrepreneurship",
            "Hospitality & Event Management",
            "International Business",
            "Management Information Systems",
            "Marketing",
            "Network Design",
            "Parliamentary Procedure",
            "Sports & Entertainment Management",
            "Technology Support & Services"
    };

    // FBLA 2026 Presentation / Performance Events (33 events)
    private static final String[] PRESENTATION_EVENTS = {
            // Objective Test + Presentation
            "Business Ethics",
            // Pre Judged & Presentation
            "Future Business Educator",
            "Future Business Leader",
            "Job Interview",
            "Digital Animation",
            "Digital Video Production",
            "Business Plan",
            "Community Service Project",
            "Local Chapter Annual Business Report",
            // Presentation
            "Broadcast Journalism",
            "Career Portfolio",
            "Data Analysis",
            "Event Planning",
            "Financial Planning",
            "Financial Statement Analysis",
            "Graphic Design",
            "Introduction to Business",
            "Introduction to Social Media Strategy",
            "Public Service Announcement",
            "Sales Presentation",
            "Social Media Strategies",
            "Supply Chain Management",
            "Visual Design",
            // Presentation / Demonstration
            "Coding & Programming",
            "Computer Game & Simulation Programming",
            "Introduction to Programming",
            "Mobile Application Development",
            "Website Coding & Development",
            "Website Design",
            // Production Test
            "Computer Applications",
            // Speech
            "Impromptu Speaking",
            "Introduction to Public Speaking",
            "Public Speaking"
    };

    // Guideline URLs for all events
    private static final Map<String, String> EVENT_GUIDELINE_URLS = new HashMap<>();
    static {
        // ===== Objective Test Events =====
        EVENT_GUIDELINE_URLS.put("Accounting", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Accounting.pdf");
        EVENT_GUIDELINE_URLS.put("Advanced Accounting", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Advanced-Accounting.pdf");
        EVENT_GUIDELINE_URLS.put("Advertising", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Advertising.pdf");
        EVENT_GUIDELINE_URLS.put("Agribusiness", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Agribusiness.pdf");
        EVENT_GUIDELINE_URLS.put("Business Communication", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Business-Communication.pdf");
        EVENT_GUIDELINE_URLS.put("Business Law", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Business-Law.pdf");
        EVENT_GUIDELINE_URLS.put("Computer Problem Solving", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Computer-Problem-Solving.pdf");
        EVENT_GUIDELINE_URLS.put("Cybersecurity", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Cybersecurity.pdf");
        EVENT_GUIDELINE_URLS.put("Data Science & AI", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Data-Science-and-AI.pdf");
        EVENT_GUIDELINE_URLS.put("Economics", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Economics.pdf");
        EVENT_GUIDELINE_URLS.put("Healthcare Administration", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Healthcare-Administration.pdf");
        EVENT_GUIDELINE_URLS.put("Human Resource Management", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Human-Resource-Management.pdf");
        EVENT_GUIDELINE_URLS.put("Insurance & Risk Management", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Insurance-and-Risk-Management.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Business Communication", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Introduction-to-Business-Communication.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Business Concepts", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Introduction-to-Business-Concepts.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Business Procedures", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Introduction-to-Business-Procedures.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to FBLA", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Introduction-to-FBLA.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Information Technology", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Introduction-to-Information-Technology.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Marketing Concepts", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Introduction-to-Marketing-Concepts.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Parliamentary Procedure", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Introduction-to-Parliamentary-Procedure.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Retail & Merchandising", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Introduction-to-Retail-and-Merchandising.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Supply Chain Management", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Introduction-to-Supply-Chain-Management.pdf");
        EVENT_GUIDELINE_URLS.put("Journalism", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Journalism.pdf");
        EVENT_GUIDELINE_URLS.put("Networking Infrastructures", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Networking-Infrastructures.pdf");
        EVENT_GUIDELINE_URLS.put("Organizational Leadership", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Organizational-Leadership.pdf");
        EVENT_GUIDELINE_URLS.put("Personal Finance", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Personal-Finance.pdf");
        EVENT_GUIDELINE_URLS.put("Project Management", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Project-Management.pdf");
        EVENT_GUIDELINE_URLS.put("Public Administration & Management", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Public-Administration-and-Management.pdf");
        EVENT_GUIDELINE_URLS.put("Real Estate", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Real-Estate.pdf");
        EVENT_GUIDELINE_URLS.put("Retail Management", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Retail-Management.pdf");
        EVENT_GUIDELINE_URLS.put("Securities & Investments", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Objective%20Tests/Securities-and-Investments.pdf");

        // ===== Presentation / Role Play / Performance Events =====
        EVENT_GUIDELINE_URLS.put("Banking & Financial Systems", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/Banking-and-Financial-Systems.pdf");
        EVENT_GUIDELINE_URLS.put("Business Management", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/Business-Management.pdf");
        EVENT_GUIDELINE_URLS.put("Entrepreneurship", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/Entrepreneurship.pdf");
        EVENT_GUIDELINE_URLS.put("Hospitality & Event Management", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/Hospitality-and-Event-Management.pdf");
        EVENT_GUIDELINE_URLS.put("International Business", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/International-Business.pdf");
        EVENT_GUIDELINE_URLS.put("Management Information Systems", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/Management-Information-Systems.pdf");
        EVENT_GUIDELINE_URLS.put("Marketing", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/Marketing.pdf");
        EVENT_GUIDELINE_URLS.put("Network Design", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/Network-Design.pdf");
        EVENT_GUIDELINE_URLS.put("Sports & Entertainment Management", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/Sports-and-Entertainment-Management.pdf");
        EVENT_GUIDELINE_URLS.put("Parliamentary Procedure", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/Parliamentary-Procedure.pdf");
        EVENT_GUIDELINE_URLS.put("Customer Service", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/Customer-Service.pdf");
        EVENT_GUIDELINE_URLS.put("Technology Support & Services", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Role%20Play%20Events/Technology-Support-and-Services.pdf");
        EVENT_GUIDELINE_URLS.put("Business Ethics", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Business-Ethics.pdf");
        EVENT_GUIDELINE_URLS.put("Future Business Educator", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Future-Business-Educator.pdf");
        EVENT_GUIDELINE_URLS.put("Future Business Leader", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Future-Business-Leader.pdf");
        EVENT_GUIDELINE_URLS.put("Job Interview", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Job-Interview.pdf");
        EVENT_GUIDELINE_URLS.put("Digital Animation", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Digital-Animation.pdf");
        EVENT_GUIDELINE_URLS.put("Digital Video Production", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Digital-Video-Production.pdf");
        EVENT_GUIDELINE_URLS.put("Business Plan", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Business-Plan.pdf");
        EVENT_GUIDELINE_URLS.put("Community Service Project", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Chapter%20Events/Community-Service-Project.pdf");
        EVENT_GUIDELINE_URLS.put("Local Chapter Annual Business Report", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Chapter%20Events/Local-Chapter-Annual-Business-Report.pdf");
        EVENT_GUIDELINE_URLS.put("Broadcast Journalism", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Broadcast-Journalism.pdf");
        EVENT_GUIDELINE_URLS.put("Career Portfolio", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Career-Portfolio.pdf");
        EVENT_GUIDELINE_URLS.put("Data Analysis", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Data-Analysis.pdf");
        EVENT_GUIDELINE_URLS.put("Event Planning", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Event-Planning.pdf");
        EVENT_GUIDELINE_URLS.put("Financial Planning", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Financial-Planning.pdf");
        EVENT_GUIDELINE_URLS.put("Financial Statement Analysis", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Financial-Statement-Analysis.pdf");
        EVENT_GUIDELINE_URLS.put("Graphic Design", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Graphic-Design.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Business", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Introduction-to-Business-Presentation.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Social Media Strategy", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Introduction-to-Social-Media-Strategy.pdf");
        EVENT_GUIDELINE_URLS.put("Public Service Announcement", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Public-Service-Announcement.pdf");
        EVENT_GUIDELINE_URLS.put("Sales Presentation", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Sales-Presentation.pdf");
        EVENT_GUIDELINE_URLS.put("Social Media Strategies", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Social-Media-Strategies.pdf");
        EVENT_GUIDELINE_URLS.put("Supply Chain Management", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Supply-Chain-Management.pdf");
        EVENT_GUIDELINE_URLS.put("Visual Design", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Visual-Design.pdf");
        EVENT_GUIDELINE_URLS.put("Coding & Programming", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Coding-and-Programming.pdf");
        EVENT_GUIDELINE_URLS.put("Computer Game & Simulation Programming", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Computer-Game-and-Simulation-Programming.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Programming", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Introduction-to-Programming.pdf");
        EVENT_GUIDELINE_URLS.put("Mobile Application Development", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Mobile-Application-Development.pdf");
        EVENT_GUIDELINE_URLS.put("Website Coding & Development", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Website-Coding-and-Development.pdf");
        EVENT_GUIDELINE_URLS.put("Website Design", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Website-Design.pdf");
        EVENT_GUIDELINE_URLS.put("Computer Applications", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Production%20Events/Computer-Applications.pdf");
        EVENT_GUIDELINE_URLS.put("Impromptu Speaking", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Impromptu-Speaking.pdf");
        EVENT_GUIDELINE_URLS.put("Introduction to Public Speaking", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Introduction-to-Public-Speaking.pdf");
        EVENT_GUIDELINE_URLS.put("Public Speaking", "https://connect.fbla.org/headquarters/files/High%20School%20Competitive%20Events%20Resources/Individual%20Guidelines/Presentation%20Events/Public-Speaking.pdf");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase safely
        try {
            firestore = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        authRepository = AuthRepository.getInstance(requireContext());

        // Setup file picker
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedFileUri = uri;
                            selectedFileName = getFileName(uri);
                            updateFileSelectionUI();
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_resources, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupUserRole();
        setupRecyclerView();
        setupEventTileGrid();
        setupPresentationTileGrid();
        setupRoleplayTileGrid();
        setupCategoryCards();
        setupClickListeners();
    }

    private void initViews(View view) {
        // Category cards view
        layoutCategoryCards = view.findViewById(R.id.layoutCategoryCards);
        cardObjectiveTesting = view.findViewById(R.id.cardObjectiveTesting);
        cardPresentationEvents = view.findViewById(R.id.cardPresentationEvents);
        cardRoleplayEvents = view.findViewById(R.id.cardRoleplayEvents);
        cardBAA = view.findViewById(R.id.cardBAA);

        // Objective testing grid
        layoutObjectiveGrid = view.findViewById(R.id.layoutObjectiveGrid);
        btnBackGrid = view.findViewById(R.id.btnBackGrid);
        recyclerEventTiles = view.findViewById(R.id.recyclerEventTiles);

        // Presentation events grid
        layoutPresentationGrid = view.findViewById(R.id.layoutPresentationGrid);
        btnBackPresentationGrid = view.findViewById(R.id.btnBackPresentationGrid);
        recyclerPresentationTiles = view.findViewById(R.id.recyclerPresentationTiles);

        // Roleplay events grid
        layoutRoleplayGrid = view.findViewById(R.id.layoutRoleplayGrid);
        btnBackRoleplayGrid = view.findViewById(R.id.btnBackRoleplayGrid);
        recyclerRoleplayTiles = view.findViewById(R.id.recyclerRoleplayTiles);

        // Event detail view (two tabs)
        layoutEventDetail = view.findViewById(R.id.layoutEventDetail);
        btnBackEventDetail = view.findViewById(R.id.btnBackEventDetail);
        textEventDetailName = view.findViewById(R.id.textEventDetailName);
        tabEventOverview = view.findViewById(R.id.tabEventOverview);
        tabStudyMaterials = view.findViewById(R.id.tabStudyMaterials);
        contentEventOverview = view.findViewById(R.id.contentEventOverview);
        contentStudyMaterials = view.findViewById(R.id.contentStudyMaterials);
        btnOpenGuidelines = view.findViewById(R.id.btnOpenGuidelines);

        // BAA info view
        layoutBAAInfo = view.findViewById(R.id.layoutBAAInfo);
        btnBackBAA = view.findViewById(R.id.btnBackBAA);
        btnOpenFBLAConnect = view.findViewById(R.id.btnOpenFBLAConnect);
        btnViewBAAResources = view.findViewById(R.id.btnViewBAAResources);

        // Resource list view
        layoutResourceList = view.findViewById(R.id.layoutResourceList);
        btnBack = view.findViewById(R.id.btnBack);
        textCurrentCategory = view.findViewById(R.id.textCurrentCategory);
        recyclerResources = view.findViewById(R.id.recyclerResources);
        progressLoading = view.findViewById(R.id.progressLoading);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutError = view.findViewById(R.id.layoutError);
        textEmptyHint = view.findViewById(R.id.textEmptyHint);
        textErrorMessage = view.findViewById(R.id.textErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);
        fabUpload = view.findViewById(R.id.fabUpload);
        badgeOfficer = view.findViewById(R.id.badgeOfficer);
    }

    private void setupUserRole() {
        UserEntity currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            isOfficer = currentUser.isOfficer();
            currentUserName = currentUser.getName();

            if (isOfficer) {
                fabUpload.setVisibility(View.VISIBLE);
                badgeOfficer.setVisibility(View.VISIBLE);
                textEmptyHint.setText(R.string.empty_hint_officer);
            } else {
                fabUpload.setVisibility(View.GONE);
                badgeOfficer.setVisibility(View.GONE);
                textEmptyHint.setText(R.string.empty_hint_member);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new ResourcesAdapter();
        recyclerResources.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerResources.setAdapter(adapter);
    }

    private void setupEventTileGrid() {
        eventTileAdapter = new EventTileAdapter(OBJECTIVE_TEST_EVENTS, EVENT_GUIDELINE_URLS, "objective");
        recyclerEventTiles.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerEventTiles.setAdapter(eventTileAdapter);
    }

    private void setupPresentationTileGrid() {
        presentationTileAdapter = new EventTileAdapter(PRESENTATION_EVENTS, EVENT_GUIDELINE_URLS, "presentation");
        recyclerPresentationTiles.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerPresentationTiles.setAdapter(presentationTileAdapter);
    }

    private void setupRoleplayTileGrid() {
        roleplayTileAdapter = new EventTileAdapter(ROLEPLAY_EVENTS, EVENT_GUIDELINE_URLS, "roleplay");
        recyclerRoleplayTiles.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerRoleplayTiles.setAdapter(roleplayTileAdapter);
    }

    private void setupCategoryCards() {
        // Objective Testing → show 2-column grid
        cardObjectiveTesting.setOnClickListener(v -> openObjectiveTestingGrid());

        // Presentation Events → show 2-column grid
        cardPresentationEvents.setOnClickListener(v -> openPresentationEventsGrid());

        // Roleplay Events → show 2-column grid
        cardRoleplayEvents.setOnClickListener(v -> openRoleplayEventsGrid());

        // BAA → show BAA info page
        cardBAA.setOnClickListener(v -> openBAAInfo());
    }

    private void hideAllLayouts() {
        layoutCategoryCards.setVisibility(View.GONE);
        layoutObjectiveGrid.setVisibility(View.GONE);
        layoutPresentationGrid.setVisibility(View.GONE);
        layoutRoleplayGrid.setVisibility(View.GONE);
        layoutEventDetail.setVisibility(View.GONE);
        layoutBAAInfo.setVisibility(View.GONE);
        layoutResourceList.setVisibility(View.GONE);
    }

    private void openObjectiveTestingGrid() {
        hideAllLayouts();
        layoutObjectiveGrid.setVisibility(View.VISIBLE);
    }

    private void openPresentationEventsGrid() {
        hideAllLayouts();
        layoutPresentationGrid.setVisibility(View.VISIBLE);
    }

    private void openRoleplayEventsGrid() {
        hideAllLayouts();
        layoutRoleplayGrid.setVisibility(View.VISIBLE);
    }

    private void openBAAInfo() {
        hideAllLayouts();
        layoutBAAInfo.setVisibility(View.VISIBLE);
    }

    private void openCategory(String category) {
        currentCategory = category;
        textCurrentCategory.setText(category);

        hideAllLayouts();
        layoutResourceList.setVisibility(View.VISIBLE);

        // Load resources for this category
        loadResources();
    }

    private void showCategoryCards() {
        currentCategory = null;
        hideAllLayouts();
        layoutCategoryCards.setVisibility(View.VISIBLE);
    }

    private void setupClickListeners() {
        btnRetry.setOnClickListener(v -> loadResources());
        fabUpload.setOnClickListener(v -> showUploadDialog());
        btnBack.setOnClickListener(v -> {
            if ("Business Achievement Awards".equals(currentCategory)) {
                openBAAInfo(); // Go back to BAA info page
            } else {
                showCategoryCards();
            }
        });
        btnBackGrid.setOnClickListener(v -> showCategoryCards());
        btnBackPresentationGrid.setOnClickListener(v -> showCategoryCards());
        btnBackRoleplayGrid.setOnClickListener(v -> showCategoryCards());

        // BAA info back button
        btnBackBAA.setOnClickListener(v -> showCategoryCards());

        // Open FBLA Connect button
        btnOpenFBLAConnect.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://connect.fbla.org"));
            startActivity(intent);
        });

        // View BAA Resources button → opens the resource list filtered to BAA
        btnViewBAAResources.setOnClickListener(v -> openCategory("Business Achievement Awards"));

        // Event detail back button → go back to whichever grid we came from
        btnBackEventDetail.setOnClickListener(v -> {
            if ("presentation".equals(previousGrid)) {
                openPresentationEventsGrid();
            } else if ("roleplay".equals(previousGrid)) {
                openRoleplayEventsGrid();
            } else {
                openObjectiveTestingGrid();
            }
        });

        // Event Overview tab
        tabEventOverview.setOnClickListener(v -> selectEventOverviewTab());

        // Study Materials tab
        tabStudyMaterials.setOnClickListener(v -> selectStudyMaterialsTab());

        // Open Guidelines PDF button
        btnOpenGuidelines.setOnClickListener(v -> {
            if (currentEventGuidelineUrl != null && !currentEventGuidelineUrl.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentEventGuidelineUrl));
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Guidelines not available yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== Event Detail View ====================

    private void openEventDetail(String eventName, String guidelineUrl, String fromGrid) {
        currentEventName = eventName;
        currentEventGuidelineUrl = guidelineUrl;
        previousGrid = fromGrid;

        textEventDetailName.setText(eventName);

        // Default to Event Overview tab
        selectEventOverviewTab();

        // Hide everything else, show event detail
        hideAllLayouts();
        layoutEventDetail.setVisibility(View.VISIBLE);
    }

    private void selectEventOverviewTab() {
        // Highlight Event Overview tab
        tabEventOverview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(R.color.navy, null)));
        tabEventOverview.setTextColor(getResources().getColor(R.color.white, null));

        // Dim Study Materials tab
        tabStudyMaterials.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(R.color.divider, null)));
        tabStudyMaterials.setTextColor(getResources().getColor(R.color.text_secondary, null));

        // Show/hide content
        contentEventOverview.setVisibility(View.VISIBLE);
        contentStudyMaterials.setVisibility(View.GONE);
    }

    private void selectStudyMaterialsTab() {
        // Dim Event Overview tab
        tabEventOverview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(R.color.divider, null)));
        tabEventOverview.setTextColor(getResources().getColor(R.color.text_secondary, null));

        // Highlight Study Materials tab
        tabStudyMaterials.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(R.color.navy, null)));
        tabStudyMaterials.setTextColor(getResources().getColor(R.color.white, null));

        // Show/hide content
        contentEventOverview.setVisibility(View.GONE);
        contentStudyMaterials.setVisibility(View.VISIBLE);
    }

    private void loadResources() {
        if (firestore == null) {
            showError("Firebase is not configured. Please check your setup.");
            return;
        }

        showLoading();

        try {
            firestore.collection(COLLECTION_RESOURCES)
                    .orderBy("uploadedAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        allResources.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            FirestoreResource resource = document.toObject(FirestoreResource.class);
                            allResources.add(resource);
                        }
                        filterResources();
                    })
                    .addOnFailureListener(e -> {
                        showError(e.getMessage());
                    });
        } catch (Exception e) {
            showError("Error loading resources: " + e.getMessage());
        }
    }

    private void filterResources() {
        List<FirestoreResource> filtered = new ArrayList<>();

        if (currentCategory == null) {
            filtered.addAll(allResources);
        } else {
            for (FirestoreResource resource : allResources) {
                if (currentCategory.equalsIgnoreCase(resource.getCategory())) {
                    filtered.add(resource);
                }
            }
        }

        adapter.setResources(filtered);

        if (filtered.isEmpty()) {
            showEmpty();
        } else {
            showContent();
        }
    }

    private void showLoading() {
        progressLoading.setVisibility(View.VISIBLE);
        recyclerResources.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showContent() {
        progressLoading.setVisibility(View.GONE);
        recyclerResources.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }

    private void showEmpty() {
        progressLoading.setVisibility(View.GONE);
        recyclerResources.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressLoading.setVisibility(View.GONE);
        recyclerResources.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        textErrorMessage.setText(message != null ? message : "Unknown error occurred");
    }

    // ==================== Upload Dialog ====================

    private void showUploadDialog() {
        if (!isOfficer) {
            Toast.makeText(getContext(), "Only officers and teachers can upload resources", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firestore == null || storage == null) {
            Toast.makeText(getContext(), "Firebase is not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_upload_resource);
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        currentUploadDialog = dialog;

        // Get views
        TextInputEditText editTitle = dialog.findViewById(R.id.editTitle);
        TextInputEditText editDescription = dialog.findViewById(R.id.editDescription);
        AutoCompleteTextView spinnerCategory = dialog.findViewById(R.id.spinnerCategory);
        Button btnSelectFile = dialog.findViewById(R.id.btnSelectFile);
        Button btnAddLink = dialog.findViewById(R.id.btnAddLink);
        LinearLayout layoutSelectedFile = dialog.findViewById(R.id.layoutSelectedFile);
        TextView textSelectedFileName = dialog.findViewById(R.id.textSelectedFileName);
        ImageButton btnClearFile = dialog.findViewById(R.id.btnClearFile);
        TextInputLayout layoutLinkInput = dialog.findViewById(R.id.layoutLinkInput);
        TextInputEditText editLink = dialog.findViewById(R.id.editLink);
        LinearLayout layoutUploadProgress = dialog.findViewById(R.id.layoutUploadProgress);
        ProgressBar progressUpload = dialog.findViewById(R.id.progressUpload);
        TextView textUploadStatus = dialog.findViewById(R.id.textUploadStatus);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnUpload = dialog.findViewById(R.id.btnUpload);

        // Setup category dropdown
        String[] categories = {"Objective Testing Resources", "Presentation Event Resources", "Business Achievement Awards"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        spinnerCategory.setAdapter(categoryAdapter);

        // Pre-fill category if we're inside a category view
        if (currentCategory != null) {
            spinnerCategory.setText(currentCategory, false);
        }

        // Reset file selection
        selectedFileUri = null;
        selectedFileName = null;

        // File selection button
        btnSelectFile.setOnClickListener(v -> {
            layoutLinkInput.setVisibility(View.GONE);
            openFilePicker();
        });

        // Add link button
        btnAddLink.setOnClickListener(v -> {
            layoutSelectedFile.setVisibility(View.GONE);
            layoutLinkInput.setVisibility(View.VISIBLE);
            selectedFileUri = null;
            selectedFileName = null;
        });

        // Clear file selection
        btnClearFile.setOnClickListener(v -> {
            selectedFileUri = null;
            selectedFileName = null;
            layoutSelectedFile.setVisibility(View.GONE);
        });

        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Upload button
        btnUpload.setOnClickListener(v -> {
            String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
            String description = editDescription.getText() != null ? editDescription.getText().toString().trim() : "";
            String category = spinnerCategory.getText() != null ? spinnerCategory.getText().toString().trim() : "";
            String link = editLink.getText() != null ? editLink.getText().toString().trim() : "";

            // Validation
            if (title.isEmpty()) {
                editTitle.setError("Title is required");
                editTitle.requestFocus();
                return;
            }

            if (description.isEmpty()) {
                editDescription.setError("Description is required");
                editDescription.requestFocus();
                return;
            }

            if (category.isEmpty()) {
                spinnerCategory.setError("Category is required");
                spinnerCategory.requestFocus();
                return;
            }

            boolean hasFile = selectedFileUri != null;
            boolean hasLink = !link.isEmpty();

            if (!hasFile && !hasLink) {
                Toast.makeText(getContext(), "Please select a file or add a link", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress
            layoutUploadProgress.setVisibility(View.VISIBLE);
            btnUpload.setEnabled(false);
            btnCancel.setEnabled(false);

            if (hasFile) {
                uploadFileAndSaveResource(title, description, category,
                        progressUpload, textUploadStatus, dialog);
            } else {
                saveResourceToFirestore(title, description, category, link, "link", dialog);
            }
        });

        dialog.show();
    }

    private void updateFileSelectionUI() {
        if (currentUploadDialog != null && selectedFileName != null) {
            LinearLayout layoutSelectedFile = currentUploadDialog.findViewById(R.id.layoutSelectedFile);
            TextView textSelectedFileName = currentUploadDialog.findViewById(R.id.textSelectedFileName);
            TextInputLayout layoutLinkInput = currentUploadDialog.findViewById(R.id.layoutLinkInput);

            layoutSelectedFile.setVisibility(View.VISIBLE);
            textSelectedFileName.setText(selectedFileName);
            layoutLinkInput.setVisibility(View.GONE);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private void uploadFileAndSaveResource(String title, String description, String category,
                                           ProgressBar progressUpload, TextView textUploadStatus,
                                           Dialog dialog) {
        if (selectedFileUri == null) return;

        String fileExtension = getFileExtension(selectedFileName);
        String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;
        StorageReference storageRef = storage.getReference().child(STORAGE_PATH + uniqueFileName);

        UploadTask uploadTask = storageRef.putFile(selectedFileUri);

        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            progressUpload.setProgress((int) progress);
            textUploadStatus.setText(getString(R.string.uploading_progress, (int) progress));
        }).addOnSuccessListener(taskSnapshot -> {
            textUploadStatus.setText(R.string.getting_download_url);
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                saveResourceToFirestore(title, description, category, uri.toString(), fileExtension, dialog);
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                resetUploadDialog(dialog);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            resetUploadDialog(dialog);
        });
    }

    private void saveResourceToFirestore(String title, String description, String category,
                                         String fileUrl, String fileType, Dialog dialog) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("title", title);
        resource.put("description", description);
        resource.put("category", category);
        resource.put("fileUrl", fileUrl);
        resource.put("fileType", fileType);
        resource.put("uploadedBy", currentUserName);
        resource.put("uploadedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_RESOURCES)
                .add(resource)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Resource uploaded successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadResources();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save resource: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetUploadDialog(dialog);
                });
    }

    private void resetUploadDialog(Dialog dialog) {
        LinearLayout layoutUploadProgress = dialog.findViewById(R.id.layoutUploadProgress);
        Button btnUpload = dialog.findViewById(R.id.btnUpload);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        layoutUploadProgress.setVisibility(View.GONE);
        btnUpload.setEnabled(true);
        btnCancel.setEnabled(true);
    }

    // ==================== Event Tile Adapter (2-column grid) ====================

    private class EventTileAdapter extends RecyclerView.Adapter<EventTileAdapter.TileViewHolder> {

        private final String[] eventNames;
        private final Map<String, String> guidelineUrls;
        private final String gridType; // "objective" or "presentation"

        EventTileAdapter(String[] eventNames, Map<String, String> guidelineUrls, String gridType) {
            this.eventNames = eventNames;
            this.guidelineUrls = guidelineUrls;
            this.gridType = gridType;
        }

        @NonNull
        @Override
        public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event_tile, parent, false);
            return new TileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
            String eventName = eventNames[position];
            holder.textEventName.setText(eventName);

            // Check if this event has a custom icon
            Integer iconRes = EVENT_ICON_MAP.get(eventName);
            if (iconRes != null) {
                // Show custom event icon - larger and fully opaque
                holder.imgPlaceholderIcon.setVisibility(View.VISIBLE);
                holder.imgPlaceholderIcon.setImageResource(iconRes);
                holder.imgPlaceholderIcon.setAlpha(1.0f);
                holder.imgPlaceholderIcon.getLayoutParams().width =
                        (int) (56 * holder.itemView.getResources().getDisplayMetrics().density);
                holder.imgPlaceholderIcon.getLayoutParams().height =
                        (int) (56 * holder.itemView.getResources().getDisplayMetrics().density);
                holder.imgPlaceholderIcon.setColorFilter(null);
                holder.imgEventTile.setImageDrawable(null);
            } else {
                // Default placeholder icon for events without custom icons
                holder.imgPlaceholderIcon.setVisibility(View.VISIBLE);
                holder.imgPlaceholderIcon.setImageResource(android.R.drawable.ic_menu_agenda);
                holder.imgPlaceholderIcon.setAlpha(0.25f);
                holder.imgPlaceholderIcon.getLayoutParams().width =
                        (int) (40 * holder.itemView.getResources().getDisplayMetrics().density);
                holder.imgPlaceholderIcon.getLayoutParams().height =
                        (int) (40 * holder.itemView.getResources().getDisplayMetrics().density);
                holder.imgEventTile.setImageDrawable(null);
            }

            // Click listener — opens event detail view with two tabs
            holder.itemView.setOnClickListener(v -> {
                String url = guidelineUrls.get(eventName);
                openEventDetail(eventName, url, gridType);
            });
        }

        @Override
        public int getItemCount() {
            return eventNames.length;
        }

        class TileViewHolder extends RecyclerView.ViewHolder {
            TextView textEventName;
            ImageView imgEventTile;
            ImageView imgPlaceholderIcon;

            TileViewHolder(View itemView) {
                super(itemView);
                textEventName = itemView.findViewById(R.id.textEventName);
                imgEventTile = itemView.findViewById(R.id.imgEventTile);
                imgPlaceholderIcon = itemView.findViewById(R.id.imgPlaceholderIcon);
            }
        }
    }

    // ==================== Resources Adapter (Firestore list) ====================

    private class ResourcesAdapter extends RecyclerView.Adapter<ResourcesAdapter.ViewHolder> {

        private List<FirestoreResource> resources = new ArrayList<>();

        public void setResources(List<FirestoreResource> resources) {
            this.resources = resources;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_firestore_resource, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FirestoreResource resource = resources.get(position);

            holder.textFileIcon.setText(resource.getFileTypeIcon());
            holder.textResourceTitle.setText(resource.getTitle());
            holder.textResourceDescription.setText(resource.getDescription());
            holder.textResourceCategory.setText(resource.getCategory() != null ? resource.getCategory() : "Other");
            holder.textFileType.setText(resource.getFileType() != null ? resource.getFileType().toUpperCase() : "FILE");

            // Download/Open button
            holder.btnDownload.setOnClickListener(v -> {
                String url = resource.getFileUrl();
                if (url != null && !url.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "No file URL available", Toast.LENGTH_SHORT).show();
                }
            });

            // Share button
            holder.btnShare.setOnClickListener(v -> {
                String url = resource.getFileUrl();
                if (url != null && !url.isEmpty()) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, resource.getTitle());
                    shareIntent.putExtra(Intent.EXTRA_TEXT,
                            resource.getTitle() + "\n\n" + resource.getDescription() + "\n\n" + url);
                    startActivity(Intent.createChooser(shareIntent, "Share Resource"));
                } else {
                    Toast.makeText(getContext(), "No file URL available to share", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return resources.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textFileIcon, textResourceTitle, textResourceDescription;
            TextView textResourceCategory, textFileType;
            Button btnDownload;
            ImageButton btnShare;

            ViewHolder(View itemView) {
                super(itemView);
                textFileIcon = itemView.findViewById(R.id.textFileIcon);
                textResourceTitle = itemView.findViewById(R.id.textResourceTitle);
                textResourceDescription = itemView.findViewById(R.id.textResourceDescription);
                textResourceCategory = itemView.findViewById(R.id.textResourceCategory);
                textFileType = itemView.findViewById(R.id.textFileType);
                btnDownload = itemView.findViewById(R.id.btnDownload);
                btnShare = itemView.findViewById(R.id.btnShare);
            }
        }
    }
}
