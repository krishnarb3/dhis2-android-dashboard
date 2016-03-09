package org.hisp.dhis.android.dashboard.ui.activities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import org.hisp.dhis.android.dashboard.DhisService;
import org.hisp.dhis.android.dashboard.R;
import org.hisp.dhis.android.dashboard.api.models.UserAccount;
import org.hisp.dhis.android.dashboard.api.models.meta.Session;
import org.hisp.dhis.android.dashboard.api.persistence.loaders.DbLoader;
import org.hisp.dhis.android.dashboard.api.persistence.loaders.Query;
import org.hisp.dhis.android.dashboard.api.persistence.preferences.LastUpdatedManager;
import org.hisp.dhis.android.dashboard.ui.adapters.AccountEditFieldAdapter;
import org.hisp.dhis.android.dashboard.ui.models.Field;
import org.hisp.dhis.android.dashboard.ui.views.GridDividerDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class EditUserProfileActivity extends BaseActivity implements LoaderCallbacks<List<Field>> {

    private static final int LOADER_ID = 66756123;
    private Session mSession;

    @Bind(R.id.recycler_view_edit_profile)
    RecyclerView mRecyclerView;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.fab)
    FloatingActionButton fab;

    AccountEditFieldAdapter mAdapter;
    private LoaderManager.LoaderCallbacks<List<Field>> mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_profile);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        mAdapter = new AccountEditFieldAdapter(this,
                getLayoutInflater());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new GridDividerDecoration(this
                .getApplicationContext()));
        mRecyclerView.setAdapter(mAdapter);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),"Updating...",Toast.LENGTH_SHORT).show();
                if (isDhisServiceBound() && isNetworkAvailable() &&
                        !getDhisService().isJobRunning(DhisService.EDIT_PROFILE)) {
                    mSession = LastUpdatedManager.getInstance().get();
                    sendUserDetails();
                    Toast.makeText(getApplicationContext()
                            , "Successfully Updated", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getApplicationContext(),"Failure",Toast.LENGTH_LONG).show();
                }
            }
        });

        mCallbacks=this;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mCallbacks = this;
        getSupportLoaderManager().initLoader(LOADER_ID,null,mCallbacks);
    }

    public void sendUserDetails() {
        List<Field> fieldList = mAdapter.getData();
        UserAccount userAccount = getUserAccountFromFields(fieldList);
        getDhisService().editProfileDetails(
                mSession.getServerUrl(), mSession.getCredentials(), userAccount);
    }

    public UserAccount getUserAccountFromFields(List<Field> fields) {
        UserAccount userAccount = new UserAccount();
        userAccount.setFirstName(fields.get(0).getValue());
        userAccount.setSurname(fields.get(1).getValue());

        String gender,genderFromField;
        genderFromField = fields.get(2).getValue();
        switch (genderFromField) {
            case "Male":
                gender = "gender_male";
                break;
            case "Female":
                gender = "gender_female";
                break;
            default:
                gender = "gender_other";
                break;
        }
        userAccount.setGender(gender);

        userAccount.setBirthday(fields.get(3).getValue());
        userAccount.setIntroduction(fields.get(4).getValue());
        userAccount.setEducation(fields.get(5).getValue());
        userAccount.setEmployer(fields.get(6).getValue());
        userAccount.setInterests(fields.get(7).getValue());
        userAccount.setJobTitle(fields.get(8).getValue());
        userAccount.setLanguages(fields.get(9).getValue());
        userAccount.setEmail(fields.get(10).getValue());
        userAccount.setPhoneNumber(fields.get(11).getValue());
        return userAccount;
    }
    @Override
    public Loader<List<Field>> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id) {
            List<DbLoader.TrackedTable> trackedTables = new ArrayList<>();
            trackedTables.add(new DbLoader.TrackedTable(UserAccount.class));
            return new DbLoader<>(this.getApplicationContext(),
                    trackedTables, new UserAccountQuery());
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<Field>> loader, List<Field> data) {
        if (loader != null && loader.getId() == LOADER_ID) {
            mAdapter.swapData(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Field>> loader) {
        // stub implementation
    }

    private static class UserAccountQuery implements Query<List<Field>> {

        @Override
        public List<Field> query(Context context) {
            UserAccount userAccount = UserAccount.getCurrentUserAccountFromDb();

            String gender = userAccount.getGender();
            if (gender != null) {
                switch (gender) {
                    case "gender_female": {
                        gender = "Female";
                        break;
                    }
                    case "gender_male": {
                        gender = "Male";
                        break;
                    }
                    case "gender_other": {
                        gender = "Other";
                        break;
                    }
                }
            }
            return Arrays.asList(
                    new Field(getString(context, R.string.first_name), userAccount.getFirstName()),
                    new Field(getString(context, R.string.surname), userAccount.getSurname()),
                    new Field(getString(context, R.string.gender), gender),
                    new Field(getString(context, R.string.birthday), userAccount.getBirthday()),
                    new Field(getString(context, R.string.introduction), userAccount.getIntroduction()),
                    new Field(getString(context, R.string.education), userAccount.getEducation()),
                    new Field(getString(context, R.string.employer), userAccount.getEmployer()),
                    new Field(getString(context, R.string.interests), userAccount.getInterests()),
                    new Field(getString(context, R.string.job_title), userAccount.getJobTitle()),
                    new Field(getString(context, R.string.languages), userAccount.getLanguages()),
                    new Field(getString(context, R.string.email), userAccount.getEmail()),
                    new Field(getString(context, R.string.phone_number), userAccount.getPhoneNumber())
            );
        }

        private static String getString(Context context, int resource) {
            return context.getString(resource);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager) this
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        else {
            return false;
        }
    }
}
