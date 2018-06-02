package com.example.zufpilosof.assignment3;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.zufpilosof.assignment3.adapter.ReviewsAdapter;
import com.example.zufpilosof.assignment3.model.Review;
import com.example.zufpilosof.assignment3.model.ServiceProvider;
import com.example.zufpilosof.assignment3.model.User;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServiceProviderDetailsActivity extends AppCompatActivity implements BillingManager.BillingUpdatesListener{

    public final String TAG = "ServiceProviderDetailsActivity";
    private ServiceProvider mServiceProvider;
    private String mKey;
    private User mUser;
    private AnalyticsManager analyticsManager = AnalyticsManager.getInstance();

    private FloatingActionButton mWriteReview;
    private Button mOrderService;

    private RecyclerView mRecyclerViewServiceProviderReviews;

    private DatabaseReference mServiceProviderReviewsRef;

    private List<Review> mReviewsList =  new ArrayList<>();

    private boolean mServiceProviderWasPurchased;

    private  TextView mPhoneTextView;
    private BillingManager mBillingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.e(TAG, "onCreate() >>");
        mBillingManager = new BillingManager(this,this);
        //try {
        //    PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
        //    TextView tvVersion = findViewById(R.id.textViewVersion);
        //
        //    tvVersion.setText(pInfo.versionName + "."+ String.valueOf(pInfo.versionCode));
        //
        //} catch (PackageManager.NameNotFoundException e) {
        //    e.printStackTrace();
        //}
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_provider_details);

        mKey = getIntent().getStringExtra("key");
        mServiceProvider = getIntent().getParcelableExtra("ServiceProvider");
        mServiceProvider.setPhone(getIntent().getStringExtra("phone"));
        mUser = getIntent().getParcelableExtra("user");
        mPhoneTextView = ((TextView) findViewById(R.id.textViewPhone));
        logServiceProviderEvent("serviceProvider_view");

        // Load the image using Glide
        Glide.with(this)
                .load(mServiceProvider.getThumbImage())
                .into((ImageView) findViewById(R.id.imageViewServiceProvider));

        ((TextView) findViewById(R.id.textViewName)).setText(mServiceProvider.getName());
        ((TextView) findViewById(R.id.textViewService)).setText(mServiceProvider.getService());
        ((TextView) findViewById(R.id.textViewLocation)).setText(mServiceProvider.getLocation());
         mPhoneTextView.setText(mServiceProvider.getPhone());
        ((TextView) findViewById(R.id.textViewYearsOfExperience)).setText(mServiceProvider.getYearsOfExperience() + " Years in field");


        mOrderService = ((Button) findViewById(R.id.buttonBuyPlay)); // fix - no need
        //
        mOrderService.setText("ORDER $" + mServiceProvider.getPrice());
        if (mUser != null) {
            Iterator i = mUser.getMyServiceRequests().iterator();

            while (i.hasNext()) {
                if (i.next().equals(mKey)) {
                    mServiceProviderWasPurchased = true;
                    mOrderService.setText("CALL");
                    mPhoneTextView.setVisibility(View.VISIBLE);
                    break;
                }
            }
        }


        mOrderService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( FirebaseAuth.getInstance().getCurrentUser().isAnonymous()) {
                    // In case the user is logged in anonymously, send him to the SignIn screen:
                    Toast.makeText(ServiceProviderDetailsActivity.this, "Please sign in to order a service.", Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, "mOrderService.onClick() >> " + mServiceProvider.getName());
                    if (mServiceProviderWasPurchased) {
                        Log.e(TAG, "mOrderService.onClick() >> Playing purchased mServiceProvider");
                        //User purchased the mServiceProvider so he can play it
                        callServiceProvider();
                    } else {
                            Log.e(TAG, "mOrderService.onClick() >> Purchase the mServiceProvider");
                            mUser.getMyServiceRequests().add(mKey);
                            mUser.updateTotalPurchase(mServiceProvider.getPrice());
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users");
                            userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(mUser);

                            // Initiate the billing flow:
                            String product = mServiceProvider.getName();
                            String sku = BillingClient.SkuType.INAPP;
                            mBillingManager.initiatePurchaseFlow(product,sku);


                            mServiceProviderWasPurchased = true;
                            mPhoneTextView.setVisibility(View.VISIBLE);
                            mOrderService.setText("CALL");

                        analyticsManager.trackPurchase(mServiceProvider);
                        analyticsManager.setUserProperty("last_service",mServiceProvider.getService());
                        analyticsManager.setUserProperty("total_purchase",Integer.toString(mUser.getTotalPurchase()));
                        analyticsManager.setUserProperty("my_serviceProvides_count",Integer.toString(mUser.getMyServiceProvidersCount()));
                    }
                    Log.e(TAG, "callServiceProvider.onClick() <<");
                }
            }
       });


        mWriteReview = (FloatingActionButton) findViewById(R.id.buttonNewReview);

        mWriteReview.setOnClickListener(new View.OnClickListener() {

               @Override
               public void onClick(View view) {
                   Log.e(TAG, "mWriteReview.onClick() >>");

                   if ( FirebaseAuth.getInstance().getCurrentUser().isAnonymous())
                       Toast.makeText(ServiceProviderDetailsActivity.this, "Please sign in to write a review.", Toast.LENGTH_LONG).show();

                   else {

                       Intent intent = new Intent(getApplicationContext(), ReviewActivity.class);
                       intent.putExtra("serviceProvider", mServiceProvider);
                       intent.putExtra("phone", mServiceProvider.getPhone());
                       intent.putExtra("key", mKey);
                       intent.putExtra("user", mUser);

                       startActivity(intent);
                       finish();

                   }
                   Log.e(TAG, "mWriteReview.onClick() <<");
               }
           }
        );

        mRecyclerViewServiceProviderReviews = findViewById(R.id.service_provider_reviews);
        mRecyclerViewServiceProviderReviews.setHasFixedSize(true);
        mRecyclerViewServiceProviderReviews.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mRecyclerViewServiceProviderReviews.setItemAnimator(new DefaultItemAnimator());


        ReviewsAdapter reviewsAdapter = new ReviewsAdapter(mReviewsList);
        mRecyclerViewServiceProviderReviews.setAdapter(reviewsAdapter);

        mServiceProviderReviewsRef = FirebaseDatabase.getInstance().getReference("service_providers/" + mKey +"/reviews");

        mServiceProviderReviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        Log.e(TAG, "onDataChange() >> service_providers/" + mKey);

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Review review = dataSnapshot.getValue(Review.class);
                            mReviewsList.add(review);
                        }
                        mRecyclerViewServiceProviderReviews.getAdapter().notifyDataSetChanged();
                        Log.e(TAG, "onDataChange(Review) <<");

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                        Log.e(TAG, "onCancelled(Review) >>" + databaseError.getMessage());
                    }
                });
        Log.e(TAG, "onCreate() <<");
    }

    private void callServiceProvider() {
        analyticsManager.trackCallEvent("serviceProvider_call", mServiceProvider);
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + mServiceProvider.getPhone()));
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    private void logServiceProviderEvent(String event) {

        analyticsManager.trackServiceProviderEvent(event,mServiceProvider);
    }

    public void onBillingClientSetupFinished() {

        Log.e(TAG,"onBillingSetupFinished() >>");

        Log.e(TAG,"onBillingSetupFinished() <<");

    }

    public void onConsumeFinished(String token, @BillingClient.BillingResponse int result) {

        Log.e(TAG,"onConsumeFinished() >> result:"+result+" ,token:"+token);


        if (result == BillingClient.BillingResponse.OK) {
            displayMessage("Product with token:"+ token+ " was consumed successfully");
        } else {
            displayMessage("Error consuming product with token:" + token + " , error code:"+result);
        }

        Log.e(TAG,"onConsumeFinished() <<");

    }

    public void onPurchasesUpdated(int resultCode,List<Purchase> purchases){

        Log.e(TAG,"onPurchasesUpdated() >> ");

        if (resultCode != BillingClient.BillingResponse.OK) {
            Log.e(TAG,"onPurchasesUpdated() << Error:"+resultCode);
            return;
        }

        for (Purchase purchase : purchases) {
            Log.e(TAG, "onPurchasesUpdated() >> " + purchase.toString());

            displayMessage("onPurchasesUpdated() >> " + purchase.getSku());

            if (purchase.getSku().contains("credit")) {
                Log.e(TAG, "onPurchasesUpdated() >> consuming " + purchase.getSku());
                //Only consume  one time product (subscription can't be consumed).
                mBillingManager.consumeAsync(purchase.getPurchaseToken());
            }
            //Update the server...
        }

        Log.e(TAG,"onPurchasesUpdated() <<");
    }
    public void displayMessage(String msg) {
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();

    }
}